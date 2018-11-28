package com.chau.ching.io;

import com.chau.ching.io.constant.Constant;
import com.chau.ching.io.http.HttpServer;
import com.chau.ching.io.idcenter.CustomSerializer;
import com.chau.ching.io.idcenter.Id;
import com.chau.ching.io.idcenter.IdCenter;
import com.chau.ching.io.util.TaskToZk;
import com.chau.ching.io.zookeeper.ZookeeperValidate;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.RandomAccessFile;
import java.net.InetAddress;

public class StartIdCenter {

    private static Logger logger = LoggerFactory.getLogger(StartIdCenter.class);



    public static void main(String[] args) throws Exception {
        String zkServers = "127.0.0.1:2181";
        InetAddress address = InetAddress.getLocalHost();
        String ip = "";
        String port = "80";
        String datacenterId = "1";
        String workId = "1";

        //args 1zk,2听ip,3监听端口，4datacenterId,5workId,其中IP可以为空
        if(args.length==4){
            zkServers = args[0];
            ip = args[1];
            //port = args[2];
            datacenterId = args[2];
            workId = args[3];

        }else if(args.length==3){
            zkServers = args[0];
            //port = args[1];
            datacenterId = args[1];
            workId = args[2];
        }else{
            throw new RuntimeException("--启动失败，参数不对--");
        }

        logger.info("--启动参数-ZK服务地址：{}----ip:{}----port:{}---数据中心ID:{}-----工作机器ID:{}---",zkServers,ip,port,datacenterId,workId);



        logger.info("-----idcenter 开始启动--");
        //ID生成器内部MQTT开始

        //new MqttServer().bind(1883);
        //ID生成器内部MQTT结束
        RandomAccessFile memoryMappedFile = new RandomAccessFile(System.currentTimeMillis()+"", "rw");
        logger.info("----开始连接zookeeper--");
        ZkClient zkc = new ZkClient(new ZkConnection(zkServers), 20000 ,new CustomSerializer());
        Id.saveSession(Constant.ID_SESSION,new IdCenter(Integer.parseInt(workId),Integer.parseInt(datacenterId),0));
        Id.saveFileSession(Constant.FEIL_SESSION,memoryMappedFile);



        logger.info("----开始连接zookeeper-{}-连接成功，本机ip-{}-",zkServers,ip);

        logger.info("----开始连接进行启动前验证-----");
        ZookeeperValidate.validate(zkc,ip,workId,datacenterId,address.getHostAddress());
        logger.info("-------启动前验证完成------");

        //开始检查目前存活的服务节点
        logger.info("----开始检查目前存活的服务节点--");

        logger.info("-----idcenter 开始绑定HTTP请求--");
        new TaskToZk().go(ip,datacenterId,workId,zkc,zkServers);
        //ID生成器服务开始 HTTP请求
        new HttpServer().bind(ip,Integer.valueOf(port));
        //ID生成器服务结束 HTTP请求


    }
}
