## v1
1. 接口类型，只支持 HTTP 接口的限流，暂时不支持 RPC 等其他类型的接口限流
2. 限流规则，只支持本地文件配置，配置文件格式只支持 YAML
3. 限流算法，只支持固定时间窗口算法
4. 限流模式，只支持单机限流

### 最小原型代码
划分职责识别类
定义属性和方法
定义类之间的交互关系
组装类并提供执行入口

RateLimiter 类串联整个限流流程
```java
public class RateLimiter {
    private static final Logger log = LoggerFactory
        .getLogger(RateLimiter.class);
    
    // 为每个 api 在内存中存储限流计数器
    private ConcurrentHashMap<String, RateLimitAlg> counters = new ConcurrentHashMap<>();
    private RateLimitRule rule;

    public RateLimiter() {
        // 将限流规则配置文件 ratelimiter-rule.yaml 中的内容读取到 RuleConfig 中
        InputStream in = null;
        RuleConfig ruleConfig = null;
        try {
            in = this.getClass().getResourceAsStream("/ratelimiter-rule.yaml");
            if (in != null) {
                Yaml yaml = new Yaml();
                ruleConfig = yaml.loadAs(in, RuleConfig.class);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error("close file error:{}", e);
                }
            }
        }

        // 将限流规则构建成支持快速查找的数据结构 RateLimitRule
        this.rule = new RateLimitRule(ruleConfig);
    }

    public boolean limit(String appId, String url) throws InternalErrorException {
        ApiLimit apiLimit = rule.getLimit(appId, url);
        if (apiLimit == null) {
            return true;
        }

        // 获取 api 对应在内存中的限流计数器
        String counterKey = appId + ":" + apiLimit.getApi();
        RateLimitAlg rateLimitCounter = counters.get(counterKey);

        if (rateLimitCounter == null) {
            RateLimitAlg newRateLimitCounter = new RateLimitAlg(apiLimit.getLimit());
            rateLimitCounter = counters.putIfAbsent(counterKey, newRateLimitCounter);
            if (rateLimitCounter == null) {
                rateLimitCounter = newRateLimitCounter;
            }
        }

        // 判断是否限流
        return rateLimitCounter.tryAcquire();
    }
}
```

RuleConfig、AppRuleConfig、ApiLimit 三个类跟文件的三层嵌套结构完全对应
```java
public class RuleConfig {
    private List<UniformRuleConfig> configs;

    public List<AppRuleConfig> getConfigs() {
        return configs;
    }

    public void setConfigs(List<AppRuleConfig> configs) {
        this.configs = configs;
    }

    public static class AppRuleConfig {
        private String appId;
        private List<ApiLimit> limits;

        public AppRuleConfig() {}

        public AppRuleConfig(String appId, List<ApiLimit> limits) {
            this.appId = appId;
            this.limits = limits;
        }
    }
}
```
```java
public class ApiLimit {
    private static final int DEFAULT_TIME_UNIT = 1; // 1 second
    private String api;
    private int limit;
    private int unit = DEFAULT_TIME_UNIT;

    public ApiLimit() {}
    public ApiLimit(String api, int limit) {
        this(api, limit, DEFAULT_TIME_UNIT);
    }

    public ApiLimit(String api, int limit, int unit) {
        this.api = api;
        this.limit = limit;
        this.unit = unit;
    }
}
```

限流过程中会频繁地查询接口对应的限流规则，为了尽可能地提高查询速度，将限流规则组织成一种支持按照 URL 快速查询的数据结构。考虑到 URL 的重复度比较高，且需要按照前缀来匹配，这里选择使用 Trie 树这种数据结构

```java
public class RateLimitRule {
    public RateLimitRule(RuleConfig ruleConfig) {}

    public ApiLimit getLimit(String appId, String api) {}
}
```

限流算法实现类
```java
public class RateLimitAlg {
    private static final long TRY_LOCK_TIMEOUT = 200L;  // 200ms
    private Stopwatch stopwatch;
    private AtomicInteger currentCount = new AtomicInteger(0);
    private final int limit;
    private Lock lock = new ReentrantLock();
  
    public RateLimitAlg(int limit) {
        this(limit, Stopwatch.createStarted());
    }

    @VisibleForTesting
    protected RateLimitAlg(int limit, Stopwatch stopwatch) {
        this.limit = limit;
        this.stopwatch = stopwatch;
    }

    public boolean tryAcquire() throws InternalErrorException {
        int updatedCount = currentCount.incrementAndGet();
        if (updatedCount <= limit) {
            return true;
        }

        try {
            if (lock.tryLock(TRY_LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
                try {
                    if (stopwatch.elapsed(TimeUnit.MILLISECONDS) > TimeUnit.SECONDS.toMillis(1)) {
                        currentCount.set(0);
                        stopwatch.reset();
                    }

                    updatedCount = currentCount.incrementAndGet();
                    return updatedCount <= limit;
                } finally {
                    lock.unlock();
                }
            } else {
                throw new InternalErrorException("tryAcquire() wait lock too long:" + TRY_LOCK_TIMEOUT + "ms");
            }
        } catch (InterruptedException e) {
            throw new InternalErrorException("tryAcquire() is interrupted by lock-time-out.", e);
        }
    }
}
```


## v2
```java
public class RateLimiter {
    private static final Logger log = LoggerFactory
        .getLogger(RateLimiter.class);

    // 为每个api在内存中存储限流计数器
    private ConcurrentHashMap<String, RateLimitAlg> counters = new ConcurrentHash<>();
    private RateLimitRule rule;

    public RateLimiter() {
        RuleConfigSource configSource = new FileRuleConfigSource();
        RuleConfig ruleConfig = configSource.load();
        this.rule = new TrieRateLimitRule(ruleConfig);
    }

    public boolean limit(String appId, String url) throws InternalErrorException {}
}
```
```java
public interface RuleConfigParser {
    RuleConfig parse(String configText);
    RuleConfig parse(InputStream in);
}

public interface RuleConfigSource {
    RuleConfig load();
}

public class FileRuleConfigSource implements RuleConfigSource {
    private static final Logger log = LoggerFactory
        .getLogger(FileRuleConfigSource.class);

    public static final String API_LIMIT_CONFIG_NAME = "ratelimiter-rule";
    public static final String YAML_EXTENSION = "yaml";
    public static final String YML_EXTENSION = "yml";
    public static final String JSON_EXTENSION = "json";

    private static final String[] SUPPORT_EXTENSIONS =
        new String[] {YAML_EXTENSION, YML_EXTENSION, JSON_EXTENSION};
    private static final Map<String, RuleConfigParser> PARSER_MAP = new HashMap<>();

    static {
        PARSER_MAP.put(YAML_EXTENSION, new YamlRuleConfigParser());
        PARSER_MAP.put(YML_EXTENSION, new YamlRuleConfigParser());
        PARSER_MAP.put(JSON_EXTENSION, new JsonRuleConfigParser());
    }

    @Override
    public RuleConfig load() {
        for (String extension : SUPPORT_EXTENSIONS) {
            InputStream in = null;
            try {
              in = this.getClass().getResourceAsStream("/" + getFileNameByExt(extension));
              if (in != null) {
                    RuleConfigParser parser = PARSER_MAP.get(extension);
                    return parser.parse(in);
              }
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        log.error("close file error:{}", e);
                    }
                }
            }
        }
        return null;
    }

    private String getFileNameByExt(String extension) {
        return API_LIMIT_CONFIG_NAME + "." + extension;
    }
```
