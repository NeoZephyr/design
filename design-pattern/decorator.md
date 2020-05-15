## 装饰器模式
装饰器模式使用用组合替代继承，但相对于简单的组合关系，有两个比较特殊的地方：
1. 装饰器类和原始类继承同样的父类，这样我们可以对原始类嵌套多个装饰器类
2. 装饰器类是对功能的增强。代理模式中，代理类附加的是跟原始类无关的功能；而在装饰器模式中，装饰器类附加的是跟原始类相关的增强功能


在 JDK 中，BufferedInputStream、DataInputStream 并非继承自 InputStream，而是 FilterInputStream 类

InputStream 是一个抽象类，它的大部分函数都有默认实现。通常来说，只需要在 BufferedInputStream 类中重新实现那些需要增加缓存功能的函数就可以了，其他函数继承 InputStream 的默认实现。但实际上，这是行不通的

虽然 BufferedInputStream 也是一个 InputStream，但它本身不作为任何 IO 通道的输入流，只有传入的 InputStream 才是具体的读取数据的输入流。因此，只有委托给传入的 InputStream 对象重新实现，才能完成最终读取数据的任务

DataInputStream 也存在跟 BufferedInputStream 同样的问题。为了避免代码重复，抽象出了一个装饰器父类 FilterInputStream。InputStream 的所有的装饰器类都继承自这个装饰器父类。这样，装饰器类只需要实现它需要增强的方法就可以了，其他方法继承装饰器父类的默认实现


## 装饰器模式 vs 代理模式
如果不需要用户关注是否使用增强/增加的功能，要隐藏实现细节，那么就使用代理模式

如果需要用户自己决定是否使用增强/增加的功能，那么就使用装饰器模式