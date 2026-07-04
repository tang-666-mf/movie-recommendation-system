package com.movie.service;

import com.movie.model.Movie;
import com.movie.model.Rating;
import com.movie.repository.MovieRepository;
import com.movie.repository.RatingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 离线分析服务
 * 对应Spark SQL离线分析的各项查询功能
 */
@Service
public class OfflineAnalysisService {

    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private RatingRepository ratingRepository;

    /** 电影分类统计 */
    public List<Map<String, Object>> categoryStats() {
        List<Object[]> data = movieRepository.countByCategory();
        return data.stream().map(row -> {
            Map<String, Object> m = new HashMap<>();
            m.put("category", row[0]);
            m.put("count", row[1]);
            return m;
        }).collect(Collectors.toList());
    }

    /** 各电影平均评分 */
    public List<Map<String, Object>> movieAvgRatings() {
        List<Object[]> data = ratingRepository.avgRatingPerMovie();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : data) {
            Long movieId = (Long) row[0];
            Double avg = (Double) row[1];
            Long cnt = (Long) row[2];
            Optional<Movie> movie = movieRepository.findById(movieId);
            Map<String, Object> m = new HashMap<>();
            m.put("movieId", movieId);
            m.put("title", movie.map(Movie::getTitle).orElse("未知"));
            m.put("category", movie.map(Movie::getCategory).orElse("Unknown"));
            m.put("avgRating", Math.round(avg * 100.0) / 100.0);
            m.put("ratingCount", cnt);
            result.add(m);
        }
        result.sort((a, b) -> Double.compare((Double) b.get("avgRating"), (Double) a.get("avgRating")));
        return result;
    }

    /** 各分类平均评分 */
    public List<Map<String, Object>> categoryAvgRatings() {
        Map<String, List<Double>> catRatings = new HashMap<>();
        Map<String, Long> catCounts = new HashMap<>();

        for (Rating r : ratingRepository.findAll()) {
            Optional<Movie> movie = movieRepository.findById(r.getMovieId());
            if (movie.isPresent()) {
                String cat = movie.get().getCategory();
                catRatings.computeIfAbsent(cat, k -> new ArrayList<>()).add(r.getRating());
                catCounts.merge(cat, 1L, Long::sum);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Double>> e : catRatings.entrySet()) {
            double avg = e.getValue().stream().mapToDouble(d -> d).average().orElse(0);
            Map<String, Object> m = new HashMap<>();
            m.put("category", e.getKey());
            m.put("avgRating", Math.round(avg * 100.0) / 100.0);
            m.put("totalRatings", catCounts.get(e.getKey()));
            result.add(m);
        }
        result.sort((a, b) -> Double.compare((Double) b.get("avgRating"), (Double) a.get("avgRating")));
        return result;
    }

    /** 用户评分活跃度 */
    public List<Map<String, Object>> userActivity() {
        List<Object[]> data = ratingRepository.userRatingStats();
        return data.stream().map(row -> {
            Map<String, Object> m = new HashMap<>();
            m.put("userId", row[0]);
            m.put("ratingCount", row[1]);
            m.put("avgRating", Math.round((Double) row[2] * 100.0) / 100.0);
            return m;
        }).collect(Collectors.toList());
    }

    /** 按分类查询电影 */
    public List<Movie> getMoviesByCategory(String category) {
        return movieRepository.findAll().stream()
                .filter(m -> m.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }
}
