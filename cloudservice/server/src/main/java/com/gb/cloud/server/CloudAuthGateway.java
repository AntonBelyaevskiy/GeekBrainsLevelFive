package com.gb.cloud.server;

import com.gb.cloud.authorization.AuthorizationHandler;
import com.gb.cloud.message.CloudAuthorization;
import com.gb.cloud.message.CloudCommand;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

//Блок отвачающий за авторизацию
public class CloudAuthGateway extends ChannelInboundHandlerAdapter {

    AuthorizationHandler handler = new AuthorizationHandler();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        try {
            if (msg == null)
                return;

            if (msg instanceof CloudAuthorization) {
                CloudAuthorization authorization = (CloudAuthorization) msg;
                if (authorization.getStatus().equals("/create")) {
                    handler.connect();

                    if (handler.isLoginNameNotBusy(authorization.getLogin())) {
                        handler.addNewUser(authorization.getLogin(), authorization.getPassword());
                        //серверу сказать, чтобы запомнил логин и отправил клиенту сообщение, чтобы юзера впустили в облако
                        ctx.fireChannelRead(msg);
                        ctx.channel().pipeline().remove(this);
                        handler.disconnect();
                    } else {
                        CloudCommand busyLogin = new CloudCommand("/busyLogin");
                        ctx.fireChannelRead(busyLogin);
                        handler.disconnect();
                    }
                }
                if (authorization.getStatus().equals("/enter")) {
                    handler.connect();

                    if(handler.isUserExist(authorization.getLogin(), authorization.getPassword())) {
                        ctx.fireChannelRead(msg);
                        ctx.channel().pipeline().remove(this);
                        handler.disconnect();
                    }else{
                        CloudCommand wrongLoginOrPassword = new CloudCommand("/wrongLoginOrPassword");
                        ctx.fireChannelRead(wrongLoginOrPassword);
                        handler.disconnect();
                    }
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
