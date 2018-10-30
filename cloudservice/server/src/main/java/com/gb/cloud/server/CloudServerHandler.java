package com.gb.cloud.server;

import com.gb.cloud.message.*;
import com.gb.cloud.tableViewElements.ElementBuilder;
import com.gb.cloud.tableViewElements.ElementForTableView;
import com.gb.cloud.transfer.Crypt;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class CloudServerHandler extends ChannelInboundHandlerAdapter {

    private String login;


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg == null)
                return;

            if (msg instanceof CloudAuthorization) {
                CloudAuthorization message = (CloudAuthorization) msg;
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
                Path path = Paths.get("server/storage/" + login + "/" + ((CloudCommand) msg).getFileName());
                switch (((CloudCommand) msg).getCommandName()) {
                    case "/downloadFile":
                        long size = Files.size(path);
                        int partOfFile = 1024 * 500 ;
                        int parts = (int) Math.ceil((double) (size / partOfFile));

                        if (size > partOfFile) {
                            sendLargeFile(path, parts, ctx); //отправляем частями
                        } else {
                            CloudSmallFile file = takeFile(((CloudCommand) msg).getFileName());
                            ctx.write(file);
                        }
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
            //действия, если прилетел маленький файл
            else if (msg instanceof CloudSmallFile) {
                System.out.println("file came on server " + ((CloudSmallFile) msg).getName());
                Path path = Paths.get("server/storage/" + login + "/" + ((CloudSmallFile) msg).getName());
                byte[] content = ((CloudSmallFile) msg).getContent();
                if (Files.exists(path)) {
                    Files.delete(path);
                    makeFile(path, content);
                } else {
                    makeFile(path, content);
                }
                //после того, как создали файл, сообщаем клиенту, что список файлов обновился
                sendFileListToClient(ctx);
            }
            //действия, если прилетел большой файл
            else if (msg instanceof CloudLargeFile) {
                System.out.println("file came on server "
                        + ((CloudLargeFile) msg).getName()
                        + " file part: "
                        + ((CloudLargeFile) msg).getPartNumber());
                Path path = Paths.get("server/storage/" + login + "/" + ((CloudLargeFile) msg).getName());

                if (((CloudLargeFile) msg).getPartNumber() == 1) {
                    byte[] content = ((CloudLargeFile) msg).getContent();
                    if (Files.exists(path)) {
                        Files.delete(path);
                        makeFile(path, content);
                    } else {
                        makeFile(path, content);
                    }
                } else {
                    byte[] content = ((CloudLargeFile) msg).getContent();
                    Files.write(path, content, StandardOpenOption.APPEND);
                }
                sendFileListToClient(ctx);

            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
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

    private CloudSmallFile takeFile(String fileName) {
        Path path = Paths.get("server/storage/" + login + "/" + fileName);
        byte[] content = new byte[(int) path.toFile().length()];
        try {
            content = Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new CloudSmallFile(fileName, content);
    }

    private void sendLargeFile(Path path, int parts, ChannelHandlerContext ctx) {

        int part = 1;
        int partSizeOfFile = 1024 * 500; //0.5 мегабайт
        int currentPlaceInArray = 0;
        int read;
        byte[] current = new byte[partSizeOfFile];

        try (InputStream fileForeSend = new BufferedInputStream(new FileInputStream(String.valueOf(path)))) {

            do {
                read = fileForeSend.read();
                if (read != -1) {
                    current[currentPlaceInArray++] = (byte) read;

                    if (currentPlaceInArray == partSizeOfFile) {
                        CloudLargeFile file = new CloudLargeFile(
                                path.getFileName().toString(),
                                current,
                                parts,
                                part);
                        ctx.write(file);

                        currentPlaceInArray = 0;
                        System.out.println("ser midl part " + part);
                        part++;
                    }
                }
                else {
                    byte[] lastPart = new byte[currentPlaceInArray];
                    System.arraycopy(current,0,lastPart,0,currentPlaceInArray);
                    CloudLargeFile file = new CloudLargeFile(
                            path.getFileName().toString(),
                            lastPart,
                            parts,
                            part);
                    ctx.write(file);
                    System.out.println("ser end part " + part);
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
