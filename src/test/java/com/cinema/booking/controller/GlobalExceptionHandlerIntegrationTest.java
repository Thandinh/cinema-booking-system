package com.cinema.booking.controller;

import com.cinema.booking.dto.request.HoldSeatRequest;
import com.cinema.booking.service.BookingService;
import com.cinema.booking.service.PaymentService;
import com.cinema.booking.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.web.resources.add-mappings=false")
@AutoConfigureMockMvc
class GlobalExceptionHandlerIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    BookingService bookingService;

    @MockitoBean
    PaymentService paymentService;

    @Test
    void unauthorizedResponses_shouldUseStableApiResponseShape() throws Exception {
        HoldSeatRequest request = new HoldSeatRequest(UUID.randomUUID(), List.of(UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/bookings/hold")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1006))
                .andExpect(jsonPath("$.message").value("Unauthenticated"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/bookings/hold"));

        verifyNoInteractions(bookingService);
    }

    @Test
    void validationErrors_shouldIncludeFieldDetailsAndRequestMetadata() throws Exception {
        mockMvc.perform(post("/api/v1/bookings/hold")
                        .with(jwtWithAuthorities("BOOKING_CREATE"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/bookings/hold"))
                .andExpect(jsonPath("$.result.errors").isArray())
                .andExpect(jsonPath("$.result.errors[0].field").isNotEmpty())
                .andExpect(jsonPath("$.result.errors[0].code").isNotEmpty())
                .andExpect(jsonPath("$.result.errors[0].message").isNotEmpty());

        verifyNoInteractions(bookingService);
    }

    @Test
    void malformedPathVariables_shouldReturnBadRequestInsteadOfServerError() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/showtimes/not-a-uuid/seats"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1025))
                .andExpect(jsonPath("$.path").value("/api/v1/bookings/showtimes/not-a-uuid/seats"))
                .andExpect(jsonPath("$.result.errors[0].field").value("showtimeId"));

        verifyNoInteractions(bookingService);
    }

    @Test
    void unsupportedHttpMethod_shouldReturnMethodNotAllowedResponse() throws Exception {
        HoldSeatRequest request = new HoldSeatRequest(UUID.randomUUID(), List.of(UUID.randomUUID()));

        mockMvc.perform(put("/api/v1/bookings/hold")
                        .with(jwtWithAuthorities("BOOKING_CREATE"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(1037))
                .andExpect(jsonPath("$.path").value("/api/v1/bookings/hold"))
                .andExpect(jsonPath("$.result.method").value("PUT"));

        verifyNoInteractions(bookingService);
    }

    @Test
    void unknownApiRoutes_shouldReturnNotFoundResponse() throws Exception {
        mockMvc.perform(get("/api/v1/route-that-does-not-exist")
                        .with(jwtWithAuthorities("MOVIE_VIEW")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1036))
                .andExpect(jsonPath("$.path").value("/api/v1/route-that-does-not-exist"));

        verifyNoInteractions(bookingService);
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
