package com.gb.cloud.server;

import com.gb.cloud.message.CloudCommand;
import com.gb.cloud.message.CloudFile;
import com.gb.cloud.message.CloudFilesList;
import com.gb.cloud.tableViewElements.ElementForTableView;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

//Блок для работы с данными, поступающими на сервер
public class CloudServerHandler extends ChannelInboundHandlerAdapter {

    private String login;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client is connected...");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null)
                return;

            //самым первым запросом клиент попросит список файлов на сервере
            if (msg instanceof CloudFilesList) {
                CloudFilesList message = (CloudFilesList) msg;
                ArrayList<ElementForTableView> files = walkToLocalDirectory(message.getLogin());
                login = message.getLogin();
                System.out.println("Ask storage's files");

                CloudFilesList answer = new CloudFilesList(login);
                answer.setServerStorageList(files);
                ctx.write(answer);
            }
            //действия, если прилетела команда
            else if (msg instanceof CloudCommand) {
                if(((CloudCommand) msg).getCommandName().equals("/downloadFile")){
                    CloudFile file = takeFile(((CloudCommand) msg).getFileName());
                    ctx.write(file);
                } else if(((CloudCommand) msg).getCommandName().equals("/deleteFile")){
                    Path path = Paths.get("server/storage/" + login + "/" + ((CloudCommand) msg).getFileName());
                    System.out.println(path);
                    Files.delete(path);
                    //посылаем обновленный список файлов клиенту
                    CloudFilesList message = new CloudFilesList(login);
                    ArrayList<ElementForTableView> files = walkToLocalDirectory(login);
                    message.setServerStorageList(files);
                    ctx.write(message);

                }
            }
            //действия, если прилетел файл
            else if (msg instanceof CloudFile) {
                System.out.println("file came on server " + ((CloudFile) msg).getName());
                Path path = Paths.get("server/storage/" + login + "/" + ((CloudFile) msg).getName());
                byte[] content = ((CloudFile) msg).getContent();
                if (Files.exists(path)) {
                    Files.delete(path);
                    makeFile(path, content);
                } else {
                    makeFile(path, content);
                }
                //после того, как создали файл, сообщаем клиенту, что список файлов обновился
                ArrayList<ElementForTableView> files = walkToLocalDirectory(login);
                CloudFilesList answer = new CloudFilesList("cloudFilesList");
                answer.setServerStorageList(files);
                ctx.write(answer);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private void makeFile(Path path, byte[] content) throws IOException {
        Files.write(path, content, StandardOpenOption.CREATE_NEW);
    }

    private CloudFile takeFile(String fileName) {
        Path path = Paths.get("server/storage/" + login + "/" + fileName);
        byte[] content = new byte[(int) path.toFile().length()];
        try {
            content = Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new CloudFile(fileName, content);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private ArrayList<ElementForTableView> walkToLocalDirectory(String login) {
        Path path = Paths.get("server/storage/" + login);
        System.out.println(path);
        final ArrayList<ElementForTableView> files = new ArrayList<>();
        try {
            Files.walkFileTree(path, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    //задать логику при обнаружении директории
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
                    String name = file.getFileName().toString();
                    String size = String.valueOf(file.toFile().length());
                    Date createDate = new Date(attributes.creationTime().to(TimeUnit.MILLISECONDS));
                    files.add(new ElementForTableView(name, size, createDate));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return files;
    }
}
