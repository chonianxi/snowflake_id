package com.chau.ching.io.listener;

import org.I0Itec.zkclient.IZkDataListener;

public class ZkLisenter implements IZkDataListener {


    public void handleDataChange(String s, Object data) throws Exception {
        System.out.println("--------handleDataChange---------");
    }

    public void handleDataDeleted(String s) throws Exception {

    }
}
