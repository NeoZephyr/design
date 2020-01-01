package com.pain.design;

public class StaticInnerLazySingleton extends Singleton {

    // Class 对象初始化
    // 1. 实例化对象
    // 2. 类中静态方法被调用
    // 3. 类中静态成员被赋值
    // 4. 类中静态成员被使用，且不是常量成员
    // 5. 为顶级类，有嵌套断言语句

    // 单例依赖 InnerClass 的初始化锁
    private static class InnerClass {
        private static StaticInnerLazySingleton instance = new StaticInnerLazySingleton();
    }

    private StaticInnerLazySingleton() {}

    public static StaticInnerLazySingleton getInstance() {
        return InnerClass.instance;
    }
}
