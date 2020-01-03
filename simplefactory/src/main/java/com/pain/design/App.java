package com.pain.design;

import java.lang.reflect.InvocationTargetException;

/**
 * Hello world!
 *
 */
public class App {
    public static void main( String[] args ) throws InvocationTargetException, NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        MassSendHandlerFactory massSendHandlerFactory = new MassSendHandlerFactory();
        MassSendHandler massSendHandler = massSendHandlerFactory.getMassSendHandlerV1("email");
        massSendHandler.send();

        massSendHandler = massSendHandlerFactory.getMassSendHandlerV2("com.pain.design.SmsMassSendHandler");
        massSendHandler.send();
    }
}
