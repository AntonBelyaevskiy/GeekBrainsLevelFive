package com.gb.cloud.message;

import com.gb.cloud.tableViewElements.ElementForTableView;

import java.util.ArrayList;

public class CloudFilesList extends CloudMessage {

    private String login;
    private ArrayList<ElementForTableView> serverStorageList;

    public CloudFilesList(String login) {
        this.login = login;
        serverStorageList = new ArrayList<>();
    }

    public String getLogin() {
        return login;
    }

    public ArrayList<ElementForTableView> getServerStorageList() {
        return serverStorageList;
    }

    public void setServerStorageList(ArrayList<ElementForTableView> serverStorageList) {
        this.serverStorageList = serverStorageList;
    }
}
