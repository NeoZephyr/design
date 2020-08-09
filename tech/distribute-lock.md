## 数据库实现
查询的时候防止幻读

```sql
select id from order where order_no= 'xxxx' for update;
```
```java
@Transactional
public int addOrderRecord(Order order) {
    if (orderDao.selectOrderRecord(order) == null) {
        int result = orderDao.addOrderRecord(order);

        if (result > 0) {
            return 1;
        }
    }

    return 0;
}
```

在 RR 事务级别，select 的 for update 操作是基于间隙锁实现的，这是一种悲观锁的实现方式，存在阻塞问题。在高并发情况下，大部分的请求都会进行排队等待。为了保证数据库的稳定性，事务的超时时间往往又设置得很小，就会出现大量事务被中断的情况


## Zookeeper 实现
```java
InterProcessMutex lock = new InterProcessMutex(client, lockPath);

if (lock.acquire(maxWait, waitUnit)) {
    try {

    } finally {
        lock.release();
    }
}
```

Zookeeper 是集群实现，可以避免单点问题，且能保证每次操作都可以有效地释放锁，这是因为一旦应用服务挂掉了，临时节点会因为 session 连接断开而自动删除掉

如果频繁地创建和删除结点，加上大量的 Watch 事件，对 Zookeeper 集群来说，压力非常大


## Redis 实现
```java
Long result = jedis.setnx(lockKey, requestId);

// 获取锁成功
if (result == 1) {
    // 如果程序突然崩溃，则无法设置过期时间，将发生死锁

    // 通过过期时间删除锁
    jedis.expire(lockKey, expireTime);
    return true;
}

return false;
```

```java
jedis.set(lockKey, requestId, "NX", "PX", expireTime);
```

也可以通过 Lua 脚本来实现锁的设置和过期时间的原子性，再通过 jedis.eval() 方法运行脚本

```lua
// 加锁脚本
private static final String SCRIPT_LOCK = "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then redis.call('pexpire', KEYS[1], ARGV[2]) return 1 else return 0 end";

// 解锁脚本
private static final String SCRIPT_UNLOCK = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
```

虽然上面的方法保证了设置锁和过期时间的原子性，但如果设置的过期时间比较短，而执行业务时间比较长，就会存在锁代码块失效的问题。因此需要将过期时间设置得足够长，来保证以上问题不会出现。但如果是在 Redis 集群环境下，由于 Redis 集群数据同步到各个节点时是异步的，当在 Master 节点获取到锁后，还没有同步到其它节点时，Master 节点崩溃了，此时新的 Master 节点依然可以获取锁，所以多个应用服务可以同时获取到锁


## Redlock
Redisson 中实现了 Redis 分布式锁，且支持单点模式和集群模式。在集群模式下，Redisson 使用了 Redlock 算法，避免在 Master 节点崩溃切换到另外一个 Master 时，多个应用同时获得锁

每次获取锁都有超时时间，如果请求超时，则认为该节点不可用。当应用服务成功获取锁的 Redis 节点超过半数时，并且获取锁消耗的实际时间不超过锁的过期时间，则获取锁成功。一旦获取锁成功，就会重新计算释放锁的时间，该时间是由原来释放锁的时间减去获取锁所消耗的时间；而如果获取锁失败，客户端依然会释放获取锁成功的节点

```xml
<dependency>
      <groupId>org.redisson</groupId>
      <artifactId>redisson</artifactId>
      <version>3.8.2</version>
</dependency>
```

```java
@Bean
public RedissonClient redissonClient() {
    Config config = new Config();
    config.useClusterServers()
            .setScanInterval(2000) // 集群状态扫描间隔时间，单位是毫秒
            .addNodeAddress("redis://127.0.0.1:7000").setPassword("1")
            .addNodeAddress("redis://127.0.0.1:7001").setPassword("1")
            .addNodeAddress("redis://127.0.0.1:7002").setPassword("1");
    return Redisson.create(config);
}
```

```java
long waitTimeout = 10;
long leaseTime = 1;
RLock lock1 = redissonClient1.getLock("lock1");
RLock lock2 = redissonClient2.getLock("lock2");
RLock lock3 = redissonClient3.getLock("lock3");
 
RedissonRedLock redLock = new RedissonRedLock(lock1, lock2, lock3);

// 同时加锁：lock1 lock2 lock3
// 在大部分节点上加锁成功就算成功，且设置总超时时间以及单个节点超时时间
redLock.trylock(waitTimeout, leaseTime, TimeUnit.SECONDS);

redLock.unlock();
```