package com.chau.ching.io.pojo;

import java.io.Serializable;

public class MachineWork implements Serializable
{
    private String machine;
    private String work;
    private String datacenterId;
    private static final long serialVersionUID = -1L;

    public String getDatacenterId() {
        return datacenterId;
    }

    public void setDatacenterId(String datacenterId) {
        this.datacenterId = datacenterId;
    }

    public String getMachine() {
        return machine;
    }

    public void setMachine(String machine) {
        this.machine = machine;
    }

    public String getWork() {
        return work;
    }

    public void setWork(String work) {
        this.work = work;
    }
}
