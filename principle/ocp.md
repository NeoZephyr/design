## 扩展开放、修改关闭
添加一个新的功能应该是，在已有代码基础上扩展代码（新增模块、类、方法等），而非修改已有代码（修改模块、类、方法等）

### 监控告警功能
```java
public class Alert {
    // 告警规则
    private AlertRule rule;

    // 邮件、短信、微信、手机等多种通知渠道
    private Notification notification;

    public Alert(AlertRule rule, Notification notification) {
        this.rule = rule;
        this.notification = notification;
    }

    public void check(String api, long requestCount, long errorCount, long durationOfSeconds) {
        long tps = requestCount / durationOfSeconds;

        if (tps > rule.getMatchedRule(api).getMaxTps()) {
            notification.notify(NotificationEmergencyLevel.URGENCY, "...");
        }

        if (errorCount > rule.getMatchedRule(api).getMaxErrorCount()) {
            notification.notify(NotificationEmergencyLevel.SEVERE, "...");
        }
    }
}
```

如果需要添加一个功能：当每秒钟接口超时请求个数，超过某个预先设置的最大值时，触发告警发送通知。修改代码如下：
```java
public void check(String api, long requestCount, long errorCount, long timeoutCount, long durationOfSeconds) {
    long tps = requestCount / durationOfSeconds;

    if (tps > rule.getMatchedRule(api).getMaxTps()) {
        notification.notify(NotificationEmergencyLevel.URGENCY, "...");
    }

    if (errorCount > rule.getMatchedRule(api).getMaxErrorCount()) {
        notification.notify(NotificationEmergencyLevel.SEVERE, "...");
    }

    long timeoutTps = timeoutCount / durationOfSeconds;

    if (timeoutTps > rule.getMatchedRule(api).getMaxTimeoutTps()) {
        notification.notify(NotificationEmergencyLevel.URGENCY, "...");
    }
}
```

这样修改代码存在挺多问题：一方面，对接口进行了修改，这就意味着调用这个接口的代码都要做相应的修改；另一方面，修改了 check() 函数，相应的单元测试都需要修改

代码重构
```java
public class Alert {
    private List<AlertHandler> alertHandlers = new ArrayList<>();

    public void addAlertHandler(AlertHandler alertHandler) {
        this.alertHandlers.add(alertHandler);
    }

    public void check(ApiStatInfo apiStatInfo) {
        for (AlertHandler handler : alertHandlers) {
            handler.check(apiStatInfo);
        }
    }
}

public class ApiStatInfo {
    private String api;
    private long requestCount;
    private long errorCount;
    private long timeoutCount;
    private long durationOfSeconds;
}

public abstract class AlertHandler {
    protected AlertRule rule;
    protected Notification notification;

    public AlertHandler(AlertRule rule, Notification notification) {
        this.rule = rule;
        this.notification = notification;
    }

    public abstract void check(ApiStatInfo apiStatInfo);
}
```
```java
public class TpsAlertHandler extends AlertHandler {
    public TpsAlertHandler(AlertRule rule, Notification notification) {
        super(rule, notification);
    }

    @Override
    public void check(ApiStatInfo apiStatInfo) {
        long tps = apiStatInfo.getRequestCount() / apiStatInfo.getDurationOfSeconds;
        if (tps > rule.getMatchedRule(apiStatInfo.getApi()).getMaxTps()) {
            notification.notify(NotificationEmergencyLevel.URGENCY, "...");
        }
    }
}
```
```java
public class ErrorAlertHandler extends AlertHandler {
    public ErrorAlertHandler(AlertRule rule, Notification notification){
        super(rule, notification);
    }

    @Override
    public void check(ApiStatInfo apiStatInfo) {
        if (apiStatInfo.getErrorCount() > rule.getMatchedRule(apiStatInfo.getApi()).getMaxErrorCount()) {
            notification.notify(NotificationEmergencyLevel.SEVERE, "...");
        }
    }
}
```


```java
public class TimeoutAlertHandler extends AlertHandler {}
```

