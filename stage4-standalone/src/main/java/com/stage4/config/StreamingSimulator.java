package com.stage4.config;

import com.stage4.service.HotMovieService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * 模拟实时点击数据流（对应Kafka数据源）
 * 每2秒产生一批随机电影点击事件
 */
@Component
@Lazy(false)
public class StreamingSimulator {

    private static final Logger log = LoggerFactory.getLogger(StreamingSimulator.class);
    private static final String[] MOVIE_IDS = {"101","102","103","104","105","106","107","108"};
    private static final Random RANDOM = new Random();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private HotMovieService hotMovieService;

    @PostConstruct
    public void init() {
        log.info("[Stage4] Real-time streaming ON - generating click events every 2s");
    }

    @Scheduled(fixedRate = 2000)
    public void generateClickEvents() {
        int count = 3 + RANDOM.nextInt(6);
        for (int i = 0; i < count; i++) {
            hotMovieService.recordClick(
                MOVIE_IDS[RANDOM.nextInt(MOVIE_IDS.length)],
                LocalDateTime.now().format(FMT));
        }
    }
}