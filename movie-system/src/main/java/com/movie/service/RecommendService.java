package com.movie.service;

import com.movie.algorithm.AlsEngine;
import com.movie.model.Movie;
import com.movie.model.MovieRecommendation;
import com.movie.model.Rating;
import com.movie.repository.MovieRepository;
import com.movie.repository.RatingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐服务: 使用纯Java ALS算法训练模型并生成推荐
 * 对应Spark MLlib ALS + HBase存储的完整流程
 */
@Service
public class RecommendService {

    private static final Logger log = LoggerFactory.getLogger(RecommendService.class);

    @Autowired
    private RatingRepository ratingRepository;
    @Autowired
    private MovieRepository movieRepository;

    // ALS训练结果缓存
    private Map<Long, List<AlsEngine.MovieScore>> recommendations = new HashMap<>();
    private volatile boolean trained = false;
    private final Map<Long, String> movieTitleMap = new HashMap<>();
    private final Map<Long, String> movieCategoryMap = new HashMap<>();

    public void init() {
        // Movie maps loaded lazily in train() after data is available
    }

    private void loadMovieMaps() {
        if (movieTitleMap.isEmpty()) {
            movieRepository.findAll().forEach(m -> {
                movieTitleMap.put(m.getMovieId(), m.getTitle());
                movieCategoryMap.put(m.getMovieId(), m.getCategory());
            });
        }
    }

    public void train() {
        try {
            log.info("[ALS] Starting ALS model training (rank=10, iter=10, reg=0.1)...");
            List<Rating> ratings = ratingRepository.findAll();
            loadMovieMaps();
            log.info("[ALS] Loaded {} ratings, {} users, {} movies",
                ratings.size(),
                ratings.stream().map(Rating::getUserId).distinct().count(),
                ratings.stream().map(Rating::getMovieId).distinct().count());

            AlsEngine als = new AlsEngine(10, 10, 0.1);
            recommendations = als.train(ratings, 3);
            trained = true;
            log.info("[ALS] Training complete! Generated recommendations for {} users", recommendations.size());
        } catch (Exception e) {
            log.error("[ALS] Training failed", e);
        }
    }

    public List<MovieRecommendation> getRecommendations(Long userId) {
        if (!trained || recommendations.isEmpty()) {
            return getFallbackRecommendations(userId);
        }

        List<AlsEngine.MovieScore> scores = recommendations.get(userId);
        if (scores == null || scores.isEmpty()) {
                return getFallbackRecommendations(userId);
        }

        if (scores == null || scores.isEmpty()) {
                return getFallbackRecommendations(userId);
        }

        return scores.stream().map(s -> {
            MovieRecommendation rec = new MovieRecommendation();
            rec.setMovieId(String.valueOf(s.getMovieId()));
            rec.setTitle(movieTitleMap.getOrDefault(s.getMovieId(), "未知电影"));
            rec.setCategory(movieCategoryMap.getOrDefault(s.getMovieId(), "Unknown"));
            rec.setPredictedRating(Math.round(s.getScore() * 100.0) / 100.0);
            return rec;
        }).collect(Collectors.toList());
    }

    /**
     * 当ALS模型尚未训练好时的备用推荐(基于电影平均评分)
     */
    private List<MovieRecommendation> getFallbackRecommendations(Long userId) {
        loadMovieMaps();
        List<Object[]> avgRatings = ratingRepository.avgRatingPerMovie();
        List<MovieRecommendation> recs = new ArrayList<>();
        int count = 0;
        for (Object[] row : avgRatings) {
            if (count >= 3) break;
            Long movieId = (Long) row[0];
            Double avgRating = (Double) row[1];
            MovieRecommendation rec = new MovieRecommendation();
            rec.setMovieId(String.valueOf(movieId));
            rec.setTitle(movieTitleMap.getOrDefault(movieId, "未知电影"));
            rec.setCategory(movieCategoryMap.getOrDefault(movieId, "Unknown"));
            rec.setPredictedRating(Math.round(avgRating * 100.0) / 100.0);
            recs.add(rec);
            count++;
        }
        return recs;
    }
}
