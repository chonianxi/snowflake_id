package com.chau.ching.io.mqtt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;

public class MqttHelper {
    private static final Map<String, ChannelHandlerContext> sessionRepository = new ConcurrentHashMap<>();

    public static void saveSession(String clientId, ChannelHandlerContext session) {
        sessionRepository.put(clientId, session);
    }


    public static ChannelHandlerContext getSession(String clientId) {

        return sessionRepository.get(clientId);
    }

    public static ChannelHandlerContext removeSession(String clientId) {

        return sessionRepository.remove(clientId);
    }

    public  static boolean removeSession(String clientId, ChannelHandlerContext session) {
        return sessionRepository.remove(clientId, session);
    }

    /**
     * 发送信息
     *
     * @param msg
     * @param clientId
     * @param packetId
     * @param flush
     */
    public static void sendMessage(MqttMessage msg, String clientId, Integer packetId, boolean flush) {
        ChannelHandlerContext ctx = getSession(clientId);
        if (ctx == null) {
            String pid = packetId == null || packetId <= 0 ? "" : String.valueOf(packetId);
            return;
        }
        sendMessage(ctx, msg, clientId, packetId, flush);
    }


    /**
     * 发送信息
     *
     * @param ctx
     * @param msg
     * @param clientId
     * @param packetId
     * @param flush
     */
    public static void sendMessage(ChannelHandlerContext ctx, MqttMessage msg, String clientId, Integer packetId, boolean flush) {
        String pid = packetId == null || packetId <= 0 ? "" : String.valueOf(packetId);
        ChannelFuture future = flush ? ctx.writeAndFlush(msg) : ctx.write(msg);
        future.addListener(f -> {
            if (f.isSuccess()) {

            } else {

            }
        });
    }
}
