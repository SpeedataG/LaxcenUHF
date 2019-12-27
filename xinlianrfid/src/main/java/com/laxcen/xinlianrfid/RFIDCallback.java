package com.laxcen.xinlianrfid;

/**
 * @author zzc
 * @date 2019/12/27
 */
public interface RFIDCallback {
    /**
     * 成功的返回
     *
     * @param rfidTagInfo 标签信息
     */
    void onResponse(RFIDTagInfo rfidTagInfo);

    /**
     * 失败的返回
     *
     * @param reason 失败的原因
     */
    int onError(int reason);
}
