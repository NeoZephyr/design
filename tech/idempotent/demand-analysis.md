## 背景
调用方访问公共服务平台的接口，会有三种可能的结果：成功、失败和超时。前两种结果非 常明确，调用方可以自己决定收到结果之后如何处理

但是，当接口请求超时时，有可能业务逻辑已经执行成功了，只是公共服务平台返回结果给调用方的时候超时了，但也有可能业务逻辑没有执行成功，比如，因为数据库当时存在集中写入，导致部分数据写入超时

如果接口只包含查询、删除、更新这些操作，天然是幂等的。超时之后，重新再执行一次，没有任何副作用。不过，删除操作需要当心 ABA 问题。删除操作超时了，又触发一次删除，但在这次删除之前，又有一次新的插入。后一次删除操作删除了新插入的数据，而新插入的数据本不应该删除。不过，大部分业务都可以容忍 ABA 问题。除此之外，update x = x + delta 这样格式的更新操作并非幂等，只有 update x = y 这样格式的更新操作才是幂等的。不过，后者也存在跟删除同样的 ABA 问题

如果接口包含修改操作，多次重复执行有可能会导致业务上的错误，这是不能接受的。如果插入的数据包含数据库唯一键，可以利用数据库唯一键的排他性，保证不会重复插入数据

对于超时，有以下几种处理方式：
1. 返回清晰明确的提醒给用户，告知执行结果未知，让用户自己判断是否重试。
2. 调用方调用其他接口，来查询超时操作的结果，明确超时操作对应的业务，是执行成功了还是失败了，然后再基于明确的结果做处理。但是这种处理方法存在一个问题，那就是并不是所有的业务操作，都方便查询操作结果
3. 调用方在遇到接口超时之后，直接发起重试操作。这样就需要接口支持幂等。可以选择在业务代码中触发重试，也可以将重试的操作放到框架中完成

对响应时间敏感的调用方来说，过长的等待时间，还不如直接返回超时给用户，推荐第一种处理方式。对响应时间不敏感的调用方来说，比如 Job 类的调用方，推荐选择后两种处理方式，能够提高处理的成功率


## 需求分析
幂等号
同一业务请求的唯一标识

在业务代码中处理幂等
```java
Idempotence idempotence = new Idempotence();
String idempotenceId = idempotence.createId();

public class OrderController {
    private Idempotence idempontence;

    public Order createOrderWithIdempotence(..., String idempotenceId) {
        boolean existed = idempotence.check(idempotenceId);

        if (existed) {
            //
        }

        idempotence.record(idempotenceId);
    }
}
```

框架层面处理幂等
```java
Idempotence idempotence = new Idempotence();
String idempotenceId = idempotence.createId();

public class OrderController {
    @IdempotenceRequired
    public Order createOrder(...) {}
}

@Aspect
public class IdempotenceSupportAdvice {
    @Autowired
    private Idempotence idempotence;

    @Pointcut("@annotation(com.pain.idempotence.annotation.IdempotenceRequired)")
    public void controllerPointcut() {}

    @Around(value = "controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean existed = idempotence.check(idempotenceId);

        if (existed) {

        }

        idempotence.record(idempotenceId);
        Object result = joinPoint.proceed();
        return result;
    }
}
```

接口调用方生成幂等号，并且跟随接口请求，将幂等号传递给接口实现方。接口实现方接收到接口请求之后，按照约定，从 HTTP Header 或者接口参数中，解析出幂等号，然后通过幂等号查询幂等框架。如果幂等号已经存在，说明业务已经执行或正在执行，则直接返回；如果幂等号不存在，说明业务没有执行过，则记录幂等号，继续执行业务


## 非功能性需求
易用性方面，希望框架接入简单方便，学习成本低。只需编写简单的配置以及少许代码，就能完成接入。除此之外，最好在统一的地方接入幂等框架，而不是将它耦合在业务代码中

在性能方面，针对每个幂等接口，在正式处理业务逻辑之前，都要添加保证幂等的处理逻辑。这或多或少地会增加接口请求的响应时间。而对于响应时间比较敏感的接口服务来说，我们要让幂等框架尽可能低延迟，尽可能减少对接口请求本身响应时间的影响

在容错性方面，不能因为幂等框架本身的异常，导致接口响应异常，影响服务本身的可用性。比如，存储幂等号的外部存储器挂掉了，幂等逻辑无法正常运行，这个时候业务接口也要能正常服务才行

