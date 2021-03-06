## 单一职责原则
一个类或者模块只负责完成一个职责

判断类的职责是否足够单一
```java
public class UserInfo {
    private long userId;
    private String username;
    private String email;
    private String telephone;
    private long createTime;
    private long lastLoginTime;
    private String avatarUrl;
    private String provinceOfAddr;
    private String cityOfAddr;
    private String regionOfAddr;
    private String detailAddr;
}
```

不同的应用场景、不同阶段的需求背景下，对同一个类的职责是否单一的判定，可能都是不一样的

1. 如果在产品中，用户的地址信息跟其他信息一样，只是单纯地用来展示，那 UserInfo 的设计是合理的
2. 如果在产品中添加了电商的模块，用户的地址信息还会用在电商物流中，那最好将地址信息从 UserInfo 中拆分出来，独立成用户物流信息
3. 如果有多个产品，需要支持统一账号系统，那么就要继续对 UserInfo 进行拆分，将跟身份认证相关的信息抽取成独立的类

在真正的软件开发中，可以先写一个粗粒度的类，满足业务需求。随着业务的发展，如果粗粒度的类越来越庞大，代码越来越多，这个时候就可以将这个粗粒度的类，拆分成几个更细粒度的类。我们可以根据以下几条原则去判断类是否职责单一：

1. 类中的代码行数、函数或属性过多，会影响代码的可读性和可维护性，需要考虑对类进行拆分
2. 类依赖的其他类过多，或者依赖类的其他类过多，不符合高内聚、低耦合的设计思想，需要考虑对类进行拆分
3. 私有方法过多，考虑能否将私有方法独立到新的类中，设置为 public 方法，供更多的类使用，从而提高代码的复用性
4. 比较难给类起一个合适名字，很难用一个业务名词概括，或者只能用一些笼统的 Manager、Context 之类的词语来命名，这就说明类的职责定义得可能不够清晰
5. 类中大量的方法都是集中操作类中的某几个属性，比如，在 UserInfo 例子中，如果一半的方法都是在操作 address 信息，那就可以考虑将这几个属性和对应的方法拆分出来

单一职责原则通过避免设计大而全的类，避免将不相关的功能耦合在一起，来提高类的内聚性。同时，类职责单一，类依赖的和被依赖的其他类也会变少，减少了代码的耦合性，以此来实现代码的高内聚、低耦合。但是，如果拆分过细会适得其反，反倒会降低内聚性，也会影响代码的可维护性


