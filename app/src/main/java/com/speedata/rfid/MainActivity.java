package com.speedata.rfid;

import android.annotation.SuppressLint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.laxcen.xinlianrfid.IRfidDevice;
import com.laxcen.xinlianrfid.IRfidDeviceImpl;
import com.laxcen.xinlianrfid.RFIDCallback;
import com.laxcen.xinlianrfid.RFIDTagInfo;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView textView;
    private IRfidDevice iRfidDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        iRfidDevice = new IRfidDeviceImpl();
    }

    private void initView() {
        findViewById(R.id.btn_init).setOnClickListener(this);
        findViewById(R.id.btn_open).setOnClickListener(this);
        findViewById(R.id.btn_close).setOnClickListener(this);
        findViewById(R.id.btn_free).setOnClickListener(this);
        findViewById(R.id.btn_single).setOnClickListener(this);
        findViewById(R.id.btn_start).setOnClickListener(this);
        findViewById(R.id.btn_stop).setOnClickListener(this);
        findViewById(R.id.btn_read).setOnClickListener(this);
        textView = findViewById(R.id.tv_info);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_init:
                boolean res = iRfidDevice.init();
                Log.d("zzc", "init()===" + res);
                Toast.makeText(this, "init()===" + res, Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_open:
                boolean res2 = iRfidDevice.open();
                Log.d("zzc", "open()===" + res2);
                Toast.makeText(this, "open()===" + res2, Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_close:
                boolean res3 = iRfidDevice.close();
                Log.d("zzc", "close()===" + res3);
                Toast.makeText(this, "close()===" + res3, Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_free:
                boolean res4 = iRfidDevice.free();
                Log.d("zzc", "free()===" + res4);
                Toast.makeText(this, "free()===" + res4, Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_single:
                RFIDTagInfo res5 = iRfidDevice.singleScan();
                if (res5 != null) {
                    Log.d("zzc", "singleScan()===" + res5.getEpcID() + "===" + res5.getOptimizedRSSI());
                    textView.setText(res5.getEpcID());
                } else {
                    Log.d("zzc", "singleScan()===" + res5);
                    textView.setText("搜索超时");
                }
                break;
            case R.id.btn_start:
                textView.setText("");
                iRfidDevice.startScan(new RFIDCallback() {
                    @Override
                    public void onResponse(RFIDTagInfo rfidTagInfo) {
                        Log.d("zzc", "startScan()===" + rfidTagInfo.getEpcID());
                        textView.append(rfidTagInfo.getEpcID() + "\n");
                    }

                    @SuppressLint("SetTextI18n")
                    @Override
                    public int onError(int reason) {
                        Log.d("zzc", "startScan()=onError==" + reason);
                        textView.setText("startScan()=onError==" + reason);
                        return 0;
                    }
                });
                break;
            case R.id.btn_stop:
                iRfidDevice.stopScan();
                Log.d("zzc", "stopScan()===");
                break;
            case R.id.btn_read:
                String data = iRfidDevice.readData(IRfidDevice.RFIDAreaEnum.USER, 0, 6);
                textView.setText(data);
                break;
            default:
                break;
        }
    }
}
