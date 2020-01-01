package com.pain.design;

public class LazySingleton extends Singleton {
    private static LazySingleton instance = null;

    private LazySingleton() {}

    public static synchronized LazySingleton getInstance() {
        if (instance == null) {
            instance = new LazySingleton();
        }

        return instance;
    }
}
