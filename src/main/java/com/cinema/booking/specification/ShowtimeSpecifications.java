package com.cinema.booking.specification;

import com.cinema.booking.entity.Cinema;
import com.cinema.booking.entity.Movie;
import com.cinema.booking.entity.Room;
import com.cinema.booking.entity.Showtime;
import com.cinema.booking.enums.ShowtimeStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ShowtimeSpecifications {

    private ShowtimeSpecifications() {
    }

    public static Specification<Showtime> searchActive(
            LocalDateTime fromTime,
            LocalDateTime toTime,
            String city,
            UUID cinemaId,
            UUID roomId,
            ShowtimeStatus status,
            String keyword,
            List<UUID> scopedCinemaIds) {

        return (root, query, criteriaBuilder) -> {
            Join<Showtime, Movie> movie = root.join("movie", JoinType.INNER);
            Join<Showtime, Room> room = root.join("room", JoinType.INNER);
            Join<Room, Cinema> cinema = room.join("cinema", JoinType.INNER);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isFalse(root.get("isDeleted")));
            predicates.add(criteriaBuilder.isFalse(movie.get("isDeleted")));
            predicates.add(criteriaBuilder.isFalse(room.get("isDeleted")));
            predicates.add(criteriaBuilder.isFalse(cinema.get("isDeleted")));

            if (fromTime != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), fromTime));
            }
            if (toTime != null) {
                predicates.add(criteriaBuilder.lessThan(root.get("startTime"), toTime));
            }
            if (city != null) {
                predicates.add(criteriaBuilder.equal(cinema.get("city"), city));
            }
            if (cinemaId != null) {
                predicates.add(criteriaBuilder.equal(cinema.get("id"), cinemaId));
            }
            if (roomId != null) {
                predicates.add(criteriaBuilder.equal(room.get("id"), roomId));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (keyword != null) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(movie.get("title")), "%" + keyword + "%"));
            }
            if (scopedCinemaIds != null) {
                predicates.add(cinema.get("id").in(scopedCinemaIds));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
