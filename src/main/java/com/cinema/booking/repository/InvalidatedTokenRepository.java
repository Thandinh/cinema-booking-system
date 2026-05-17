package com.cinema.booking.repository;

import com.cinema.booking.entity.InvalidatedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;

public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {
    void deleteAllByExpiryTimeBefore(Date date);
}
