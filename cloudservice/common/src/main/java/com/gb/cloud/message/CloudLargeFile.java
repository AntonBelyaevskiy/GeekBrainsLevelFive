package com.gb.cloud.message;

public class CloudLargeFile extends CloudMessage {
    private String name;
    private byte[] content;
    private int parts;
    private int partNumber;

    public CloudLargeFile(String name, byte[] content, int parts, int numberOfPart) {
        this.name = name;
        this.content = content;
        this.parts = parts;
        this.partNumber = numberOfPart;
    }

    public String getName() {
        return name;
    }

    public byte[] getContent() {
        return content;
    }

    public int getParts() {
        return parts;
    }

    public int getPartNumber() {
        return partNumber;
    }
}
