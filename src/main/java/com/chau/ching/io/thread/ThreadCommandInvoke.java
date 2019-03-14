package com.chau.ching.io.thread;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadCommandInvoke<Task extends Runnable> implements ThreadCommand<Task> {
    private static ThreadCommandInvoke instance = null;

    //任务队列
    public volatile static  LinkedBlockingQueue<Runnable> taskBlockingQueue = new LinkedBlockingQueue<Runnable>(819200);

    //线程名称
    private static AtomicInteger threadNameNum = new AtomicInteger();
    //最少线程
    private static final int  minThreadNum = 2;
    //最大线程
    private static final int maxThreadNum = Runtime.getRuntime().availableProcessors()>2?Runtime.getRuntime().availableProcessors():2;
    //当前工作的线程数量
    private static int workThreadNum = minThreadNum;
    //当前运行的线程
    private static LinkedList<TaskThread> workThread = new LinkedList();


    public static ThreadCommandInvoke getInstance(){
        if (null==instance){
            synchronized (ThreadCommandInvoke.class){
                if (null==instance){
                    instance = new ThreadCommandInvoke(Runtime.getRuntime().availableProcessors());
                }
            }
        }
        return instance;
    }

    public ThreadCommandInvoke(){
        initThreadPool(minThreadNum);
    }

    public ThreadCommandInvoke(int num){
        initThreadPool(num);
    }

    private void initThreadPool(int num){
        if (num < minThreadNum){
            num = minThreadNum;
        }
        if (num > maxThreadNum){
            num = maxThreadNum;
        }
        for (int i=0;i<num;i++){
            TaskThread<Task> taskThread = new TaskThread<Task>();
            Thread thread = new Thread(taskThread,"snowflake-thread-" + threadNameNum.incrementAndGet());
            thread.start();
            workThread.addFirst(taskThread);
        }
    }





    @Override
    public void execute(Task task) {
        try {
            taskBlockingQueue.offer(task,3000, TimeUnit.MICROSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void shutdown() {
        for (int i = 0;i < workThread.size(); i++){
            TaskThread taskThread = workThread.get(i);
            taskThread.shutDown();
        }
    }
}
