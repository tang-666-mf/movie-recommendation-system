package com.movie.algorithm;

import com.movie.model.Rating;
import java.util.*;

/**
 * 纯Java实现的ALS协同过滤算法
 * 对应Spark MLlib ALS, 无需Spark集群即可运行
 */
public class AlsEngine {

    private final int rank;
    private final int iterations;
    private final double regParam;
    private final Random random;

    public AlsEngine(int rank, int iterations, double regParam) {
        this.rank = rank;
        this.iterations = iterations;
        this.regParam = regParam;
        this.random = new Random(42L);
    }

    /**
     * 训练ALS模型, 返回每个用户的前topN推荐
     */
    public Map<Long, List<MovieScore>> train(List<Rating> ratings, int topN) {
        // 构建用户和物品索引
        Set<Long> userIds = new HashSet<>();
        Set<Long> movieIds = new HashSet<>();
        for (Rating r : ratings) {
            userIds.add(r.getUserId());
            movieIds.add(r.getMovieId());
        }
        List<Long> userList = new ArrayList<>(userIds);
        List<Long> movieList = new ArrayList<>(movieIds);
        Collections.sort(userList);
        Collections.sort(movieList);

        Map<Long, Integer> userIndex = new HashMap<>();
        Map<Long, Integer> movieIndex = new HashMap<>();
        for (int i = 0; i < userList.size(); i++) userIndex.put(userList.get(i), i);
        for (int i = 0; i < movieList.size(); i++) movieIndex.put(movieList.get(i), i);
        Map<Integer, Long> indexToMovie = new HashMap<>();
        for (Map.Entry<Long, Integer> e : movieIndex.entrySet()) indexToMovie.put(e.getValue(), e.getKey());

        int numUsers = userList.size();
        int numMovies = movieList.size();

        // 初始化用户和物品特征矩阵
        double[][] userFeatures = new double[numUsers][rank];
        double[][] movieFeatures = new double[numMovies][rank];
        for (int u = 0; u < numUsers; u++)
            for (int f = 0; f < rank; f++)
                userFeatures[u][f] = random.nextDouble() * 0.1;
        for (int m = 0; m < numMovies; m++)
            for (int f = 0; f < rank; f++)
                movieFeatures[m][f] = random.nextDouble() * 0.1;

        // 构建用户评分列表
        Map<Integer, List<int[]>> userRatings = new HashMap<>();
        for (Rating r : ratings) {
            int u = userIndex.get(r.getUserId());
            int m = movieIndex.get(r.getMovieId());
            userRatings.computeIfAbsent(u, k -> new ArrayList<>()).add(new int[]{m, (int)(r.getRating() * 100)});
        }

        // 构建物品评分列表
        Map<Integer, List<int[]>> movieRatings = new HashMap<>();
        for (Rating r : ratings) {
            int u = userIndex.get(r.getUserId());
            int m = movieIndex.get(r.getMovieId());
            movieRatings.computeIfAbsent(m, k -> new ArrayList<>()).add(new int[]{u, (int)(r.getRating() * 100)});
        }

        // 迭代训练
        for (int it = 0; it < iterations; it++) {
            // 固定物品特征, 求解用户特征
            for (int u = 0; u < numUsers; u++) {
                solveLeastSquares(userFeatures[u], movieFeatures, userRatings.getOrDefault(u, Collections.emptyList()), regParam);
            }
            // 固定用户特征, 求解物品特征
            for (int m = 0; m < numMovies; m++) {
                solveLeastSquares(movieFeatures[m], userFeatures, movieRatings.getOrDefault(m, Collections.emptyList()), regParam);
            }
            if ((it + 1) % 3 == 0) {
                System.out.println("  ALS iter " + (it + 1) + "/" + iterations);
            }
        }

        // 生成推荐结果
        Map<Long, List<MovieScore>> recommendations = new HashMap<>();
        // 每个用户已评分的电影
        Map<Long, Set<Long>> ratedByUser = new HashMap<>();
        for (Rating r : ratings) {
            ratedByUser.computeIfAbsent(r.getUserId(), k -> new HashSet<>()).add(r.getMovieId());
        }

        for (Long uid : userList) {
            int u = userIndex.get(uid);
            Set<Long> rated = ratedByUser.getOrDefault(uid, Collections.emptySet());
            List<MovieScore> scores = new ArrayList<>();

            for (Long mid : movieList) {
                if (rated.contains(mid)) continue;
                int m = movieIndex.get(mid);
                double pred = 0;
                for (int f = 0; f < rank; f++) {
                    pred += userFeatures[u][f] * movieFeatures[m][f];
                }
                scores.add(new MovieScore(mid, Math.min(5.0, Math.max(0.5, pred))));
            }

            scores.sort((a, b) -> Double.compare(b.score, a.score));
            recommendations.put(uid, scores.subList(0, Math.min(topN, scores.size())));
        }

        return recommendations;
    }

    private void solveLeastSquares(double[] targetVec, double[][] fixedMat, List<int[]> ratingList, double reg) {
        int r = targetVec.length;
        double[][] A = new double[r][r];
        double[] b = new double[r];

        for (int[] rl : ratingList) {
            int idx = rl[0];
            double value = rl[1] / 100.0;
            double[] feat = fixedMat[idx];
            for (int i = 0; i < r; i++) {
                b[i] += feat[i] * value;
                for (int j = 0; j < r; j++) {
                    A[i][j] += feat[i] * feat[j];
                }
            }
        }
        for (int i = 0; i < r; i++) A[i][i] += reg;

        // 高斯消元求解
        int n = r;
        double[][] aug = new double[n][n + 1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, aug[i], 0, n);
            aug[i][n] = b[i];
        }

        for (int col = 0; col < n; col++) {
            int maxRow = col;
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(aug[row][col]) > Math.abs(aug[maxRow][col])) maxRow = row;
            }
            double[] temp = aug[col]; aug[col] = aug[maxRow]; aug[maxRow] = temp;

            double pivot = aug[col][col];
            if (Math.abs(pivot) < 1e-10) continue;
            for (int j = col; j <= n; j++) aug[col][j] /= pivot;

            for (int row = 0; row < n; row++) {
                if (row != col) {
                    double factor = aug[row][col];
                    for (int j = col; j <= n; j++) aug[row][j] -= factor * aug[col][j];
                }
            }
        }

        for (int i = 0; i < n; i++) targetVec[i] = aug[i][n];
    }

    public static class MovieScore {
        private final long movieId;
        private final double score;

        public MovieScore(long movieId, double score) {
            this.movieId = movieId;
            this.score = score;
        }

        public long getMovieId() { return movieId; }
        public double getScore() { return score; }
    }
}
