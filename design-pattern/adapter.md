## 实现方式
接口定义
```java
public interface ITarget {
    void f1();
    void f2();
    void fc();
}
```

不兼容 ITarget 接口定义的类
```java
public class Adaptee {
    public void fa() {}
    public void fb() {}
    public void fc() {}
}
```

1. 如果 Adaptee 接口并不多，两种实现方式都可以接受
2. 如果 Adaptee 接口很多，而且 Adaptee 和 ITarget 接口定义大部分都相同，推荐使用类适配器，因为 Adaptor 复用父类 Adaptee 的接口，比起对象适配器的实现方式，Adaptor 的代码量要少一些
3. 如果 Adaptee 接口很多，而且 Adaptee 和 ITarget 接口定义大部分都不相同，推荐使用对象适配器，因为组合结构相对于继承更加灵活

### 类适配器
```java
public class Adaptor extends Adaptee implements ITarget {
    public void f1() {
        super.fa();
    }

    public void f2() {}
}
```

### 对象适配器
```java
public class Adaptor implements ITarget {
    private Adaptee adaptee;

    public Adaptor(Adaptee adaptee) {
        this.adaptee = adaptee;
    }

    public void f1() {
        adaptee.fa();
    }

    public void f2() {   
    }

    public void fc() {
        adaptee.fc();
    }
}
```


## 应用场景
### 封装有缺陷的接口
依赖的外部系统在接口设计方面有缺陷，引入之后会影响到代码的可测试性。为了隔离设计上的缺陷，希望对外部系统提供的接口进行二次封装，抽象出更好的接口设计

```java
public class CD {
    public static void staticFunc() {}
    public void uglyNamingFunc() {}
    public void tooManyParamsFunc(int paramA, int paramB) {}
    public void lowPerformanceFunc() {}
}
```

使用适配器模式进行重构
```java
public class ITarget {
    void function1();
    void function2();
    void fucntion3(ParamsWrapperDefinition paramsWrapper);
    void function4();
}
```
```java
public class CDAdaptor extends CD implements ITarget {
    public void function1() {
        super.staticFunc();
    }

    public void function2() {
        super.uglyNamingFunc();
    }

    public void function3(ParamsWrapperDefinition paramsWrapper) {
        super.tooManyParamsFunc(paramsWrapper.getParamA(), paramsWrapper.getParamB());
    }

    public void function4() {}
}
```

### 统一多个类的接口
某个功能的实现依赖多个外部系统。通过适配器模式，将它们的接口适配为统一的接口定义，然后就可以使用多态的特性来复用代码逻辑

假设我们的系统要对用户输入的文本内容做敏感词过滤，我们引入了多款第三方敏感词过滤系统，依次对用户输入的内容进行过滤，过滤掉尽可能多的敏感词。但是，每个系统提供的过滤接口都是不同的。这就意味着我们没法复用一套逻辑来调用各个系统

```java
public class ASensitiveWordsFilter {
    public String filterSexyWords(String text) {}
    public String filterPoliticalWords(String text) {}
}
```
```java
public class BSensitiveWordsFilter {
    public String filter(String text) {}
}
```
```java
public class CSensitiveWordsFilter {
    public String filter(String text, String mask) {}
}
```

```java
public class RiskManagement {
    private ASensitiveWordsFilter aFilter = new ASensitiveWordsFilter();
    private BSensitiveWordsFilter bFilter = new BSensitiveWordsFilter();
    private CSensitiveWordsFilter cFilter = new CSensitiveWordsFilter();

    public String filterSensitiveWords(String text) {
        String maskedText = aFilter.filterSexyWords(text);
        maskedText = aFilter.filterPoliticalWords(maskedText);
        maskedText = bFilter.filter(maskedText);
        maskedText = cFilter.filter(maskedText, "***");
        return maskedText;
    }
}
```

使用适配器模式，将所有系统的接口适配为统一的接口定义，以便于复用调用敏感词过滤的代码

```java
public interface ISensitiveWordsFilter {
    String filter(String text);
}
```
```java
public class ASensitiveWordsFilterAdaptor implements ISensitiveWordsFilter {
    private ASensitiveWordsFilter aFilter;

    public String filter(String text) {
        String maskedText = aFilter.filterSexyWords(text);
        maskedText = aFilter.filterPoliticalWords(maskedText);
        return maskedText;
    }
}
```
```java
public class RiskManagement {
    private List<ISensitiveWordsFilter> filters = new ArrayList<>();

    public void addSensitiveWordsFilter(ISensitiveWordsFilter filter) {
        filters.add(filter);
    }

    public String filterSensitiveWords(String text) {
        String maskedText = text;

        for (ISensitiveWordsFilter filter : filters) {
            maskedText = filter.filter(maskedText);
        }

        return maskedText;
    }
}
```

### 替换依赖的外部系统
当替换项目中依赖时，利用适配器模式，可以减少对代码的改动

```java
public interface IA {
    void fa();
}
```
```java
public class A implements IA {
    public void fa() {}
}
```
```java
public class Demo {
    private IA a;

    public Demo(IA a) {
        this.a = a;
    }
}

Demo d = new Demo(new A());
```

将外部系统 A 替换成外部系统 B
```java
public class BAdaptor implemnts IA {
    private B b;

    public BAdaptor(B b) {
        this.b = b;
    }

    public void fa() {
        b.fb();
    }
}
```

```java
Demo d = new Demo(new BAdaptor(new B()));
```

### 兼容老版本接口
在版本升级的时候，对于一些要废弃的接口，不直接将其删除，而是标注为 deprecated，暂时保留，并将内部实现逻辑委托为新的接口实现

JDK1.0 中包含一个遍历集合容器的类 Enumeration。JDK2.0 对这个类进行了重构，将它改名为 Iterator 类，并且对它的代码实现做了优化。但是如果将 Enumeration 直接 从 JDK2.0 中删除，那使用 JDK1.0 的项目如果切换到 JDK2.0，代码就会编译不通过。为了做到兼容使用低版本 JDK 的老代码， 可以暂时保留 Enumeration 类，并将其实现替换为直接调用 Itertor

```java
public class Collections {
    public static Emueration emumeration(final Collection c) {
        return new Enumeration() {
            Iterator i = c.iterator();

            public boolean hasMoreElments() {
                return i.hashNext();
            }

            public Object nextElement() {
                return i.next();
            }
        }
    }
}
```

### 适配不同格式的数据


## 代理、桥接、装饰器、适配器
代理模式：在不改变原始类接口的条件下，为原始类定义一个代理类，主要目的是控制访问，而非加强功能，这是它跟装饰器模式最大的不同

桥接模式：将接口部分和实现部分分离，从而让它们可以较为容易、也相对独立地加以改变

装饰器模式：在不改变原始类接口的情况下，对原始类功能进行增强，并且支持多个装饰器的嵌套使用

适配器模式：一种事后的补救策略。适配器提供跟原始类不同的接口，而代理模式、装饰器模式提供的都是跟原始类相同的接口

