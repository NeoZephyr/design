package com.pain.design;

public class EmailMassSendHandler implements MassSendHandler {
    @Override
    public void send() {
        System.out.println("Email send...");
    }
}
