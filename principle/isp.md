## 接口隔离原则
客户端不应该强迫依赖它不需要的接口

接口隔离原则跟单一职责原则有点类似，不过单一职责原则针对的是模块、类、接口的设计，而接口隔离原则相对于单一职责原则，一方面更侧重于接口的设计，另一方面它的思考的角度不同。它提供了一种判断接口是否职责单一的标准：通过调用者如何使用接口来间接地判定。如果调用者只使用部分接口或接口的部分功能，那接口的设计就不够职责单一

### API 接口集合
```java
public interface UserService {
    boolean register(String cellphone, String password);
    boolean login(String cellphone, String password);
    UserInfo getUserInfoById(long id);
    UserInfo getUserInfoByCellphone(String cellphone);
}

public class UserServiceImpl implements UserService {}
```

现在，后台管理系统要实现删除用户的功能，看上去只需要在 UserService 中新添加一个删除接口就可以了。但是，这样做隐藏了一些安全隐患：删除用户是一个非常慎重的操作，只能通过后台管理系统来执行，所以这个接口只限于给后台管理系统使用。如果放到 UserService 中，那所有使用到 UserService 的系统都可以调用这个接口，不加限制地被其他业务系统调用，就有可能导致误删用户。解决方案是：参照接口隔离原则，将删除接口单独放到另外一个接口 RestrictedUserService 中，然后将 RestrictedUserService 只打包提供给后台管理系统来使用

```java
public interface RestrictedUserService {
    boolean deleteUserByCellphone(String cellphone);
    boolean deleteUserById(long id);
}

public class UserServiceImpl implements UserService, RestrictedUserService {}
```

### 单个 API 接口
```java
public class Statistics {
    private Long max;
    private Long min;
    private Long average;
    private Long sum;
    private Long percentile99;
    private Long percentile999;
}

public Statistics count(Collection<Long> dataSet) {
    Statistics statistics = new Statistics();
    // ...
    return statistics;
}
```

count() 函数的功能不够单一，包含很多不同的统计功能。可以把 count() 函数拆成如下几个更小粒度的函数，每个函数负责一个独立的统计功能
```java
public Long max(Collection<Long> dataSet) {}
public Long min(Collection<Long> dataSet) {}
public Long average(Colletion<Long> dataSet) {}
```

### OOP 中的接口
```java
public class RedisConfig {
    private ConfigSource configSource;
    private String address;
    private int timeout;
    private int maxTotal;

    public RedisConfig(ConfigSource configSource) {
        this.configSource = configSource;
    }

    public String getAddress() {
        return this.address;
    }

    // 从 configSource 加载配置到 address/timeout/maxTotal...
    public void update() {}
}
```

添加新的功能，支持 Redis 和 Kafka 配置信息的热更新
```java
public interface Updater {
    void update();
}

public class RedisConfig implements Updater {

    @Override
    public void update() {}
}
```
```java
public class ScheduledUpdater {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private long initialDelayInSeconds;
    private long periodInSeconds;
    private Updater updater;

    public ScheduleUpdater(Updater updater, long initialDelayInSeconds, long periodInSeconds) {
        this.updater = updater;
        this.initialDelayInSeconds = initialDelayInSeconds;
        this.periodInSeconds = periodInSeconds;
    }

    public void run() {
        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                updater.update();
            }
        }, this.initialDelayInSeconds, this.periodInSeconds, TimeUnit.SECONDS)
    }
}
```
```java
ConfigSource configSource = new ZookeeperConfigSource();
RedisConfig redisConfig = new RedisConfig(configSource);
ScheduledUpdater redisConfigUpdater = new ScheduledUpdater(redisConfig, 300, 10);
redisConfigUpdater.run();
```

添加新的功能，支持 Redis 和 MySQL 配置信息的查看
```java
public interface Viewer {
    String outputInPlainText();
    Map<String, String> output();
}

public class RedisConfig implements Updater {

    @Override
    public String outputInPlainText() {}

    @Override
    public Map<String, String> output() {}
}
```
```java
public class SimpleHttpServer {
    private String host;
    private int port;
    private Map<String, List<Viewer>> viewers = new HashMap<>();

    public SimpleHttpServer(String host, int port) {}

    public void addViewers(String urlDirectory, Viewer viewer) {
        if (!viewers.containsKey(urlDirectory)) {
            viewers.put(urlDirectory, new ArrayList<Viewer>());
        }

        this.viewers.get(urlDirectory).add(viewer);
    }

    public void run() {}
}
```
```java
ConfigSource configSource = new ZookeeperConfigSource();
RedisConfig redisConfig = new RedisConfig(configSource);
SimpleHttpServer simpleHttpServer = new SimpleHttpServer(“127.0.0.1”, 8080);
simpleHttpServer.addViewer("/config", redisConfig);
simpleHttpServer.addViewer("/config", mysqlConfig);
simpleHttpServer.run();
```

通过这个设计可以看出：ScheduledUpdater 只依赖 Updater 这个跟热更新相关的接口，不需要被强迫去依赖不需要的 Viewer 接口；SimpleHttpServer 只依赖跟查看信息相关的 Viewer 接口，不依赖不 需要的 Updater 接口，满足接口隔离原则

这样做便于扩展、复用。比如有一个新的需求，开发一个 Metrics 性能统计模块，并且希望将 Metrics 也通过 SimpleHttpServer 显示在网页上，只需呀进行如下操作：
```java
public class ApiMetrics implements Viewer {}
```

