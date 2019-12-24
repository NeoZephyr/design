封装
对类中的属性进行限制，提升可读性、易用性

抽象
忽略非关键性的实现细节

继承
1. 表示 is-a 关系
2. 代码复用（继承层次过深导致可读性、可维护性变差)
3. 支持多态特性

多态
1. 继承加方法重写
2. duck-typing

提高代码可扩展性和复用性

面向对象语言写出面向过程代码
1. 滥用 getter、setter 方法
违反了面向对象的封装特性
getter 返回可修改对象

2. 滥用全局变量和全局方法
将 Constants 类拆解为功能更加单一的多个类
细化 Utils 类

3. 定义数据和方法分离的类

抽象类
1. 解决代码复用
2. 强制要求子类重写抽象方法

接口
1. 解耦
2. 可扩展

抽象类来模拟接口
```java
public class MockInterface {
    // 避免被实例化
    protected MockInterface() {}
    public void funcA() {
        throw new MethodUnSupportedException();
    }
}
```

基于接口而非实现编程
1. 函数的命名不能暴露任何实现细节
2. 封装具体的实现细节
3. 为实现类定义抽象的接口，实现类依赖统一的接口定义，遵从一致的上传功能协议。使用者依赖接口，而不是具体的实现类来编程

继承与组合
method1
```java
public class AbstractBird {
    public void fly() {}
}

public class Ostrich extends AbstractBird {
    public void fly() {
        throw new UnSupportedMethodException("Can't fly");
    }
}
```
1. 徒增了编码的工作量
2. 违背了最小知识原则，暴露不该暴露的接口给外部

method2
```java
public class AbstractBird {
}

public class AbstractFlyableBird extends AbstractBird {
    public void fly() {}
}

public class AbstractUnFlyableBird extends AbstractBird {
}

public class AbstractFlyableTweetableBird extends AbstractFlyableBird {
}

public class Ostrich extends AbstractUnFlyableBird {
}
```
1. 类的继承层次越来越深、继承关系会越来越复杂，导致代码的可读性变差
2. 破坏了类的封装特性，将父类的实现细节暴露给了子类。子类的实现依赖父类的实现，两者高度耦合，一旦父类代码修改，就会影响所有子类的逻辑。

method3: 通过组合、接口、委托三个技术手段替换掉继承
```java
public interface Flyable {
    void fly();
}

public interface Tweetable {
    void tweet();
}

public interface EggLayable {
    void layEgg();
}

public class FlyAbility implements Flyable {
    public void fly() {}
}

public class Ostrich implements Tweetable, EggLayable {
    public void tweet() {}
    public void layEgg() {}
}

public class Sparrow impelents Flayable, Tweetable, EggLayable {
    private FlyAbility flyAbility

    public void fly() {
        FlyAbility.fly();
    }

    public void layEgg() {}
    public void tweet() {}
}
```

钱包交易流水：交易流水 id，交易时间，交易金额，交易类型，入账钱包账号，出账钱包账号，虚拟钱包交易流水 id
虚拟钱包交易流水：交易流水 id，交易时间，交易金额，交易类型，虚拟钱包账号，钱包交易流水 id

贫血模型
```java
public class VirtualWalletController {
    private VirtualWalletService virtualWalletService;

    public BigDecimal getBalance(Long walletId) {}
    public void debit(Long walletId, BigDecimal amount) {}
    public void credit(Long walletId, BigDecimal amount) {}
    public void transfer(Long fromWalletId, Long toWalletId, BigDecimal amount) {}
}

public class VirtualWalletBo {
    private Long id;
    private Long createTime;
    private BigDecimal balance;
}

public class VirtualWalletService {
    private VirtualWalletRepository walletRepo;
    private VirtualWalletTransactionRepository transactionRepo;

    public VirtualWalletBo getVirtualWallet(Long walletId) {
        VirtualWalletEntity walletEntity = walletRepo.getWalletEntity(walletId);
        VirtualWalletBo walletBo = convert(walletEntity);
        return walletBo;
    }

    public BigDecimal getBalance(Long walletId) {
        return walletRepo.getBalance(walletId);
    }

    public void debit(Long walletId, BigDecimal amount) {
        VirtualWalletEntity walletEntity = walletRepo.getWalletEntity(walletId);
        BigDecimal balance = walletEntity.getBalance();
        if (balance.compareTo(amount) < 0) {
            throw new NoSufficientBalanceException("");
        }
        walletRepo.updateBalance(walletId, balance.subtract(amount));
    }

    public void credit(Long walletId, BigDecimal amount) {
        VirtualWalletEntity walletEntity = walletRepo.getWalletEntity(walletId);
        BigDecimal balance = walletEntity.getBalance();
        walletRepo.updateBalance(walletId, balance.add(amount));
    }

    public void transfer(Long fromWalletId, Long toWalletId, BigDecimal amount) {
        VirtualWalletTransactionEntity transactionEntity = new VirtualWalletTransactionEntity();
        transactionEntity.setAmount(amount);
        transactionEntity.setCreateTime(System.currentTimeMillis());
        transactionEntity.setFromWalletId(fromWalletId);
        transactionEntity.setToWalletId(toWalletId);
        transactionEntity.setStatus(Status.TO_BE_EXECUTED);

        Long transactionId = transactionRepo.saveTransaction(transactionEntity);

        try {
            debit(fromWalletId, amount);
            credit(toWalletId, amount);
        } catch (InsufficientBalanceException e) {
            transactionRepo.updateStatus(transactionId, Status.CLOSED);
        } catch (Exception e) {
            transactionRepo.updateStatus(transactionId, Status.FAILED);
        }

        transactionRepo.updateStatus(transactionId, Status.EXECUTED);
    }
}
```

充血模型
Service 类负责与 Repository 交流，调用 Respository 类的方法，获取数据库中的数据，转化成领域模型，然后由领域模型来完成业务逻辑，最后调用 Repository 类的方法，将数据存回数据库

Service 类负责跨领域模型的业务聚合功能

Service 类负责一些非功能性及与三方系统交互的工作。比如幂等、事务、发邮件、发消 息、记录日志、调用其他系统的 RPC 接口等

```java
public class VirtualWallet {
    private Long id;
    private Long createTime = System.currentTimeMillis();
    private BigDecimal balance = BigDecimal.ZERO;

    public VirtualWallet(Long preAllocatedId) {
        this.id = preAllocatedId;
    }

    public BigDecimal balance() {
        return this.balance;
    }

    public void debit(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("");
        }

        this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAmountException("");
        }

        this.balance.add(amount);
    }

    public void freeze(BigDecimal amount) {}
    public void unfreeze(BigDecimal amount) {}
}


public class VirtualWalletService {
    private VirtualWalletRepository walletRepo;
    private VirtualWalletTransactionRepository transactionRepo;

    public VirtualWallet getVirtualWallet(Long walletId) {
        VirtualWalletEntity walletEntity = walletRepo.getWalletEntity(walletId);
        VirtualWallet wallet = convert(walletEntity);
        return wallet;
    }

    public BigDecimal getBalance(Long walletId) {
        return walletRepo.getBalance(walletId);
    }

    public void debit(Long walletId, BigDecimal amount) {
        VirtualWalletEntity walletEntity = walletRepo.getWalletEntity(walletId);
        VirtualWallet wallet = convert(walletEntity);
        wallet.debit(amount);
        walletRepo.updateBalance(walletId, wallet.balance());
    }

    public void credit(Long walletId, BigDecimal amount) {
        VirtualWalletEntity walletEntity = walletRepo.getWalletEntity(walletId);
        VirtualWallet wallet = convert(walletEntity);
        wallet.credit(amount);
        walletRepo.updateBalance(walletId, wallet.balance());
    }

    public void transfer(Long fromWalletId, Long toWalletId, BigDecimal amount) {
        // ...
    }
}
```
