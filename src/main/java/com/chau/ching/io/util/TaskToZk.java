package com.chau.ching.io.util;

import com.chau.ching.io.constant.Constant;
import com.chau.ching.io.idcenter.CustomSerializer;
import com.chau.ching.io.idcenter.Id;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;

import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

public class TaskToZk {

    public void go(String ip, String datacenterId, String workId, ZkClient zkc,String zkServers){
        Runnable runnable = new Runnable() {
            public void run() {
                // task to run goes here
                String timeNode = Constant.ZK_TIME_LIST+"/"+datacenterId+"/"+workId;
                String id = String.valueOf(Id.getSession(Constant.ID_SESSION).nextId());
                //System.out.println("Hello !!");
                try{
                    zkc.writeData(timeNode,id);
                }catch(Exception e){
                    e.printStackTrace();
                    //ZkClient zkc = new ZkClient(new ZkConnection(zkServers), 20000 ,new CustomSerializer());

                }

                //System.out.println("Hello !!"+ip+"/"+datacenterId+"/"+workId+"/"+zkc.exists(Constant.ZK_TIME_LIST)+"/"+System.currentTimeMillis());
            }
        };
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        // 第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间 
        service.scheduleAtFixedRate(runnable, 2, 2, TimeUnit.SECONDS);
    }

}
