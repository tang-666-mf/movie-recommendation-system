package com.stage5.service;
import com.stage5.algorithm.AlsEngine;
import com.stage5.model.MovieRecommendation;
import com.stage5.model.Rating;
import com.stage5.repository.MovieRepository;
import com.stage5.repository.RatingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class RecommendService {
    private static final Logger log = LoggerFactory.getLogger(RecommendService.class);
    @Autowired private RatingRepository ratingRepository;
    @Autowired private MovieRepository movieRepository;
    private Map<Long, List<AlsEngine.MovieScore>> recommendations = new HashMap<>();
    private volatile boolean trained = false;
    private final Map<Long, String> movieTitleMap = new HashMap<>();
    private final Map<Long, String> movieCategoryMap = new HashMap<>();

    public void train() {
        loadMovieMaps();
        try {
            List<Rating> ratings = ratingRepository.findAll();
            log.info("[ALS] Training with {} ratings, {} users, {} movies",
                ratings.size(), ratings.stream().map(Rating::getUserId).distinct().count(),
                ratings.stream().map(Rating::getMovieId).distinct().count());
            if (ratings.isEmpty()) { log.warn("[ALS] No ratings to train"); return; }
            AlsEngine als = new AlsEngine(10, 10, 0.1);
            recommendations = als.train(ratings, 3);
            trained = true;
            log.info("[ALS] Complete! Generated recs for {} users", recommendations.size());
        } catch (Exception e) { log.error("[ALS] Training failed", e); }
    }

    public List<MovieRecommendation> getRecommendations(Long userId) {
        if (!trained || recommendations.isEmpty()) return getFallbackRecommendations(userId);
        List<AlsEngine.MovieScore> scores = recommendations.get(userId);
        if (scores == null || scores.isEmpty()) return getFallbackRecommendations(userId);
        return scores.stream().map(s -> {
            MovieRecommendation rec = new MovieRecommendation();
            rec.setMovieId(String.valueOf(s.getMovieId()));
            rec.setTitle(movieTitleMap.getOrDefault(s.getMovieId(), "Unknown"));
            rec.setCategory(movieCategoryMap.getOrDefault(s.getMovieId(), "Unknown"));
            rec.setPredictedRating(Math.round(s.getScore() * 100.0) / 100.0);
            return rec;
        }).collect(Collectors.toList());
    }

    private List<MovieRecommendation> getFallbackRecommendations(Long userId) {
        loadMovieMaps();
        List<MovieRecommendation> recs = new ArrayList<>();
        int count = 0;
        for (Object[] row : ratingRepository.avgRatingPerMovie()) {
            if (count >= 3) break;
            Long movieId = (Long) row[0]; Double avg = (Double) row[1];
            recs.add(new MovieRecommendation(String.valueOf(movieId),
                movieTitleMap.getOrDefault(movieId, "Unknown"),
                movieCategoryMap.getOrDefault(movieId, "Unknown"),
                Math.round(avg * 100.0) / 100.0));
            count++;
        }
        return recs;
    }

    private void loadMovieMaps() {
        if (movieTitleMap.isEmpty()) {
            movieRepository.findAll().forEach(m -> {
                movieTitleMap.put(m.getMovieId(), m.getTitle());
                movieCategoryMap.put(m.getMovieId(), m.getCategory());
            });
        }
    }
    public boolean isTrained() { return trained; }
}