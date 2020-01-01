package com.pain.design;

public class DoubleCheckLazySingleton extends Singleton {
    // 避免重排序
    private volatile static DoubleCheckLazySingleton instance = null;

    private DoubleCheckLazySingleton() {}

    public static DoubleCheckLazySingleton getInstance() {
        if (instance == null) {
            synchronized (DoubleCheckLazySingleton.class) {
                if (instance == null) {
                    // 1. 分配内存
                    // 2. 初始化
                    // 3. 赋值给 instance
                    instance = new DoubleCheckLazySingleton();
                }
            }
        }

        return instance;
    }
}
