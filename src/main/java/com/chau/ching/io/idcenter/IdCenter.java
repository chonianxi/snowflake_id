package com.chau.ching.io.idcenter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** Copyright 2010-2012 Twitter, Inc.*/

public class IdCenter {

    private Logger logger = LoggerFactory.getLogger(IdCenter.class);

    private long workerId;
    private long datacenterId;
    private long sequence;

    public IdCenter(long workerId, long datacenterId, long sequence){
        // sanity check for workerId
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("ID生成器不能小于0或者大于%d",maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("数据中心ID不能小于0或者大于%d",maxDatacenterId));
        }
        logger.info("开始工作. 时间位 {}, 数据中心位 {},ID生成器位 {}, 序列化自增长位 {}, ID生成器ID： {}",
                timestampLeftShift, datacenterIdBits, workerIdBits, sequenceBits, workerId);

        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.sequence = sequence;
    }

    private long twepoch = 1542428099514L;

    private long workerIdBits = 4L;
    private long datacenterIdBits = 5L;
    private long maxWorkerId = -1L ^ (-1L << workerIdBits);
    private long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    private long sequenceBits = 13L;

    private long workerIdShift = sequenceBits;
    private long datacenterIdShift = sequenceBits + workerIdBits;
    private long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    private long sequenceMask = -1L ^ (-1L << sequenceBits);

    private long lastTimestamp = -1L;

    public long getWorkerId(){
        return workerId;
    }

    public long getDatacenterId(){
        return datacenterId;
    }

    public long getTimestamp(){
        return System.currentTimeMillis();
    }

    public synchronized long nextId() {
        long timestamp = timeGen();

        if (timestamp < lastTimestamp) {

            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                try {
                    //时间偏差大小小于5ms，则等待两倍时间
                    wait(offset << 1);//wait
                    timestamp = timeGen();
                    if (timestamp < lastTimestamp) {
                        //还是小于，抛异常并上报
                        throw new RuntimeException("时间不对了"+timestamp);
                    }
                } catch (InterruptedException e) {

                }
            } else {
                //throw
                throw new RuntimeException("时间不对了"+timestamp);
            }


            logger.info("时间变小了，出错了.  停止服务，最后服务时间 {}.", lastTimestamp);
            throw new RuntimeException(String.format("因时间变小了，倒退.  ID生成器停止服务 {} 毫秒 ",
                    lastTimestamp - timestamp));
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }

        lastTimestamp = timestamp;
        return ((timestamp - twepoch) << timestampLeftShift) |
                (datacenterId << datacenterIdShift) |
                (workerId << workerIdShift) |
                sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen(){
        return System.currentTimeMillis();
    }

}
