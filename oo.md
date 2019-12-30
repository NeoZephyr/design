## 鉴权功能

### ooa
#### 基础分析
通过用户名加密码来做认证，给每个服务调用方下发 appId 和一个对应的密钥。调用方每次请求时，都携带自己的 appId 和密钥。服务在接收到请求后，解析出 appId 和密钥，然后进行验证

#### 分析优化
1. appId 与密钥的验证方式，密钥容易被截获，是不安全的。可以借助加密算法，对密钥进行加密传输

对传输信息进行加密，虽然密钥不会被泄露，但是加密之后的密钥及 appId 照样可以被未认证系统截获，未认证系统可以携带这个加密之后的密钥以及对应的 appId，伪装成已认证系统来访问我们的服务

2. 借助 OAuth 的验证思路，调用方将请求接口的 URL 跟 appID、密钥拼接在一起进行加密，生成一个 token。然后将这个 token 及 appId，随 URL 一块传递给服务端。微服务端接收请求后，根据 appId 从数据库中取出对应的密码，并通过同样的 token 生成算法，生成另外一个 token 进行验证

由于每个 URL 拼接上 appId、密钥生成的 token 都是固定的。未认证系统截获 URL、token 和 appId 之后，还是可以通过重放攻击的方式，伪装成认证系统，调用对应的接口

3. 优化 token 生成算法，引入一个随机变量，让每次接口请求生成的 token 都不一样。选择时间戳作为随机变量，将 URL、appId、密码、时间戳四者进行加密来生成 token。服务端在接受请求后，会验证当前时间戳跟传递过来的时间戳，是否在一定的时间窗口内（比如一分钟）。如果超过一分钟，则判定 token 过期，拒绝接口请求。如果没有超过一分钟，然后再对 token 进行验证

未认证系统还是可以在一个时间段内，通过截获请求、重放请求，来调用我们的接口

4. 引入一个唯一标识 nonce 来标记每一次请求，服务端在接受请求后，会验证 nonce 是否是已使用的

### ood
#### 划分职责
根据需求描述，把其中涉及的功能点罗列出来，然后再去看哪些功能点职责相近，操作同样的属性，可否应该归为同一个类。在拆解需求描述之后，得到如下功能点列表：
1. 把 URL、appId、密钥、时间戳拼接为一个字符串
2. 对字符串通过加密算法加密生成 token
3. 将 token、appId、时间戳拼接到 URL 中，形成新的 URL
4. 解析 URL，得到 token、appId、时间戳等信息
5. 从存储中取出 appId 和对应的密钥
6. 根据时间戳判断 token 是否过期失效
7. 验证两个 token 是否匹配

我们发现，1、2、6、7 都是跟 token 有关，负责 token 的生成、验证；3、4 都是在处理 URL，负责 URL 的拼接、解析；5 是操作 appId 和密钥，负责从存储中读取 appId 和密钥。因此，可以粗略地得到三个核心的类：AuthToken、Url、CredentialStorage

如果面对大型的软件以及复杂的需求开发，涉及的功能点可能会很多，对应的类也会比较多。我们可以首先进行模块划分，将需求先简单划分成几个小的、独立的功能模块，然后再在模块内部，应用上述方法，进行面向对象设计

#### 类设计
方法的设计，识别出需求描述中的动词，作为候选的方法，再进一步过滤筛选
属性的设计，识别出功能点中的名词，作为候选属性，然后同样进行过滤筛选

AuthToken
```java
public AuthToken(String token, long createTime);
public AuthToken(String token, long createTime, long expireTimeInterval);
```
```java
public static AuthToken create(String baseUrl, long createTime, Map<String, String> params);
public String getToken();
public boolean isExpired();
public boolean match(AuthToken authToken);
```

```java
private static final long DEFAULT_EXPIRED_TIME_INTERVAL = 60 * 1000;
private String token;
private long createTime;
private long expireTimeInterval = DEFAULT_EXPIRED_TIME_INTERVAL;
```

ApiRequest
```java
public ApiRequest(String baseUrl, String token, String appId, long timestamp);
```
```java
public static ApiRequest createFromFullUrl(String url);
public String getBaseUrl();
public String getToken();
public String getAppId();
public long getTimestamp();
```

```java
private String baseUrl;
private String token;
private String appId;
private long timestamp;
```

CredentialStorage
```java
String getPasswordByAppId(String appId);
```

#### 类之间的关系
泛化，可以简单理解为继承
```java
public class A {}
public class B extends A {}
```

实现，一般是指接口和实现类之间的关系
```java
public interface A {}
public class B implements A {}
```

聚合是一种包含关系，A 类对象包含 B 类对象，B 类对象的生命周期可以不依赖 A 类对象的生命周期
```java
public class A {
    private B b;
    public A(B b) {
        this.b = b;
    }
}
```

组合也是一种包含关系。A 类对象包含 B 类对象，B 类对象的生命周期依赖 A 类对象的生命周期
```java
public class A {
    private B b;
    public A() {
        this.b = new B();
    }
}
```

关联是一种非常弱的关系，包含聚合、组合两种关系。如果 B 类对象是 A 类的成员变量，那 B 类和 A 类就是关联关系
```java
public class A {
    private B b;
    public A(B b) {
        this.b = b;
    }
}

public class A {
    private B b;
    public A() {
        this.b = new B();
    }
}
```

依赖是一种比关联关系更加弱的关系，包含关联关系。不管是 B 类对象是 A 类对象的成员变量，还是 A 类的方法使用 B 类对象作为参数或者返回值、局部变量，只要 B 类对象和 A 类对象有任何使用关系，都称它们有依赖关系
```java
public class A {
    private B b;
    public A(B b) {
        this.b = b;
    }
}

public class A {
    private B b;
    public A() {
        this.b = new B();
    }
}

public class A {
    public void func(B b) {}
}
```

#### 组装类并提供执行入口
设计一个最顶层的 ApiAuthencator 接口类，暴露一组给外部调用者使用的 API 接口，作为触发执行鉴权逻辑的入口

ApiAuthencator
```java
void auth(String url);
void auth(ApiRequest ApiRequest);
```

DefaultApiAuthencator
```java
private CredentialStorage credentialStorage;
```
```java
public DefaultApiAuthencator();
public DefaultApiAuthencator(CredentialStorage credentialStorage);
```
```java
void auth(String url);
void auth(ApiRequest ApiRequest);
```

```java
public class DefaultApiAuthencatorImpl implements ApiAuthencator {
    private CredentialStorage credentialStorage;

    public ApiAuthencator() {
        this.credentialStorage = new MysqlCredentialStorage();
    }

    public ApiAuthencator(CredentialStorage credentialStorage) {
        this.credentialStorage = credentialStorage;
    }

    @Override
    public void auth(String url) {
        ApiRequest apiRequest = ApiRequest.buildFromUrl(url);
        auth(apiRequest);
    }

    @Override
    public void auth(ApiRequest apiRequest) {
        String appId = apiRequest.getAppId();
        String token = apiRequest.getToken();
        long timestamp = apiRequest.getTimestamp();
        String originalUrl = apiRequest.getOriginalUrl();

        AuthToken clientAuthToken = new AuthToken(token, timestamp);

        if (clientAuthToken.isExpired()) {
            throw new RuntimeException("Token is expired.");
        }

        String password = credentialStorage.getPasswordByAppId(appId);
        AuthToken serverAuthToken = AuthToken.generate(originalUrl, appId, password);
        if (!serverAuthToken.match(clientAuthToken)) {
            throw new RuntimeException("Token verfication failed.");
        }
    }
}
```

