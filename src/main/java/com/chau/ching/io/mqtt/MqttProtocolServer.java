package com.chau.ching.io.mqtt;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.CharsetUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MqttProtocolServer {

        Logger logger = LoggerFactory.getLogger(MqttProtocolServer.class);

        @Value("${mqtt.user}")
        private String mqttUser;
        @Value("${mqtt.password}")
        private String mqttPassword;

        public static final MqttProtocolServer INSTANCE = new MqttProtocolServer();
        private MqttVersion version;
        private String clientId;
        private String userName;
        private String brokerId;
        private boolean connected;
        private boolean cleanSession;
        private int keepAlive;
        private int keepAliveMax;
        private MqttPublishMessage willMessage;




        public void processCONNECT(ChannelHandlerContext ctx, MqttConnectMessage msg) throws Exception {
            //RedisUtil.setKey("woaini", "woaini");
            // TODO Auto-generated method stub
            logger.info("---processCONNECT-{}",msg);
            this.version = MqttVersion.fromProtocolNameAndLevel(msg.variableHeader().name(), (byte) msg.variableHeader().version());
            this.clientId = msg.payload().clientIdentifier();
            this.cleanSession = msg.variableHeader().isCleanSession();
            //logger.info("---clientId--connent111-{}",clientId);
            if (msg.variableHeader().keepAliveTimeSeconds() > 0 && msg.variableHeader().keepAliveTimeSeconds() <= this.keepAliveMax) {
                this.keepAlive = msg.variableHeader().keepAliveTimeSeconds();
            }

            //MQTT 3.1之后可能存在为空的客户ID。所以要进行处理。如果客户ID是空，而且还在保存处理相关的信息。这样子是不行。
            //必须有客户ID我们才能存保相关信息。
            if (StringUtils.isBlank(this.clientId)) {
                if (!this.cleanSession) {
                    MqttHelper.sendMessage(
                            ctx,
                            MqttMessageFactory.newMessage(
                                    new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0),
                                    null,
                                    null
                            ),"INVALID",null,true
                    );
                    ctx.close().sync();
                    return;

                } else {
                    this.clientId =  "c"+java.util.UUID.randomUUID().toString().replaceAll("-", "");
                }
            }

            //有可能发送俩次的连接包。如果已经存在连接就是关闭当前的连接。
        /*if (this.connected) {
            ctx.close();
            return;
        }*/


            //如果会话中已经存储了这个新连接的ID，就关闭之前的clientID
            if (null!=MqttHelper.getSession(msg.payload().clientIdentifier())) {
                logger.error("clientid:{},已经存在，关闭旧链接",msg.payload().clientIdentifier());
                MqttHelper.getSession(msg.payload().clientIdentifier()).close().sync();

            }


            boolean userNameFlag = msg.variableHeader().hasUserName();
            boolean passwordFlag = msg.variableHeader().hasPassword();
            this.userName = msg.payload().userName();

            String password = "" ;
            if( msg.payload().passwordInBytes() != null  && msg.payload().passwordInBytes().length > 0)
                password =   new String(msg.payload().passwordInBytes());

            boolean mistake = false;

            //如果有用户名标示，那么就必须有密码标示。
            //当有用户名标的时候，用户不能为空。
            //当有密码标示的时候，密码不能为空。
            //处理身份验证（userNameFlag和passwordFlag）
            if (msg.variableHeader().hasUserName() &&
                    msg.variableHeader().hasPassword()) {
                String userName = msg.payload().userName();
                String pwd =  new String(msg.payload().passwordInBytes(), CharsetUtil.UTF_8);
                //此处对用户名和密码做验证
                if (!checkAuthenValid(userName, pwd)) {
                    logger.error("clientid:{},用户名和密码出错",msg.payload().clientIdentifier());
                    MqttHelper.sendMessage(
                            ctx,
                            MqttMessageFactory.newMessage(
                                    new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                                    new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, true),
                                    null
                            ),this.clientId,null,true
                    );
                    ctx.close().sync();
                    return;
                }
            }

            //处理心跳包时间
            int keepAlive = msg.variableHeader().keepAliveTimeSeconds();
            //ctx.channel().pipeline().addFirst("idleStateHandler", new IdleStateHandler(keepAlive, Integer.MAX_VALUE, Integer.MAX_VALUE, TimeUnit.SECONDS));

            MqttHelper.sendMessage(
                    ctx,
                    MqttMessageFactory.newMessage(
                            new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                            new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED, !this.cleanSession),
                            null
                    ),this.clientId,null,true
            );

            ChannelHandlerContext lastSession = MqttHelper.removeSession(clientId);
            if (lastSession != null) {
                lastSession.close();
            }

            String willTopic = msg.payload().willTopic();
            String willMessage = "";
            if(msg.payload().willMessageInBytes() != null && msg.payload().willMessageInBytes().length > 0) {
                willMessage =  new String(msg.payload().willMessageInBytes());
            }


            if (msg.variableHeader().isWillFlag() && StringUtils.isNotEmpty(willTopic) && StringUtils.isNotEmpty(willMessage)) {
                this.willMessage = (MqttPublishMessage) MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.valueOf(msg.variableHeader().willQos()), msg.variableHeader().isWillRetain(), 0),
                        new MqttPublishVariableHeader(willTopic, 0),
                        Unpooled.wrappedBuffer(willMessage.getBytes())
                );
            }

            this.connected = true;
            logger.info("---clientId--connent-{}",clientId);
            MqttHelper.saveSession(clientId, ctx);

        }



        private boolean checkAuthenValid(String userName, String pwd) {
            if (mqttUser.equals(userName.trim()) && mqttPassword.equals(pwd.trim())) {
                return true;
            }
            return false;
        }






        public void processPUBREC(ChannelHandlerContext ctx, MqttMessage msg) {
            // TODO Auto-generated method stub
            //客户端收到主题消息的确认 ,服务端返回PUBREL(第三次报文),PUBCOMP客户端确认(第四次报文)
            //logger.info("---processPUBREC-{}",msg);
            MqttHelper.sendMessage(
                    ctx,
                    MqttMessageFactory.newMessage(
                            new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.EXACTLY_ONCE, false, 0),
                            msg.variableHeader(),
                            null
                    ),this.clientId,null,true
            );

        }


        public void processPUBCOMP(ChannelHandlerContext ctx, MqttMessage msg) {
            // TODO Auto-generated method stub
            MqttMessageIdVariableHeader mv = (MqttMessageIdVariableHeader) msg.variableHeader();
            if(null!=msg.payload()) {
                try {
                    String pubcomp = new String(ByteBufUtil.getBytes((ByteBuf) msg.payload()),CharsetUtil.UTF_8);
                    logger.info("--pubcomp---:{}",pubcomp);
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }

            logger.info("---processPUBCOMP-消息{}已经确认消费",mv.messageId());
        }

        public void processSUBSCRIBE(ChannelHandlerContext ctx, MqttSubscribeMessage msg) {
            // TODO Auto-generated method stub
            //客户端请求订阅
            //返回SUBACK
            logger.info("---processSUBSCRIBE-{}",msg);
            MqttHelper.sendMessage(
                    ctx,
                    MqttMessageFactory.newMessage(
                            new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.EXACTLY_ONCE, false, 0),
                            msg.variableHeader(),
                            new MqttSubAckPayload(2)
                    ),this.clientId,null,true
            );

            List<MqttTopicSubscription> mvlist = msg.payload().topicSubscriptions();


            Map map = new ConcurrentHashMap();
            for (MqttTopicSubscription mv:mvlist) {
                //记录订阅
            }



            logger.info("---clientId--sub-{}",clientId);
        }







        public void processUNSUBSCRIBE(ChannelHandlerContext ctx, MqttUnsubscribeMessage msg) {
            // TODO Auto-generated method stub
            logger.info("---processUNSUBSCRIBE-{}",msg);
            MqttHelper.sendMessage(
                    ctx,
                    MqttMessageFactory.newMessage(
                            new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                            msg.variableHeader(),
                            null
                    ),this.clientId,null,true
            );

            List<String> topics = msg.payload().topics();
        }

        public void processPINGREQ(ChannelHandlerContext ctx, MqttMessage msg) {
            // TODO Auto-generated method stub
            //logger.info("---processPINGREQ-{}",msg);
            if(null!=msg.payload()) {
                try {
                    String pingreq = new String(ByteBufUtil.getBytes((ByteBuf) msg.payload()),CharsetUtil.UTF_8);
                    logger.info("---pingreq--:{}",pingreq);
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }
            MqttHelper.sendMessage(
                    ctx,
                    MqttMessageFactory.newMessage(
                            new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0),
                            null,
                            null
                    ),this.clientId,null,true
            );
            //心跳机制
        }







        public void processDISCONNECT(ChannelHandlerContext ctx, MqttMessage msg) {
            // TODO Auto-generated method stub
            logger.info("---processDISCONNECT-{}",msg);
            if (!this.connected) {
                ctx.close();
                return;
            }
            MqttHelper.removeSession(clientId, ctx);


            this.willMessage = null;
            this.connected = false;
            ctx.close();


            //记录断开连接
        }












        //发布消息，客户端服务端都可以发 打印机发送打印成功
        public void processPUBLISH(ChannelHandlerContext ctx, MqttPublishMessage msg) {
            //PrintSuccess
            //logger.info("---processPUBLISH-{},{}",msg.variableHeader().topicName(),HexUtil.hexStr2Str(ByteBufUtil.hexDump(msg.payload())));
            logger.info("---processPUBLISH-{},{}",msg.variableHeader().topicName(),new String(ByteBufUtil.getBytes(msg.payload()),CharsetUtil.UTF_8));

            if (msg.variableHeader().topicName().equals("MqttConstant.printHeat")) {
                String[] heat = new String(ByteBufUtil.getBytes(msg.payload()),CharsetUtil.UTF_8).split(";");
                if(6==heat.length) {
                    //打印机心跳

                    //如果有遗留未消费消息则发送遗留消息

                }

            }

            if (msg.variableHeader().topicName().equals("MqttConstant.printsucess")) {
                //打印机成功回调
                String[] heat = new String(ByteBufUtil.getBytes(msg.payload()),CharsetUtil.UTF_8).split(";");
                if(4==heat.length) {

                    if(heat[3].split("-").length==2) {
                        String ssn = heat[1].replaceAll("\\[", "").replaceAll("\\]", "");
                        //打印机打印已经接收
                    }

                    if(heat[3].split("-").length==1) {
                        String ssn = heat[1].replaceAll("\\[", "").replaceAll("\\]", "");
                        //打印机打印完成
                        Map<String,String> mapMsg = new ConcurrentHashMap();
                        mapMsg.put("sn", ssn);
                        mapMsg.put("type", "1");
                        mapMsg.put("time", System.currentTimeMillis()+"");
                        mapMsg.put("uuid", heat[3]);
                    }



                }

            }

            int packid = 1;
            if(msg.variableHeader().packetId()>1) {
                packid=msg.variableHeader().packetId();
            }
            //logger.info("---msg.fixedHeader().qosLevel().value()<2-{}",(msg.fixedHeader().qosLevel().value()<2));
            if (msg.fixedHeader().qosLevel().value()<2) {
                MqttHelper.sendMessage(
                        ctx,
                        MqttMessageFactory.newMessage(
                                new MqttFixedHeader(MqttMessageType.PUBACK, false, msg.fixedHeader().qosLevel(), false, 0),
                                MqttMessageIdVariableHeader.from(packid),
                                null
                        ),this.clientId,null,true
                );


            }else {
                MqttHelper.sendMessage(
                        ctx,
                        MqttMessageFactory.newMessage(
                                new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.EXACTLY_ONCE, false, 0),
                                MqttMessageIdVariableHeader.from(packid),
                                null
                        ),this.clientId,null,true
                );
            }

        }






        public void processPUBREL(ChannelHandlerContext ctx, MqttMessage msg) {
            logger.info("---processPUBREL-{}",msg);
            MqttMessageIdVariableHeader msgid = (MqttMessageIdVariableHeader) msg.variableHeader();
            MqttHelper.sendMessage(
                    ctx,
                    MqttMessageFactory.newMessage(
                            new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.EXACTLY_ONCE, false, 0),
                            msgid,
                            null
                    ),this.clientId,null,true
            );

        }
}
