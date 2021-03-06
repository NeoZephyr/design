## 消息丢失
### 消息生产丢失
一旦发生抖动，消息就有可能因为网络的错误而丢失

发送超时后就将消息重新发一次。一般来说，如果不是消息队列发生故障，或者是到消息队列的网络断开，重试 2~3 次就可以了

### 消息队列中丢失
kafka 中，如果发生机器掉电或者机器异常重启，Page Cache 中还没有来得及刷盘的消息就会丢失

如果把刷盘的间隔设置很短，或者设置累积一条消息就就刷盘，会对性能有比较大的影响

可以考虑以集群方式部署 Kafka 服务，通过部署多个副本备份数据，保证消息尽量不丢失。

由于默认消息是异步地从 Leader 复制到 Follower 的，所以一旦 Leader 宕机，还没有来得及复制到 Follower 的消息还是会丢失。Kafka 为生产者提供了 acks 选项，当设置为 all 时，生产者发送的每一条消息除了发给 Leader 外还会发给所有的 ISR，并且必须得到 Leader 和所有 ISR 的确认后才被认为发送成功。这样，只有 Leader 和所有的 ISR 都挂了，消息才会丢失

1. 如果需要确保消息一条都不能丢失，建议不要开启消息队列的同步刷盘，而是使用集群的方式来解决，可以配置当所有 ISR Follower 都接收到消息才返回成功
2. 如果对消息的丢失有一定的容忍度，建议不部署集群，即使以集群方式部署，也建议配置只发送给一个 Follower 就返回成功

### 消费过程丢失
消费的过程分为三步：接收消息、处理消息、更新消费进度

一定要等到消息接收和处理完成后才能更新消费进度


## 幂等
### 消息生产
给每一个生产者一个唯一的 ID，并且为生产的每一条消息赋予一个唯一 ID，消息队列的服务端会存储生产者 ID 到最后一条消息 ID 的映射。当某一个生产者产生新的消息时，消息队列服务端会比对消息 ID 是否与存储的最后一条 ID 一致，如果一致，就 认为是重复的消息，服务端会自动丢弃

### 消息消费
在通用层面，可以在消息被生产的时候，使用发号器给它生成一个全局唯一的消息 ID，消息被处理之后，把这个 ID 存储在数据库中，在处理下一条消息之前，先从数据库里面查询这个全局 ID 是否被消费过，如果被消费过就放弃消费

如果消息在处理之后，还没有来得及写入数据库，消费者宕机了重启之后发现数据库中并没有这条消息，还是会重复执行两次消费逻辑，这时需要引入事务机制，保证消息处理和写入数据库必须同时成功或者同时失败，这样消息处理的成本就更高了

乐观锁方式
给数据中增加一个版本号的字段，在生产消息时先查询数据的版本号，并且将版本号连同消息一起发送给消息队列。消费端在拿到消息和版本号后，在执行操作的时候带上版本号



