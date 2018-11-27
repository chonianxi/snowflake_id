package com.chau.ching.io.mqtt;

import com.chau.ching.io.constant.Constant;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.util.concurrent.DefaultThreadFactory;

public class MqttServer {

    public void bind(int port) throws InterruptedException {
        String os = System.getProperty("os.name");

        //EpollEventLoopGroup
        EventLoopGroup bossMqtt = null;//new NioEventLoopGroup(1, new DefaultThreadFactory("mqttserver1", true));
        EventLoopGroup workMqtt = null;//new NioEventLoopGroup(1, new DefaultThreadFactory("mqttserver2", true));

        if (os.toLowerCase().indexOf(Constant.OS_WINDOWS)>-1){
            bossMqtt = new NioEventLoopGroup(1, new DefaultThreadFactory("mqttserver1", true));
            workMqtt = new NioEventLoopGroup(1, new DefaultThreadFactory("mqttserver2", true));
        }else{
            bossMqtt = new EpollEventLoopGroup(1, new DefaultThreadFactory("mqttserver1", true));
            workMqtt = new EpollEventLoopGroup(1, new DefaultThreadFactory("mqttserver2", true));
        }

        try {
            ServerBootstrap bootstrapMqtt = new ServerBootstrap();

            bootstrapMqtt.group(bossMqtt,workMqtt)

                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipe = ch.pipeline();
                            pipe.addLast("MQTTEncoder", MqttEncoder.INSTANCE);
                            pipe.addLast("MQTTDecoder",new MqttDecoder());
                            pipe.addLast("MQTTProcess",new MqttProcess());
                        }
                    })
                    .localAddress(port)
                    //.option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30);
            if (os.toLowerCase().indexOf(Constant.OS_WINDOWS)>-1){
                bootstrapMqtt.channel(NioServerSocketChannel.class);
            }else{
                bootstrapMqtt.channel(EpollServerSocketChannel.class);
            }

            ChannelFuture futureMqtt = bootstrapMqtt.bind().sync();
            futureMqtt.channel().closeFuture().sync();
        }catch(Exception e) {
            e.printStackTrace();
        }finally {
            workMqtt.shutdownGracefully().sync();
            bossMqtt.shutdownGracefully().sync();

        }

    }


}
