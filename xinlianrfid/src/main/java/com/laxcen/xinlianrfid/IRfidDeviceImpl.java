package com.laxcen.xinlianrfid;


import android.os.Handler;
import android.serialport.DeviceControlSpd;
import android.util.Log;

import com.laxcen.xinlianrfid.utils.StringUtils;
import com.uhf.speedatagapi.cls.Reader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * RFID扫描枪接口实现类
 *
 * @author zzc
 * @date 2019/12/27
 */
public class IRfidDeviceImpl implements IRfidDevice {
    private static Reader Mreader;
    private ReaderParams Rparams;
    private Handler handler = null;
    private static int antportc;
    private boolean nostop = false;
    private boolean isOpen = false;
    private RFIDTagInfo rfidTagInfo;
    private DeviceControlSpd deviceControlSpd;
    private String SERIALPORT_SD60 = "/dev/ttyMT0";
    private RFIDCallback rfidCallback = null;
    private List<RFIDTagInfo> arrayList = new ArrayList<>();

    @Override
    public boolean init() {
        Mreader = new Reader();
        Rparams = new ReaderParams();
        try {
            deviceControlSpd = new DeviceControlSpd(DeviceControlSpd.PowerType.EXPAND, 9, 14);
            deviceControlSpd.PowerOnDevice();
            Reader.READER_ERR er = Mreader.InitReader_Notype(SERIALPORT_SD60, 1);
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                antportc = 1;
                isOpen = true;
                return true;
            } else {
                isOpen = false;
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean free() {
        if (Mreader != null) {
            Mreader.CloseReader();
        }
        if (Rparams != null) {
            Rparams = null;
        }
        if (deviceControlSpd != null) {
            try {
                deviceControlSpd.PowerOffDevice();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        isOpen = false;
        return true;
    }

    @Override
    public boolean open() {
        if (Mreader == null) {
            return init();
        }
        return true;
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public boolean close() {
        return free();
    }

    RFIDCallback mRfidCallback = new RFIDCallback() {
        @Override
        public void onResponse(RFIDTagInfo mRfidTagInfo) {
            rfidTagInfo = new RFIDTagInfo();
            rfidTagInfo.setEpcID(mRfidTagInfo.getEpcID());
            rfidTagInfo.setTid(mRfidTagInfo.getTid());
            rfidTagInfo.setOptimizedRSSI(mRfidTagInfo.getOptimizedRSSI());
            Log.d("zzc", "rfidTagInfo======onResponse=" + mRfidTagInfo.getOptimizedRSSI());
            arrayList.add(rfidTagInfo);
            for (int i = 1; i < arrayList.size(); i++) {
                if (arrayList.get(i).getOptimizedRSSI() < arrayList.get(i - 1).getOptimizedRSSI()) {
                    RFIDTagInfo temp;
                    temp = arrayList.get(i - 1);
                    arrayList.set(i - 1, arrayList.get(i));
                    arrayList.set(i, temp);
                }
            }
        }

        @Override
        public int onError(int reason) {
            return reason;
        }
    };

    @Override
    public RFIDTagInfo singleScan() {
        rfidTagInfo = null;
        this.rfidCallback = mRfidCallback;
        selectCard(1, 32, new byte[]{0x00}, false);
        if (arrayList != null) {
            arrayList.clear();
        }
        inventory();
        for (int i = 0; i < 10; i++) {
            if (rfidTagInfo != null) {
                Log.d("zzc", "rfidTagInfo=======" + i);
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (arrayList != null && arrayList.size() > 1) {
            selectCard(1, 32, StringUtils.stringToByte(arrayList.get(arrayList.size() - 1).getEpcID()), true);
            return arrayList.get(arrayList.size() - 1);
        } else {
            if (rfidTagInfo != null) {
                selectCard(1, 32, StringUtils.stringToByte(rfidTagInfo.getEpcID()), true);
            }
            return rfidTagInfo;
        }
    }


    @Override
    public void startScan(RFIDCallback rfidCallback) {
        if (inSearch) {
            return;
        }
        selectCard(1, 32, new byte[]{0x00}, false);
        this.rfidCallback = rfidCallback;
        if (handler == null) {
            handler = new Handler();
        }
        inSearch = true;
        handler.postDelayed(inv_thread, 0);
    }


    @Override
    public void stopScan() {
        if (!inSearch) {
            return;
        }
        inSearch = false;
        try {
            if (handler != null) {
                handler.removeCallbacks(inv_thread);
                handler = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int write(String content) {
        return writeArea(1, 2, content);
    }

    @Override
    public boolean setPower(int power) {
        Reader.AntPowerConf apcf = Mreader.new AntPowerConf();
        apcf.antcnt = antportc;
        int[] rpow = new int[apcf.antcnt];
        int[] wpow = new int[apcf.antcnt];
        for (int i = 0; i < apcf.antcnt; i++) {
            Reader.AntPower jaap = Mreader.new AntPower();
            jaap.antid = i + 1;
            jaap.readPower = (short) (power * 100);
            rpow[i] = jaap.readPower;
            jaap.writePower = (short) (power * 100);
            wpow[i] = jaap.writePower;
            apcf.Powers[i] = jaap;
        }
        try {
            Reader.READER_ERR er = Mreader.ParamSet(
                    Reader.Mtr_Param.MTR_PARAM_RF_ANTPOWER, apcf);
            if (er != Reader.READER_ERR.MT_OK_ERR) {
                return false;
            } else {
                Rparams.rpow = rpow;
                Rparams.wpow = wpow;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public String readData(RFIDAreaEnum bank, int offset, int length) {
        try {
            int area;
            switch (bank) {
                case EPC:
                    area = 1;
                    break;
                case TID:
                    area = 2;
                    break;
                case USER:
                    area = 3;
                    break;
                case RESERVED:
                    area = 0;
                    break;
                default:
                    return null;
            }
            byte[] rdata = new byte[length];
            byte[] rpaswd = new byte[4];
            Mreader.Str2Hex("00000000", 8, rpaswd);
            Reader.READER_ERR er = Reader.READER_ERR.MT_OK_ERR;
            int trycount = 3;
            do {
                er = Mreader.GetTagData(Rparams.opant,
                        (char) area, offset / 2, length / 2,
                        rdata, rpaswd, (short) Rparams.optime);
                trycount--;
                if (trycount < 1) {
                    break;
                }
            } while (er != Reader.READER_ERR.MT_OK_ERR);
            int errorCode;
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                errorCode = 0;
                return StringUtils.byteToHexString(rdata, rdata.length);
            } else if (er == Reader.READER_ERR.MT_IO_ERR) {
                errorCode = 1;
            } else if (er == Reader.READER_ERR.MT_INTERNAL_DEV_ERR) {
                errorCode = 2;
            } else if (er == Reader.READER_ERR.MT_CMD_FAILED_ERR) {
                errorCode = 3;
            } else if (er == Reader.READER_ERR.MT_CMD_NO_TAG_ERR) {
                errorCode = 4;
            } else if (er == Reader.READER_ERR.MT_M5E_FATAL_ERR) {
                errorCode = 5;
            } else if (er == Reader.READER_ERR.MT_OP_NOT_SUPPORTED) {
                errorCode = 6;
            } else if (er == Reader.READER_ERR.MT_INVALID_PARA) {
                errorCode = 7;
            } else if (er == Reader.READER_ERR.MT_INVALID_READER_HANDLE) {
                errorCode = 8;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_HIGN_RETURN_LOSS) {
                errorCode = 9;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET) {
                errorCode = 10;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_NO_ANTENNAS) {
                errorCode = 11;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_HIGH_TEMPERATURE) {
                errorCode = 12;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_READER_DOWN) {
                errorCode = 13;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_UNKNOWN_ERR) {
                errorCode = 14;
            } else if (er == Reader.READER_ERR.M6E_INIT_FAILED) {
                errorCode = 15;
            } else if (er == Reader.READER_ERR.MT_OP_EXECING) {
                errorCode = 16;
            } else if (er == Reader.READER_ERR.MT_UNKNOWN_READER_TYPE) {
                errorCode = 17;
            } else if (er == Reader.READER_ERR.MT_OP_INVALID) {
                errorCode = 18;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_BY_FAILED_RESET_MODLUE) {
                errorCode = 19;
            } else if (er == Reader.READER_ERR.MT_MAX_ERR_NUM) {
                errorCode = 20;
            } else {
                errorCode = 20;
            }
            return errorCode + "";
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public int write(String tid, String content) {
        byte[] tidBytes = StringUtils.stringToByte(tid);
        selectCard(2, 0, tidBytes, true);
        return writeArea(2, 0, content);
    }

    @Override
    public boolean writeUser(String content) {
        int res = writeArea(3, 0, content);
        return res == 0;
    }

    private volatile boolean inSearch = false;
    private Runnable inv_thread = new Runnable() {
        @Override
        public void run() {
            if (inventory() != 0) {
                stopScan();
            } else {
                Log.d(TAG, "run:5555555555555==next");
                if (handler != null) {
                    handler.postDelayed(this, Rparams.sleep);
                }
            }
        }
    };


    private int inventory() {
        Log.d(TAG, "run: 1111111111111111111111");
        String tag = null;
        int[] tagcnt = new int[1];
        tagcnt[0] = 0;
        synchronized (this) {
            Reader.READER_ERR er;
//                int[] uants = Rparams.uants;
            Log.d(TAG, "run: 2222222222222222222222222222");
            if (nostop) {
                Log.d(TAG, "run: 2222222222222222222222222222==AsyncGetTagCount==");
                er = Mreader.AsyncGetTagCount(tagcnt);
            } else {
                Log.d(TAG, "run: 2222222222222222222222222222==TagInventory_Raw==");
                er = Mreader.TagInventory_Raw(Rparams.uants,
                        Rparams.uants.length,
                        (short) Rparams.readtime, tagcnt);
            }
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                if (tagcnt[0] > 0) {
                    for (int i = 0; i < tagcnt[0]; i++) {
                        Log.d(TAG, "run: 33333333333");
                        Reader.TAGINFO tfs = Mreader.new TAGINFO();
                        if (nostop) {
                            Log.d(TAG, "run: 33333333333==AsyncGetNextTag");
                            er = Mreader.AsyncGetNextTag(tfs);
                        } else {
                            Log.d(TAG, "run: 33333333333==GetNextTag");
                            er = Mreader.GetNextTag(tfs);
                        }
                        if (er == Reader.READER_ERR.MT_OK_ERR) {
                            byte[] n_epc = tfs.EpcId;
                            byte[] n_tid = tfs.EmbededData;
                            String strEPCTemp = StringUtils.byteToHexString(n_epc, n_epc.length);
                            String strDataTemp = null;
                            if (n_tid != null) {
                                strDataTemp = StringUtils.byteToHexString(n_tid, n_tid.length);
                            }
                            Log.d(TAG, "run: 4444444444");
                            int rssi = tfs.RSSI + 100;
                            if (rssi < 0) {
                                rssi = 0;
                            } else if (rssi > 100) {
                                rssi = 100;
                            }
                            RFIDTagInfo tagInfo = new RFIDTagInfo();
                            tagInfo.setEpcID(strEPCTemp);
                            tagInfo.setTid(strDataTemp);
                            tagInfo.setOptimizedRSSI(rssi);
                            rfidCallback.onResponse(tagInfo);
                        }
                    }
                }
            } else {
                Log.d(TAG, "run: err");
                int errCode = -1;
                if (er == Reader.READER_ERR.MT_IO_ERR) {
                    errCode = 1;
                } else if (er == Reader.READER_ERR.MT_INTERNAL_DEV_ERR) {
                    errCode = 2;
                } else if (er == Reader.READER_ERR.MT_CMD_FAILED_ERR) {
                    errCode = 3;
                } else if (er == Reader.READER_ERR.MT_CMD_NO_TAG_ERR) {
                    errCode = 4;
                } else if (er == Reader.READER_ERR.MT_M5E_FATAL_ERR) {
                    errCode = 5;
                } else if (er == Reader.READER_ERR.MT_OP_NOT_SUPPORTED) {
                    errCode = 6;
                } else if (er == Reader.READER_ERR.MT_INVALID_PARA) {
                    errCode = 7;
                } else if (er == Reader.READER_ERR.MT_INVALID_READER_HANDLE) {
                    errCode = 8;
                } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_HIGN_RETURN_LOSS) {
                    errCode = 9;
                } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET) {
                    errCode = 10;
                } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_NO_ANTENNAS) {
                    errCode = 11;
                } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_HIGH_TEMPERATURE) {
                    errCode = 12;
                } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_READER_DOWN) {
                    errCode = 13;
                } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_UNKNOWN_ERR) {
                    errCode = 14;
                } else if (er == Reader.READER_ERR.M6E_INIT_FAILED) {
                    errCode = 15;
                } else if (er == Reader.READER_ERR.MT_OP_EXECING) {
                    errCode = 16;
                } else if (er == Reader.READER_ERR.MT_UNKNOWN_READER_TYPE) {
                    errCode = 17;
                } else if (er == Reader.READER_ERR.MT_OP_INVALID) {
                    errCode = 18;
                } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_BY_FAILED_RESET_MODLUE) {
                    errCode = 19;
                } else if (er == Reader.READER_ERR.MT_MAX_ERR_NUM) {
                    errCode = 20;
                } else {
                    errCode = 20;
                }
                rfidCallback.onError(errCode);
                return errCode;
            }
        }
        return 0;
    }

    private int selectCard(int bank, int addr, byte[] cont, boolean mFlag) {
        Reader.READER_ERR er;
        try {
            if (mFlag) {
                if (cont == null) {
                    return -1;
                }
                Reader.TagFilter_ST g2tf = Mreader.new TagFilter_ST();
                g2tf.fdata = cont;
                g2tf.flen = cont.length * 8;
                g2tf.isInvert = 0;
                g2tf.bank = bank;
                g2tf.startaddr = addr;
                er = Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_FILTER, g2tf);
            } else {
                er = Mreader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_FILTER, null);
            }
            if (er != Reader.READER_ERR.MT_OK_ERR) {
                return -1;
            }
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private int writeArea(int area, int addr, String content) {
        try {
            byte[] contentBytes = StringUtils.stringToByte(content);
            if ((contentBytes.length % 2) != 0) {
                return -3;
            }
            byte[] rpaswd = new byte[4];
            Mreader.Str2Hex("00000000", 8, rpaswd);
            Reader.READER_ERR er = Reader.READER_ERR.MT_OK_ERR;
            int trycount = 3;
            do {
                er = Mreader.WriteTagData(Rparams.opant,
                        (char) area, addr, contentBytes, contentBytes.length, rpaswd,
                        (short) Rparams.optime);
                trycount--;
                if (trycount < 1) {
                    break;
                }
            } while (er != Reader.READER_ERR.MT_OK_ERR);
            Log.d(TAG, "write_area: end");
            int errorCode;
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                errorCode = 0;
            } else if (er == Reader.READER_ERR.MT_IO_ERR) {
                errorCode = 1;
            } else if (er == Reader.READER_ERR.MT_INTERNAL_DEV_ERR) {
                errorCode = 2;
            } else if (er == Reader.READER_ERR.MT_CMD_FAILED_ERR) {
                errorCode = 3;
            } else if (er == Reader.READER_ERR.MT_CMD_NO_TAG_ERR) {
                errorCode = 4;
            } else if (er == Reader.READER_ERR.MT_M5E_FATAL_ERR) {
                errorCode = 5;
            } else if (er == Reader.READER_ERR.MT_OP_NOT_SUPPORTED) {
                errorCode = 6;
            } else if (er == Reader.READER_ERR.MT_INVALID_PARA) {
                errorCode = 7;
            } else if (er == Reader.READER_ERR.MT_INVALID_READER_HANDLE) {
                errorCode = 8;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_HIGN_RETURN_LOSS) {
                errorCode = 9;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET) {
                errorCode = 10;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_NO_ANTENNAS) {
                errorCode = 11;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_HIGH_TEMPERATURE) {
                errorCode = 12;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_READER_DOWN) {
                errorCode = 13;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_UNKNOWN_ERR) {
                errorCode = 14;
            } else if (er == Reader.READER_ERR.M6E_INIT_FAILED) {
                errorCode = 15;
            } else if (er == Reader.READER_ERR.MT_OP_EXECING) {
                errorCode = 16;
            } else if (er == Reader.READER_ERR.MT_UNKNOWN_READER_TYPE) {
                errorCode = 17;
            } else if (er == Reader.READER_ERR.MT_OP_INVALID) {
                errorCode = 18;
            } else if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_BY_FAILED_RESET_MODLUE) {
                errorCode = 19;
            } else if (er == Reader.READER_ERR.MT_MAX_ERR_NUM) {
                errorCode = 20;
            } else {
                errorCode = 20;
            }
            return errorCode;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public class ReaderParams {

        //save param
        public int opant;

        public List<String> invpro;
        public String opro;
        public int[] uants;
        public int readtime;
        public int sleep;

        public int checkant;
        public int[] rpow;
        public int[] wpow;

        public int region;
        public int[] frecys;
        public int frelen;

        public int session;
        public int qv;
        public int wmode;
        public int blf;
        public int maxlen;
        public int target;
        public int gen2code;
        public int gen2tari;

        public String fildata;
        public int filadr;
        public int filbank;
        public int filisinver;
        public int filenable;

        public int emdadr;
        public int emdbytec;
        public int emdbank;
        public int emdenable;

        public int antq;
        public int adataq;
        public int rhssi;
        public int invw;
        public int iso6bdeep;
        public int iso6bdel;
        public int iso6bblf;
        public int option;
        //other params

        public String password;
        public int optime;

        public ReaderParams() {
            opant = 1;
            invpro = new ArrayList<String>();
            invpro.add("GEN2");
            uants = new int[1];
            uants[0] = 1;
            sleep = 0;
            readtime = 50;
            optime = 1000;
            opro = "GEN2";
            checkant = 1;
            rpow = new int[]{2700, 2000, 2000, 2000};
            wpow = new int[]{2000, 2000, 2000, 2000};
            region = 1;
            frelen = 0;
            session = 0;
            qv = -1;
            wmode = 0;
            blf = 0;
            maxlen = 0;
            target = 0;
            gen2code = 2;
            gen2tari = 0;

            fildata = "";
            filadr = 32;
            filbank = 1;
            filisinver = 0;
            filenable = 0;

            emdadr = 0;
            emdbytec = 0;
            emdbank = 1;
            emdenable = 0;

            adataq = 0;
            rhssi = 1;
            invw = 0;
            iso6bdeep = 0;
            iso6bdel = 0;
            iso6bblf = 0;
            option = 0;
        }
    }
}
