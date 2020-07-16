## v1
```java
public class Idempotence {
    private JedisCluster jedisCluster;

    public Idempotence(String redisClusterAddress, GenericObjectPoolConfig config) {
        String[] addressArray= redisClusterAddress.split(";");
        Set<HostAndPort> redisNodes = new HashSet<>();

        for (String address : addressArray) {
            String[] hostAndPort = address.split(":");
            redisNodes.add(
                new HostAndPort(hostAndPort[0], Integer.valueOf(hostAndPort))
            );
        }
        this.jedisCluster = new JedisCluster(redisNodes, config);
    }

    public String genId() {
        return UUID.randomUUID().toString();
    }

    public boolean saveIfAbsent(String idempotenceId) {
        Long success = jedisCluster.setnx(idempotenceId, "1");
        return success == 1;
    }

    public void delete(String idempotenceId) {
        jedisCluster.del(idempotenceId);
    }
}
```


## v2
```java
public class Idempotence {
    private IdempotenceStorage storage;

    public Idempotence(IdempotenceStorage storage) {
        this.storage = storage;
    }

    public boolean saveIfAbsent(String idempotenceId) {
        return storage.saveIfAbsent(idempotenceId);
    }

    public void delete(String idempotenceId) {
        storage.delete(idempotenceId);
    }
}
```
```java
public class IdempotenceIdGenerator {
    public String generateId() {
        return UUID.randomUUID().toString();
    }
}
```
```java
public interface IdempotenceStorage {
    boolean saveIfAbsent(String idempotenceId);
    void delete(String idempotenceId);
}
```
```java
public class RedisClusterIdempotenceStorage implements IdempotenceStorage {
    private JedisCluster jedisCluster;

    public RedisIdempotenceStorage(String redisClusterAddress, GenericObjectPoolConfig config) {
        Set<HostAndPort> redisNodes = parseHostAndPorts(redisClusterAddress);
        this.jedisCluster = new JedisCluster(redisNodes, config);
    }

    public RedisIdempotenceStorage(JedisCluster jedisCluster) {
        this.jedisCluster = jedisCluster;
    }

    /**
     * Save {@idempotenceId} into storage if it does not exist.
     * @param idempotenceId the idempotence ID
     * @return true if the {@idempotenceId} is saved, otherwise return false
     */
    public boolean saveIfAbsent(String idempotenceId) {
        Long success = jedisCluster.setnx(idempotenceId, "1");
        return success == 1;
    }

    public void delete(String idempotenceId) {
        jedisCluster.del(idempotenceId);
    }

    protected Set<HostAndPort> parseHostAndPorts(String redisClusterAddress) {
        String[] addressArray= redisClusterAddress.split(";");
        Set<HostAndPort> redisNodes = new HashSet<>();

        for (String address : addressArray) {
            String[] hostAndPort = address.split(":");
            redisNodes.add(
                new HostAndPort(hostAndPort[0], Integer.valueOf(hostAndPort))
            );
        }

        return redisNodes;
    }
}
```