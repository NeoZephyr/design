```java
@Intercepts({
    @Signature(type = StatementHandler.class, method= "query", args = {Statement.class, ResultHandler.class}),
    @Signature(type = StatementHandler.class, method= "update", args = {Statement.class}),
    @Signature(type = StatementHandler.class, method= "batch", args = {Statement.class})
})
public class SqlCostTimeInterceptor implements Interceptor {
    private static Logger logger = LoggerFactory.getLogger(SqlCostTimeInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object target = invocation.getTarget();
        long startTime = System.currentTimeMillis();
        StatementHandler statementHandler = (StatementHandler) target;

        try {
            return invocation.proceed();
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            BoundSql boundSql = statementHandler.getBoundSql();
            String sql = boundSql.getSql();

            logger.info("执行 SQL:[ {} ]执行耗时[ {} ms]", sql, costTime);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        System.out.println("插件配置的信息:"+properties);
    }
}
```

```xml
<!-- MyBatis全局配置文件:mybatis-config.xml -->
<plugins>
    <plugin interceptor="com.SqlCostTimeInterceptor">
        <property name="someProperty" value="100"/>
    </plugin>
</plugins>
```

@Intercepts 注解可以嵌套 @Signature 注解。一个 @Signature 注解标明一个要拦截的目标方法

@Signature 注解包含三个元素：type、method、args。其中，type 指明要拦截的类、method 指明方法名、args 指明方法的参数列表。通过指定这三个元素，就能完全确定一个要拦截的方法

默认情况下，MyBatis Plugin 允许拦截的方法如下：
Executor 类：update, query, flushStatements, commit, rollback, getTransaction, close, isClosed
ParameterHandler 类：getParameterObject, setParameters
ResultSetHandler 类：handleResultSets, handleOutputParameters
StatementHandler 类：prepare, parameterize, batch, update, query

MyBatis 底层是通过 Executor 类来执行 SQL。Executor 类会创建 StatementHandler、ParameterHandler、ResultSetHandler 三个对象

使用 ParameterHandler 设置 SQL 中的占位符参数
使用 StatementHandler 执行 SQL 语句
使用 ResultSetHandler 封装执行结果

MyBatis 框架会读取全局配置文件，解析出 Interceptor，并且将它注入到 Configuration 类的 InterceptorChain 对象中

```java
public interface Interceptor {
    Object intercept(Invocation invocation) throws Throwable;
    Object plugin(Object target);
    void setProperties(Properties properties);
}
```
```java
public class InterceptorChain {
    private final List<Interceptor> interceptors = new ArrayList<Interceptor>();

    public Object pluginAll(Object target) {
        for (Interceptor interceptor : interceptors) {
            target = interceptor.plugin(target);
        }

        return target;
    }

    public void addInterceptor(Interceptor interceptor) {
        interceptors.add(interceptor);
    }

    public List<Interceptor> getInterceptors() {
        return Collections.unmodifiableList(interceptors);
    }
}
```

```java
public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
    executorType = executorType == null ? defaultExecutorType : executorType;
    executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
    Executor executor;

    if (ExecutorType.BATCH == executorType) {
        executor = new BatchExecutor(this, transaction);
    } else if (ExecutorType.REUSE == executorType) {
        executor = new ReuseExecutor(this, transaction);
    } else {
        executor = new SimpleExecutor(this, transaction);
    }

    if (cacheEnabled) {
        executor = new CachingExecutor(executor);
    }

    executor = (Executor) interceptorChain.pluginAll(executor);
    return executor;
}
```
```java
public ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
    ParameterHandler parameterHandler = mappedStatement.getLang().createParameterHandler(mappedStatement, parameterObject, boundSql);
    parameterHandler = (ParameterHandler) interceptorChain.pluginAll(parameterHandler);
    return parameterHandler;
}
```
```java
public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds, ParameterHandler parameterHandler, ResultHandler resultHandler, BoundSql boundSql) {
    ResultSetHandler resultSetHandler = new DefaultResultSetHandler(executor, mappedStatement, parameterHandler, resultHandler, boundSql, rowBounds);
    resultSetHandler = (ResultSetHandler) interceptorChain.pluginAll(resultSetHandler);
    return resultSetHandler;
}
```
```java
public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    StatementHandler statementHandler = new RoutingStatementHandler(executor, mappedStatement, parameterObject, rowBounds, resultHandler, boundSql);
    statementHandler = (StatementHandler) interceptorChain.pluginAll(statementHandler);
    return statementHandler;
}
```

```java
public class Plugin implements InvocationHandler {
    private final Object target;
    private final Interceptor interceptor;
    private final Map<Class<?>, Set<Method>> signatureMap;

    private Plugin(Object target, Interceptor interceptor, Map<Class<?>, Set<Method>> signatureMap) {
        this.target = target;
        this.interceptor = interceptor;
        this.signatureMap = signatureMap;
    }

    // 用来生成target的动态代理
    public static Object wrap(Object target, Interceptor interceptor) {
        Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor);
        Class<?> type = target.getClass();
        Class<?>[] interfaces = getAllInterfaces(type, signatureMap);

        if (interfaces.length > 0) {
            return Proxy.newProxyInstance(
                type.getClassLoader(),
                interfaces,
                new Plugin(target, interceptor, signatureMap)
            );
        }

        return target;
    }

    // 调用 target 上的 f() 方法，会触发该方法
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            Set<Method> methods = signatureMap.get(method.getDeclaringClass());

            if (methods != null && methods.contains(method)) {
                return interceptor.intercept(new Invocation(target, method, args));
            }

            return method.invoke(target, args);
        } catch (Exception ex) {
            throw ExceptionUtil.unwrapThrowable(e);
        }
    }

    private static Map<Class<?>, Set<Method>> getSignatureMap(Interceptor interceptor) {
        Intercepts interceptsAnnotation = interceptor.getClass().getAnnotation(Intercepts.class);

        if (interceptsAnnotation == null) {
            throw new PluginException("No @Intercepts annotation was found in in interceptor " + interceptor.getClass().getName());
        }

        Signature[] sigs = interceptsAnnotation.value();
        Map<Class<?>, Set<Method>> signatureMap = new HashMap<Class<?>, Set<Method>>();

        for (Signature sig : sigs) {
            Set<Method> methods = signatureMap.get(sig.type());

            if (methods == null) {
                methods = new HashSet<Method>();
                signatureMap.put(sig.type(), methods);
            }

            try {
                Method method = sig.type().getMethod(sig.method(), sig.args());
                methods.add(method);
            } catch (NoSuchMethodException e) {
                throw new PluginException("Could not find method on " + sig.type() + " named " + sig.method + ". Cause: " + e, e);
            }
        }

        return signatureMap;
    }
}
```
```java
private static Class<?>[] getAllInterfaces(Class<?> type, Map<Class<?>, Set<Method>> signatureMap) {
    Set<Class<?>> interfaces = new HashSet<Class<?>>();

    while (type != null) {
        for (Class<?> c : type.getInterfaces()) {
            if (signatureMap.containsKey(c)) {
                interfaces.add(c);
            }
        }

        type = type.getSuperclass();
    }

    return interfaces.toArray(new Class<?>[interfaces.size()]);
}
```

Plugin 借助 Java InvocationHandler 实现的动态代理类。用来代理给 target 对象添加 Interceptor 功能

wrap() 静态方法是一个工具函数，用来生成 target 对象的动态代理对象。只有 interceptor 与 target 互相匹配的时候，wrap() 方法才会返回代理对象，否则就返回 target 对象本身

MyBatis 中的职责链模式的实现方式比较特殊。它对同一个目标对象嵌套多次代理，每个代理对象代理一个拦截器功能