package com.laxcen.xinlianrfid;

import java.util.Comparator;

/**
 * @author zzc
 * @date 2019/12/27
 */
public class RFIDTagInfo implements Cloneable {
    /**
     * TID
     */
    public String tid;

    /**
     * EPC id
     */
    public String epcID;

    /**
     * RSSI 信号值
     * 统一转换为0 ～100
     */
    public int optimizedRSSI;

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getEpcID() {
        return epcID;
    }

    public void setEpcID(String epcID) {
        this.epcID = epcID;
    }

    public int getOptimizedRSSI() {
        return optimizedRSSI;
    }

    public void setOptimizedRSSI(int optimizedRSSI) {
        this.optimizedRSSI = optimizedRSSI;
    }

}
