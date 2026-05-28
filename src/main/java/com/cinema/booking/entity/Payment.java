package com.cinema.booking.entity;

import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue
    UUID id;

    // ON DELETE SET NULL trong DB → nullable = true ở đây
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    Booking booking;

    @Column(precision = 10, scale = 2)
    BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    PaymentMethod method;

    String transactionNo;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    PaymentStatus status = PaymentStatus.PENDING;

    // Lưu toàn bộ response từ VNPay/MoMo dưới dạng JSONB để tra cứu sau
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    Map<String, Object> providerResponse;

    LocalDateTime paymentTime;
}
