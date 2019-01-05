package com.chau.ching.io.idcenter;


import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Id {

    private static Id id;



    public Id(){}


    public static Id getInstance(){
        if (null==id){
            synchronized (Id.class){
                if (null==id){
                    id = new Id();
                }
            }
        }
        return id;
    }

    private static volatile  Map<String, IdCenter>  sessionRepository = new ConcurrentHashMap<>();

    private static volatile Map<String, RandomAccessFile> sessionFileRepository = new ConcurrentHashMap<>();

    public static volatile Map<String, FileChannel> sessionFileChannelRepository = new ConcurrentHashMap<>();

    public static volatile Long counter = -19L;

    public static Long counterAdd(){
        counter = counter+19;
        return counter;
    }

    public static Long initCounter(){
        counter = 0L;
        return counter;
    }

    public static void saveFileChannelSession(String clientId, FileChannel session) {
        sessionFileChannelRepository.put(clientId, session);
    }

    public static FileChannel getFileChannelSession(String clientId) {

        return sessionFileChannelRepository.get(clientId);
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
