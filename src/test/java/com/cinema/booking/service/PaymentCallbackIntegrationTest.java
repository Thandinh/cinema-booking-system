package com.cinema.booking.service;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Cinema;
import com.cinema.booking.entity.Movie;
import com.cinema.booking.entity.Payment;
import com.cinema.booking.entity.Room;
import com.cinema.booking.entity.Showtime;
import com.cinema.booking.entity.User;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.enums.MovieStatus;
import com.cinema.booking.enums.PaymentEventType;
import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.enums.PaymentStatus;
import com.cinema.booking.enums.ShowtimeStatus;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.CinemaRepository;
import com.cinema.booking.repository.MovieRepository;
import com.cinema.booking.repository.PaymentEventRepository;
import com.cinema.booking.repository.PaymentRepository;
import com.cinema.booking.repository.RoomRepository;
import com.cinema.booking.repository.RefundRepository;
import com.cinema.booking.repository.ShowtimeRepository;
import com.cinema.booking.repository.UserRepository;
import com.cinema.booking.support.PostgresIntegrationTest;
import com.cinema.booking.util.VNPayUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest(properties = {
        "vnpay.hash-secret=test-vnpay-hash-secret",
        "sepay.webhook-hmac-secret=test-sepay-webhook-hmac-secret",
        "sepay.description-prefix=CBK"
})
class PaymentCallbackIntegrationTest extends PostgresIntegrationTest {

    private static final String CALLBACK_TEST_USERNAME = "payment_callback_test_user";

    @Autowired
    PaymentService paymentService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    MovieRepository movieRepository;

    @Autowired
    CinemaRepository cinemaRepository;

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    ShowtimeRepository showtimeRepository;

    @Autowired
    BookingRepository bookingRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    PaymentEventRepository paymentEventRepository;

    @Autowired
    RefundRepository refundRepository;

    @MockitoBean
    BookingService bookingService;

    @BeforeEach
    void setUp() {
        clearBusinessData();
    }

    @AfterEach
    void tearDown() {
        clearBusinessData();
    }

    @Test
    void vnpayCallback_shouldMarkPaymentSuccessAndDelegateBookingSuccessWhenSignatureIsValid() {
        Payment payment = createPendingPayment("VNPAY_TXN_SUCCESS", "secure-token-success");
        MockHttpServletRequest request = signedVnpayRequest(payment, "00", "test-vnpay-hash-secret");

        String redirect = paymentService.handleVNPayCallback(request);

        Payment updatedPayment = paymentRepository.findByTransactionNo("VNPAY_TXN_SUCCESS").orElseThrow();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(updatedPayment.getPaymentTime()).isNotNull();
        assertThat(redirect).startsWith("redirect:/payment/result?status=SUCCESS");
        verify(bookingService).handlePaymentSuccess("secure-token-success");
        verify(bookingService, never()).handlePaymentFailure("secure-token-success");
    }

    @Test
    void vnpayCallback_shouldRejectInvalidSignatureWithoutMutatingPayment() {
        Payment payment = createPendingPayment("VNPAY_TXN_INVALID_SIGNATURE", "secure-token-invalid-signature");
        MockHttpServletRequest request = signedVnpayRequest(payment, "00", "wrong-secret");

        String redirect = paymentService.handleVNPayCallback(request);

        Payment unchangedPayment = paymentRepository.findByTransactionNo("VNPAY_TXN_INVALID_SIGNATURE").orElseThrow();
        assertThat(unchangedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(unchangedPayment.getPaymentTime()).isNull();
        assertThat(redirect).isEqualTo("redirect:/payment/result?status=FAILED&reason=invalid-signature");
        verify(bookingService, never()).handlePaymentSuccess("secure-token-invalid-signature");
        verify(bookingService, never()).handlePaymentFailure("secure-token-invalid-signature");
    }

    @Test
    void vnpayCallback_shouldUseBookingTokenFromTransactionReference() {
        Payment payment = createPendingPayment("VNPAY_TXN_DB_TOKEN", "secure-token-from-db");
        MockHttpServletRequest request = signedVnpayRequest(
                payment,
                "00",
                "test-vnpay-hash-secret",
                "tampered-token-from-order-info");

        String redirect = paymentService.handleVNPayCallback(request);

        assertThat(redirect).startsWith("redirect:/payment/result?status=SUCCESS");
        verify(bookingService).handlePaymentSuccess("secure-token-from-db");
        verify(bookingService, never()).handlePaymentSuccess("tampered-token-from-order-info");
    }

    @Test
    void vnpayCallback_shouldMarkPaymentFailedAndReleaseBookingWhenProviderDeclines() {
        Payment payment = createPendingPayment("VNPAY_TXN_FAILED", "secure-token-failed");
        MockHttpServletRequest request = signedVnpayRequest(payment, "24", "test-vnpay-hash-secret");

        String redirect = paymentService.handleVNPayCallback(request);

        Payment updatedPayment = paymentRepository.findByTransactionNo("VNPAY_TXN_FAILED").orElseThrow();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(updatedPayment.getPaymentTime()).isNull();
        assertThat(redirect).startsWith("redirect:/payment/result?status=FAILED");
        verify(bookingService).handlePaymentFailure("secure-token-failed");
        verify(bookingService, never()).handlePaymentSuccess("secure-token-failed");
    }

    @Test
    void sepayWebhook_shouldMarkPaymentSuccessWhenSignatureAndAmountAreValid() {
        Payment payment = createPendingPayment("CBK1234567890", "secure-token-sepay-success", PaymentMethod.SEPAY);
        String rawPayload = """
                {"gateway":"MBBank","transactionDate":"2026-07-30 21:41:00","accountNumber":"0342347716","code":null,"content":"139949933279-CBK1234567890 thanh toan ve","transferType":"in","description":"BankAPINotify 139949933279-CBK1234567890 thanh toan ve","transferAmount":200000,"referenceCode":"FT26211010184775","id":70787101}
                """.trim();
        MockHttpServletRequest request = signedSePayRequest(rawPayload, "1785422477",
                "test-sepay-webhook-hmac-secret");

        Map<String, Object> response = paymentService.handleSePayWebhook(rawPayload, request);

        Payment updatedPayment = paymentRepository.findByTransactionNo("CBK1234567890").orElseThrow();
        assertThat(response).containsEntry("success", true);
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(updatedPayment.getPaymentTime()).isNotNull();
        verify(bookingService).handlePaymentSuccess("secure-token-sepay-success");
        verify(bookingService, never()).handlePaymentFailure("secure-token-sepay-success");
    }

    @Test
    void sepayWebhook_shouldRejectInvalidSignatureWithoutMutatingPayment() {
        createPendingPayment("CBK1234567891", "secure-token-sepay-invalid", PaymentMethod.SEPAY);
        String rawPayload = """
                {"gateway":"MBBank","transactionDate":"2026-07-30 21:41:00","accountNumber":"0342347716","code":null,"content":"CBK1234567891 thanh toan ve","transferType":"in","description":"CBK1234567891 thanh toan ve","transferAmount":200000,"referenceCode":"FT26211010184776","id":70787102}
                """.trim();
        MockHttpServletRequest request = signedSePayRequest(rawPayload, "1785422477", "wrong-secret");

        Map<String, Object> response = paymentService.handleSePayWebhook(rawPayload, request);

        Payment unchangedPayment = paymentRepository.findByTransactionNo("CBK1234567891").orElseThrow();
        assertThat(response)
                .containsEntry("success", false)
                .containsEntry("message", "Invalid webhook authentication");
        assertThat(unchangedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(unchangedPayment.getPaymentTime()).isNull();
        verify(bookingService, never()).handlePaymentSuccess("secure-token-sepay-invalid");
        verify(bookingService, never()).handlePaymentFailure("secure-token-sepay-invalid");
    }

    @Test
    void sepayWebhook_shouldRejectAmountMismatchWithoutMutatingPayment() {
        createPendingPayment("CBK1234567892", "secure-token-sepay-amount", PaymentMethod.SEPAY);
        String rawPayload = """
                {"gateway":"MBBank","transactionDate":"2026-07-30 21:41:00","accountNumber":"0342347716","code":null,"content":"CBK1234567892 thanh toan ve","transferType":"in","description":"CBK1234567892 thanh toan ve","transferAmount":150000,"referenceCode":"FT26211010184777","id":70787103}
                """.trim();
        MockHttpServletRequest request = signedSePayRequest(rawPayload, "1785422477",
                "test-sepay-webhook-hmac-secret");

        Map<String, Object> response = paymentService.handleSePayWebhook(rawPayload, request);

        Payment unchangedPayment = paymentRepository.findByTransactionNo("CBK1234567892").orElseThrow();
        assertThat(response)
                .containsEntry("success", false)
                .containsEntry("message", "Amount mismatch");
        assertThat(unchangedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(unchangedPayment.getPaymentTime()).isNull();
        assertThat(paymentEventRepository.findAll())
                .anySatisfy(event -> {
                    assertThat(event.getTransactionNo()).isEqualTo("CBK1234567892");
                    assertThat(event.getEventType()).isEqualTo(PaymentEventType.SEPAY_AMOUNT_MISMATCH);
                    assertThat(event.getSuccess()).isFalse();
                });
        verify(bookingService, never()).handlePaymentSuccess("secure-token-sepay-amount");
        verify(bookingService, never()).handlePaymentFailure("secure-token-sepay-amount");
    }

    @Test
    void sepayWebhook_shouldBeIdempotentWhenPaymentWasAlreadyProcessed() {
        Payment payment = createPendingPayment("CBK1234567893", "secure-token-sepay-processed", PaymentMethod.SEPAY);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentTime(LocalDateTime.now().minusMinutes(1));
        paymentRepository.saveAndFlush(payment);
        String rawPayload = """
                {"gateway":"MBBank","transactionDate":"2026-07-30 21:41:00","accountNumber":"0342347716","code":null,"content":"CBK1234567893 thanh toan ve","transferType":"in","description":"CBK1234567893 thanh toan ve","transferAmount":200000,"referenceCode":"FT26211010184778","id":70787104}
                """.trim();
        MockHttpServletRequest request = signedSePayRequest(rawPayload, "1785422477",
                "test-sepay-webhook-hmac-secret");

        Map<String, Object> response = paymentService.handleSePayWebhook(rawPayload, request);

        assertThat(response)
                .containsEntry("success", true)
                .containsEntry("message", "Already processed");
        assertThat(paymentRepository.findByTransactionNo("CBK1234567893").orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.SUCCESS);
        assertThat(paymentEventRepository.findAll())
                .anySatisfy(event -> {
                    assertThat(event.getTransactionNo()).isEqualTo("CBK1234567893");
                    assertThat(event.getEventType()).isEqualTo(PaymentEventType.PAYMENT_ALREADY_PROCESSED);
                    assertThat(event.getSuccess()).isTrue();
                });
        verifyNoMoreInteractions(bookingService);
    }

    @Test
    void sepayWebhook_shouldExpirePaymentWhenBookingWindowHasExpired() {
        Payment payment = createPendingPayment("CBK1234567894", "secure-token-sepay-expired", PaymentMethod.SEPAY);
        Booking booking = payment.getBooking();
        booking.setPaymentExpiresAt(LocalDateTime.now().minusMinutes(1));
        bookingRepository.saveAndFlush(booking);
        String rawPayload = """
                {"gateway":"MBBank","transactionDate":"2026-07-30 21:41:00","accountNumber":"0342347716","code":null,"content":"CBK1234567894 thanh toan ve","transferType":"in","description":"CBK1234567894 thanh toan ve","transferAmount":200000,"referenceCode":"FT26211010184779","id":70787105}
                """.trim();
        MockHttpServletRequest request = signedSePayRequest(rawPayload, "1785422477",
                "test-sepay-webhook-hmac-secret");

        Map<String, Object> response = paymentService.handleSePayWebhook(rawPayload, request);

        assertThat(response)
                .containsEntry("success", true)
                .containsEntry("message", "Refund pending");
        assertThat(paymentRepository.findByTransactionNo("CBK1234567894").orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.REFUND_PENDING);
        assertThat(paymentEventRepository.findAll())
                .anySatisfy(event -> {
                    assertThat(event.getTransactionNo()).isEqualTo("CBK1234567894");
                    assertThat(event.getEventType()).isEqualTo(PaymentEventType.REFUND_REQUESTED);
                    assertThat(event.getSuccess()).isTrue();
                });
        verify(bookingService).expirePendingBooking(booking.getId());
        verify(bookingService, never()).handlePaymentSuccess("secure-token-sepay-expired");
        verify(bookingService, never()).handlePaymentFailure("secure-token-sepay-expired");
    }

    private void clearBusinessData() {
        refundRepository.deleteAllInBatch();
        paymentEventRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        showtimeRepository.deleteAllInBatch();
        roomRepository.deleteAllInBatch();
        cinemaRepository.deleteAllInBatch();
        movieRepository.deleteAllInBatch();
    }

    private Payment createPendingPayment(String transactionNo, String secureToken) {
        return createPendingPayment(transactionNo, secureToken, PaymentMethod.VNPAY);
    }

    private Payment createPendingPayment(String transactionNo, String secureToken, PaymentMethod method) {
        String suffix = UUID.randomUUID().toString();
        User user = findOrCreateCallbackTestUser();
        Movie movie = movieRepository.save(Movie.builder()
                .title("Payment Callback Movie " + suffix)
                .duration(120)
                .releaseDate(LocalDate.now().minusDays(1))
                .status(MovieStatus.NOW_SHOWING)
                .isDeleted(false)
                .build());
        Cinema cinema = cinemaRepository.save(Cinema.builder()
                .name("Payment Callback Cinema " + suffix)
                .city("TP Ho Chi Minh")
                .isActive(true)
                .isDeleted(false)
                .build());
        Room room = roomRepository.save(Room.builder()
                .cinema(cinema)
                .name("Screen 01")
                .isDeleted(false)
                .build());
        Showtime showtime = showtimeRepository.save(Showtime.builder()
                .movie(movie)
                .room(room)
                .startTime(LocalDateTime.now().plusHours(2))
                .endTime(LocalDateTime.now().plusHours(4))
                .basePrice(new BigDecimal("200000.00"))
                .status(ShowtimeStatus.UPCOMING)
                .isDeleted(false)
                .build());
        Booking booking = bookingRepository.save(Booking.builder()
                .user(user)
                .showtime(showtime)
                .totalPrice(new BigDecimal("200000.00"))
                .discountAmount(BigDecimal.ZERO)
                .status(BookingStatus.PENDING)
                .secureToken(secureToken)
                .paymentExpiresAt(LocalDateTime.now().plusMinutes(5))
                .build());
        return paymentRepository.save(Payment.builder()
                .booking(booking)
                .amount(new BigDecimal("200000.00"))
                .method(method)
                .transactionNo(transactionNo)
                .status(PaymentStatus.PENDING)
                .build());
    }

    private User findOrCreateCallbackTestUser() {
        return userRepository.findByUsername(CALLBACK_TEST_USERNAME)
                .orElseGet(() -> userRepository.save(User.builder()
                        .username(CALLBACK_TEST_USERNAME)
                        .password("test-only-password-hash")
                        .email("payment-callback-test@example.invalid")
                        .emailVerified(true)
                        .isActive(true)
                        .isDeleted(false)
                        .build()));
    }

    private MockHttpServletRequest signedVnpayRequest(Payment payment, String responseCode, String hashSecret) {
        return signedVnpayRequest(payment, responseCode, hashSecret, payment.getBooking().getSecureToken());
    }

    private MockHttpServletRequest signedVnpayRequest(
            Payment payment,
            String responseCode,
            String hashSecret,
            String orderInfoSecureToken) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("vnp_TxnRef", payment.getTransactionNo());
        fields.put("vnp_ResponseCode", responseCode);
        fields.put("vnp_OrderInfo", "Thanh toan ve xem phim|" + orderInfoSecureToken);
        fields.put("vnp_Amount", payment.getAmount().multiply(new BigDecimal("100")).toBigInteger().toString());

        MockHttpServletRequest request = new MockHttpServletRequest();
        fields.forEach(request::addParameter);
        request.addParameter("vnp_SecureHash", sign(fields, hashSecret));
        return request;
    }

    private MockHttpServletRequest signedSePayRequest(String rawPayload, String timestamp, String hmacSecret) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Sepay-Timestamp", timestamp);
        request.addHeader("X-Sepay-Signature", "sha256=" + hmacSha256(hmacSecret, timestamp + "." + rawPayload));
        return request;
    }

    private String sign(Map<String, String> rawFields, String hashSecret) {
        List<String> encodedNames = new ArrayList<>();
        Map<String, String> encodedFields = new LinkedHashMap<>();
        rawFields.forEach((key, value) -> {
            String encodedKey = URLEncoder.encode(key, StandardCharsets.US_ASCII);
            encodedNames.add(encodedKey);
            encodedFields.put(encodedKey, URLEncoder.encode(value, StandardCharsets.US_ASCII));
        });
        encodedNames.sort(String::compareTo);

        StringBuilder hashData = new StringBuilder();
        for (int i = 0; i < encodedNames.size(); i++) {
            String fieldName = encodedNames.get(i);
            hashData.append(fieldName).append('=').append(encodedFields.get(fieldName));
            if (i < encodedNames.size() - 1) {
                hashData.append('&');
            }
        }
        return VNPayUtil.hmacSHA512(hashSecret, hashData.toString());
    }

    private String hmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Cannot sign SePay test payload", ex);
        }
    }
}
