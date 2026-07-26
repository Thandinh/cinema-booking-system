package com.cinema.booking.repository;

import com.cinema.booking.entity.StaffCinema;
import com.cinema.booking.entity.StaffCinemaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StaffCinemaRepository extends JpaRepository<StaffCinema, StaffCinemaId> {

    boolean existsByIdStaffIdAndIdCinemaId(UUID staffId, UUID cinemaId);

    List<StaffCinema> findAllByIdStaffId(UUID staffId);

    @Query("""
            SELECT sc FROM StaffCinema sc
            JOIN FETCH sc.cinema
            WHERE sc.staff.id IN :staffIds
            """)
    List<StaffCinema> findAllWithCinemaByStaffIds(@Param("staffIds") List<UUID> staffIds);

    @Query("SELECT sc.cinema.id FROM StaffCinema sc WHERE sc.staff.id = :staffId")
    List<UUID> findCinemaIdsByStaffId(@Param("staffId") UUID staffId);

    @Modifying
    @Query("DELETE FROM StaffCinema sc WHERE sc.staff.id = :staffId")
    void deleteByStaffId(@Param("staffId") UUID staffId);
}
