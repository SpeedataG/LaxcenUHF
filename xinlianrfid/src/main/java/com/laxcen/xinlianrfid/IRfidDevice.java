package com.laxcen.xinlianrfid;

import android.content.Context;

/**
 * RFID扫描枪接口方法
 *
 * @author zzc
 * @date 2019/12/27
 */
public interface IRfidDevice {

    /**
     * 初始化设备
     *
     * @return 成功后返回true, 失败返回false
     */
    boolean init();

    /**
     * 释放设备资源
     *
     * @return 成功后返回true, 失败返回false
     */
    boolean free();

    /**
     * 打开设备，如果初始化设备之后RFID功能就可以使用，这个方法返回设备初始化的值
     *
     * @return 成功后返回true, 失败返回false
     */
    boolean open();

    /**
     * 判断该RFID是否可用
     *
     * @return true, 此时设备可以使用读写功能， false 此时设备不可以使用读写功能
     **/
    boolean isOpen();

    /**
     * 关闭设备
     *
     * @return true 关闭成功， false 关闭失败
     */
    boolean close();

    /**
     * RFID单次扫描
     *
     * @return 返回标签信息
     */
    RFIDTagInfo singleScan();

    /**
     * 开始连续扫描
     */
    void startScan(RFIDCallback rfidCallback);

    /**
     * 关闭连续扫描
     */
    void stopScan();

    /**
     * 对EPC进行写入
     *
     * @param content 写入到EPC的内容
     */
    int write(String content);

    /**
     * 设置功率
     *
     * @param power 0~30的整数
     */
    boolean setPower(int power);

    /**
     * 读取数据
     *
     * @param bank   数据所在区域，如EPC TID USER RESERVED，
     * @param offset 起始位置偏移量 单位是1个字
     * @param length 要读取的长度 单位是1个字
     * @return 读取到的内容
     */
    String readData(RFIDAreaEnum bank, int offset,
                    int length);

    /**
     * 根据tid，写入epc数据
     *
     * @param tid     指定要写入的RFID标签的TID
     * @param content 要写入的内容
     */
    int write(String tid, String content);

    /**
     * 往USER去区写入数据
     */
    boolean writeUser(String content);

    /**
     * RFID标签的区域标示
     */
    enum RFIDAreaEnum {
        EPC, TID, USER, RESERVED
    }
}
