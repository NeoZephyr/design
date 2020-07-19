分库分表后，每个节点只保存部分的数据，这样可以有效地减少单个数据库节点和单个数据表中存储的数据量，在解决了数据存储瓶颈的同时也能有效的提升数据查询的性能

同时，因为数据被分配到多个数据库节点上，数据的写入请求也从请求单一主库变成了请求多个数据分片节点，在一定程度上也会提升并发写入的性能

## 数据库垂直拆分
垂直拆分的原则一般是按照业务类型来拆分，核心思想是专库专用，将业务耦合度比较高的表拆分到单独的库中。这样可以把不同的业务的数据分拆到不同的数据库节点上，一旦数据库发生故障时只会影响到某一个模块的功能，不会影响到整体功能，从而实现了数据层面的故障隔离


## 水平拆分
水平拆分指的是将单一数据表按照某一种规则拆分到多个数据库和多个数据表中，关注点在数据的特点

拆分的规则有下面这两种：

### 字段哈希值拆分
这种拆分规则比较适用于实体表，比如说用户表，内容表。一般按照这些实体表的 id 字段来拆分

### 字段区间拆分
比较常用的是时间字段

使用这种拆分规则后，数据表要提前建立好


## 分库分表问题
1. 引入分区键，也就是我们对数据库做分库分表所依据的字段

之后所有的查询都需要带上这个字段，才能找到数据所在的库和表，否则就只能向所有的数据库和数据表发送查询命令

针对这个问题，通常有一些相应的解决思路：
在用户库中我们使用 id 作为分区键，这时如果需要按照昵称来查询用户时，可以建立一个昵称和 id 的映射表，先通过昵称查询到 id，再通过 id 查询完整的数据

2. 一些数据库的特性在实现时可能变得很困难。比如说多表的 join，count 等