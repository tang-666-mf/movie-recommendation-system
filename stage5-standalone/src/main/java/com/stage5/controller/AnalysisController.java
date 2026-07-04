package com.stage5.controller;
import com.stage5.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {
    @Autowired private AnalysisService analysisService;
    @GetMapping("/category-stats") public Map<String,Object> catStats() { return r(analysisService.categoryStats()); }
    @GetMapping("/movie-avg-ratings") public Map<String,Object> movieAvg() { return r(analysisService.movieAvgRatings()); }
    @GetMapping("/category-avg-ratings") public Map<String,Object> catAvg() { return r(analysisService.categoryAvgRatings()); }
    @GetMapping("/user-activity") public Map<String,Object> userAct() { return r(analysisService.userActivity()); }
    private Map<String,Object> r(Object d) { Map<String,Object> m=new HashMap<>(); m.put("code",200); m.put("data",d); m.put("timestamp",System.currentTimeMillis()); return m; }
}