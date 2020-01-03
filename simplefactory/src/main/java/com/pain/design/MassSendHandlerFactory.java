package com.pain.design;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class MassSendHandlerFactory {

    public MassSendHandler getMassSendHandlerV1(String type) {
        MassSendHandler massSendHandler = null;

        if ("sms".equals(type)) {
            massSendHandler = new SmsMassSendHandler();
        } else if ("email".equals(type)) {
            massSendHandler = new EmailMassSendHandler();
        }

        return massSendHandler;
    }

    public MassSendHandler getMassSendHandlerV2(String className) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotFoundException {
        MassSendHandler massSendHandler = null;
        Class<?> clazz = Class.forName(className);
        Constructor constructor = clazz.getDeclaredConstructor();

        if (constructor != null) {
            massSendHandler = (MassSendHandler) constructor.newInstance();
        }

        return massSendHandler;
    }
}
