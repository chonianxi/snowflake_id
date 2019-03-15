package com.chau.ching.io.http;

import com.chau.ching.io.constant.Constant;
import com.chau.ching.io.idcenter.Id;
import com.chau.ching.io.thread.ThreadCommandInvoke;
import com.chau.ching.io.thread.ThreadModel;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderValues;

import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@ChannelHandler.Sharable
public class HttpServerProcess extends ChannelInboundHandlerAdapter {
    private ByteBufToBytes reader;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        if(msg instanceof  DefaultHttpRequest){
            DefaultHttpRequest defaultHttpRequest = (DefaultHttpRequest)msg;
            if (defaultHttpRequest.getDecoderResult().toString().toUpperCase().equals(Constant.HTTP_SUCESS)){
                if(defaultHttpRequest.getUri().equals(Constant.HTTP_GETID)){//生成ID
                    String id = String.valueOf(Id.getSession(Constant.ID_SESSION).nextId());
                    //ThreadModel threadModel = new ThreadModel(id);
                    ThreadCommandInvoke.getInstance().execute(id);
                    FullHttpResponse response = new DefaultFullHttpResponse(
                            HTTP_1_1, OK, Unpooled.wrappedBuffer(id
                            .getBytes()));
                    response.headers().set(CONTENT_TYPE, Constant.HTTP_TEXT);
                    response.headers().set(CONTENT_LENGTH,
                            response.content().readableBytes());
                    response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                    ctx.write(response);
                    ctx.flush();
                }else if(defaultHttpRequest.getUri().equals(Constant.HTTP_HEARTBEAT)){//健康检查
                    FullHttpResponse response = new DefaultFullHttpResponse(
                            HTTP_1_1, OK, Unpooled.wrappedBuffer(Constant.HTTP_ALIVE
                            .getBytes()));
                    response.headers().set(CONTENT_TYPE, Constant.HTTP_TEXT);
                    response.headers().set(CONTENT_LENGTH,
                            response.content().readableBytes());
                    response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                    ctx.write(response);
                    ctx.flush();
                }else{//其他返回空
                    FullHttpResponse response = new DefaultFullHttpResponse(
                            HTTP_1_1, OK, Unpooled.wrappedBuffer(""
                            .getBytes()));
                    response.headers().set(CONTENT_TYPE, Constant.HTTP_TEXT);
                    response.headers().set(CONTENT_LENGTH,
                            response.content().readableBytes());
                    response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                    ctx.write(response);
                    ctx.flush();
                }

            }
        }

    }
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        ctx.close();
    }
}
