package com.cinema.booking.configuration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String MOVIES = "movies";
    public static final String CINEMAS = "cinemas";
    public static final String CINEMA_MAP = "cinema-map";
    public static final String ROOMS_BY_CINEMA = "rooms-by-cinema";
    public static final String SEATS_BY_ROOM = "seats-by-room";
    public static final String PROMOTIONS = "promotions";
}
