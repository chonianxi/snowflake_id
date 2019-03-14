package com.chau.ching.io.thread;

import com.chau.ching.io.constant.Constant;
import com.chau.ching.io.idcenter.Id;

import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ThreadModel implements Runnable {
    private String id;

    public ThreadModel(String id){
        this.id = id;
    }

    @Override
    public void run() {
        try{
            RandomAccessFile memoryMappedFile = Id.getFileSession(Constant.FEIL_SESSION);
            MappedByteBuffer out = memoryMappedFile.getChannel().map(FileChannel.MapMode.READ_WRITE, Id.getInsertPosint(), Constant.count);
            try{
                out.put(id.getBytes());
                out.put((byte) '\r');
                out.put((byte) '\n');
            }catch(BufferOverflowException e){
                memoryMappedFile = new RandomAccessFile(Constant.LOG_FILE_PATH + System.currentTimeMillis()+"", "rw");
                out = memoryMappedFile.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, Constant.count);
                out.put(id.getBytes());
                out.put((byte) '\r');
                out.put((byte) '\n');
            }
        }catch(Exception e){
            e.printStackTrace();
        }

    }
}
