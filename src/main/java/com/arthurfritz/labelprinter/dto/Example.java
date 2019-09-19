package com.arthurfritz.labelprinter.dto;

public class Example {

    private String name;
    private String lastName;
    private String base64;

    public Example(String name, String lastName, String base64) {
        this.name = name;
        this.lastName = lastName;
        this.base64 = base64;
    }

    public String getName() {
        return name;
    }

    public String getLastName() {
        return lastName;
    }

    public String getBase64() {
        return base64;
    }
}
