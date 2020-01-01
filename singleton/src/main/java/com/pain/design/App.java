package com.pain.design;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Hello world!
 *
 */
public class App {

    // tools: jad 反编译
    public static void main( String[] args ) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // testHungrySingleton();
        // testLazySingleton();
        // testDoubleCheckLazySingleton();
        // testStaticInnerLazySingleton();

        // crackSingletonBySerialize();
        // crackSingletonByReflection();

        testEnumSingleton();
    }

    private static void testHungrySingleton() {
        new Thread(new Runnable() {
            public void run() {
                Singleton singleton = HungrySingleton.getInstance();
                System.out.println(Thread.currentThread().getName() + " - " + singleton);
            }
        }).start();

        new Thread(new Runnable() {
            public void run() {
                Singleton singleton = HungrySingleton.getInstance();
                System.out.println(Thread.currentThread().getName() + " - " + singleton);
            }
        }).start();

    }

    private static void testLazySingleton() {
        new Thread(new Runnable() {
            public void run() {
                Singleton singleton = LazySingleton.getInstance();
                System.out.println(Thread.currentThread().getName() + " - " + singleton);
            }
        }).start();

        new Thread(new Runnable() {
            public void run() {
                Singleton singleton = LazySingleton.getInstance();
                System.out.println(Thread.currentThread().getName() + " - " + singleton);
            }
        }).start();
    }

    private static void testDoubleCheckLazySingleton() {
        new Thread(new Runnable() {
            public void run() {
                Singleton singleton = DoubleCheckLazySingleton.getInstance();
                System.out.println(Thread.currentThread().getName() + " - " + singleton);
            }
        }).start();

        new Thread(new Runnable() {
            public void run() {
                Singleton singleton = DoubleCheckLazySingleton.getInstance();
                System.out.println(Thread.currentThread().getName() + " - " + singleton);
            }
        }).start();
    }

    private static void testStaticInnerLazySingleton() {
        new Thread(new Runnable() {
            public void run() {
                Singleton singleton = StaticInnerLazySingleton.getInstance();
                System.out.println(Thread.currentThread().getName() + " - " + singleton);
            }
        }).start();

        new Thread(new Runnable() {
            public void run() {
                Singleton singleton = StaticInnerLazySingleton.getInstance();
                System.out.println(Thread.currentThread().getName() + " - " + singleton);
            }
        }).start();
    }

    private static void crackSingletonBySerialize() throws IOException, ClassNotFoundException {
        Singleton singleton = HungrySingleton.getInstance();

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("singleton"));
        oos.writeObject(singleton);
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("singleton"));
        Singleton newSingleton = (Singleton) ois.readObject();

        System.out.println(singleton);
        System.out.println(newSingleton);
        System.out.println(singleton == newSingleton);
    }

    private static void crackSingletonByReflection() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class clazz = HungrySingleton.class;
        Constructor constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);

        Singleton newSingleton = (Singleton) constructor.newInstance();
        Singleton singleton = HungrySingleton.getInstance();

        System.out.println(singleton);
        System.out.println(newSingleton);
        System.out.println(singleton == newSingleton);
    }

    private static void testEnumSingleton() throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        EnumSingleton singleton = EnumSingleton.getInstance();

        singleton.setData(new Object());

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("singleton"));
        oos.writeObject(singleton);
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("singleton"));
        EnumSingleton serialSingleton = (EnumSingleton) ois.readObject();

        System.out.println(singleton.getData());
        System.out.println(serialSingleton.getData());
        System.out.println(singleton.getData() == serialSingleton.getData());

        singleton.doSth();

        Class clazz = EnumSingleton.class;
        Constructor constructor = clazz.getDeclaredConstructor(String.class, int.class);
        constructor.setAccessible(true);

        EnumSingleton reflectSingleton = (EnumSingleton) constructor.newInstance("name", 10);
        System.out.println(reflectSingleton);
        System.out.println(singleton == reflectSingleton);
    }
}
