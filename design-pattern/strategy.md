## 策略模式
解耦策略的定义、创建、使用这三部分

策略的定义
```java
public interface DiscountStrategy {
    double calDiscount(Order order);
}
```

策略的创建
```java
public class DiscountStrategyFactory {
    private static final Map<OrderType, DiscountStrategy> strategies = new HashMap();

    static {
        strategies.put(OrderType.NORMAL, new NormalDiscountStrategy());
        strategies.put(OrderType.GROUPON, new GrouponDiscountStrategy());
        strategies.put(OrderType.PROMOTION, new PromotionDiscountStrategy());
    }

    public static DiscountStrategy getDiscountStrategy(OrderType type) {
        return strategies.get(type);
    }
}
```

策略的使用
```java
public class OrderService {
    public double discount(Order order) {
        OrderType type = order.getType();
        DiscountStrategy discountStrategy = DiscountStrategyFactory.getDiscountStrategy(type);
        return discountStrategy.calDiscount(order);
    }
}
```

如果业务场景需要每次都创建不同的策略对象，就需要另外一种工厂类的实现方式：
```java
public class DiscountStrategyFactory {
    public static DiscountStrategy getDiscountStrategy(OrderType type) {
        if (type == null) {
            throw new IllegalAccessException("Type should not be null");
        }

        if (type.equals(OrderType.NORMAL)) {
            return new NormalDiscountStrategy();
        } else if (type.equals(OrderType.GROUPON)) {
            return new GrouponDiscountStrategy();
        } else if (type.equals(OrderType.PROMOTION)) {
            return new PromotionDiscountStrategy();
        }

        return null;
    }
}
```

彻底移除 if-else
```java
public class DiscountStrategyHolder {
    private DiscountStrategy discountStrategy;
    private OrderType orderType;

    public boolean match(OrderType orderType) {
        return this.orderType.equals(orderType);
    }
}
```
```java
OrderType orderType;
DiscountStrategyHolder candHolder = nulll
for (DiscountStrategyHolder holder : holders) {
    if (holder.match(orderType)) {
        candHolder = holder;
        break;
    }
}
```
