## 主从复制
MySQL 的主从复制就是将 binlog 中的数据从主库传输到从库上，一般这个过程是异步的。流程是这样的：
1. 主库创建一个 log dump 线程来发送 binlog 给从库
2. 从库创建一个 IO 线程，用以请求主库更新的 binlog，并且把接收到的 binlog 信息写入一个叫做 relay log 的日志文件中
3. 从库还会创建一个 SQL 线程读取 relay log 中的内容，并且在从库中做回放，最终实现主从的一致性

随着从库数量增加，从库连接上来的 IO 线程比较多，主库也需要创建同样多的 log dump 线程来处理复制的请求，对于主库资源消耗比较高，同时受限于主库的网络带宽，所以在实际使用中，一般一个主库最多挂 3～5 个从库

一般我们会把从库落后的时间作为一个重点的数据库指标做监控和报警，正常的时间是在毫秒级别，一旦落后的时间达到了秒级别就需要告警了


## 访问数据库
### TDDL
对数据源进行代理，每个数据源对应一个数据库，可能是主库，可能是从库。当有一个数据库请求时，中间件将 SQL 语句发给某一个指定的数据源来处理，然后将处理结果返回

这种方案简单易用，没有多余的部署成本。但是缺乏多语言的支持，而且版本升级也依赖使用方更新，比较困难

### 代理层
代理中间件部署在独立的服务器上，业务代码如同在使用单一数据库一样使用它，实际上它内部管理着很多的数据源，当有数据库请求时，它会对 SQL 语句做必要的改写，然后发往指定的数据源

代理层中间件可以很好地支持多语言。由于是独立部署的，比较方便进行维护升级。但是，所有的 SQL 语句都需要跨两次网络，在性能上会有一些损耗

