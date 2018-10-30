package com.gb.cloud.tableViewElements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ElementBuilder {

    public static ElementForTableView buildElement(Path path){
        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            return new ElementForTableView(path.getFileName().toString(),
                    String.valueOf(path.toFile().length()),
                    new Date(attributes.creationTime().to(TimeUnit.MILLISECONDS)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ElementForTableView("no element","",null);
    }
}
