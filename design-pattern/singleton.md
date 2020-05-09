## 单例缺陷
单例对 OOP 特性的支持不友好。比如在将来，希望针对不同的业务采用不同的 ID 生成算法，会涉及比较大的改动

单例类不需要显示创建、不需要依赖参数传递，在函数中直接调用就可以，使得调用关系非常隐蔽

单例对代码的扩展性不友好。比如在将来，需要在代码中创建两个实例或多个实例，就要对代码有比较大的改动

单例对代码的可测试性不友好

单例不支持有参数的构造函数