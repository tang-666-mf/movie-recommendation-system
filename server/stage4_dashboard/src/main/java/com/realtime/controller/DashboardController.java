package com.realtime.controller;

import com.realtime.model.HotMovie;
import com.realtime.service.HotMovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 大屏数据接口
 */
@RestController
@RequestMapping("/api")
public class DashboardController {

    @Autowired
    private HotMovieService hotMovieService;

    /**
     * GET /api/hot-movies?topN=10
     * 返回热门电影排行榜 (JSON)
     */
    @GetMapping("/hot-movies")
    public Map<String, Object> getHotMovies(@RequestParam(defaultValue = "10") int topN) {
        List<HotMovie> movies = hotMovieService.getHotMovies(topN);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", movies);
        result.put("total", movies.size());
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * GET /api/health
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
