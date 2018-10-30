package com.gb.cloud.message;

public class CloudCommand extends CloudMessage {
    private String commandName;
    private String fileName;


    public CloudCommand(String commandName) {
        this.commandName = commandName;
    }

    public CloudCommand(String commandName, String fileName) {
        this.commandName = commandName;
        this.fileName = fileName;
    }

    public String getCommandName() {
        return commandName;
    }

    public String getFileName() {
        return fileName;
    }
}
