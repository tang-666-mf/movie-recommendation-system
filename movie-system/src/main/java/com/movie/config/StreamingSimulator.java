package com.movie.config;

import com.movie.service.HotMovieService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * 模拟实时点击数据流 (对应Kafka + Spark Structured Streaming)
 * 每2秒产生一批随机电影点击事件
 */
@Component
public class StreamingSimulator {

    private static final Logger log = LoggerFactory.getLogger(StreamingSimulator.class);
    private static final String[] MOVIE_IDS = {"101", "102", "103", "104", "105", "106", "107", "108"};
    private static final Random RANDOM = new Random();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private HotMovieService hotMovieService;

    private long eventCounter = 0;

    @PostConstruct
    public void init() {
        log.info("[Streaming] Real-time click stream simulator started (2s interval)");
    }

    @Scheduled(fixedRate = 2000)
    public void generateClickEvents() {
        // 每次模拟3-8个点击事件
        int count = 3 + RANDOM.nextInt(6);
        for (int i = 0; i < count; i++) {
            String movieId = MOVIE_IDS[RANDOM.nextInt(MOVIE_IDS.length)];
            String timestamp = LocalDateTime.now().format(FMT);
            hotMovieService.recordClick(movieId, timestamp);
            eventCounter++;
        }
        log.debug("[Streaming] Generated {} click events (total: {})", count, eventCounter);
    }
}
