package com.cinema.booking.controller;

import com.cinema.booking.dto.request.HoldSeatRequest;
import com.cinema.booking.dto.request.BookingSearchRequest;
import com.cinema.booking.dto.request.TicketCheckInRequest;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.dto.response.HoldSeatResponse;
import com.cinema.booking.dto.response.SeatMapItemResponse;
import com.cinema.booking.dto.response.TicketResponse;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.enums.SeatStatusType;
import com.cinema.booking.enums.SeatType;
import com.cinema.booking.enums.TicketStatus;
import com.cinema.booking.service.BookingService;
import com.cinema.booking.service.PaymentService;
import com.cinema.booking.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.frontend-url=http://localhost:5173")
@AutoConfigureMockMvc
class BookingPaymentSecurityIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    BookingService bookingService;

    @MockitoBean
    PaymentService paymentService;

    @Test
    void getSeatMap_shouldBePublic() throws Exception {
        UUID showtimeId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        when(bookingService.getSeatMap(showtimeId)).thenReturn(List.of(SeatMapItemResponse.builder()
                .seatStatusId(UUID.randomUUID())
                .seatId(seatId)
                .rowLabel("A")
                .seatNumber(1)
                .seatType(SeatType.NORMAL)
                .rowIndex(0)
                .colIndex(0)
                .priceMultiplier(BigDecimal.ONE)
                .price(new BigDecimal("100000.00"))
                .status(SeatStatusType.AVAILABLE)
                .build()));

        mockMvc.perform(get("/api/v1/bookings/showtimes/{showtimeId}/seats", showtimeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].seatId").value(seatId.toString()))
                .andExpect(jsonPath("$.result[0].status").value("AVAILABLE"));

        verify(bookingService).getSeatMap(showtimeId);
    }

    @Test
    void holdSeats_shouldRejectAnonymousAndMissingPermission() throws Exception {
        HoldSeatRequest request = new HoldSeatRequest(UUID.randomUUID(), List.of(UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/bookings/hold")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/bookings/hold")
                        .with(jwtWithAuthorities("MOVIE_VIEW"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1007));

        verifyNoInteractions(bookingService);
    }

    @Test
    void holdSeats_shouldAllowUserWithBookingCreatePermission() throws Exception {
        UUID showtimeId = UUID.randomUUID();
        UUID seatId = UUID.randomUUID();
        HoldSeatRequest request = new HoldSeatRequest(showtimeId, List.of(seatId));
        when(bookingService.holdSeats(any(HoldSeatRequest.class))).thenReturn(HoldSeatResponse.builder()
                .showtimeId(showtimeId)
                .heldSeatIds(List.of(seatId))
                .holdUntil(LocalDateTime.now().plusMinutes(5))
                .estimatedTotalPrice(new BigDecimal("100000.00"))
                .message("Seats held successfully")
                .build());

        mockMvc.perform(post("/api/v1/bookings/hold")
                        .with(jwtWithAuthorities("BOOKING_CREATE"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.showtimeId").value(showtimeId.toString()))
                .andExpect(jsonPath("$.result.heldSeatIds[0]").value(seatId.toString()));

        verify(bookingService).holdSeats(any(HoldSeatRequest.class));
    }

    @Test
    void adminBookingList_shouldRequireBookingViewAllPermission() throws Exception {
        Page<BookingResponse> response = new PageImpl<>(List.of(BookingResponse.builder()
                .id(UUID.randomUUID())
                .secureToken("secure-token")
                .status(BookingStatus.SUCCESS)
                .movieTitle("Test Movie")
                .build()));
        when(bookingService.getAllBookings(any(BookingSearchRequest.class), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/bookings")
                        .with(jwtWithAuthorities("BOOKING_VIEW_OWN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1007));

        mockMvc.perform(get("/api/v1/bookings")
                        .with(jwtWithAuthorities("BOOKING_VIEW_ALL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].status").value("SUCCESS"));

        verify(bookingService).getAllBookings(any(BookingSearchRequest.class), any());
    }

    @Test
    void checkInTicket_shouldRequireTicketCheckInPermission() throws Exception {
        UUID cinemaId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();
        TicketCheckInRequest request = new TicketCheckInRequest();
        request.setQrCode("CBT1.00000000000000000000000000000000.AAAAAAAAAAAAAAAAAAAAAA.AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        request.setCinemaId(cinemaId);
        request.setShowtimeId(showtimeId);
        when(bookingService.checkInTicket(request.getQrCode(), cinemaId, showtimeId)).thenReturn(TicketResponse.builder()
                .id(UUID.randomUUID())
                .qrCode(request.getQrCode())
                .status(TicketStatus.USED)
                .checkedInByUsername("staff1")
                .alreadyCheckedIn(false)
                .build());

        mockMvc.perform(post("/api/v1/bookings/tickets/check-in")
                        .with(jwtWithAuthorities("TICKET_VIEW_OWN"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1007));

        mockMvc.perform(post("/api/v1/bookings/tickets/check-in")
                        .with(jwtWithAuthorities("TICKET_CHECKIN"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("USED"))
                .andExpect(jsonPath("$.result.checkedInByUsername").value("staff1"));

        verify(bookingService).checkInTicket(request.getQrCode(), cinemaId, showtimeId);
    }

    @Test
    void initiatePayment_shouldRequirePaymentCreatePermission() throws Exception {
        UUID bookingId = UUID.randomUUID();
        when(paymentService.initiatePayment(eq(bookingId), eq(PaymentMethod.VNPAY), eq(new BigDecimal("200000.00")), any()))
                .thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?txn=123");

        mockMvc.perform(post("/api/v1/payments/initiate")
                        .with(jwtWithAuthorities("PAYMENT_VIEW_OWN"))
                        .param("bookingId", bookingId.toString())
                        .param("method", "VNPAY")
                        .param("amount", "200000.00"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1007));

        mockMvc.perform(post("/api/v1/payments/initiate")
                        .with(jwtWithAuthorities("PAYMENT_CREATE"))
                        .param("bookingId", bookingId.toString())
                        .param("method", "VNPAY")
                        .param("amount", "200000.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?txn=123"));

        verify(paymentService).initiatePayment(eq(bookingId), eq(PaymentMethod.VNPAY), eq(new BigDecimal("200000.00")), any());
    }

    @Test
    void paymentCallbacks_shouldBePublicAndRedirectToFrontend() throws Exception {
        when(paymentService.handleVNPayCallback(any()))
                .thenReturn("redirect:/payment/result?status=SUCCESS&bookingId=booking-1&txn=txn-1");
        when(paymentService.handleMomoReturn(any()))
                .thenReturn("redirect:/payment/result?status=FAILED&bookingId=booking-2&txn=txn-2");

        mockMvc.perform(get("/api/v1/payments/vnpay-callback"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "http://localhost:5173/payment/result?status=SUCCESS&bookingId=booking-1&txn=txn-1"));

        mockMvc.perform(get("/api/v1/payments/momo-return"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "http://localhost:5173/payment/result?status=FAILED&bookingId=booking-2&txn=txn-2"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor jwtWithAuthorities(String... authorities) {
        return jwt()
                .jwt(jwt -> jwt
                        .claim("userId", UUID.randomUUID().toString())
                        .claim("scope", String.join(" ", authorities)))
                .authorities(List.of(authorities).stream()
                        .map(SimpleGrantedAuthority::new)
                        .map(GrantedAuthority.class::cast)
                        .toList());
    }
}
