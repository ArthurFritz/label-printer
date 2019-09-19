package com.arthurfritz.labelprinter.dto;

import javax.validation.constraints.NotBlank;

public class Message {

    @NotBlank
    private String name;
    @NotBlank
    private String lastName;
    private String printer;
    @NotBlank
    private String qrCode;

    public Message(){
    }

    public String getName() {
        return name;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPrinter() {
        return printer;
    }

    public String getQrCode() {
        return qrCode;
    }

}
