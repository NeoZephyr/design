package com.pain.design;

public class SmsMassSendHandler implements MassSendHandler {
    @Override
    public void send() {
        System.out.println("Sms send...");
    }
}
