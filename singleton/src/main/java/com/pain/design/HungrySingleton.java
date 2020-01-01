package com.pain.design;

import java.io.Serializable;

public class HungrySingleton extends Singleton implements Serializable {
    private static HungrySingleton instance = new HungrySingleton();

    private HungrySingleton() {

        // 防止反射破坏
        if (instance != null) {
            throw new RuntimeException("forbidden");
        }
    }

    public static HungrySingleton getInstance() {
        return instance;
    }

    /**
     * 防止序列化破坏单例
     * @return
     */
    private Object readResolve() {
        return instance;
    }
}
