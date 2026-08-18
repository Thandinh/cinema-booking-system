import { spawn } from 'node:child_process';
import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';


const chromePath = 'C:/Program Files/Google/Chrome/Application/chrome.exe';
const baseUrl = 'http://127.0.0.1:5173';
const debugPort = 9333;
const outputDir = path.resolve('docs/thesis_assets/screenshots');
const profileDir = path.resolve(`tmp/thesis-chrome-profile-${Date.now()}`);
const sleep = milliseconds => new Promise(resolve => setTimeout(resolve, milliseconds));


class CdpClient {
  constructor(socket) {
    this.socket = socket;
    this.nextId = 1;
    this.pending = new Map();
    this.events = new Map();
    socket.addEventListener('message', event => {
      const message = JSON.parse(event.data);
      if (message.id) {
        const pending = this.pending.get(message.id);
        if (!pending) return;
        this.pending.delete(message.id);
        if (message.error) pending.reject(new Error(message.error.message));
        else pending.resolve(message.result);
        return;
      }
      const listeners = this.events.get(message.method) ?? [];
      this.events.delete(message.method);
      listeners.forEach(listener => listener(message.params));
    });
  }

  send(method, params = {}) {
    const id = this.nextId++;
    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve, reject });
      this.socket.send(JSON.stringify({ id, method, params }));
    });
  }

  once(method, timeout = 10_000) {
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error(`Timeout waiting for ${method}`)), timeout);
      const listener = params => {
        clearTimeout(timer);
        resolve(params);
      };
      this.events.set(method, [...(this.events.get(method) ?? []), listener]);
    });
  }
}


async function waitForChrome() {
  for (let attempt = 0; attempt < 50; attempt += 1) {
    try {
      const response = await fetch(`http://127.0.0.1:${debugPort}/json/version`);
      if (response.ok) return;
    } catch {
      // Chrome is still starting.
    }
    await sleep(200);
  }
  throw new Error('Chrome DevTools endpoint did not start');
}


async function createPage() {
  const response = await fetch(
    `http://127.0.0.1:${debugPort}/json/new?${encodeURIComponent(baseUrl)}`,
    { method: 'PUT' },
  );
  if (!response.ok) throw new Error(`Cannot create Chrome page: ${response.status}`);
  const target = await response.json();
  const socket = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((resolve, reject) => {
    socket.addEventListener('open', resolve, { once: true });
    socket.addEventListener('error', reject, { once: true });
  });
  return new CdpClient(socket);
}


async function evaluate(client, expression) {
  const response = await client.send('Runtime.evaluate', {
    expression,
    awaitPromise: true,
    returnByValue: true,
  });
  if (response.exceptionDetails) {
    throw new Error(response.exceptionDetails.exception?.description || response.exceptionDetails.text);
  }
  return response.result?.value;
}


async function navigate(client, pathname) {
  const loaded = client.once('Page.loadEventFired').catch(() => null);
  await client.send('Page.navigate', { url: `${baseUrl}${pathname}` });
  await loaded;
  await sleep(1800);
}


async function waitFor(client, expression, timeout = 15_000) {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    if (await evaluate(client, expression)) return;
    await sleep(250);
  }
  throw new Error(`Condition was not met: ${expression}`);
}


async function screenshot(client, figure, { scrollTop = 0 } = {}) {
  await evaluate(client, `window.scrollTo({ top: ${scrollTop}, behavior: 'instant' }); true`);
  await sleep(400);
  const result = await client.send('Page.captureScreenshot', {
    format: 'png',
    fromSurface: true,
    captureBeyondViewport: false,
  });
  await writeFile(path.join(outputDir, `figure_${figure.replace('.', '_')}.png`), Buffer.from(result.data, 'base64'));
}


async function login(client, username, password, targetPath) {
  const credentials = JSON.stringify({ username, password });
  const result = await evaluate(client, `(async () => {
    localStorage.clear();
    const authResponse = await fetch('/auth/token', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: ${JSON.stringify(credentials)}
    });
    const authBody = await authResponse.json();
    const token = authBody?.result?.accessToken || authBody?.result?.token;
    if (!authResponse.ok || !token) return { ok: false, status: authResponse.status, body: authBody };
    const profileResponse = await fetch('/api/v1/users/me', {
      credentials: 'include',
      headers: { Authorization: 'Bearer ' + token }
    });
    const profileBody = await profileResponse.json();
    const user = profileBody.result;
    const permissions = [...new Set((user.roles || []).flatMap(role => (role.permissions || []).map(permission => permission.name)))];
    localStorage.setItem('access_token', token);
    localStorage.setItem('user_info', JSON.stringify({
      id: user.id,
      username: user.username,
      firstName: user.firstName,
      lastName: user.lastName,
      email: user.email,
      avatarUrl: user.avatarUrl,
      emailVerified: user.emailVerified
    }));
    localStorage.setItem('permissions', JSON.stringify(permissions));
    localStorage.setItem('cinema:selectedCity', 'Đà Nẵng');
    localStorage.setItem('cinema:selectedCitySource', 'manual');
    return { ok: true, permissions: permissions.length };
  })()`);
  if (!result?.ok) throw new Error(`Cannot log in as ${username}: ${JSON.stringify(result)}`);
  await navigate(client, targetPath);
}


async function findMovieAndShowtime(client) {
  return evaluate(client, `(async () => {
    const moviesBody = await fetch('/api/v1/movies?page=0&size=100').then(response => response.json());
    const movies = moviesBody?.result?.content || [];
    const preferredMovie = movies.find(movie => movie.title === 'Dune: Part Two') || movies[0];
    for (const movie of [preferredMovie, ...movies.filter(item => item.id !== preferredMovie?.id)]) {
      const showtimeBody = await fetch('/api/v1/showtimes/movie/' + movie.id).then(response => response.json());
      const showtimes = (showtimeBody.result || [])
        .filter(showtime => showtime.status !== 'CANCELLED' && new Date(showtime.startTime).getTime() > Date.now())
        .sort((left, right) => new Date(left.startTime) - new Date(right.startTime));
      if (showtimes.length) return { movieId: preferredMovie.id, showtimeId: showtimes[0].id };
    }
    return { movieId: preferredMovie?.id, showtimeId: null };
  })()`);
}


async function createPendingBooking(client, showtimeId) {
  return evaluate(client, `(async () => {
    const token = localStorage.getItem('access_token');
    const headers = { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token };
    const existingBody = await fetch('/api/v1/bookings/my?page=0&size=50', { headers }).then(response => response.json());
    const existing = (existingBody?.result?.content || []).find(booking =>
      booking.status === 'PENDING' && new Date(booking.paymentExpiresAt || 0).getTime() > Date.now()
    );
    if (existing) return { id: existing.id, existing: true };

    const seatBody = await fetch('/api/v1/showtimes/' + ${JSON.stringify(showtimeId)} + '/seats', { headers }).then(response => response.json());
    const seat = (seatBody.result || []).find(item => item.status === 'AVAILABLE');
    if (!seat) return { error: 'No available seat' };
    const request = { showtimeId: ${JSON.stringify(showtimeId)}, seatIds: [seat.seatId] };
    const holdResponse = await fetch('/api/v1/bookings/hold', {
      method: 'POST', headers, body: JSON.stringify(request)
    });
    const holdBody = await holdResponse.json();
    if (!holdResponse.ok || holdBody.code !== 1000) return { error: 'Hold failed', body: holdBody };
    const bookingResponse = await fetch('/api/v1/bookings', {
      method: 'POST', headers, body: JSON.stringify(request)
    });
    const bookingBody = await bookingResponse.json();
    if (!bookingResponse.ok || !bookingBody?.result?.id) return { error: 'Booking failed', body: bookingBody };
    return { id: bookingBody.result.id, existing: false };
  })()`);
}


async function main() {
  await mkdir(outputDir, { recursive: true });
  const chrome = spawn(chromePath, [
    '--headless=new',
    `--remote-debugging-port=${debugPort}`,
    `--user-data-dir=${profileDir}`,
    '--disable-gpu',
    '--disable-extensions',
    '--hide-scrollbars',
    '--no-first-run',
    '--use-fake-device-for-media-stream',
    '--use-fake-ui-for-media-stream',
    '--window-size=1440,1000',
  ], { stdio: 'ignore', windowsHide: true });

  try {
    await waitForChrome();
    const client = await createPage();
    await client.send('Page.enable');
    await client.send('Runtime.enable');
    await client.send('Emulation.setDeviceMetricsOverride', {
      width: 1440,
      height: 1000,
      deviceScaleFactor: 1.25,
      mobile: false,
    });

    await navigate(client, '/');
    await waitFor(client, `document.body.innerText.includes('Hôm nay bạn muốn xem phim gì?')`);
    await screenshot(client, '4.1');

    const selection = await findMovieAndShowtime(client);
    if (!selection.movieId || !selection.showtimeId) throw new Error('No future movie/showtime is available');
    await navigate(client, `/movies/${selection.movieId}`);
    await waitFor(client, `document.querySelectorAll('a[href*="/seat-selection/"]').length > 0`);
    await screenshot(client, '4.2');

    await login(client, process.env.THESIS_USER_USERNAME || 'user1', process.env.THESIS_USER_PASSWORD || '123456', `/seat-selection/${selection.showtimeId}`);
    await waitFor(client, `document.body.innerText.includes('MÀN HÌNH') || document.body.innerText.includes('Màn hình')`);
    await screenshot(client, '4.3');

    const booking = await createPendingBooking(client, selection.showtimeId);
    if (!booking?.id) throw new Error(`Cannot prepare checkout: ${JSON.stringify(booking)}`);
    await navigate(client, `/checkout/${booking.id}`);
    await waitFor(client, `document.body.innerText.includes('Phương thức thanh toán')`);
    await evaluate(client, `(() => {
      const method = [...document.querySelectorAll('button')].find(button => button.innerText.includes('Quét QR ngân hàng'));
      if (method) method.click();
      const pay = [...document.querySelectorAll('button')].find(button => button.innerText.includes('Thanh toán Quét QR ngân hàng'));
      if (pay) pay.click();
      return Boolean(pay);
    })()`);
    await waitFor(client, `Boolean(document.getElementById('sepay-qr-section'))`, 20_000);
    await evaluate(client, `document.getElementById('sepay-qr-section').scrollIntoView({ block: 'center', behavior: 'instant' }); true`);
    await sleep(700);
    await screenshot(client, '4.4', { scrollTop: await evaluate(client, 'window.scrollY') });

    await navigate(client, '/my/bookings');
    await waitFor(client, `document.body.innerText.includes('Vé của tôi')`);
    await screenshot(client, '4.5');

    await login(client, process.env.THESIS_ADMIN_USERNAME || 'admin', process.env.THESIS_ADMIN_PASSWORD || 'admin123', '/admin/dashboard');
    await waitFor(client, `document.body.innerText.includes('Biểu đồ doanh thu')`);
    await screenshot(client, '4.6');

    await navigate(client, '/admin/showtimes');
    await waitFor(client, `document.body.innerText.includes('Quản lý suất chiếu')`);
    await screenshot(client, '4.7');

    await login(client, process.env.THESIS_STAFF_USERNAME || 'staff1', process.env.THESIS_STAFF_PASSWORD || '123456', '/staff/scanner');
    await waitFor(client, `document.body.innerText.includes('Soát vé')`);
    await screenshot(client, '4.8');

    console.log(`Captured 8 UI screenshots into ${outputDir}`);
  } finally {
    chrome.kill();
  }
}


await main();
