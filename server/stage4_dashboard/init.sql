-- 创建数据库
CREATE DATABASE IF NOT EXISTS movie_analysis
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE movie_analysis;

-- 实时热门电影表 (Structured Streaming写入)
CREATE TABLE IF NOT EXISTS hot_movies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    movieId VARCHAR(20) NOT NULL,
    window_start DATETIME NOT NULL,
    window_end DATETIME NOT NULL,
    click_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_window (window_start, click_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
