## 需求
获取接口调用的各种统计信息，并且支持将统计结果以各种显示格式输出到各种终端，以方便查看

## 分析
### 功能性需求分析
1. 接口统计信息
2. 统计信息的类型
3. 统计信息显示格式
4. 统计信息显示终端
5. 统计触发方式，包括主动和被动两种
6. 统计时间区间
7. 统计时间间隔

### 非功能性需求分析
1. 易用性：是否易集成、易插拔、跟业务代码是否松耦合、提供的接口是否够灵活等
2. 性能：低延迟，内存的消耗不能太大等
3. 扩展性：使用者可以在不修改框架源码，甚至不拿到框架源码的情况下，为框架扩展新的功能

```java
// 方便扩展编解码方式、日志、拦截器等
Feign feign = Feign.builder()
        .logger(new CustomizedLogger())
        .encoder(new FormEncoder(new JacksonEncoder()))
        .decoder(new JacksonDecoder())
        .errorDecoder(new ResponseErrorDecoder())
        .requestInterceptor(new RequestHeadersInterceptor())
        .build();

public class RequestHeadersInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        // ...
        template.header("timestamp", "...");
    }
}

public class CustomizedLogger extends Feign.Logger {}

public class ResponseErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        // ...
    }
}
```

4. 容错性：不能因为框架本身的异常导致接口请求出错，对外暴露的接口抛出的所有运行时、非运行时异常都进行捕获处理

5. 通用性：是否还可以处理其他事件的统计信息，比如 SQL 请求时间的统计信息、业务统计信息


## 最小原型设计
```java
public class Metrics {
    private Map<String, List<Double>> responseTimes = new HashMap<>();
    private Map<String, List<Double>> timestamps = new HashMap<>();
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public void recordResponseTime(String apiName, double responseTime) {
        responseTimes.putIfAbsent(apiName, new ArrayList<>());
        responseTimes.get(apiName).add(responseTime);
    }

    public void recordTimestamp(String apiName, double timestamp) {
        timestamps.putIfAbsent(apiName, new ArrayList<>());
        timestamps.get(apiName).add(timestamp);
    }

    public void startRepeatedReport(long period, TimeUnit unit) {
        executor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                Gson gson = new Gson();
                Map<String, Map<String, Double>> stats = new HashMap<>();

                for (Map.Entry<String, List<Double>> entry : responseTimes.entrySet()) {
                    String apiName = entry.getKey();
                    List<Double> apiRespTimes = entry.getValue();
                    stats.putIfAbsent(apiName, new HashMap<>());
                    stats.get(apiName).put("max", max(apiRespTimes));
                    stats.get(apiName).put("avg", avg(apiRespTimes));
                }

                for (Map.Entry<String, List<Double>> entry : timestamps.entrySet()) {
                    String apiName = entry.getKey();
                    List<Double> apiTimestamps = entry.getValue();
                    stats.putIfAbsent(apiName, new HashMap<>());
                    stats.get(apiName).put("count", (double)apiTimestamps.size());
                }

                System.out.println(gson.toJson(stats));
            }
        }, 0, period, unit);
    }

    private double max(List<Double> dataset) {
        // TODO
    }
    private double avg(List<Double> dataset) {
        // TODO
    }
}
```
```java
public class UserController {
    private Metrics metrics = new Metrics();

    public UserController() {
        metrics.startRepeatedReport(60, TimeUnit.SECONDS);
    }

    public void register(UserVo user) {
        long startTimestamp = System.currentTimeMillis();
        metrics.recordTimestamp("regsiter", startTimestamp);
        // TODO
        long respTime = System.currentTimeMillis() - startTimestamp;
        metrics.recordResponseTime("register", respTime);
    }

    public UserVo login(String telephone, String password) {
        long startTimestamp = System.currentTimeMillis();
        metrics.recordTimestamp("login", startTimestamp);
        // TODO
        long respTime = System.currentTimeMillis() - startTimestamp;
        metrics.recordResponseTime("login", respTime);
    }
}
```


## v1.0 实现
数据采集：打点采集原始数据，包括记录每次接口请求的响应时间和请求时间
存储：将采集的原始数据保存下来，以便之后做聚合统计。暂时只支持 Redis 这一种存储方式
聚合统计：将原始数据聚合为统计数据，包括响应时间的最大值、最小值、平均值、99 百分位值，以及接口请求的次数和 tps
显示：将统计数据以某种格式显示到终端，暂时只支持主动推送给命令行和邮件。命令行间隔 n 秒统计显示上 m 秒的数据，邮件每日统计上日的数据

### 类设计
MetricsCollector 类负责提供 API，来采集接口请求的原始数据
MetricsStorage 接口负责原始数据存储
Aggregator 类负责根据原始数据计算统计数据
ConsoleReporter 类、EmailReporter 类分别负责以一定频率统计并发送统计数据到命令行和邮件


MetricsCollector 负责采集和存储数据，职责相对单一。基于接口而非实现编程，通过依赖注入的方式来传递 MetricsStorage 对象，可以在不需要修改代码的情况下，灵活地替换不同的存储方式，满足开闭原则
```java
public class MetricsCollector {
    // 基于接口而非实现编程
    private MetricsStorage metricsStorage;

    // 依赖注入
    public MetricsCollector(MetricsStorage metricsStorage) {
        this.metricsStorage = metricsStorage;
    }

    public void recordRequest(RequestInfo requestInfo) {
        if (requestInfo == null || StringUtils.isBlank(requestInfo.getApiName())) {
            return;
        }

        metricsStorage.saveRequestInfo(requestInfo);
    }
}

public class RequestInfo {
    private String apiName;
    private double responseTime;
    private long timestamp;
}
```

一次性取太长时间区间的数据，可能会导致拉取太多的数据到内存中，可能会触发 OOM
```java
public interface MetricsStorage {
    void saveRequestInfo(RequestInfo requestInfo);
    List<RequestInfo> getRequestInfos(String apiName, long startTimeInMillis, long endTimeInMillis);
    Map<String, List<RequestInfo>> getRequestInfos(long startTimeInMillis, long endTimeInMillis);
}

public class RedisMetricsStorage implements MetricsStorage {
    public void saveRequestInfo(RequestInfo requestInfo) {}

    public List<RequestInfo> getRequestInfos(String apiName, long startTimeInMillis, long endTimeInMillis) {}

    public Map<String, List<RequestInfo>> getRequestInfos(long startTimestamp, long endTimeInMillis) {}
}
```

负责各种统计数据的计算。当需要扩展新的统计功能的时候，需要修改函数代码，随着越来越多的统计功能添加进来，这个函数的代码量会持续增加，可读性、可维护性就会变差。因此，这个类的设计可能存在职责不够单一、不易扩展等问题
```java
public class Aggregator {
    public static RequestStat aggregate(List<RequestInfo> requestInfos, long durationInMillis) {
        double maxRespTime = Double.MIN_VALUE;
        double minRespTime = Double.MAX_VALUE;
        double avgRespTime = -1;
        double p999RespTime = -1;
        double p99RespTime = -1;
        double sumRespTime = 0;
        long count = 0;

        for (RequestInfo requestInfo : requestInfos) {
            ++count;
            double respTime = requestInfo.getResponseTime();

            if (maxRespTime < respTime) {
                maxRespTime = respTime;
            }

            if (minRespTime > respTime) {
                minRespTime = respTime;
            }

            sumRespTime += respTime;
        }

        if (count != 0) {
            avgRespTime = sumRespTime / count;
        }

        long tps = (long)(count / durationInMillis * 1000);

        Collections.sort(requestInfos, new Comparator<RequestInfo>() {
            public int compare(RequestInfo o1, RequestInfo o2) {
                double diff = o1.getResponseTime() - o2.getResponseTime();

                if (diff < 0.0) {
                    return -1;
                } else if (diff > 0.0) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        int idx999 = (int)(count * 0.999);
        int idx99 = (int)(count * 0.99);

        if (count != 0) {
            p999RespTime = requestInfos.get(idx999).getResponseTime();
            p99RespTime = requestInfos.get(idx99).getResponseTime();
        }

        RequestStat requestStat = new RequestStat();
        requestStat.setMaxResponseTime(maxRespTime);
        requestStat.setMinResponseTime(minRespTime);
        requestStat.setAvgResponseTime(avgRespTime);
        requestStat.setP999ResponseTime(p999RespTime);
        requestStat.setP99ResponseTime(p99RespTime);
        requestStat.setCount(count);
        requestStat.setTps(tps);
        return requestStat;
    }
}

public class RequestStat {
    private double maxResponseTime;
    private double minResponseTime;
    private double avgResponseTime;
    private double p999ResponseTime;
    private double p99ResponseTime;
    private long count;
    private long tps;
}
```

在 ConsoleReporter 和 EmailReporter 这两个类中，取数据、做统计的逻辑都是相同的，可以抽取出来复用，否则就违反了 DRY 原则。而且整个类负责的事情比较多，职责不单一。特别是显示部分的代码，可能会比较复杂，最好是将显示部分的代码逻辑拆分成独立的类。除此之外，因为代码中涉及线程操作，并且调用了 Aggregator 的静态函数，所以代码的可测试性不好
```java
public class ConsoleReporter {
    private MetricsStorage metricsStorage;
    private ScheduledExecutorService executor;

    public ConsoleReporter(MetricsStorage metricsStorage) {
        this.metricsStorage = metricsStorage;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void startRepeatedReport(long periodInSeconds, long durationInSeconds) {
        executor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                long durationInMillis = durationInSeconds * 1000;
                long endTimeInMillis = System.currentTimeMillis();
                long startTimeInMillis = endTimeInMillis - durationInMillis;
                Map<String, List<RequestInfo>> requestInfos = metricsStorage.getRequestInfos(startTimeInMillis, endTimeInMillis);
                Map<String, RequestStat> stats = new HashMap<>();

                for (Map.Entry<String, List<RequestInfo>> entry : requestInfos.entrySet()) {
                    String apiName = entry.getKey();
                    List<RequestInfo> requestInfosPerApi = entry.getValue();
                    RequestStat requestStat = Aggregator.aggregate(requestInfosPerApi, durationInMillis);
                    stats.put(apiName, requestStat);
                }

                Gson gson = new Gson();
                System.out.println(gson.toJson(stats));
            }
        }, 0, periodInSeconds, TimeUnit.SECONDS);
    }
}

public class EmailReporter {
    private static final Long DAY_HOURS_IN_SECONDS = 86400L;

    private MetricsStorage metricsStorage;
    private EmailSender emailSender;
    private List<String> toAddresses = new ArrayList<>();

    public EmailReporter(MetricsStorage metricsStorage) {
        this(metricsStorage, new EmailSender(/* 省略参数 */));
    }

    public EmailReporter(MetricsStorage metricsStorage, EmailSender emailSender) {
        this.metricsStorage = metricsStorage;
        this.emailSender = emailSender;
    }

    public void addToAddress(String address) {
        toAddresses.add(address);
    }

    public void startDailyReport() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date firstTime = calendar.getTime();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                long durationInMillis = DAY_HOURS_IN_SECONDS * 1000;
                long endTimeInMillis = System.currentTimeMillis();
                long startTimeInMillis = endTimeInMillis - durationInMillis;

                Map<String, List<RequestInfo>> requestInfos = metricsStorage.getRequestInfos(startTimeInMillis, endTimeInMillis);
                Map<String, RequestStat> stats = new HashMap<>();

                for (Map.Entry<String, List<RequestInfo>> entry : requestInfos.entrySet()) {
                    String apiName = entry.getKey();
                    List<RequestInfo> requestInfosPerApi = entry.getValue();
                    RequestStat requestStat = Aggregator.aggregate(requestInfosPerApi, durationInMillis);
                    stats.put(apiName, requestStat);
                }

                // TODO: 格式化为 html 格式，并且发送邮件
            }
        }, firstTime, DAY_HOURS_IN_SECONDS * 1000);
    }
}
```

执行入口
```java
public static void main(String[] args) {
    MetricsStorage storage = new RedisMetricsStorage();
    ConsoleReporter consoleReporter = new ConsoleReporter(storage);
    consoleReporter.startRepeatedReport(60, 60);

    EmailReporter emailReporter = new EmailReporter(storage);
    emailReporter.addToAddress("");
    emailReporter.startDailyReport();

    MetricsCollector collector = new MetricsCollector(storage);
    collector.recordRequest(new RequestInfo("register", 123, 10234));
    collector.recordRequest(new RequestInfo("register", 223, 11234));
    collector.recordRequest(new RequestInfo("register", 323, 12334));
    collector.recordRequest(new RequestInfo("login", 23, 12434));
    collector.recordRequest(new RequestInfo("login", 1223, 14234));

    try {
        Thread.sleep(100000);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}
```

