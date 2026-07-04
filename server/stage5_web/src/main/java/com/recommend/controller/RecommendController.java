package com.recommend.controller;

import com.recommend.model.MovieRecommendation;
import com.recommend.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 推荐Web控制器
 */
@Controller
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    // 电影ID到名称的映射
    private static final Map<String, String> MOVIE_NAMES = new HashMap<>();
    static {
        MOVIE_NAMES.put("101", "流浪地球 (The Wandering Earth)");
        MOVIE_NAMES.put("102", "给阿嫲的情书");
        MOVIE_NAMES.put("103", "楚门的世界 (The Truman Show)");
        MOVIE_NAMES.put("104", "泰坦尼克号 (Titanic)");
        MOVIE_NAMES.put("105", "阿凡达 (Avatar)");
        MOVIE_NAMES.put("106", "盗梦空间 (Inception)");
        MOVIE_NAMES.put("107", "霸王别姬 (Farewell My Concubine)");
        MOVIE_NAMES.put("108", "喜剧之王 (The King of Comedy)");
    }

    private static final Map<String, String> MOVIE_CATEGORIES = new HashMap<>();
    static {
        MOVIE_CATEGORIES.put("101", "Sci-Fi"); MOVIE_CATEGORIES.put("102", "Sci-Fi");
        MOVIE_CATEGORIES.put("103", "Drama"); MOVIE_CATEGORIES.put("104", "Romance");
        MOVIE_CATEGORIES.put("105", "Sci-Fi"); MOVIE_CATEGORIES.put("106", "Sci-Fi");
        MOVIE_CATEGORIES.put("107", "Drama"); MOVIE_CATEGORIES.put("108", "Comedy");
    }

    /**
     * 推荐首页
     */
    @GetMapping("/")
    public String index() {
        return "recommend";
    }

    /**
     * REST API: 根据用户ID获取推荐
     * GET /api/recommend?userId=1
     */
    @GetMapping("/api/recommend")
    @ResponseBody
    public Map<String, Object> getRecommendations(@RequestParam String userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<MovieRecommendation> recs = recommendService.getRecommendations(userId);

            // 如果HBase中没有数据,使用模拟数据演示
            if (recs.isEmpty()) {
                recs = getDemoRecommendations(userId);
            }

            // 补全电影信息
            for (MovieRecommendation rec : recs) {
                if (rec.getTitle() == null) {
                    rec.setTitle(MOVIE_NAMES.getOrDefault(rec.getMovieId(), "未知电影"));
                }
                if (rec.getCategory() == null) {
                    rec.setCategory(MOVIE_CATEGORIES.getOrDefault(rec.getMovieId(), "Unknown"));
                }
            }

            result.put("code", 200);
            result.put("data", recs);
            result.put("userId", userId);
            result.put("message", "success");
        } catch (Exception e) {
            result.put("code", 500);
            result.put("message", "查询推荐失败: " + e.getMessage());
            result.put("data", new ArrayList<>());
        }
        return result;
    }

    /**
     * 演示用模拟数据 (当HBase不可用时)
     */
    private List<MovieRecommendation> getDemoRecommendations(String userId) {
        List<MovieRecommendation> demo = new ArrayList<>();
        // 根据用户ID生成个性化模拟推荐
        switch (userId) {
            case "1":
                demo.add(new MovieRecommendation("106", "盗梦空间 (Inception)", "Sci-Fi", 4.8));
                demo.add(new MovieRecommendation("101", "流浪地球 (The Wandering Earth)", "Sci-Fi", 4.5));
                demo.add(new MovieRecommendation("102", "给阿嫲的情书", "Sci-Fi", 4.0));
                break;
            case "2":
                demo.add(new MovieRecommendation("105", "阿凡达 (Avatar)", "Sci-Fi", 5.0));
                demo.add(new MovieRecommendation("101", "流浪地球 (The Wandering Earth)", "Sci-Fi", 4.7));
                demo.add(new MovieRecommendation("106", "盗梦空间 (Inception)", "Sci-Fi", 4.3));
                break;
            case "3":
                demo.add(new MovieRecommendation("103", "楚门的世界 (The Truman Show)", "Drama", 4.9));
                demo.add(new MovieRecommendation("104", "泰坦尼克号 (Titanic)", "Romance", 4.6));
                demo.add(new MovieRecommendation("102", "给阿嫲的情书", "Sci-Fi", 3.8));
                break;
            case "4":
                demo.add(new MovieRecommendation("101", "流浪地球 (The Wandering Earth)", "Sci-Fi", 4.6));
                demo.add(new MovieRecommendation("105", "阿凡达 (Avatar)", "Sci-Fi", 4.5));
                demo.add(new MovieRecommendation("106", "盗梦空间 (Inception)", "Sci-Fi", 3.9));
                break;
            case "100":
                demo.add(new MovieRecommendation("101", "流浪地球 (The Wandering Earth)", "Sci-Fi", 5.0));
                demo.add(new MovieRecommendation("105", "阿凡达 (Avatar)", "Sci-Fi", 4.6));
                demo.add(new MovieRecommendation("102", "给阿嫲的情书", "Sci-Fi", 3.2));
                break;
            default:
                // 新用户推荐最受欢迎的电影
                demo.add(new MovieRecommendation("101", "流浪地球 (The Wandering Earth)", "Sci-Fi", 4.5));
                demo.add(new MovieRecommendation("105", "阿凡达 (Avatar)", "Sci-Fi", 4.3));
                demo.add(new MovieRecommendation("103", "楚门的世界 (The Truman Show)", "Drama", 4.0));
                break;
        }
        return demo;
    }
}
