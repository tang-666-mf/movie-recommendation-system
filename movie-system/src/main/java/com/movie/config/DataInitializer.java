package com.movie.config;

import com.movie.model.Movie;
import com.movie.model.Rating;
import com.movie.repository.MovieRepository;
import com.movie.repository.RatingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private RatingRepository ratingRepository;
    @Autowired(required = false)
    private com.movie.service.RecommendService recommendService;

    @Override
    public void run(String... args) throws Exception {
        if (movieRepository.count() > 0) {
            System.out.println("[Init] Data already loaded, skipping.");
            return;
        }

        System.out.println("[Init] Loading movies.csv...");
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ClassPathResource("data/movies.csv").getInputStream(), StandardCharsets.UTF_8))) {
            String line = br.readLine(); // header
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    Movie m = new Movie(Long.parseLong(parts[0].trim()), parts[1].trim(), parts[2].trim());
                    movieRepository.save(m);
                }
            }
        }
        System.out.println("[Init] Loaded " + movieRepository.count() + " movies.");

        System.out.println("[Init] Loading ratings.csv...");
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ClassPathResource("data/ratings.csv").getInputStream(), StandardCharsets.UTF_8))) {
            String line = br.readLine(); // header
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    Rating r = new Rating(
                        Long.parseLong(parts[0].trim()),
                        Long.parseLong(parts[1].trim()),
                        Double.parseDouble(parts[2].trim()),
                        Long.parseLong(parts[3].trim())
                    );
                    ratingRepository.save(r);
                }
            }
        }
        System.out.println("[Init] Loaded " + ratingRepository.count() + " ratings.");
        System.out.println("[Init] Data initialization complete.");
        if (recommendService != null) {
            recommendService.train();
        }
    }
}
