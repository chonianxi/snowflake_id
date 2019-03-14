package com.chau.ching.io.thread;

public class TaskThread<Task extends Runnable> implements Runnable{
    private volatile static boolean  running = true;

    @Override
    public void run() {
        Task task = null;
        while(running){
            try {
                task = (Task) ThreadCommandInvoke.taskBlockingQueue.take();
                if (null!=task){
                    task.run();
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
