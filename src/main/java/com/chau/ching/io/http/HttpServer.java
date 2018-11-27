package com.chau.ching.io.http;

import com.chau.ching.io.constant.Constant;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class HttpServer {

    private Logger logger = LoggerFactory.getLogger(HttpServer.class);

    /*@Value("${host}")
    private String host;
    @Value("${port}")
    private String port1;*/

    public void bind(String host,int port) throws InterruptedException {
        //Integer port = Integer.valueOf(port1);

        logger.info("---绑定服务器IP--{}",host);
        logger.info("---绑定服务器port--{}",port);


        String os = System.getProperty("os.name");

        logger.info("---服务器运行环境--{}",os);

        int parentThreadGroupSize = 4;
        if (null!=Integer.getInteger("netty.server.parentgroup.size")){
            parentThreadGroupSize = Integer.getInteger("netty.server.parentgroup.size");
        }
        int childThreadGroupSize = 8;
        if (null!=Integer.getInteger("netty.server.childgroup.size")){
            childThreadGroupSize = Integer.getInteger("netty.server.childgroup.size");
        }
        //EpollEventLoopGroup
        EventLoopGroup boss = null;//new NioEventLoopGroup(parentThreadGroupSize, new DefaultThreadFactory("server1", true));
        EventLoopGroup work = null;//new NioEventLoopGroup(childThreadGroupSize, new DefaultThreadFactory("server2", true));

        if (os.toLowerCase().indexOf(Constant.OS_WINDOWS)>-1){
            boss = new NioEventLoopGroup(parentThreadGroupSize, new DefaultThreadFactory("server1", true));
            work = new NioEventLoopGroup(childThreadGroupSize, new DefaultThreadFactory("server2", true));
        }else{
            boss = new EpollEventLoopGroup(parentThreadGroupSize, new DefaultThreadFactory("server1", true));
            work = new EpollEventLoopGroup(childThreadGroupSize, new DefaultThreadFactory("server2", true));
        }


        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss,work)
                    //.channel(EpollServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipe = ch.pipeline();
                            pipe.addLast("HttpResponseEncoder", new HttpResponseEncoder());
                            pipe.addLast("HttpRequestDecoder",new HttpRequestDecoder());
                            pipe.addLast("HttpServerProcess",new HttpServerProcess());
                        }
                    })
                    //.option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.SO_SNDBUF,2048)
                    .option(ChannelOption.SO_RCVBUF,2048)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000);

            //bootstrap.localAddress(Integer.valueOf(args[0]));
            if (os.toLowerCase().indexOf(Constant.OS_WINDOWS)>-1){
                bootstrap.channel(NioServerSocketChannel.class);
            }else{
                bootstrap.channel(EpollServerSocketChannel.class);
            }
            if (StringUtils.isEmpty(host)){
                bootstrap.localAddress(port);
            }else{
                bootstrap.localAddress(host,port);
            }

            ChannelFuture future = bootstrap.bind().sync();
            logger.info("-----idcenter http 绑定已经完成，服务启动成功---");
            future.channel().closeFuture().sync();
        }catch(Exception e) {
            e.printStackTrace();
        }finally {
            work.shutdownGracefully().sync();
            boss.shutdownGracefully().sync();

        }
    }

}
