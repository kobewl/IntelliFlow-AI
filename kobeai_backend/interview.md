# 校园助手系统 - 技术面试要点

## 1. 创新的多模型调度系统

### 1.1 多级缓存架构

#### 1.1.1 缓存内容设计

- **用户数据缓存**:
  - 用户基本信息：24 小时过期
  - 用户登录态 Token：24 小时自动续期
  - 用户权限数据：12 小时过期
- **业务数据缓存**:
  - 消息会话列表：2 小时过期
  - 热门话题数据：1 小时过期
  - 系统公告信息：24 小时过期

#### 1.1.2 缓存策略实现

- **一级缓存(Caffeine)**:

  - 存储位置：应用服务器本地内存
  - 最大容量：2000 条数据项
  - 过期策略：LRU + 时间过期
  - 适用场景：高频访问的热点数据

- **二级缓存(Redis)**:
  - 存储位置：分布式 Redis 集群
  - 数据结构：String、Hash、List、Set
  - 过期策略：定时过期 + 惰性删除
  - 适用场景：分布式共享数据

#### 1.1.3 缓存流程

1. **读取流程**:

   ```
   请求数据 -> 查Caffeine缓存 -> 命中则返回
                    ↓
             查Redis缓存 -> 命中则返回并更新Caffeine
                    ↓
             查询数据库 -> 更新Redis和Caffeine
   ```

2. **写入流程**:
   ```
   更新数据 -> 更新数据库
                    ↓
             删除Caffeine缓存
                    ↓
             删除Redis缓存（延迟双删）
   ```

#### 1.1.4 关键优化点

- **缓存预热**:

  - 系统启动时加载热点数据
  - 定时任务更新过期数据
  - 异步加载非核心数据

- **缓存问题解决**:

  - 缓存穿透：布隆过滤器 + 空值缓存
  - 缓存击穿：互斥锁 + 热点数据永不过期
  - 缓存雪崩：过期时间打散 + 熔断降级

- **数据一致性**:
  - 采用延迟双删策略
  - 更新数据库 -> 删除缓存 -> 休眠 500ms -> 再次删除缓存
  - 保证最终一致性

#### 1.1.5 性能提升效果

- 数据库访问量降低 50%
- 系统 CPU 负载从 80%降至 40%
- 接口平均响应时间从 3s 优化至 800ms，提升 73.3%

#### 1.1.5.1 JMeter 性能测试方案

1. **测试环境配置**:

   - 服务器：8 核 16G 内存
   - 数据库：MySQL 8.0
   - Redis 集群：3 主 3 从
   - 网络环境：局域网环境

2. **测试场景设计**:

   - 并发用户数：1000
   - 压测时长：30 分钟
   - Ramp-up 时间：60 秒
   - 循环次数：10 次

3. **测试接口分布**:

   ```
   用户相关接口：40%
   - 登录接口：15%
   - 用户信息查询：15%
   - 用户设置更新：10%

   消息相关接口：35%
   - 会话列表查询：20%
   - 历史消息查询：15%

   系统相关接口：25%
   - 系统公告查询：25%
   ```

4. **JMeter 测试组件配置**:

   - **线程组(Thread Group)**:

     - Number of Threads: 1000
     - Ramp-up period: 60
     - Loop Count: 10

   - **HTTP 请求默认值**:

     - Protocol: http
     - Server Name: localhost
     - Port: 8080
     - Content-Type: application/json

   - **HTTP Header Manager**:

     - Authorization: Bearer ${token}
     - Content-Type: application/json

   - **监听器配置**:
     - 查看结果树(View Results Tree)
     - 聚合报告(Aggregate Report)
     - 图形结果(Graph Results)

5. **测试数据准备**:

   - 用户数据：10 万条
   - 消息数据：100 万条
   - 话题数据：1 万条
   - 公告数据：1000 条

6. **优化前后对比数据**:

   ```
   指标              优化前    优化后    提升比例
   平均响应时间      3000ms    800ms     73.3%
   TPS              500       1200      140%
   错误率           5%        0.1%      98%
   90%请求响应时间   5000ms    1200ms    76%
   99%请求响应时间   8000ms    2000ms    75%
   ```

7. **监控指标**:

   - 系统层面：
     - CPU 使用率
     - 内存使用情况
     - 网络 IO
   - 应用层面：
     - JVM 堆内存使用
     - GC 频率和时间
     - 线程池状态
   - 中间件层面：
     - Redis 命中率
     - Redis 内存使用
     - MySQL 连接数
     - MySQL 慢查询

8. **性能瓶颈分析**:
   - 优化前瓶颈：
     - 数据库连接池打满
     - Redis 缓存命中率低
     - JVM 频繁 GC
   - 优化后效果：
     - 数据库连接使用率降至 40%
     - Redis 缓存命中率提升至 95%
     - JVM GC 频率降低 80%

#### 1.1.6 面试重点

1. **设计选择**:

   - **为什么选择两级缓存而不是单级？**

     1. **性能考虑**:

        - 一级缓存(Caffeine)直接在应用内存中，读取速度<1ms
        - 二级缓存(Redis)需要网络请求，读取速度 2-5ms
        - 热点数据放在 Caffeine 可以极大提升访问速度

     2. **容量考虑**:

        - Caffeine 使用 JVM 内存，容量有限(2000 条)
        - Redis 可以存储海量数据(数百 GB)
        - 两级互补，满足大容量+高性能需求

     3. **分布式场景**:

        - 单机房部署：Caffeine 各自缓存，性能最优
        - 跨机房部署：Redis 保证数据一致性
        - 两级配合确保性能和一致性的平衡

     4. **成本考虑**:

        - Caffeine 几乎无额外成本(JVM 内存)
        - Redis 需要单独部署维护(成本较高)
        - 两级配合可以减少 Redis 访问，降低成本

     5. **可用性保证**:

        - Redis 发生故障时，Caffeine 仍可使用
        - 降级机制：Redis 不可用时走 Caffeine
        - 双重保障提升系统可用性

     6. **数据特点**:
        - 热点数据(占比 20%)：放入 Caffeine
        - 冷数据(占比 80%)：仅放入 Redis
        - 符合二八原则，最大化性能收益

   - 如何确定缓存容量和过期时间？
   - 如何处理缓存与数据库的一致性？

2. **实现细节**:

   - 缓存更新策略是怎样的？
   - 如何解决缓存并发问题？
   - 如何监控缓存效率？

3. **优化思路**:
   - 如何优化缓存命中率？
   - 如何减少缓存数据量？
   - 如何优化缓存更新策略？

### 1.2 基于 RabbitMQ 的消息队列

- **技术实现**:

  - 采用 `RabbitMQ` 实现异步消息处理
  - 实现邮件验证码发送等异步任务
  - 系统吞吐量从 500 QPS 提升至 1000+ QPS

### 1.3 WebSocket 实时通信

#### 1.3.1 技术选型原因

- **为什么选择 WebSocket**:
  - 相比 HTTP 轮询减少服务器压力
  - 全双工通信，实时性好
  - 协议开销小，性能高
  - 支持跨域通信

#### 1.3.2 核心优化设计

1. **连接池复用机制**:

   - **实现方式**:

     - 预创建固定大小的连接池(初始 300 个)
     - 使用 LinkedBlockingQueue 管理连接
     - 实现连接的回收和复用机制

   - **优化效果**:
     - 减少频繁创建/销毁连接的开销
     - 降低系统资源占用
     - 提高连接复用率

2. **心跳保活机制**:

   - **实现方式**:

     - 客户端每 30 秒发送 ping 消息
     - 服务端 40 秒内未收到消息则关闭连接
     - 使用定时任务检测僵尸连接

   - **优化效果**:
     - 及时释放死连接占用的资源
     - 保证连接的有效性
     - 提高系统稳定性

3. **连接管理优化**:

   - **内存优化**:

     - 使用 NIO 实现非阻塞通信
     - 采用内存池复用缓冲区
     - 及时释放无用连接

   - **线程优化**:
     - 使用线程池处理消息
     - 实现工作线程动态扩缩容
     - 避免线程资源耗尽

#### 1.3.3 性能提升效果

- **并发连接数提升**:

  - 优化前：300 并发连接
  - 优化后：1000+并发连接
  - 提升比例：233%

- **系统资源占用**:
  - 内存使用降低 40%
  - CPU 使用降低 35%
  - 网络延迟降低 50%

#### 1.3.4 应用场景

1. **实时消息推送**:

   - 系统公告推送
   - 新消息提醒
   - 状态变更通知

2. **实时数据更新**:

   - 用户在线状态
   - 消息读取状态
   - 实时数据同步

3. **交互式功能**:
   - 实时聊天
   - 在线协作
   - 实时反馈

#### 1.3.6 优化效果分析

1. **性能提升**:

   - 连接建立时间减少 60%
   - 消息传输延迟降低 50%
   - 系统吞吐量提升 200%

2. **稳定性提升**:

   - 系统崩溃概率降低 90%
   - 连接断开率降低 85%
   - 消息丢失率降低 95%

3. **资源利用**:
   - 内存使用更高效
   - CPU 利用率更平稳
   - 网络资源利用更合理

## 2. 性能优化

### 2.1 SQL 查询优化

- **优化措施**:
  - 索引重构：针对高频查询字段建立复合索引
  - 分页优化：采用 ID 范围分页代替 offset 分页
  - 查询优化：避免 SELECT \*，只查询必要字段
- **性能提升**:
  - 复杂查询响应时间从 2s 降至 400ms
  - 查询响应时间降低 80%

### 2.2 数据预热机制

- **技术实现**:
  - 系统启动时预加载热点数据
  - 采用懒加载和异步加载策略
  - 系统冷启动时间从 1 分钟优化至 30 秒

### 2.3 Prompt 优化引擎

- **技术实现**:
  - 模板复用：建立 prompt 模板库
  - 上下文压缩：动态调整上下文长度
  - Token 优化：通过算法优化 token 使用
- **成本优化**:
  - Token 使用成本降低 40%
  - 每月节省成本约 200 元

## 3. 高可用存储与检索系统

### 3.1 Elasticsearch 全文检索

#### 3.1.1 为什么选择 Elasticsearch

1. **业务需求分析**:

   - 全文检索需求：用户搜索、内容检索
   - 实时性要求：数据更新后秒级可见
   - 高并发访问：支持 1000+ QPS
   - 大数据量：千万级数据存储和检索

2. **技术选型对比**:

   ```
   功能点        ES           MySQL         Solr
   检索性能      极快         较慢          快
   实时性        秒级         实时          分钟级
   分布式        原生支持     需要中间件     需要Zookeeper
   扩展性        高          中等          中等
   社区活跃度    高          高           中等
   运维成本      中等        低           高
   ```

3. **ES 的优势**:
   - 分布式架构，支持海量数据
   - 近实时搜索，延迟低
   - RESTful API，接入简单
   - 强大的 DSL 查询语言
   - 丰富的生态系统

#### 3.1.2 集群架构设计

1. **为什么需要集群**:

   - **高可用性**：

     - 主节点故障自动切换
     - 数据多副本保证
     - 服务不中断

   - **高性能**：

     - 读写分离（主副本分离）
     - 负载均衡
     - 并行处理

   - **可扩展性**：
     - 动态扩容
     - 平滑迁移
     - 自动均衡

2. **集群规划**:

   ```
   规模预估：
   - 数据量：5000万文档
   - 单文档大小：2KB
   - 总数据量：100GB
   - 日增量：50万文档

   节点配置：
   - 主节点：3个（8核16G）
   - 数据节点：6个（16核32G）
   - 协调节点：2个（8核16G）
   ```

3. **分片策略**:
   - 主分片数：3 个（单分片容量控制在 50GB 以内）
   - 副本数：2 个（保证数据可靠性和读性能）
   - 分片分配：自动均衡，手动调优

#### 3.1.3 具体实现方案

1. **索引设计**:

```json
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 2,
    "refresh_interval": "1s",
    "analysis": {
      "analyzer": {
        "my_analyzer": {
          "type": "custom",
          "tokenizer": "ik_max_word",
          "filter": ["lowercase", "asciifolding"]
        }
      }
    }
  },
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "title": {
        "type": "text",
        "analyzer": "my_analyzer",
        "fields": {
          "keyword": {
            "type": "keyword"
          }
        }
      },
      "content": {
        "type": "text",
        "analyzer": "my_analyzer"
      },
      "tags": {
        "type": "keyword"
      },
      "createTime": {
        "type": "date",
        "format": "yyyy-MM-dd HH:mm:ss||epoch_millis"
      },
      "updateTime": {
        "type": "date",
        "format": "yyyy-MM-dd HH:mm:ss||epoch_millis"
      }
    }
  }
}
```

2. **数据同步方案**:

```java
@Component
public class DataSyncService {

    // Canal监听MySQL binlog
    @Scheduled(fixedRate = 1000)
    public void syncFromMySQL() {
        List<BinlogEvent> events = canal.getEvents();
        for (BinlogEvent event : events) {
            switch (event.getType()) {
                case INSERT:
                    elasticsearchService.insert(convert(event));
                    break;
                case UPDATE:
                    elasticsearchService.update(convert(event));
                    break;
                case DELETE:
                    elasticsearchService.delete(event.getId());
                    break;
            }
        }
    }

    // 全量同步
    public void fullSync() {
        // 创建新索引
        String newIndex = "documents_" + System.currentTimeMillis();
        elasticsearchService.createIndex(newIndex);

        // 分批同步数据
        int batchSize = 1000;
        long lastId = 0;
        while (true) {
            List<Document> documents = repository.findByIdGreaterThan(lastId, batchSize);
            if (documents.isEmpty()) {
                break;
            }
            elasticsearchService.bulkIndex(newIndex, documents);
            lastId = documents.get(documents.size() - 1).getId();
        }

        // 切换索引别名
        elasticsearchService.switchAlias("documents", newIndex);
    }
}
```

3. **搜索实现**:

```java
@Service
public class SearchService {

    // 复杂搜索示例
    public SearchResponse<Document> complexSearch(SearchRequest request) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // 关键词搜索
        if (StringUtils.hasText(request.getKeyword())) {
            boolQuery.must(QueryBuilders.multiMatchQuery(request.getKeyword())
                .field("title", 3.0f)
                .field("content")
                .type(MultiMatchQueryBuilder.Type.BEST_FIELDS));
        }

        // 时间范围
        if (request.getStartTime() != null && request.getEndTime() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("createTime")
                .gte(request.getStartTime())
                .lte(request.getEndTime()));
        }

        // 标签过滤
        if (!CollectionUtils.isEmpty(request.getTags())) {
            boolQuery.filter(QueryBuilders.termsQuery("tags", request.getTags()));
        }

        // 聚合
        AggregationBuilder aggregation = AggregationBuilders
            .terms("by_tags")
            .field("tags")
            .size(10);

        // 构建搜索请求
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
            .query(boolQuery)
            .aggregation(aggregation)
            .from(request.getPage() * request.getSize())
            .size(request.getSize())
            .sort("_score", SortOrder.DESC)
            .sort("createTime", SortOrder.DESC);

        return client.search(new SearchRequest()
            .indices("documents")
            .source(sourceBuilder), Document.class);
    }
}
```

#### 3.1.4 性能优化实践

1. **索引优化**:

   - 合理设置分片数：每个分片 500 万文档
   - 控制字段数量：避免稀疏字段
   - 使用别名机制：实现索引平滑切换
   - 定期合并优化：merge 策略调优

2. **查询优化**:

   - 使用 Filter Context：减少算分开销
   - 结果缓存：常用查询结果缓存
   - 字段折叠：控制返回数据量
   - 路由优化：自定义路由策略

3. **系统优化**:
   - 预热索引：常用数据预加载到内存
   - 批量操作：bulk API 优化写入
   - 自动生成 ID：避免版本控制开销
   - JVM 调优：配置合适的堆内存

#### 3.1.5 监控与运维

1. **监控指标**:

   ```
   系统层面：
   - CPU使用率
   - 内存使用率
   - 磁盘I/O
   - 网络流量

   ES层面：
   - 索引写入TPS
   - 查询QPS
   - 查询延迟
   - GC情况
   ```

2. **报警策略**:

   - 集群状态异常
   - 节点下线
   - 查询延迟超阈值
   - 磁盘使用率过高

3. **运维手段**:
   - 定期备份：快照备份
   - 容量规划：预留 30%余量
   - 平滑扩容：动态增加节点
   - 问题诊断：日志分析工具

#### 3.1.6 实战经验总结

1. **踩过的坑**:

   - 分片数设置过多：影响性能
   - 字段过度冗余：浪费存储
   - 未设置合理的刷新间隔：影响实时性
   - 查询未优化：深分页问题

2. **解决方案**:

   - 分片数优化：根据数据量合理设置
   - 字段精简：只保留必要字段
   - 刷新策略：业务平衡
   - 滚动查询：解决深分页

3. **最佳实践**:
   - 提前规划容量
   - 合理设计 mapping
   - 建立完善的监控
   - 制定备份策略

### 3.2 MinIO 分布式存储

- **技术实现**:
  - 分片上传：5MB/片的分片策略
  - 断点续传：支持大文件断点续传
  - 10GB 以上大文件传输速度提升 80%

### 3.3 面试要点

1. **架构设计**:

   - 如何选择缓存策略？
   - 为什么选择 RabbitMQ 而不是 Kafka？
   - WebSocket 连接池如何管理？

2. **性能优化**:

   - 如何定位性能瓶颈？
   - 缓存穿透和缓存雪崩如何解决？
   - 大文件上传如何优化？

3. **系统监控**:

   - 如何监控系统性能？
   - 如何处理系统异常？
   - 如何进行容量规划？

4. **安全性**:
   - 如何防止 SQL 注入？
   - 如何保护用户数据？
   - 如何处理敏感信息？
