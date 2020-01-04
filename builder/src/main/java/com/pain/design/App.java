package com.pain.design;

/**
 * Hello world!
 *
 */
public class App {
    public static void main( String[] args ) {
        // refer guava cache
        Cache cache = new Cache.CacheBuilder()
                .buildKey("customer")
                .buildLimit(200 * 1024)
                .buildPath("/tmp")
                .buildTime(5 * 60 * 1000)
                .build();

        System.out.println(cache);
    }
}