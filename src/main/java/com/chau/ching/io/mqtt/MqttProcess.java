package com.chau.ching.io.mqtt;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttIdentifierRejectedException;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageFactory;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttUnacceptableProtocolVersionException;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class MqttProcess extends SimpleChannelInboundHandler {
    private Logger logger = LoggerFactory.getLogger(MqttProcess.class);
    private int i=0;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        ctx.fireExceptionCaught(cause);
        if("远程主机强迫关闭了一个现有的连接。".equals(cause.getMessage())) {
            logger.info("---MqttProcess-fireExceptionCaught--{}",cause.getMessage());
            ctx.close().sync();
        }
        logger.info("---MqttProcess---{}",cause.getMessage());

    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.info("---MqttProcess--channelRead--{}",(MqttMessage)msg);

        MqttMessage message = (MqttMessage) msg;

        if (message.decoderResult().isFailure()) {
            Throwable cause = message.decoderResult().cause();
            if (cause instanceof MqttUnacceptableProtocolVersionException) {
                MqttHelper.sendMessage(ctx,
                        MqttMessageFactory.newMessage(
                                new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                                new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION, false),
                                null
                        ),"INVALID",null,true
                );

            }else if (cause instanceof MqttIdentifierRejectedException) {
                MqttHelper.sendMessage(ctx,
                        MqttMessageFactory.newMessage(
                                new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                                new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, false),
                                null
                        ),"INVALID",null,true
                );
            }

            ctx.close();
            return;
        }


        if(!message.fixedHeader().messageType().equals(MqttMessageType.PINGREQ)) {
            //logger.info("---message--:{}",message);
        }
        //logger.info("---MqttProcess--message.fixedHeader().messageType()--"+message.fixedHeader().messageType());
        switch(message.fixedHeader().messageType()) {
            //客户端请求连接服务端
            case CONNECT:
                MqttProtocolServer.INSTANCE.processCONNECT(ctx,(MqttConnectMessage)msg);
                break;
        	/*//连接确认，服务端到客户端
        	case CONNACK:
        		MqttProtocolServer.INSTANCE.processCONNECTACK(ctx,(MqttConnAckMessage)msg);
        		break;*/
            //发布消息，客户端服务端都可以发 打印机发送打印成功 作为客户端收到消息时处理 QOS 小于2直接回复ACK
            case PUBLISH:
                MqttProtocolServer.INSTANCE.processPUBLISH(ctx,(MqttPublishMessage)msg);
                break;
            //消息收到响应报文，QOS1和QOS0需要只需要2次握手
            /**case PUBACK:
             MqttProtocolServer.INSTANCE.processPUBACK(ctx,(MqttPubAckMessage)msg);
             break;*/
            //客户端收到消息回复报文，QOS2的情况
            case PUBREC:
                MqttProtocolServer.INSTANCE.processPUBREC(ctx,(MqttMessage)msg);
                break;
            //客户端收到消息回复报文，QOS2的情况
            case PUBREL:
                MqttProtocolServer.INSTANCE.processPUBREL(ctx,(MqttMessage)msg);
                break;
            //客户端收到服务端PUBREL报文，QOS2的情况，表明该消息已经消费完成，如果没有，则把消息加入遗嘱
            case PUBCOMP:
                MqttProtocolServer.INSTANCE.processPUBCOMP(ctx,(MqttMessage)msg);
                break;
            //客户端请求订阅
            case SUBSCRIBE:
                MqttProtocolServer.INSTANCE.processSUBSCRIBE(ctx,(MqttSubscribeMessage)msg);
                break;
//        	case SUBACK:
//        		MqttProtocolServer.INSTANCE.processSUBACK(ctx,(MqttSubAckMessage)msg);
//        		break;
            //取消订阅
            case UNSUBSCRIBE:
                MqttProtocolServer.INSTANCE.processUNSUBSCRIBE(ctx,(MqttUnsubscribeMessage)msg);
                break;
//        	case UNSUBACK:
//        		MqttProtocolServer.INSTANCE.processUNSUBACK(ctx,(MqttUnsubAckMessage)msg);
//        		break;
            //心跳请求，需要响应PINGRESP
            case PINGREQ:
                MqttProtocolServer.INSTANCE.processPINGREQ(ctx,(MqttMessage)msg);
                break;
        	/*case PINGRESP:
        		MqttProtocolServer.INSTANCE.processPINGRESP(ctx,(MqttMessage)msg);
        		break;*/
            //断开连接
            case DISCONNECT:
                MqttProtocolServer.INSTANCE.processDISCONNECT(ctx,(MqttMessage)msg);
                break;
            default:
                break;
        }

    }

	/*@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
	    //心跳失败，关闭
		logger.info("-----心跳失败-----");
		ctx.close().sync();
	}*/
}
