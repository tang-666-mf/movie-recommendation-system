package com.movie.controller;

import com.movie.model.Movie;
import com.movie.service.OfflineAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 离线分析接口
 * 对应原 stage3 Spark SQL 查询功能
 */
@RestController
@RequestMapping("/api/analysis")
public class OfflineAnalysisController {

    @Autowired
    private OfflineAnalysisService analysisService;

    @GetMapping("/category-stats")
    public Map<String, Object> categoryStats() {
        return wrapResult(analysisService.categoryStats());
    }

    @GetMapping("/movie-avg-ratings")
    public Map<String, Object> movieAvgRatings() {
        return wrapResult(analysisService.movieAvgRatings());
    }

    @GetMapping("/category-avg-ratings")
    public Map<String, Object> categoryAvgRatings() {
        return wrapResult(analysisService.categoryAvgRatings());
    }

    @GetMapping("/user-activity")
    public Map<String, Object> userActivity() {
        return wrapResult(analysisService.userActivity());
    }

    @GetMapping("/movies-by-category")
    public Map<String, Object> moviesByCategory(@RequestParam(defaultValue = "Sci-Fi") String category) {
        List<Movie> movies = analysisService.getMoviesByCategory(category);
        return wrapResult(movies);
    }

    private Map<String, Object> wrapResult(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", data);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
