## 业务场景
钱包交易流水：交易流水 id，交易时间，交易金额，交易类型，入账钱包账号，出账钱包账号，虚拟钱包交易流水 id
虚拟钱包交易流水：交易流水 id，交易时间，交易金额，交易类型，虚拟钱包账号，钱包交易流水 id

### 贫血模型
```java
public class VirtualWalletController {
    private VirtualWalletService virtualWalletService;

    public BigDecimal getBalance(Long walletId) {}
    public void debit(Long walletId, BigDecimal amount) {}
    public void credit(Long walletId, BigDecimal amount) {}
    public void transfer(Long fromWalletId, Long toWalletId, BigDecimal amount){}
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


### 充血模型
Service 类负责与 Repository 交流，调用 Respository 类的方法，获取数据库中的数据，转化成领域模型，然后由领域模型来完成业务逻辑，最后调用 Repository 类的方法，将数据存回数据库

Service 类负责跨领域模型的业务聚合功能

Service 类负责一些非功能性及与三方系统交互的工作。比如幂等、事务、发邮件、发消息、记录日志、调用其他系统的 RPC 接口等

Controller 层主要负责接口的暴露，Repository 层主要负责与数据库打交互

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

## 分层开发
### 代码复用
同一个 Repository 可能会被多个 Service 来调用，同一个 Service 可能会被多个 Controller 调用

### 隔离变化
Repository 层封装了对数据库访问的操作，提供了抽象的数据访问接口。基于接口而非实现编程的设计思想，Service 层使用 Repository 层提供的接口，并不关心其底层依赖的是哪种具体的数据库。Repository 层最稳定，而 Controller 层经常会变动。分层之后，Controller 层中代码的频繁改 动并不会影响到稳定的 Repository 层

### 隔离关注点
Repository 层只关注数据的读写；Service 层只关注业务逻辑，不关注数据的来源；Controller 层只关注与外界打交道，数据校验、封装、格式转换，并不关心业务逻辑

### 提高代码的可测试性
分层之后，Repsitory 层的代码通过依赖注入的方式供 Service 层使用。当要测试包含核心业务逻辑的 Service 层代码的时候，可以用 mock 的数据源替代真实的数据库，注入到 Service 层代码中

### 应对系统的复杂性
当一个类或一个函数的代码过多之后，可读性、可维护性就会变差，需要进行拆分。拆分有垂直和水平两个方向。水平方向基于业务来做拆分，就是模块化；垂直方向基于流程来做拆分，就是分层

## BO vs VO vs Entity
在实际的开发中，VO、BO、Entity 可能存在大量重复字段。可以通过继承或者组合解决代码重复问题