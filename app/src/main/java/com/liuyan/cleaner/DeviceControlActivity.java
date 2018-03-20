package com.liuyan.cleaner;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

public class DeviceControlActivity extends Activity implements View.OnClickListener {
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private static final String UUID_CHAR = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private Button start_btn;
    private Button planClean_btn, connect_btn, releaseWater_btn, washWater_btn;     //定时按钮，连接按钮，排水按钮，冲刷按钮
    private  final String mDeviceAddress="C8:FD:19:4B:21:E7";
    private final String mDeviceName = "DX-BT05超声波洗鞋机";
    private TextView device_addr, device_name, connect_state, work_state, water_temp, plan_clean;
    private int extraTime = 61;      //剩余时间，分钟；61代表未启用定时清洗
    private List<BluetoothGattService> test;
    private BluetoothLeService mBluetoothLeService;
    private  BluetoothGattCharacteristic temp=null;  //用来获取BLE设备的串口服务
    public byte[] bytes=new byte[20];
    private int DevStatus = 2;    //设备状态，0-停止，1-已连接，2-未连接，3-超声清洗，4-中间排水，5-冲刷，6-排水，7-已排水
    private byte instSend[] = new byte[1];   //APP向单片机发送的指令，设置定时：0-60 minutes， 手动排水：61，手动冲洗：62，运行：63，关机：64
    // Code to manage Service lifecycle.
    //获取服务
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("信息", "Unable to initialize Bluetooth");
//                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
            Log.e("连接测试","连接成功");
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    public final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                // 连接成功！
                Log.e("测试点1","连接成功");
                connect_state.setText("连接状态: 连接成功");
                start_btn.setBackgroundResource(R.drawable.start_button_pressed);
                start_btn.setText("Start");
                start_btn.setContextClickable(true);
                device_addr.setText("设备地址: " + mDeviceAddress);
                device_name.setText("设备名称: " + mDeviceName);
                DevStatus = 0;

                final MediaPlayer connect_audio=MediaPlayer.create(DeviceControlActivity.this,R.raw.connected);
                connect_audio.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        connect_audio.start();
                      new Thread(new Runnable() {
                          @Override
                          public void run() {
                              try {
                                  Thread.sleep(500);
                                  connect_audio.stop();
                                  connect_audio.release();
                                  Log.e("连接音效播放器","已经释放");
                              } catch (InterruptedException e) {
                                  e.printStackTrace();
                              }
                          }
                      });
                    }
                });
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                connectFailed();
                final MediaPlayer disconnect_audio=MediaPlayer.create(DeviceControlActivity.this,R.raw.disconnected);
                disconnect_audio.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        disconnect_audio.start();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(500);
                                    disconnect_audio.stop();
                                    disconnect_audio.release();
                                    Log.e("断开连接音效播放器","已经释放");
                                } catch (InterruptedException e) {e.printStackTrace();}
                            }

                        });
                    }
                });
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                test = mBluetoothLeService.getSupportedGattServices();
                for(int i=0;i<test.size();i++){
                    Log.e("服务"+i,test.get(i).toString());
                }
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String receiveData = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                char revData = receiveData.charAt(0);
                byte byteData = (byte)revData;
;                Log.e("String数据: ",receiveData);
                System.out.println("String数据: "+receiveData);
                System.out.println(revData);
                System.out.println(byteData);

//                Toast.makeText(context,receiveData,Toast.LENGTH_SHORT).show();    //test1

                dataProcess(byteData);
            }
        }
    };

    //重载函数：接受byte数据处理，byte解析
    public void dataProcess(byte receiveData){
        // byte.bit[0]-byte.bit[1]为帧头：revData[00]-->工作状态，revData[01]-->水温，revData[10]-->定时剩余（min）
        // byte后6 bit为数据
        int frameHead = receiveData >> 6;        //取头帧
        int frameData = receiveData & (byte)63;  //取数据
//        System.out.println("frame head = "+frameHead);
//        System.out.println("frame data = "+frameData);

        //start按钮初始化
        start_btn.setContextClickable(true);
        start_btn.setText("Stop");
        start_btn.setBackgroundResource(R.drawable.stop_button_pressed);

        // 工作状态显示，0-停止，1-已连接，2-未连接，3-超声清洗，4-中间排水，5-冲刷，6-排水，7-已排水
        if(frameHead == 0){
            switch (frameData){
                case 0:
                    work_state.setText("工作状态: 待机");
                    start_btn.setContextClickable(true);
                    start_btn.setText("Start");
                    start_btn.setBackgroundResource(R.drawable.start_button_pressed);
                    DevStatus = 0;
                    break;
                case 1:
                    work_state.setText("工作状态: 连接成功！");
                    DevStatus = 1;
                    break;
                //case 2: 无意义，无连接时不会受到数据帧
                case 3:
                    work_state.setText("工作状态: 超声清洗");
                    DevStatus = 3;
                    break;
                case 4:
                    work_state.setText("工作状态: 中间排水");
                    DevStatus = 4;
                    break;
                case 5:
                    work_state.setText("工作状态: 冲洗中");
                    DevStatus = 5;
                    break;
                case 6:
                    work_state.setText("工作状态: 排水中");
                    DevStatus = 6;
                    break;
                case 7:
                    work_state.setText("工作状态: 已排水");
                    DevStatus = 7;
                    break;
                default:
                    work_state.setText("工作状态: ");
            }

        } else if(frameHead == 1){
            // 水温显示
            water_temp.setText("水温: " + frameData + "℃");
        } else if(frameHead == 2){
            // 定时显示
            if(extraTime != 61){
                extraTime = frameData;
                plan_clean.setText("定时: " + extraTime + "分钟");
            }else {
                extraTime = -1;
                plan_clean.setText("定时: None");
            }
        }
    }


    //接受数据处理函数，字符串解析
    public void dataProcess(String receiveData){
        Log.e("接收的数据",receiveData);
        // String转化为byte[]：revData[0]-->工作状态，revData[1]-->水温，revData[2]-->定时剩余（min）
        byte revData[] = receiveData.getBytes(Charset.forName("ISO8859-1"));

        System.out.println("****** revData: *******");
//        System.out.println(revData[0]);
//        System.out.println(revData[1]);
//        System.out.println(revData[2]);
        //start按钮初始化
        start_btn.setContextClickable(true);
        start_btn.setText("Stop");
        start_btn.setBackgroundResource(R.drawable.stop_button_pressed);

        // 工作状态显示，0-停止，1-已连接，2-未连接，3-超声清洗，4-中间排水，5-冲刷，6-排水，7-已排水
        switch (revData[0]){
            case 0:
                work_state.setText("工作状态: 待机");
                start_btn.setContextClickable(true);
                start_btn.setText("Start");
                start_btn.setBackgroundResource(R.drawable.start_button_pressed);
                DevStatus = 0;
                break;
            case 1:
                work_state.setText("工作状态: 连接成功！");
                DevStatus = 1;
                break;
            //case 2: 无意义，无连接时不会受到数据帧
            case 3:
                work_state.setText("工作状态: 超声清洗");
                DevStatus = 3;
                break;
            case 4:
                work_state.setText("工作状态: 中间排水");
                DevStatus = 4;
                break;
            case 5:
                work_state.setText("工作状态: 冲洗中");
                DevStatus = 5;
                break;
            case 6:
                work_state.setText("工作状态: 排水中");
                DevStatus = 6;
                break;
            case 7:
                work_state.setText("工作状态: 已排水");
                DevStatus = 7;
                break;
            default:
                work_state.setText("工作状态: ");
        }

        // 水温显示
        water_temp.setText("水温: " + revData[1] + "℃");

        // 定时显示
        if(extraTime != 61){
            extraTime = revData[2];
            plan_clean.setText("定时: " + extraTime + "分钟");
        }else {
            extraTime = 61;
            plan_clean.setText("定时: None");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent gattServiceIntent = new Intent(this,BluetoothLeService.class);
        bindService(gattServiceIntent,mServiceConnection,BIND_AUTO_CREATE);    //绑定服务
        // 获取控件byId
        start_btn = (Button)findViewById(R.id.start_btn);
        connect_btn = (Button)findViewById(R.id.connect);
        planClean_btn = (Button)findViewById(R.id.planClean);
        releaseWater_btn = (Button)findViewById(R.id.release_water);
        washWater_btn = (Button)findViewById(R.id.wash_water);
        device_addr = (TextView)findViewById(R.id.device_addr);
        device_name = (TextView)findViewById(R.id.device_name);
        connect_state = (TextView)findViewById(R.id.connect_state);
        work_state = (TextView)findViewById(R.id.workState) ;
        water_temp = (TextView) findViewById(R.id.temperature);
        plan_clean = (TextView)findViewById(R.id.time);
        // click事件
        start_btn.setOnClickListener(this);
        connect_btn.setOnClickListener(this);
        planClean_btn.setOnClickListener(this);
        releaseWater_btn.setOnClickListener(this);
        washWater_btn.setOnClickListener(this);
    }

    // 按钮点击事件处理函数，主要是完成数据指令传送
    //APP向单片机发送的指令，设置定时：0-60 minutes， 手动排水：61，手动冲洗：62，运行：63，关机：64
    @Override
    public void onClick(View v) {
        int id=v.getId();
        instSend[0] = -1;
        switch(id){
            // 连接按钮
            case R.id.connect: {
                mBluetoothLeService.connect(mDeviceAddress);
                DevStatus = 1;
                instSend[0] = 64;
                break;
            }
            //开始按钮
            case R.id.start_btn: {
                start_btn.setContextClickable(true);
                if(DevStatus == 0){    //如果当前状态是待机
                    start_btn.setText("Stop");
                    start_btn.setBackgroundResource(R.drawable.stop_button_pressed);
                    work_state.setText("工作状态: 清洗");
                    DevStatus = 1;    //已连接，也是开始运行
                    instSend[0] = 63;
                }else if(DevStatus != 2){    //如果当前状态不是未连接
                    start_btn.setText("Start");
                    start_btn.setBackgroundResource(R.drawable.start_button_pressed);
                    work_state.setText("工作状态: 待机");
                    DevStatus = 0;
                    instSend[0] = 64;
                }
                break;
            }
            //手动排水按钮
            case R.id.release_water: {
                DevStatus = 6;
                instSend[0] = 61;
                break;
            }
            //手动冲刷按钮
            case R.id.wash_water: {
                DevStatus = 5;
                instSend[0] = 62;
                break;
            }
            //定时按钮,弹窗选择时间定时
            case R.id.planClean: {
                instSend[0] = (byte)extraTime;
                break;
            }
        }

        // 封装帧，发送数据
        String instStr = new String(instSend);
        sendData(instStr);
        Toast.makeText(this,instStr + "Send successfully!",Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.e("", "Connect request result=" + result);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    //广播过滤器
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    //发送数据函数
     public void sendData(String data){
         if (test!=null){
             temp = test.get(3).getCharacteristic(UUID.fromString(UUID_CHAR));
             int flag = temp.getProperties();
             if ((flag | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                 byte[] value = new byte[20];
                 value[0] = (byte) 0x00;
                 bytes = data.getBytes();
                 temp.setValue(value[0], BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                 temp.setValue(bytes);
                 mBluetoothLeService.writeCharacteristic(temp);
                 temp.setValue(value[0], BluetoothGattCharacteristic.FORMAT_UINT8, 0);
             }
             if ((flag | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                 mBluetoothLeService.setCharacteristicNotification(temp, true);
                 Log.e("点击事件"+data,"已运行");
             }
         }else{
             Log.e("temp","请重新连接");
             connectFailed();
         }
     }

     // 连接失败
    private void connectFailed(){
        connect_state.setText("连接状态: 断开连接,请点击重新连接！");
        device_addr.setText("设备地址: ");
        device_name.setText("设备名称: ");
        work_state.setText("工作状态: ");
        water_temp.setText("水温: ");
        plan_clean.setText("定时: ");
        start_btn.setBackgroundResource(R.drawable.start_button_normal);
        start_btn.setText("设备未连接");
        start_btn.setClickable(false);
        DevStatus = 2;
        Log.e("测试点2","断开连接");
    }

    //报警弹窗函数
    public void alarm(int resource, final String message){
        final MediaPlayer mediaPlayer=MediaPlayer.create(DeviceControlActivity.this,resource);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
                AlertDialog.Builder builder= new AlertDialog.Builder(DeviceControlActivity.this);
                builder.setTitle("警告");
                builder.setMessage(message);
                builder.setNegativeButton("停止警报",new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mediaPlayer.stop();
                        mediaPlayer.release();
                    }
                });
                AlertDialog dialog= builder.create();
                dialog.show();
            }
        });
    }

}
