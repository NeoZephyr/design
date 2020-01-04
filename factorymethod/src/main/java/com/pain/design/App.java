package com.pain.design;

/**
 * Hello world!
 *
 */
public class App {
    /**
     * Collection extends Iterable -> AbstractFactory extends AbstractAble
     * ArrayList extends Collection -> SpecificFactoryA extends AbstractFactory
     * LinkedList extends Collection -> SpecificFactoryB extends AbstractFactory
     * Iterator<E> iterator() {} -> (Exporter exporter() {} / Sender sender() {})
     * @param args
     */
    public static void main( String[] args ) {
        System.out.println( "Hello World!" );
    }
}
