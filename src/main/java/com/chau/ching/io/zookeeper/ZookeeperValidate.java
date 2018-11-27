package com.chau.ching.io.zookeeper;

import com.chau.ching.io.constant.Constant;
import com.chau.ching.io.idcenter.Id;
import com.chau.ching.io.pojo.MachineWork;
import com.chau.ching.io.util.JsonUtils;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ZookeeperValidate {

    private static Logger logger = LoggerFactory.getLogger(ZookeeperValidate.class);


    public static void validate(ZkClient zkc,String ip,String workId,String datacenterId,String hostip){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (StringUtils.isEmpty(ip)){
            ip = hostip;
        }

        ConcurrentHashMap map = new ConcurrentHashMap();
        String timeNode = "";//时间节点
        String hostNode = Constant.ZK_HOST_LIST + "/" + datacenterId;//主机节点列表
        String hostSelfNode = Constant.ZK_HOST_LIST + "/" + datacenterId +  "/" +workId ;//主机节点
        timeNode = Constant.ZK_TIME_LIST+"/"+datacenterId+"/"+workId;
        String id = String.valueOf(Id.getSession(Constant.ID_SESSION).nextId());


        //查找节点，验证目前已经注册的节点，获取自己的ID
        if (!zkc.exists(hostNode)){//如果不存在数据中心节点列表，说明本机是该数据中心第一个ID生成器，把自己加入节点列表
            //增加自身节点
            zkc.createPersistent(hostSelfNode,true);
            zkc.writeData(hostSelfNode, ip);
            //zkc.createPersistent(Constant.ZK_TIME_LIST+"/"+ip,true);
            //初始化自身节点的服务最近提供时间
            zkc.createPersistent(timeNode,true);
            zkc.writeData(timeNode,id);
            //zkc.writeData(Constant.ZK_TIME_LIST+"/"+ip,System.currentTimeMillis());

            //当前的活跃机器数
            //zkc.createPersistent(Constant.ZK_IDCENTER_ID_MAX,true);
            //zkc.writeData(Constant.ZK_IDCENTER_ID_MAX,1+"");
        }else{
            //得到当前数据中心下面所有的workId列表
            List<String> list = zkc.getChildren(hostNode);

            boolean existSelf = false;
            logger.info("----**********************************************************************************-----");
            logger.info("----**********************************************************************************-----");
            logger.info("----**********************************************************************************-----");
            for (String work : list){
                String machineIp = zkc.readData(hostNode+"/"+ work);

                if (work.equals(workId)){//找到当前数据中心下面的当前ID生成节点
                    if (machineIp.equals(ip)) {//找到本机，说明上次服务崩了
                        long lastTime = Long.valueOf(zkc.readData(timeNode));
                        if ((Long.valueOf(id)-lastTime)<1){
                            logger.info("----OMG，本机当前时间小于以前的存活时间，NTP还是以前时间设置问题，请找大大-----");
                            throw new RuntimeException("本机时间不对，请注意！");
                        }
                        zkc.writeData(timeNode,id);
                        existSelf = true;
                        logger.info("----更新完成本机存活时间-----");
                    }else{//看对方是否还在提供服务
                        //检测机器是否存活
                        if(isLive("http://"+ machineIp + Constant.HTTP_HEARTBEAT,Constant.HTTP_ALIVE)){
                            logger.error("--{}-还在为数据中心{}-的{}-提供服务---",machineIp,datacenterId,workId);
                            throw new RuntimeException(machineIp+"还在提供:"+datacenterId+"生成ID:"+workId+"服务，请注意！");
                        }

                    }
                }else{//当前节点不是本节点
                    //检测机器是否存活
                    if(!isLive("http://"+ machineIp + Constant.HTTP_HEARTBEAT,Constant.HTTP_ALIVE)){
                        logger.info("--{}-机器的服务已经挂了，不健康了，数据中心:{},工作ID:{}---",machineIp,datacenterId,workId);
                    }else{
                        logger.info("--{}-机器的服务还活着，数据中心:{},工作ID:{}---",machineIp,datacenterId,workId);
                    }
                }
            }
            logger.info("----**********************************************************************************-----");
            logger.info("----**********************************************************************************-----");
            logger.info("----**********************************************************************************-----");


            if (!existSelf){//本身不存在，是新加入的节点
                zkc.createPersistent(hostSelfNode,true);
                zkc.writeData(hostSelfNode,ip);
                zkc.createPersistent(timeNode,true);
                zkc.writeData(timeNode,id);
            }

        }
    }


    public static boolean isLive (String urlHttp,String sucessMsg){
        try{
            URL url = new URL(urlHttp);
            HttpURLConnection urlCon = (HttpURLConnection)url.openConnection();
            urlCon.setConnectTimeout(2000);
            urlCon.setReadTimeout(2000);
            //连接
            urlCon.connect();
            //得到响应码
            int responseCode = urlCon.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK){
                //得到响应流
                InputStream inputStream = urlCon.getInputStream();
                //获取响应
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuffer line = new StringBuffer();
                String temp ;
                while (null!=(temp = reader.readLine())){
                    line.append(temp);
                }
                reader.close();
                //该干的都干完了,记得把连接断了
                urlCon.disconnect();
                if (line.equals(sucessMsg)){
                    return true;
                }
            }
            urlCon.disconnect();
        }catch(IOException e){
            e.printStackTrace();
            logger.error("--{}-连接出错，请注意--",urlHttp);
        }

        return false;

    }

}
