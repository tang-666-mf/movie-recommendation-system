package com.movie.controller;

import com.movie.model.HotMovie;
import com.movie.service.HotMovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 实时大屏数据接口
 * 对应原 stage4_dashboard 的功能
 */
@RestController
@RequestMapping("/api/dashboard")
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
        result.put("timestamp", System.currentTimeMillis());
        result.put("updateTime", hotMovieService.getLastUpdateTime());
        return result;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
