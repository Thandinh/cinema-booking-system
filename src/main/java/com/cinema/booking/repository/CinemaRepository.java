package com.cinema.booking.repository;

import com.cinema.booking.entity.Cinema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, UUID> {

    Page<Cinema> findAllByIsDeletedFalse(Pageable pageable);

    Page<Cinema> findAllByIsActiveTrueAndIsDeletedFalse(Pageable pageable);

    @Query("SELECT c FROM Cinema c WHERE c.id = :id AND c.isDeleted = false")
    Optional<Cinema> findActiveById(UUID id);

    boolean existsByNameAndIsDeletedFalse(String name);
}
