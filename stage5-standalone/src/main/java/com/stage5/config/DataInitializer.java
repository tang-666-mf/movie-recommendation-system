package com.stage5.config;
import com.stage5.model.*;
import com.stage5.repository.*;
import com.stage5.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired private MovieRepository movieRepository;
    @Autowired private RatingRepository ratingRepository;
    @Autowired(required = false) private RecommendService recommendService;
    @Override
    public void run(String... args) throws Exception {
        if (movieRepository.count() > 0) {
            System.out.println("[Stage5] Data already loaded, starting ALS training...");
            if (recommendService != null) recommendService.train();
            return;
        }
        System.out.println("[Stage5] Loading movies.csv...");
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ClassPathResource("data/movies.csv").getInputStream(), StandardCharsets.UTF_8))) {
            String line; br.readLine();
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",");
                if (p.length >= 3) movieRepository.save(new Movie(Long.parseLong(p[0].trim()), p[1].trim(), p[2].trim()));
            }
        }
        System.out.println("[Stage5] Loaded " + movieRepository.count() + " movies.");
        System.out.println("[Stage5] Loading ratings.csv...");
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ClassPathResource("data/ratings.csv").getInputStream(), StandardCharsets.UTF_8))) {
            String line; br.readLine();
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] p = line.split(",");
                if (p.length >= 4) ratingRepository.save(new Rating(
                    Long.parseLong(p[0].trim()), Long.parseLong(p[1].trim()),
                    Double.parseDouble(p[2].trim()), Long.parseLong(p[3].trim())));
            }
        }
        System.out.println("[Stage5] Loaded " + ratingRepository.count() + " ratings. Starting ALS...");
        if (recommendService != null) recommendService.train();
        System.out.println("[Stage5] Ready! http://localhost:8081/recommend.html");
    }
}