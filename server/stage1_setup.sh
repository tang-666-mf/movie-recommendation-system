#!/bin/bash
# ============================================================
# 阶段1: 大数据底层存储与消息总线搭建
# 启动ZooKeeper、Hadoop、Kafka、HBase集群
# ============================================================

echo "===== 阶段1: 集群搭建 ====="

# 1. 启动ZooKeeper
echo ">>> 启动ZooKeeper..."
/usr/local/zookeeper/bin/zkServer.sh start
sleep 3
echo ">>> ZooKeeper 状态:"
/usr/local/zookeeper/bin/zkServer.sh status

# 2. 启动Hadoop集群 (HDFS + YARN)
echo ">>> 启动Hadoop HDFS..."
/usr/local/hadoop/sbin/start-dfs.sh
sleep 3
echo ">>> 启动Hadoop YARN..."
/usr/local/hadoop/sbin/start-yarn.sh
sleep 3
echo ">>> Hadoop 进程检查:"
jps | grep -E "NameNode|DataNode|ResourceManager|NodeManager"

# 3. 启动Kafka
echo ">>> 启动Kafka..."
/usr/local/kafka/bin/kafka-server-start.sh -daemon /usr/local/kafka/config/server.properties
sleep 5
echo ">>> 创建Kafka Topic: movie_logs (3分区, 2副本)..."
/usr/local/kafka/bin/kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --replication-factor 2 \
  --partitions 3 \
  --topic movie_logs
echo ">>> Kafka Topic 列表:"
/usr/local/kafka/bin/kafka-topics.sh --list --bootstrap-server localhost:9092
echo ">>> Topic 详情:"
/usr/local/kafka/bin/kafka-topics.sh --describe --bootstrap-server localhost:9092 --topic movie_logs

# 4. 启动HBase
echo ">>> 启动HBase..."
/usr/local/hbase/bin/start-hbase.sh
sleep 5
echo ">>> 创建HBase表: movie_reco (列族 info)..."
/usr/local/hbase/bin/hbase shell <<EOF
create 'movie_reco', 'info'
list 'movie_reco'
describe 'movie_reco'
exit
EOF

echo "===== 阶段1 完成 ====="
echo "Kafka Topic: movie_logs (3分区, 2副本)"
echo "HBase Table: movie_reco (列族: info)"
echo "请截图: ① Kafka Topic列表  ② HBase Master Web UI "
