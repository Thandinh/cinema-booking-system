package com.cinema.booking.service;

import com.cinema.booking.enums.ErrorCode;
import com.cinema.booking.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketQrCodeServiceTest {

    TicketQrCodeService ticketQrCodeService;

    @BeforeEach
    void setUp() {
        ticketQrCodeService = new TicketQrCodeService();
        ReflectionTestUtils.setField(
                ticketQrCodeService,
                "qrSecret",
                "test-ticket-qr-secret-with-at-least-32-characters");
        ticketQrCodeService.init();
    }

    @Test
    void generatedTokenCanBeValidated() {
        String qrCode = ticketQrCodeService.generate(UUID.randomUUID());

        assertTrue(qrCode.length() <= 100);
        assertEquals(qrCode, ticketQrCodeService.normalizeAndValidate(qrCode));
    }

    @Test
    void tamperedTokenIsRejected() {
        String qrCode = ticketQrCodeService.generate(UUID.randomUUID());
        String tamperedQrCode = qrCode.substring(0, qrCode.length() - 1)
                + (qrCode.endsWith("A") ? "B" : "A");

        AppException exception = assertThrows(
                AppException.class,
                () -> ticketQrCodeService.normalizeAndValidate(tamperedQrCode));

        assertEquals(ErrorCode.INVALID_QR_CODE, exception.getErrorCode());
    }

    @Test
    void blankTokenIsRejected() {
        AppException exception = assertThrows(
                AppException.class,
                () -> ticketQrCodeService.normalizeAndValidate(" "));

        assertEquals(ErrorCode.TICKET_QR_REQUIRED, exception.getErrorCode());
    }
}
