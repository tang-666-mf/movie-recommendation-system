package com.movie.controller;

import com.movie.model.MovieRecommendation;
import com.movie.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 推荐系统接口
 */
@Controller
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    @GetMapping("/")
    public String index() {
        return "recommend";
    }

    @GetMapping("/recommend")
    public String recommendPage() {
        return "redirect:/recommend.html";
    }

    @GetMapping("/dashboard")
    public String dashboardPage() {
        return "redirect:/dashboard.html";
    }

    @GetMapping("/api/recommend")
    @ResponseBody
    public Map<String, Object> getRecommendations(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<MovieRecommendation> recs = recommendService.getRecommendations(userId);
            result.put("code", 200);
            result.put("data", recs);
            result.put("userId", userId);
            result.put("total", recs.size());
            result.put("message", "success");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询推荐失败: " + e.getMessage());
            result.put("data", new ArrayList<>());
        }
        return result;
    }
}
