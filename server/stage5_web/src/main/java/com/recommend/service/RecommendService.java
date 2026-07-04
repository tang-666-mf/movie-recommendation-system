package com.recommend.service;

import com.recommend.model.MovieRecommendation;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 推荐查询服务 - 从HBase检索推荐结果
 */
@Service
public class RecommendService {

    @Autowired
    private Connection hbaseConnection;

    private static final byte[] TABLE_NAME = Bytes.toBytes("movie_reco");
    private static final byte[] FAMILY = Bytes.toBytes("info");

    /**
     * 根据UserID获取推荐电影列表
     * 从HBase的 movie_reco 表中读取推荐数据
     */
    public List<MovieRecommendation> getRecommendations(String userId) throws Exception {
        List<MovieRecommendation> recommendations = new ArrayList<>();

        Table table = hbaseConnection.getTable(TableName.valueOf(TABLE_NAME));
        Get get = new Get(Bytes.toBytes(userId));

        Result result = table.get(get);
        if (result.isEmpty()) {
            table.close();
            return recommendations;
        }

        // 解析HBase中的推荐数据
        // 列命名规范: rec_movieId_{id}, title_{id}, rating_{id}
        for (int i = 1; i <= 3; i++) {
            byte[] movieIdBytes = result.getValue(FAMILY, Bytes.toBytes("rec_movieId_" + i));
            byte[] titleBytes = result.getValue(FAMILY, Bytes.toBytes("title_" + i));
            byte[] ratingBytes = result.getValue(FAMILY, Bytes.toBytes("rating_" + i));

            if (movieIdBytes != null && titleBytes != null && ratingBytes != null) {
                MovieRecommendation rec = new MovieRecommendation();
                rec.setMovieId(Bytes.toString(movieIdBytes));
                rec.setTitle(Bytes.toString(titleBytes));
                rec.setPredictedRating(Double.parseDouble(Bytes.toString(ratingBytes)));
                recommendations.add(rec);
            }
        }

        table.close();

        // 按预测评分降序排列
        recommendations.sort((a, b) -> Double.compare(b.getPredictedRating(), a.getPredictedRating()));
        return recommendations;
    }
}
