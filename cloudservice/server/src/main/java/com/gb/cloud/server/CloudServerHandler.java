package com.gb.cloud.server;

import com.gb.cloud.message.*;
import com.gb.cloud.tableViewElements.ElementBuilder;
import com.gb.cloud.tableViewElements.ElementForTableView;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;

public class CloudServerHandler extends ChannelInboundHandlerAdapter {

    private String login;

    private ArrayBlockingQueue<Object> incomingData = new ArrayBlockingQueue<>(30);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        try {
            if (msg == null)
                return;

            incomingData.put(msg);

            if (msg instanceof CloudAuthorization) {
                CloudAuthorization message = (CloudAuthorization) incomingData.poll();
                if (message == null) {
                    return;
                }
                login = message.getLogin();
                CloudCommand admitUser = new CloudCommand("/allowToEnter");
                if (message.getStatus().equals("/create")) {
                    //создаем папку для юзера
                    Files.createDirectories(Paths.get("server/storage/" + login));
                    //отправляем команду, чтобы юзера впустили в облако
                    ctx.write(admitUser);
                    sendFileListToClient(ctx);
                }
                if (message.getStatus().equals("/enter")) {
                    //отправляем команду, чтобы юзера впустили в облако
                    ctx.write(admitUser);
                    sendFileListToClient(ctx);
                }
            }

            //действия, если прилетела команда
            else if (msg instanceof CloudCommand) {

                CloudCommand message = (CloudCommand) incomingData.poll();
                if (message == null) {
                    return;
                }

                Path path = Paths.get("server/storage/" + login + "/" + message.getFileName());
                switch (message.getCommandName()) {
                    case "/downloadFile":
                        long size = Files.size(path);
                        int partOfFile = 1024 * 500;
                        int parts = (int) Math.ceil((double) size / (double) partOfFile);
                        sendFile(path, parts, ctx);
                        break;
                    case "/deleteFile":
                        System.out.println(path);
                        Files.delete(path);
                        sendFileListToClient(ctx);
                        break;
                    case "/busyLogin":
                        ctx.write(msg);
                        break;
                    case "/wrongLoginOrPassword":
                        ctx.write(msg);
                        break;
                }
            }
            //действия, если прилетел файл
            else if (msg instanceof CloudFile) {
                CloudFile message = (CloudFile) incomingData.poll();
                if (message == null) {
                    return;
                }
                System.out.println("file came on server " + ((CloudFile) msg).getName()
                        + " file part: " + message.getPartNumber());
                saveFile(ctx, message);
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private void saveFile(ChannelHandlerContext ctx, CloudFile msg) throws IOException {
        Path path = Paths.get("server/storage/" + login + "/" + msg.getName());
        if (msg.getPartNumber() == 1) {
            byte[] content = msg.getContent();
            if (Files.exists(path)) {
                Files.delete(path);
                makeFile(path, content);
            } else {
                makeFile(path, content);
            }
        } else {
            byte[] content = msg.getContent();
            Files.write(path, content, StandardOpenOption.APPEND);
        }
        sendFileListToClient(ctx);
    }

    private void sendFileListToClient(ChannelHandlerContext ctx) {
        CloudFilesList message = new CloudFilesList(login);
        ArrayList<ElementForTableView> files = walkToLocalDirectory(login);
        message.setServerStorageList(files);
        ctx.write(message);
    }

    private void makeFile(Path path, byte[] content) throws IOException {
        Files.write(path, content, StandardOpenOption.CREATE_NEW);
    }

    private void sendFile(Path path, int parts, ChannelHandlerContext ctx) {

        int part = 1;
        int partSizeOfFile = 1024 * 500; //0.5 мегабайт
        int read;
        byte[] current = new byte[partSizeOfFile];

        try (InputStream fileForeSend = new BufferedInputStream(new FileInputStream(String.valueOf(path)))) {

            do {
                read = fileForeSend.read(current);
                if (read != -1) {
                    if (read == partSizeOfFile) {
                        CloudFile file = new CloudFile(
                                path.getFileName().toString(),
                                current,
                                parts,
                                part);
                        ctx.write(file);

                        System.out.println("ser middle part " + part);
                        part++;
                    }else {
                        byte[] lastPart = Arrays.copyOfRange(current,0,read);
                        CloudFile file = new CloudFile(
                                path.getFileName().toString(),
                                lastPart,
                                parts,
                                part);
                        ctx.write(file);
                        System.out.println("end part " + part);
                    }
                }
            } while (read != -1);

        } catch (IOException e) {
            e.printStackTrace();
        }
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
        ArrayList<ElementForTableView> files = new ArrayList<>();
        try {
            files = (ArrayList<ElementForTableView>) Files.list(Paths.get("server/storage/" + login))
                    .map(ElementBuilder::buildElement)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return files;
    }
}
