package com.stage4.service;

import com.stage4.model.HotMovie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@Service
@Lazy(false)
public class HotMovieService {

    private static final Logger log = LoggerFactory.getLogger(HotMovieService.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long WINDOW_MS = 10000;
    private static final long SLIDE_MS = 5000;

    /** CSV文件最后修改时间戳，用于自动检测修改 */
    private long csvLastModified = 0;

    /** CSV文件最后修改时间戳（用于自动检测文件变化） */

    private final ConcurrentLinkedDeque<ClickEvent> clickEvents = new ConcurrentLinkedDeque<>();
    private final Map<String, String> movieTitleMap = createMovieMap();

    private static Map<String, String> createMovieMap() {
        Map<String, String> m = new HashMap<>();
        m.put("101", "流浪地球 (The Wandering Earth)");
        m.put("102", "给阿嫲的情书");
        m.put("103", "楚门的世界 (The Truman Show)");
        m.put("104", "泰坦尼克号 (Titanic)");
        m.put("105", "阿凡达 (Avatar)");
        m.put("106", "盗梦空间 (Inception)");
        m.put("107", "霸王别姬 (Farewell My Concubine)");
        m.put("108", "喜剧之王 (The King of Comedy)");
        return m;
    }

    @PostConstruct
    public void init() {
        // loadHistoricalData() — startup loading disabled
        // recordCsvTimestamp() removed - auto CSV detection disabled
        recordCsvTimestamp();
        Thread t = new Thread(this::windowComputationLoop, "window-calc");
        t.start();
        log.info("[Stage4] Window started: {}ms window, {}ms slide", WINDOW_MS, SLIDE_MS);
    }

    private void loadHistoricalData() {
        try {
            java.io.InputStream is = null;
            try { is = new java.io.FileInputStream("src/main/resources/data/kafka_click_logs.csv"); } catch (Exception e) {}
            if (is == null) is = getClass().getClassLoader().getResourceAsStream("data/kafka_click_logs.csv");
            java.util.Scanner sc = new java.util.Scanner(is, "UTF-8");
            sc.nextLine();
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;
                String[] p = line.split(",");
                if (p.length >= 1 && !p[0].trim().isEmpty()) recordClick(p[0].trim(), LocalDateTime.now().format(FMT));
            }
            sc.close();
            log.info("[Stage4] Loaded historical click data from CSV");
        } catch (Exception e) {
            log.warn("[Stage4] Could not load historical data: {}", e.getMessage());
        }
    }

    public void recordClick(String movieId, String timestamp) {
        clickEvents.addLast(new ClickEvent(movieId, timestamp));
        long cutoff = System.currentTimeMillis() - 60000;
        while (!clickEvents.isEmpty() && clickEvents.peekFirst().timestampMs < cutoff)
            clickEvents.pollFirst();
    }

    private void windowComputationLoop() {
        while (true) {
            try {
                Thread.sleep(SLIDE_MS);
                checkCsvReload();
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
        for (ClickEvent event : clickEvents)
            if (event.timestampMs >= windowStart && event.timestampMs <= now)
                countMap.merge(event.movieId, 1L, Long::sum);
        if (countMap.isEmpty()) return;

        List<HotMovie> movies = new ArrayList<>();
        int[] idx = {1};
        countMap.entrySet().stream()
            .sorted((a,b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(10).forEachOrdered(e -> movies.add(
                new HotMovie(e.getKey(), movieTitleMap.getOrDefault(e.getKey(), "未知"), e.getValue(), idx[0]++)));
        currentHotMovies = movies;
        lastUpdateTime = LocalDateTime.now().format(FMT);
    }

    public List<HotMovie> getHotMovies(int topN) {
        return currentHotMovies.stream().limit(topN).collect(Collectors.toList());
    }

    public String getLastUpdateTime() { return lastUpdateTime; }

    public void manualClick(String movieId) {
        recordClick(movieId, LocalDateTime.now().format(FMT));
    }

    /** 在IDEA中修改 kafka_click_logs.csv 后，调用此接口重新加载 */
    public void reloadFromCsv() {
        long before = clickEvents.size();
        loadHistoricalData();
        long after = clickEvents.size();
        log.info("[Stage4] CSV reloaded: {} new events added (total: {})", after - before, after);
    }


    /** 自动检测CSV文件是否已修改，若修改则重载数据 */

    public Map<String, String> getSupportedMovies() {
        return new HashMap<>(movieTitleMap);
    }


    /** 自动检测CSV文件修改，有变化则重载数据 */
    private void checkCsvReload() {
        try {
            java.io.File f = new java.io.File("src/main/resources/data/kafka_click_logs.csv");
            if (!f.isFile()) return;
            long t = f.lastModified();
            if (t > csvLastModified) {
                csvLastModified = t;
                long before = clickEvents.size();
                loadHistoricalData();
                computeHotMovies();
                log.info("[Stage4] CSV changed, auto-reloaded, +{} events (total: {})", clickEvents.size() - before, clickEvents.size());
            }
        } catch (Exception e) {
            log.warn("[Stage4] CSV check: {}", e.getMessage());
        }
    }

    /** 启动时记录CSV当前修改时间，避免误触发 */
    private void recordCsvTimestamp() {
        try {
            java.io.File f = new java.io.File("src/main/resources/data/kafka_click_logs.csv");
            if (f.isFile()) csvLastModified = f.lastModified();
        } catch (Exception e) {}
    }
    private static class ClickEvent {
        final String movieId;
        final long timestampMs;
        ClickEvent(String movieId, String timestampStr) {
            this.movieId = movieId;
            long ts;
            try { ts = LocalDateTime.parse(timestampStr, FMT).atZone(java.time.ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli(); }
            catch (Exception e) { ts = System.currentTimeMillis(); }
            this.timestampMs = ts;
        }
    }
}
