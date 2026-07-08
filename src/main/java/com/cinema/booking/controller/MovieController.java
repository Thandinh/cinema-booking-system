package com.cinema.booking.controller;

import com.cinema.booking.dto.request.MovieCreationRequest;
import com.cinema.booking.dto.request.MovieUpdateRequest;
import com.cinema.booking.dto.response.ApiResponse;
import com.cinema.booking.dto.response.MovieResponse;
import com.cinema.booking.enums.MovieStatus;
import com.cinema.booking.service.MovieService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MovieController {

    MovieService movieService;

    /**
     * Tạo phim mới.
     * Yêu cầu quyền: MOVIE_CREATE
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('MOVIE_CREATE')")
    public ApiResponse<MovieResponse> createMovie(@Valid @RequestBody MovieCreationRequest request) {
        return ApiResponse.<MovieResponse>builder()
                .code(1000)
                .message("Movie created successfully")
                .result(movieService.createMovie(request))
                .build();
    }

    /**
     * Lấy danh sách phim (không phân biệt trạng thái).
     * Yêu cầu quyền: MOVIE_VIEW
     */
    @GetMapping
    public ApiResponse<Page<MovieResponse>> getAllMovies(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @RequestParam(required = false) MovieStatus status) {
        
        Page<MovieResponse> page = (status != null) 
                ? movieService.getMoviesByStatus(status, pageable)
                : movieService.getAllMovies(pageable);
                
        return ApiResponse.<Page<MovieResponse>>builder()
                .code(1000)
                .result(page)
                .build();
    }

    /**
     * Lấy chi tiết 1 phim.
     * Yêu cầu quyền: MOVIE_VIEW
     */
    @GetMapping("/{id}")
    public ApiResponse<MovieResponse> getMovieById(@PathVariable UUID id) {
        return ApiResponse.<MovieResponse>builder()
                .code(1000)
                .result(movieService.getMovieById(id))
                .build();
    }

    /**
     * Cập nhật thông tin phim.
     * Yêu cầu quyền: MOVIE_UPDATE
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MOVIE_UPDATE')")
    public ApiResponse<MovieResponse> updateMovie(
            @PathVariable UUID id,
            @Valid @RequestBody MovieUpdateRequest request) {
        return ApiResponse.<MovieResponse>builder()
                .code(1000)
                .message("Movie updated successfully")
                .result(movieService.updateMovie(id, request))
                .build();
    }

    /**
     * Xoá mềm phim.
     * Yêu cầu quyền: MOVIE_DELETE
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MOVIE_DELETE')")
    public ApiResponse<Void> deleteMovie(@PathVariable UUID id) {
        movieService.deleteMovie(id);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Movie deleted successfully")
                .build();
    }
}
