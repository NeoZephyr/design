结构型：类或对象组合的经典结构，解决特定应用场景的问题


```java
public enum NotificationEmergencyLevel {
    SEVERE, URGENCY, NORMAL, TRIVIAL
}
```
```java
public class Notification {
    private List<String> emailAddresses;
    private List<String> telephones;
    private List<String> wechatIds;

    public Notification() {}

    public void setEmailAddress(List<String> emailAddress) {
        this.emailAddresses = emailAddress;
    }

    public void setTelephones(List<String> telephones) {
        this.telephones = telephones;
    }

    public void setWechatIds(List<String> wechatIds) {
        this.wechatIds = wechatIds;
    }

    public void notify(NotificationEmergencyLevel level, String message) {
        if (level.equals(NotificationEmergencyLevel.SEVERE)) {
            // 自动语音电话
        } else if (level.equals(NotificationEmergencyLevel.URGENCY)) {
            // 发微信
        } else if (level.equals(NotificationEmergencyLevel.NORMAL)) {
            // 发邮件
        } else if (level.equals(NotificationEmergencyLevel.TRIVIAL)) {
            // 发邮件
        }
    }
}
```
```java
public class ErrorAlertHandler extends AlertHandler {
    public ErrorAlertHandler(AlertRule rule, Notification notification) {
        super(rule, notification);
    }

    public void check(ApiStatInfo apiStatInfo) {
        if (apiStatInfo.getErrorCount() > rule.getMatchedRule(apiStatInfo.getApi())) {
            notification.notify(NotificationEmergencyLevel.SEVERE, "...");
        }
    }
}
```

有很多 if-else 分支逻辑，有无限膨胀的可能，比较难维护

将不同渠道的发送逻辑剥离出来，形成独立的消息发送类 (MsgSender 相关类)。其中，Notification 类相当于抽象，MsgSender 类相当于实现，两者可以独立开发，通过组合关系（也就是桥梁）任意组合在一起

```java
public interface MsgSender {
    void send(String message);
}
```
```java
public class TelephoneMsgSender implements MsgSender {
    private List<String> telephones;

    public TelephoneMsgSender(List<String> telephones) {
        this.telephones = telephones;
    }

    public void send(String message) {}
}
```
```java
public class EmailMsgSender implements MsgSender {}

public class WechatMsgSender implements MsgSender {}
```

```java
public abstract class Notification {
    protected MsgSender msgSender;

    public Notification(MsgSender msgSender) {
        this.msgSender = msgSender;
    }

    public abstract void notify(String message);
}
```

```java
public class SevereNotification extends Notification {
    public SevereNotification(MsgSender msgSender) {
        super(msgSender);
    }

    public void notify(String message) {
        msgSender.send(message);
    }
}
```
```java
public class UrgencyNotification extends Notification {}
```
```java
public class NormalNotification extends Notification {}
```
```java
public class TrivialNotification extends Notification {}
```

