package com.pain.red.singleton;

import java.util.concurrent.atomic.AtomicLong;

public class StaticInnerIdGenerator extends IdGenerator {
    private AtomicLong id = new AtomicLong(0);

    private StaticInnerIdGenerator() {
        if (IdGeneratorHolder.instance != null) {
            throw new RuntimeException("forbidden");
        }
    }

    public static IdGenerator getInstance() {
        return IdGeneratorHolder.instance;
    }

    private static class IdGeneratorHolder {
        private static IdGenerator instance = new StaticInnerIdGenerator();
    }

    long genId() {
        return id.incrementAndGet();
    }

    public Object readResolve() {
        return IdGeneratorHolder.instance;
    }
}
