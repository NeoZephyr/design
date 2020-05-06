package com.pain.red.singleton;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public class HungryIdGenerator extends IdGenerator implements Serializable {
    private AtomicLong id = new AtomicLong(0);
    private static final IdGenerator instance = new HungryIdGenerator();

    /**
     * 防止反射破坏
     */
    private HungryIdGenerator() {
        if (instance != null) {
            throw new RuntimeException("forbidden");
        }
    }

    public static IdGenerator getInstance() {
        return instance;
    }

    long genId() {
        return id.incrementAndGet();
    }

    /**
     * 防止序列化破坏单例
     * @return
     */
    public Object readResolve() {
        return instance;
    }
}
