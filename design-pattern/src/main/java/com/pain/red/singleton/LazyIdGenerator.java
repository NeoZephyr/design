package com.pain.red.singleton;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public class LazyIdGenerator extends IdGenerator implements Serializable {
    private AtomicLong id = new AtomicLong(0);
    private static IdGenerator instance;

    private LazyIdGenerator() {
        if (instance != null) {
            throw new RuntimeException("forbidden");
        }
    }

    private static synchronized IdGenerator getInstance() {
        if (instance == null) {
            instance = new LazyIdGenerator();
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
