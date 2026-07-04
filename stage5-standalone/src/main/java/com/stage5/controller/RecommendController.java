package com.stage5.controller;
import com.stage5.model.MovieRecommendation;
import com.stage5.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@Controller
public class RecommendController {
    @Autowired private RecommendService recommendService;
    @GetMapping("/") public String index() { return "redirect:/recommend.html"; }
    @GetMapping("/api/recommend") @ResponseBody
    public Map<String, Object> getRecommendations(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<MovieRecommendation> recs = recommendService.getRecommendations(userId);
            result.put("code", 200); result.put("data", recs); result.put("userId", userId);
            result.put("total", recs.size()); result.put("message", "success");
            result.put("modelStatus", recommendService.isTrained() ? "ALS模型已训练" : "使用评分均值");
        } catch (Exception e) {
            result.put("code", 500); result.put("message", "失败: " + e.getMessage());
            result.put("data", new ArrayList<>());
        }
        return result;
    }
}