package com.cinema.booking;

import com.cinema.booking.configuration.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableConfigurationProperties(JwtProperties.class)
@EnableScheduling
@SpringBootApplication
public class CinemaBookingSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(CinemaBookingSystemApplication.class, args);
	}

}
