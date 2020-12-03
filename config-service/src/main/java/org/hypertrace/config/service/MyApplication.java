package org.hypertrace.config.service;

class MyApplication {

    public static void main(String[] args) {
        System.out.println("Started application. GetText: " + new MyServiceImplementation().getText());
    }
}