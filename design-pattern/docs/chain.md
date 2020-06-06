行为型：解决类或对象之间的交互问题


链表方式
```java
public abstract class Handler {
    protected Handler successor = null;

    public void setSuccessor(Handler successor) {
        this.successor = successor;
    }

    public final void handle() {
        boolean handled = doHandle();

        if (successor != null && !handled) {
            successor.handle();
        }
    }

    protected abstract boolean doHandle();
}
```
```java
public class HandlerA extends Handler {
    public boolean doHandle() {
        boolean handled = false;
        return handled;
    }
}
```
```java
public class HandlerB extends Handler {
    public boolean handle() {
        boolean handled = false;
        return handled;
    }
}
```

```java
public class HandlerChain {
    private Handler head = null;
    private Handler tail = null;

    public void addHandler(Handler handler) {
        handler.setSuccessor(null);

        if (head == null) {
            head = handler;
            tail = handler;
            return;
        }

        tail.setSuccessor(handler);
        tail = handler;
    }

    public void handle() {
        if (head != null) {
            head.handle();
        }
    }
}
```

```java
public class Application {
    public static void main(String[] args) {
        HandlerChain chain = new HandlerChain();
        chain.addHandler(new HandlerA());
        chain.addHandler(new HandlerB());
        chain.handle();
    }
}
```

数组方式
```java
public interface IHandler {
    boolean handle();
}
```
```java
public class HandlerA implements IHandler {
    public boolean handle() {
        boolean handled = false;
        return handled;
    }
}

public class HandlerB implements IHandler {
    public boolean handle() {
        boolean handled = false;
        return handled;
    }
}
```
```java
public class HandlerChain {
    private List<IHandler> handlers = new ArrayList<>();

    public void addHandler(IHandler handler) {
        this.handlers.add(handler);
    }

    public void handle() {
        for (IHandler handler : handlers) {
            boolean handled = handler.handle();

            if (handled) {
                break;
            }
        }
    }
}
```

对于支持 UGC 的应用来说，用户生成的内容可能会包含一些敏感词。针对这个应用场景，可以利用职责链模式来过滤这些敏感词。对于包含敏感词的内容，有两种处理方式，一种是直接禁止发布，另一种是给敏感词打马赛克之后再发布

```java
public interface SensitiveWordFilter {
    boolean doFilter(Content content);
}
```
```java
public class SexyWordFilter implements SensitiveWordFilter {
    public boolean doFilter(Content content) {
        boolean legal = true;
        return legal;
    }
}
```
```java
public class SensitiveWordFilterChain {
    private List<SensitiveWordFilter> filters = new ArrayList<>();

    public void addFilter(SensitiveWordFilter filter) {
        this.filters.add(filter);
    }

    public boolean filter(Content content) {
        for (SensitiveWordFilter filter : filters) {
            if (!filter.doFilter(content)) {
                return false;
            }
        }

        return true;
    }
}
```

```java
public class ApplicationDemo {
    public static void main(String[] args) {
        SensitiveWordFilterChain filterChain = new SensitiveWordFilterChain();
        filterChain.addFilter(new AdsWordFilter());
        filterChain.addFilter(new SexyWordFilter());
        filterChain.addFilter(new PoliticalWordFilter());

        boolean legal = filterChain.filter(new Content());

        if (!legal) {
            //
        } else {}
    }
}
```

Servlet Filter
```java
public final class ApplicationFilterChain implements FilterChain {
    private int pos = 0;
    private int n;
    private ApplicationFilterConfig[] filters;
    private Servlet servlet;

    public void doFilter(ServletRequest request, ServletResponse response) {
        if (pos < n) {
            ApplicationFilterConfig filterConfig = filters[pos++];
            Filter filter = filterConfig.getFilter();
            filter.doFilter(request, response, this);
        } else {
            servlet.service(request, response);
        }
    }

    public void addFilter(ApplicationFilterConfig filterConfig) {
        for (ApplicationFilterConfig filter : filters) {
            if (filter == filterConfig) {
                return;
            }
        }

        if (n == filters.length) {
            ApplicationFilterConfig[] newFilters =
                new ApplicationFilterConfig[n];
            System.arraycopy(filters, 0, newFilters, 0, n);
            filters = newFilters;
        }

        filters[n++] = filterConfig;
    }
}
```
支持双向拦截，既能拦截客户端发送来的请求，也能拦截发送给客户端的响应

Spring Interceptor
```java
public class HandlerExecutionChain {
    private final Object handler;
    private HandlerInterceptor[] interceptors;

    public void addInterceptor(HandlerInterceptor interceptor) {
        initInterceptorList().add(interceptor);
    }

    boolean applyPreHandle(HttpServletRequest request, HttpServletResponse response) {
        HandlerInterceptor[] interceptors = getInterceptors();

        if (!ObjectUtils.isEmpty(interceptors)) {
            for (int i = 0; i < interceptors.length; i++) {
                HandlerInterceptor interceptor = interceptors[i];
                if (!interceptor.preHandle(request, response, this.handler)) {
                    triggerAfterCompletion(request, response, null);
                    return false;
                }
            }
        }

        return true;
    }

    void applyPostHandle(HttpServletRequest request, HttpServletResponse response) {
        HandlerInterceptor[] interceptors = getInterceptors();

        if (!ObjectUtils.isEmpty(interceptors)) {
            for (int i = interceptors.length - 1; i >= 0; i--) {
                HandlerInterceptor interceptor = interceptors[i];
                interceptor.postHandle(request, response, this.handler, mv);
            }
        }
    }

    void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HandlerInterceptor[] interceptors = getInterceptors();

        if (!ObjectUtils.isEmpty(interceptors)) {
            for (int i = this.interceptorIndex; i >= 0; i--) {
                HandlerInterceptor interceptor = interceptors[i];

                try {
                    interceptor.afterCompletion(request, response, this.handler, ex);
                } catch (Throwable ex2) {
                    logger.error("HandlerInterceptor.afterCompletion threw exception", ex2);
                }
            }
        }
    }
}
```