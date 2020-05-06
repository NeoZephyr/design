package com.pain.red.singleton;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public class DoubleCheckIdGenerator extends IdGenerator implements Serializable {
    private AtomicLong id = new AtomicLong(0);

    // 避免重排序
    private static volatile IdGenerator instance;

    private DoubleCheckIdGenerator() {
        if (instance != null) {
            throw new RuntimeException("forbidden");
        }
    }

    public static IdGenerator getInstance() {
        if (instance == null) {
            synchronized (DoubleCheckIdGenerator.class) {
                if (instance == null) {
                    // 1. 分配内存
                    // 2. 初始化
                    // 3. 赋值给 instance
                    instance = new DoubleCheckIdGenerator();
                }
            }
        }

        return instance;
    }

    long genId() {
        return id.incrementAndGet();
    }

    public Object readResolve() {
        return instance;
    }
}
