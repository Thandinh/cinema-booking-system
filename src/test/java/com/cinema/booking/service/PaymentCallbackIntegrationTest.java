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
import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.enums.PaymentStatus;
import com.cinema.booking.enums.ShowtimeStatus;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.CinemaRepository;
import com.cinema.booking.repository.MovieRepository;
import com.cinema.booking.repository.PaymentRepository;
import com.cinema.booking.repository.RoomRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "vnpay.hash-secret=test-vnpay-hash-secret")
class PaymentCallbackIntegrationTest extends PostgresIntegrationTest {

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
        assertThat(redirect).isEqualTo("redirect:/payment-failed?reason=invalid-signature");
        verify(bookingService, never()).handlePaymentSuccess("secure-token-invalid-signature");
        verify(bookingService, never()).handlePaymentFailure("secure-token-invalid-signature");
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

    private void clearBusinessData() {
        paymentRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        showtimeRepository.deleteAllInBatch();
        roomRepository.deleteAllInBatch();
        cinemaRepository.deleteAllInBatch();
        movieRepository.deleteAllInBatch();
    }

    private Payment createPendingPayment(String transactionNo, String secureToken) {
        String suffix = UUID.randomUUID().toString();
        User user = userRepository.findByUsername("user1").orElseThrow();
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
                .method(PaymentMethod.VNPAY)
                .transactionNo(transactionNo)
                .status(PaymentStatus.PENDING)
                .build());
    }

    private MockHttpServletRequest signedVnpayRequest(Payment payment, String responseCode, String hashSecret) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("vnp_TxnRef", payment.getTransactionNo());
        fields.put("vnp_ResponseCode", responseCode);
        fields.put("vnp_OrderInfo", "Thanh toan ve xem phim|" + payment.getBooking().getSecureToken());
        fields.put("vnp_Amount", payment.getAmount().multiply(new BigDecimal("100")).toBigInteger().toString());

        MockHttpServletRequest request = new MockHttpServletRequest();
        fields.forEach(request::addParameter);
        request.addParameter("vnp_SecureHash", sign(fields, hashSecret));
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
}
