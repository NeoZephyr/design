结构型：类或对象组合的经典结构，解决特定应用场景的问题


## 应用场景
1. 业务系统的非功能性需求开发。比如：监控、统计、鉴权、限流、事务、幂等、日志
2. 代理模式在 RPC、缓存中的应用

主要目的是控制访问，而非加强功能


## 代理模式的实现
一般情况下，可以让代理类和原始类实现同样的接口。但是，如果原始类并没有定义接口，并且原始类代码并是别人开发维护的。在这种情况下，可以通过让代理类继承原始类的方法来实现代理模式


## 继承 vs 组合
继承模式只需要针对需要扩展的方法进行代理，但是只能针对单一父类进行代理

```java
public class UserControllerProxy extends UserController {
    private MetricsCollector metricsCollector;

    public UserControllerProxy() {
        this.metricsCollector = new MetricsCollector();
    }
}
```

组合模式更加灵活，接口的所有子类都可以代理，对于不需要扩展的方法也进行了代理

```java
public class MetricsCollectorProxy {
    private MetricsCollector metricsCollector;

    public MetricsCollectorProxy() {
        this.metricsCollector = new MetricsCollector();
    }

    public Object createProxy(Object proxiedObject) {
        Class<?>[] interfaces = proxiedObject.getClass().getInterfaces();
        DynamicProxyHandler handler = new DynamicProxyHandler(proxiedObject);
        return Proxy.newProxyInstance(proxiedObject.getClass().getClassLoader(), interfaces, handler);
    }

    private class DynamicProxyHandler implements InvocationHandler {
        private Object proxiedObject;

        public DynamicProxyHandler(Object proxiedObject) {
            this.proxiedObject = proxiedObject;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            long startTimestamp = System.currentTimeMillis();
            Object result = method.invoke(proxiedObject, args);
            long endTimeStamp = System.currentTimeMillis();
            long responseTime = endTimeStamp - startTimestamp;
            String apiName = proxiedObject.getClass().getName() + ":" + method.getName();
            RequestInfo requestInfo = new RequestInfo(apiName, responseTime, startTimestamp, endTimeStamp);
            metricsCollector.recordRequest(requestInfo);
            return result;
        }
    }
}
```

```java
MetricsCollectorProxy proxy = new MetricsCollectorProxy();
IUserController userController = (IUserController) proxy.createProxy(new UserController());
```