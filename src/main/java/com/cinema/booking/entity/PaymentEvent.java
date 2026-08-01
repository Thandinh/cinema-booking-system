package com.cinema.booking.entity;

import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.enums.PaymentEventType;
import com.cinema.booking.enums.PaymentMethod;
import com.cinema.booking.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "payment_events")
public class PaymentEvent extends BaseEntity {

    @Id
    @GeneratedValue
    UUID id;

    UUID paymentId;
    UUID bookingId;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    PaymentMethod method;

    @Column(length = 255)
    String transactionNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    PaymentEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    PaymentStatus paymentStatusBefore;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    PaymentStatus paymentStatusAfter;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    BookingStatus bookingStatusBefore;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    BookingStatus bookingStatusAfter;

    Boolean success;

    @Column(length = 1000)
    String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    Map<String, Object> payload;
}
