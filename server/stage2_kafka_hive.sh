#!/bin/bash
# ============================================================
# 阶段2: 数据流转与离线数仓映射
# Kafka生产消费测试 + Hive映射HBase外部表
# ============================================================

echo "===== 阶段2: Kafka + Hive ====="

# 1. Kafka 控制台消费测试 (后台运行)
echo ">>> 启动Kafka消费者 (后台)..."
/usr/local/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic movie_logs \
  --from-beginning &
CONSUMER_PID=$!
echo "消费者PID: $CONSUMER_PID"

# 2. Kafka 控制台生产测试 (发送示例数据)
echo ">>> 发送示例点击数据到 movie_logs..."
/usr/local/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic movie_logs <<EOF
101,2024-06-28 10:00:01
101,2024-06-28 10:00:02
102,2024-06-28 10:00:03
105,2024-06-28 10:00:05
101,2024-06-28 10:00:06
105,2024-06-28 10:00:07
102,2024-06-28 10:00:08
101,2024-06-28 10:00:09
EOF
sleep 3

# 3. 停止消费者
kill $CONSUMER_PID 2>/dev/null
echo ">>> Kafka 消费端测试完成, 请截图展示消费到的数据"

# 4. Hive 映射 HBase 外部表
echo ">>> Hive 创建外部表映射 HBase movie_reco..."
/usr/local/hive/bin/hive -e "
CREATE EXTERNAL TABLE IF NOT EXISTS movie_reco_hbase (
    user_id STRING,
    rec_movies STRING
)
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES ('hbase.columns.mapping' = ':key,info:recommendations')
TBLPROPERTIES ('hbase.table.name' = 'movie_reco');
"

echo ">>> Hive 验证表结构:"
/usr/local/hive/bin/hive -e "DESCRIBE movie_reco_hbase;"
/usr/local/hive/bin/hive -e "SHOW TABLES;"

echo "===== 阶段2 完成 ====="
echo "请截图: ① Kafka消费端展示实时数据  ② Hive建表结果"
