package com.chau.ching.io.idcenter;


import java.io.RandomAccessFile;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Id {

    private static final AtomicLong atomicLong = new AtomicLong(0);

    private static final Map<String, IdCenter> sessionRepository = new ConcurrentHashMap<>();

    private static final Map<String, RandomAccessFile> sessionFileRepository = new ConcurrentHashMap<>();

    public static long getInsertPosint(){
        //atomicLong = ;
        return atomicLong.addAndGet(19);
    }

    public static void saveSession(String clientId, IdCenter session) {
        sessionRepository.put(clientId, session);
    }

    public static IdCenter getSession(String clientId) {

        return sessionRepository.get(clientId);
    }

    public static void saveFileSession(String clientId, RandomAccessFile session) {
        sessionFileRepository.put(clientId, session);
    }

    public static RandomAccessFile getFileSession(String clientId) {

        return sessionFileRepository.get(clientId);
    }


}
