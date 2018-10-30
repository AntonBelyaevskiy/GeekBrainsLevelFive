package com.gb.cloud.message;



public class CloudSmallFile extends CloudMessage {
    private String name;
    private byte[] content;

    public CloudSmallFile(String name, byte[] content) {
        this.name = name;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public byte[] getContent() {
        return content;
    }
}
