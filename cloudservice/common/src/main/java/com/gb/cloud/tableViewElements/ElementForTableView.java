package com.gb.cloud.tableViewElements;

import java.io.Serializable;
import java.util.Date;

public class ElementForTableView implements Serializable {

    private String name;
    private String size;
    private Date createDate;

    public ElementForTableView(String name, String size, Date createDate) {
        this.name = name;
        this.size = String.valueOf((int)Math.ceil((double) Integer.parseInt(size)/1024)) + " КБ";
        this.createDate = createDate;


    }

    public String getName() {
        return name;
    }

    public String getSize() {
        return size;
    }

    public Date getCreateDate() {
        return createDate;
    }
}
