package com.pain.design;

public class Cache {

    private String key;
    private Integer time;
    private String path;
    private Integer limit;

    Cache(CacheBuilder cacheBuilder) {
        this.key = cacheBuilder.key;
        this.time = cacheBuilder.time;
        this.path = cacheBuilder.path;
        this.limit = cacheBuilder.limit;
    }

    @Override
    public String toString() {
        return "Cache{" +
                "key='" + key + '\'' +
                ", time=" + time +
                ", path='" + path + '\'' +
                ", limit=" + limit +
                '}';
    }

    static class CacheBuilder {
        private String key;
        private Integer time;
        private String path;
        private Integer limit;

        public CacheBuilder buildKey(String key) {
            this.key = key;
            return this;
        }

        public CacheBuilder buildTime(Integer time) {
            this.time = time;
            return this;
        }

        public CacheBuilder buildPath(String path) {
            this.path = path;
            return this;
        }

        public CacheBuilder buildLimit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public Cache build() {
            return new Cache(this);
        }
    }
}
