## 高内聚、松耦合

### 高内聚
相近的功能放到同一个类中，不相近的功能不放到同一个类中。相近的功能往往会被同时修改，放到同一个类中，修改会比较集中，代码容易维护

### 松耦合
类与类之间的依赖关系简单清晰，即使两个类有依赖关系，一个类的代码改动不会或者很少导致依赖类的代码改动

### 最小知识原则
#### 不该有直接依赖关系的类之间，不要有依赖

爬取网页的功能

负责底层网络通信，根据请求获取数据
```java
public class NetworkTransporter {
    public Byte[] send(HtmlRequest htmlRequest) {}
}
```

通过 URL 获取网页
```java
public class HtmlDownloader {
    private NetworkTransporter transporter;

    public Html downloadHtml(String url) {
        Byte[] rawHtml = transporter.send(new HtmlRequest(url));
        return new Html(rawHtml);
    }
}
```

网页文档
```java
public class Document {
    private Html html;
    private String url;

    public Document(String url) {
        this.url = url;
        HtmlDownloader downloader = new HtmlDownloader();
        this.html = downloader.downloadHtml(url);
    }
}
```

以上代码虽然能够工作，但根据迪米特法则，可以看出有诸多缺陷：
1. NetworkTransporter 类作为底层网络通信类，功能应该尽可能通用，而不只是服务于下载 HTML，因此依赖了不该有直接依赖关系的 HtmlRequest 类
2. Document 类构造函数中的 downloader.downloadHtml() 逻辑复杂，耗时长，不应该放到构造函数中；HtmlDownloader 对象在构造函数中通过 new 来创建，违反了基于接口而非实现编程的设计思想；Document 网页文档没必要依赖 HtmlDownloader 类，违背了迪米特法则

```java
public class NetworkTransporter {
    public Byte[] send(String address, Byte[] data) {}
}
```
```java
public class HtmlDownloader {
    private NetworkTransporter transporter;

    public Html downloadHtml(String url) {
        HtmlRequest htmlRequest = new HtmlRequest(url);
        Byte[] rawHtml = transporter.send(htmlRequest.getAddress(),
            htmlRequest.getContent().getBytes());
        return new Html(rawHtml);
    }
}
```
```java
public class Document {
    private Html html;
    private String url;

    public Document(String url, Html html) {
        this.html = html;
        this.url = url;
    }
}

public class DocumentFactory {
    private HtmlDownloader downloader;

    public DocumentFactory(HtmlDownloader downloader) {
        this.downloader = downloader;
    }

    public Document createDocument(String url) {
        Html html = downloader.downloadHtml(url);
        return new Document(url, html);
    }
}
```

#### 有依赖关系的类之间，尽量只依赖必要的接口
Serialization 类负责对象的序列化和反序列化，如果我们的项目中，有些类只用到了序列化操作。基于迪米特法则后半部分：有依赖关系的类之间，尽量只依赖必要的接口。只用到序列化操作的那部分类不应该依赖反序列化接口。我们可以将 Serialization 类拆分为如下两个更小粒度的类：
```java
public class Serializer {
    public String serialize(Object object) {}
}

public class Deserializer {
    public Object deserialize(String str) {}
}
```

可以看出，尽管拆分之后的代码更能满足迪米特法则，但却违背了高内聚的设计思想。如果我们修改了序列化的实现方式，比如从 JSON 换成了 XML，那反序列化的实现逻辑也需要一并修改。通过引入两个接口解决不能高内聚的问题：
```java
public interface Serializable {
    String serialize(Object object);
}

public interface Deserializable {
    Object deserialize(String text);
}
```
```java
public class Serialization implements Serializable, Deserializable {

    @Override
    public String serialize(Object object) {}

    @Override
    public Object deserialize(String str) {}
}
```





