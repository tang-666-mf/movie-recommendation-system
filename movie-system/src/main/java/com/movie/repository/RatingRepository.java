package com.movie.repository;

import com.movie.model.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByUserId(Long userId);

    @Query("SELECT r.movieId, AVG(r.rating), COUNT(r) FROM Rating r GROUP BY r.movieId ORDER BY AVG(r.rating) DESC")
    List<Object[]> avgRatingPerMovie();

    @Query("SELECT r.userId, COUNT(r), AVG(r.rating) FROM Rating r GROUP BY r.userId ORDER BY COUNT(r) DESC")
    List<Object[]> userRatingStats();
}
