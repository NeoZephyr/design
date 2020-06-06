行为型：解决类或对象之间的交互问题


从网站上爬取 PDF、PPT、Word 等资源文件。通过工具把这些资源文件中的文本内容抽取出来放到 txt 文件中

```java
public abstract class ResourceFile {
    protected String filePath;

    public ResourceFile(String filePath) {
        this.filePath = filePath;
    }

    public abstract void extract2txt();
}
```
```java
public class PPTFile extends ResourceFile {
    public PPTFile(String filePath) {
        super(filePath);
    }

    public void extract2txt() {
        System.out.println("Extract PPT.");
    }
}
```
```java
public class PdfFile extends ResourceFile {
    public PdfFile(String filePath) {
        super(filePath);
    }

    public void extract2txt() {
        System.out.println("Extract PDF.");
    }
}
```
```java
public class WordFile extends ResourceFile {
    public WordFile(String filePath) {
        super(filePath);
    }

    public void extract2txt() {
        System.out.println("Extract WORD.");
    }
}
```

如果还需要支持压缩、提取文件元信息等一系列的功能，继续按照上面的实现思路，就会存在这样几个问题:
违背开闭原则，添加一个新的功能，所有类的代码都要修改
每个类的代码都不断膨胀，可读性和可维护性都变差了
所有比较上层的业务逻辑都耦合到 PdfFile、PPTFile、WordFile 类中，导致这些类的职责不够单一


```java
public abstract class ResourceFile {
    protected String filePath;

    public ResourceFile(String filePath) {
        this.filePath = filePath;
    }
}
```
```java
public class PdfFile extends ResourceFile {
    public PdfFile(String filePath) {
        super(filePath);
    }
}
```
```java
public class Extractor {
    public void extract2txt(PPTFile pptFile) {
        System.out.println("Extract PPT.");
    }

    public void extract2txt(PdfFile pdfFile) {
        System.out.println("Extract PDF.");
    }

    public void extract2txt(WordFile wordFile) {
        System.out.println("Extract WORD.");
    }
}
```

```java
Extractor extractor = new Extractor();
List<ResourceFile> resourceFiles = listAllResourceFiles(args[0]);

for (ResourceFile resourceFile : resourceFiles) {
    extractor.extract2txt(resourceFile);
}
```
多态是一种动态绑定，可以在运行时获取对象的实际类型，来运行实际类型对应的方法。而函数重载是一种静态绑定，在编译时并不能获取对象的实际类型，而是根据声明类型执行声明类型对应的方法

由于并没有在 Extractor 类中定义参数类型是 ResourceFile 的重载函数，所以编译无法通过


```java
public abstract class ResourceFile {
    protected String filePath;

    public ResourceFile(String filePath) {
        this.filePath = filePath;
    }

    abstract public void accept(Extractor extractor);

    abstract public void accept(Compressor compressor);
}
```
```java
public class PdfFile extends ResourceFile {
    public PdfFile(String filePath) {
        super(filePath);
    }

    public void accept(Extractor extractor) {
        extractor.extract2txt(this);
    }

    public void accept(Compressor compressor) {
        compressor.compress(this);
    }
}
```
```java
Extractor extractor = new Extractor();
List<ResourceFile> resourceFiles = listAllResourceFiles(args[0]);

for (ResourceFile resourceFile : resourceFiles) {
    resourceFile.accept(extractor);
}
```

添加一个新的业务，还是需要修改每个资源文件类，违反了开闭原则。针对这个问题，我们抽象出来一个 Visitor 接口，包含是三个命名非常通用的 visit() 重载函数，分别处理三种不同类型的资源文件

```java
public abstract class ResourceFile {
    protected String filePath;
    
    public ResourceFile(String filePath) {
        this.filePath = filePath;
    }

    abstract public void accept(Visitor vistor);
}
```
```java
public class PdfFile extends ResourceFile {
    public PdfFile(String filePath) {
        super(filePath);
    }

    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
```
```java
public interface Visitor {
    void visit(PdfFile pdfFile);
    void visit(PPTFile pdfFile);
    void visit(WordFile pdfFile);
}
```
```java
public class Extractor implements Visitor {
    public void visit(PPTFile pptFile) {
        System.out.println("Extract PPT.");
    }

    public void visit(PdfFile pdfFile) {
        System.out.println("Extract PDF.");
    }

    public void visit(WordFile wordFile) {
        System.out.println("Extract WORD.");
    }
}
```
```java
public class Compressor implements Visitor {
    public void visit(PPTFile pptFile) {
        System.out.println("Compress PPT.");
    }

    public void visit(PdfFile pdfFile) {
        System.out.println("Compress PDF.");
    }

    public void visit(WordFile wordFile) {
        System.out.println("Compress WORD.");
    }
}
```

```java
Extractor extractor = new Extractor();
List<ResourceFile> resourceFiles = listAllResourceFiles(args[0]);

for (ResourceFile resourceFile : resourceFiles) {
    resourceFile.accept(extractor);
}
```

Single Dispatch
```java
public class ParentClass {
    public void f() {
        System.out.println("I am ParentClass's f().");
    }
}

public class ChildClass extends ParentClass {
    public void f() {
        System.out.println("I am ChildClass's f().");
    }
}

public class SingleDispatchClass {
    public void polymorphismFunction(ParentClass p) {
        p.f();
    }

    public void overloadFunction(ParentClass p) {
        System.out.println("I am overloadFunction(ParentClass p).");
    }

    public void overloadFunction(ChildClass c) {
        System.out.println("I am overloadFunction(ChildClass c).");
    }
}
```
```java
SingleDispatchClass demo = new SingleDispatchClass();
ParentClass p = new ChildClass();
demo.polymorphismFunction(p);
demo.overloadFunction(p);
```


```java
public abstract class ResourceFile {
    protected String filePath;

    public ResourceFile(String filePath) {
        this.filePath = filePath;
    }

    public abstract ResourceFileType getType();
}
```
```java
public class PdfFile extends ResourceFile {
    public PdfFile(String filePath) {
        super(filePath);
    }

    public ResourceFileType getType() {
        return ResourceFileType.PDF;
    }
}
```
```java
public interface Extractor {
    void extract2txt(ResourceFile resourceFile);
}
```
```java
public class PdfExtractor implements Extractor {
    public void extract2txt(ResourceFile resourceFile) {}
}
```
```java
public class ExtractorFactory {
    private static final Map<ResourceFileType, Extractor> extractors = new HashMap<>();

    static {
        extractors.put(ResourceFileType.PDF, new PdfExtractor());
        extractors.put(ResourceFileType.PPT, new PPTExtractor());
        extractors.put(ResourceFileType.WORD, new WordExtractor());
    }

    public static Extractor getExtractor(ResourceFileType type) {
        return extractors.get(type);
    }
}
```

当需要添加压缩功能时，只需要添加一个 Compressor 接口，PdfCompressor、PPTCompressor、 WordCompressor 三个实现类，以及创建它们的 CompressorFactory 工厂类


