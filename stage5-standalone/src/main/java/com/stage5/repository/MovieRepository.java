package com.stage5.repository;
import com.stage5.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    @Query("SELECT m.category, COUNT(m) FROM Movie m GROUP BY m.category ORDER BY COUNT(m) DESC")
    List<Object[]> countByCategory();
}