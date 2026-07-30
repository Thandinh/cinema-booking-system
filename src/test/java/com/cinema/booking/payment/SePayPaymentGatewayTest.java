package com.cinema.booking.payment;

import com.cinema.booking.configuration.SePayConfig;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Payment;
import com.cinema.booking.enums.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SePayPaymentGatewayTest {

    @Test
    void createPaymentUrl_shouldRegenerateQrWhenCachedAmountIsStale() {
        SePayConfig config = new SePayConfig();
        config.setEnabled(true);
        config.setBankCode("MB");
        config.setAccountNumber("0342347716");
        config.setAccountName("Cinema Booking");
        config.setQrBaseUrl("https://vietqr.app/img");

        Payment payment = Payment.builder()
                .amount(new BigDecimal("245000.00"))
                .method(PaymentMethod.SEPAY)
                .transactionNo("CBK1234567890")
                .providerResponse(new LinkedHashMap<>(Map.of(
                        "sepayQr", Map.of(
                                "qrUrl", "https://vietqr.app/img?amount=275000",
                                "bankCode", "MB",
                                "accountNumber", "0342347716",
                                "accountName", "Cinema Booking",
                                "amount", 275000,
                                "transferCode", "CBK1234567890",
                                "transferContent", "CBK1234567890 thanh toan ve"
                        )
                )))
                .build();
        Booking booking = Booking.builder()
                .paymentExpiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        String paymentUrl = new SePayPaymentGateway(config).createPaymentUrl(payment, booking, null);

        assertThat(paymentUrl).contains("amount=245000");
        assertThat(paymentUrl).doesNotContain("amount=275000");
        assertThat(paymentUrl).contains("transferCode=CBK1234567890");
    }
}
