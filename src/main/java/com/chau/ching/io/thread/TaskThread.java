package com.chau.ching.io.thread;

import com.chau.ching.io.constant.Constant;

import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class TaskThread<Task extends Runnable> implements Runnable{
    private volatile static boolean  running = true;
    private Long line = 0L;

    private ThreadLocal<FileChannel> threadLocal = new ThreadLocal<>();

    @Override
    public void run() {
        String id = null;
        while(running){
            try {
                synchronized(this) {
                    id = ThreadCommandInvoke.taskBlockingQueue.take();
                    if (null != id) {
                        try {
                            RandomAccessFile memoryMappedFile;
                            MappedByteBuffer out;
                            FileChannel fileChannel;
                            line = line +19;
                            if (null == threadLocal || null == threadLocal.get()) {
                                memoryMappedFile = new RandomAccessFile(Constant.LOG_FILE_PATH + Thread.currentThread().getName() + System.currentTimeMillis() + "", "rw");
                                fileChannel = memoryMappedFile.getChannel();
                                out = fileChannel.map(FileChannel.MapMode.READ_WRITE, line, Constant.count);
                                threadLocal.set(memoryMappedFile.getChannel());
                            } else {
                                fileChannel = threadLocal.get();
                                out = fileChannel.map(FileChannel.MapMode.READ_WRITE, line, Constant.count);
                            }

                            try {
                                out.put(id.getBytes());
                                out.put((byte) '\r');
                                out.put((byte) '\n');
                            } catch (BufferOverflowException e) {
                                line = 0L;
                                memoryMappedFile = new RandomAccessFile(Constant.LOG_FILE_PATH + Thread.currentThread().getName() + System.currentTimeMillis() + "", "rw");
                                fileChannel = memoryMappedFile.getChannel();
                                out = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, Constant.count);
                                threadLocal.set(fileChannel);
                                out.put(id.getBytes());
                                out.put((byte) '\r');
                                out.put((byte) '\n');
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public void shutDown(){
        this.running = false;
    }
}
