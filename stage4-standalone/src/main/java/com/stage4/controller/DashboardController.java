package com.stage4.controller;

import com.stage4.model.HotMovie;
import com.stage4.service.HotMovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class DashboardController {

    @Autowired
    private HotMovieService hotMovieService;

    @GetMapping("/hot-movies")
    public Map<String, Object> getHotMovies(@RequestParam(defaultValue = "10") int topN) {
        List<HotMovie> movies = hotMovieService.getHotMovies(topN);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", movies);
        result.put("total", movies.size());
        result.put("updateTime", hotMovieService.getLastUpdateTime());
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    @PostMapping("/click")
    public Map<String, Object> addClick(@RequestBody Map<String, String> body) {
        String movieId = body.get("movieId");
        if (movieId == null || movieId.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("code", 400);
            err.put("message", "movieId required");
            return err;
        }
        hotMovieService.manualClick(movieId.trim());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "click recorded for movie " + movieId);
        return result;
    }

    @GetMapping("/movies")
    public Map<String, Object> getMovies() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", hotMovieService.getSupportedMovies());
        return result;
    }

    /** 在IDEA中修改kafka_click_logs.csv后，调用此接口重载数据 */
    @GetMapping("/reload")
    public Map<String, Object> reloadData() {
        hotMovieService.reloadFromCsv();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "CSV data reloaded, new events added to click stream");
        return result;
    }
}