package com.chau.ching.io.thread;

public interface ThreadCommand<Task extends Runnable> {

    // 执行一个task，这个task需要实现Runnable
    void execute(String id);
    // 关闭线程池
    void shutdown();



}
