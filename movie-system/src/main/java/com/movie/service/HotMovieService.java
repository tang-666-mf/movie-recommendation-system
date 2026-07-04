package com.movie.service;

import com.movie.model.HotMovie;
import com.movie.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * 实时热门电影计算服务
 * 模拟Spark Structured Streaming的window算子(10s窗口,5s滑动)
 * 对应原 stage4_streaming 的 HotMovieStreaming.scala
 */
@Service
public class HotMovieService {

    private static final Logger log = LoggerFactory.getLogger(HotMovieService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long WINDOW_MS = 10000;
    private static final long SLIDE_MS = 5000;

    private final ConcurrentLinkedDeque<ClickEvent> clickEvents = new ConcurrentLinkedDeque<>();

    @Autowired
    private MovieRepository movieRepository;

    private final Map<String, String> movieTitleMap = new HashMap<>();

    @PostConstruct
    public void init() {
        Thread t = new Thread(this::windowComputationLoop, "window-calc");
        t.setDaemon(true);
        t.start();
        log.info("[HotMovie] Window={}ms Slide={}ms", WINDOW_MS, SLIDE_MS);
    }

    private synchronized void ensureMovieMap() {
        if (movieTitleMap.isEmpty()) {
            movieRepository.findAll().forEach(m ->
                movieTitleMap.put(String.valueOf(m.getMovieId()), m.getTitle()));
        }
    }

    public void recordClick(String movieId, String timestamp) {
        clickEvents.addLast(new ClickEvent(movieId, timestamp));
        long cutoff = System.currentTimeMillis() - 60000;
        while (!clickEvents.isEmpty() && clickEvents.peekFirst().timestampMs < cutoff) {
            clickEvents.pollFirst();
        }
    }

    private void windowComputationLoop() {
        while (true) {
            try {
                Thread.sleep(SLIDE_MS);
                computeHotMovies();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private volatile List<HotMovie> currentHotMovies = new ArrayList<>();
    private volatile String lastUpdateTime = null;

    private void computeHotMovies() {
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_MS;

        Map<String, Long> countMap = new HashMap<>();
        for (ClickEvent event : clickEvents) {
            if (event.timestampMs >= windowStart && event.timestampMs <= now) {
                countMap.merge(event.movieId, 1L, Long::sum);
            }
        }
        if (countMap.isEmpty()) return;

        List<Map.Entry<String, Long>> sorted = countMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .collect(Collectors.toList());

        List<HotMovie> movies = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<String, Long> entry : sorted) {
            HotMovie hm = new HotMovie();
            hm.setMovieId(entry.getKey());
            hm.setTitle(movieTitleMap.getOrDefault(entry.getKey(), "未知电影"));
            hm.setClickCount(entry.getValue());
            hm.setRank(rank++);
            movies.add(hm);
        }
        currentHotMovies = movies;
        lastUpdateTime = LocalDateTime.now().format(FMT);
    }

    public List<HotMovie> getHotMovies(int topN) {
        ensureMovieMap();
        return currentHotMovies.stream().limit(topN).collect(Collectors.toList());
    }

    public String getLastUpdateTime() {
        return lastUpdateTime;
    }

    private static class ClickEvent {
        final String movieId;
        final long timestampMs;

        ClickEvent(String movieId, String timestampStr) {
            this.movieId = movieId;
            long ts;
            try {
                LocalDateTime dt = LocalDateTime.parse(timestampStr, FMT);
                ts = dt.atZone(java.time.ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
            } catch (Exception e) {
                ts = System.currentTimeMillis();
            }
            this.timestampMs = ts;
        }
    }
}
