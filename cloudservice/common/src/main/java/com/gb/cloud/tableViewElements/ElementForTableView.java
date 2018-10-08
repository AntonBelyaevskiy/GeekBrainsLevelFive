package com.gb.cloud.tableViewElements;

import java.io.Serializable;
import java.util.Date;

public class ElementForTableView implements Serializable {

    private String name;
    private String size;
    private Date createDate;

    public ElementForTableView(String name, String size, Date createDate) {
        this.name = name;
        this.size = size + " КБ";
        this.createDate = createDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

}
