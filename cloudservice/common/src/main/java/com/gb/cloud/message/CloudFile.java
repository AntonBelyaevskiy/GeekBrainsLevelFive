package com.gb.cloud.message;

public class CloudFile extends CloudMessage {
    private String name;
    private byte[] content;
    private int parts;
    private int partNumber;

    public CloudFile(String name, byte[] content) {
        this.name = name;
        this.content = content;
    }

    public CloudFile(String name, byte[] content, int parts, int numberOfPart) {
        this(name, content);
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
