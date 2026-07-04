package com.realtime.service;

import com.realtime.model.HotMovie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 实时热门电影服务 - 从MySQL读取流计算聚合结果
 */
@Service
public class HotMovieService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 电影ID -> 标题映射
    private static final Map<String, String> MOVIE_MAP = new HashMap<>();
    static {
        MOVIE_MAP.put("101", "流浪地球 (The Wandering Earth)");
        MOVIE_MAP.put("102", "给阿嫲的情书");
        MOVIE_MAP.put("103", "楚门的世界 (The Truman Show)");
        MOVIE_MAP.put("104", "泰坦尼克号 (Titanic)");
        MOVIE_MAP.put("105", "阿凡达 (Avatar)");
        MOVIE_MAP.put("106", "盗梦空间 (Inception)");
        MOVIE_MAP.put("107", "霸王别姬 (Farewell My Concubine)");
        MOVIE_MAP.put("108", "喜剧之王 (The King of Comedy)");
    }

    /**
     * 获取当前热门电影排行榜 (Top 10)
     */
    public List<HotMovie> getHotMovies(int topN) {
        String sql = "SELECT movieId, window_start, window_end, click_count " +
                     "FROM hot_movies " +
                     "ORDER BY window_start DESC, click_count DESC " +
                     "LIMIT ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, topN);
        List<HotMovie> hotMovies = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> row : rows) {
            HotMovie movie = new HotMovie();
            movie.setMovieId((String) row.get("movieId"));
            movie.setClickCount(((Number) row.get("click_count")).longValue());
            movie.setWindowStart(String.valueOf(row.get("window_start")));
            movie.setWindowEnd(String.valueOf(row.get("window_end")));
            movie.setRank(rank++);
            movie.setTitle(getMovieTitle(movie.getMovieId()));
            hotMovies.add(movie);
        }
        return hotMovies;
    }

    private String getMovieTitle(String movieId) {
        return MOVIE_MAP.getOrDefault(movieId, "未知电影 (Unknown)");
    }
}
