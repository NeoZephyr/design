package com.pain.red.singleton;

import java.util.concurrent.atomic.AtomicLong;

public enum  EnumIdGenerator {
    INSTANCE;

    private AtomicLong id = new AtomicLong(0);

    public long genId() {
        return id.incrementAndGet();
    }
}
