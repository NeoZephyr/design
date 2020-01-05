## 接口调用统计
获取接口调用的各种统计信息，并且支持将统计结果以各种显示格式输出到各种终端，以方便查看

### 分析
#### 功能性需求分析
1. 接口统计信息
2. 统计信息的类型
3. 统计信息显示格式
4. 统计信息显示终端
5. 统计触发方式，包括主动和被动两种
6. 统计时间区间
7. 统计时间间隔

#### 非功能性需求分析
1. 易用性：是否易集成、易插拔、跟业务代码是否松耦合、提供的接口是否够灵活等
2. 性能：低延迟，内存的消耗不能太大等
3. 扩展性：使用者可以在不修改框架源码，甚至不拿到框架源码的情况下，为框架扩展新的功能

```java
// 方便扩展编解码方式、日志、拦截器等
Feign feign = Feign.builder()
        .logger(new CustomizedLogger())
        .encoder(new FormEncoder(new JacksonEncoder()))
        .decoder(new JacksonDecoder())
        .errorDecoder(new ResponseErrorDecoder())
        .requestInterceptor(new RequestHeadersInterceptor())
        .build();

public class RequestHeadersInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        // ...
    }
}

public class CustomizedLogger extends feign.Logger {}

public class ResponseErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        // ...
    }
}
```

4. 容错性：不能因为框架本身的异常导致接口请求出错，对外暴露的接口抛出的所有运行时、非运行时异常都进行捕获处理

5. 通用性：是否还可以处理其他事件的统计信息，比如 SQL 请求时间的统计信息、业务统计信息

### 框架设计


