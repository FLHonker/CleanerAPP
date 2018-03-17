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

import java.util.List;
import java.util.UUID;

public class DeviceControlActivity extends Activity implements View.OnClickListener {
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private Button start_button;
    private Button planClean,connect;     //设置按钮
    private  boolean start_flag=false;
    private  final String mDeviceAddress="C8:FD:19:4B:21:E7";
    private TextView device_addr;
    private TextView connect_state;
    private TextView work_state;
    private TextView water_temp;
    private TextView plan_clean;
    private List<BluetoothGattService> test;
    private BluetoothLeService mBluetoothLeService;
    private  BluetoothGattCharacteristic temp=null;  //用来获取BLE设备的串口服务
    public byte[] bytes=new byte[20];
    private static  final String REGEX="e";
//    private static  Pattern pattern;
//    private static  Matcher matcher;
//    private static  String  REPLACE=".";
    private  final int scale=2;
    private  final int roundingMode=4;
    private boolean working_state=false;
    private int DevStatus = 2;    //设备状态，0-停止，1-运行，2-未连接，3-已排水
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
                Log.e("测试点1","连接成功");
                connect_state.setText("连接状态: 连接成功");
                start_button.setBackgroundResource(R.drawable.start_button_pressed);
                start_button.setText("Start");
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
                connect_state.setText("连接状态："+" 断开连接,请点击扫描重新连接！");
                start_button.setBackgroundResource(R.drawable.start_button_normal);
                start_button.setText("设备未运行");
                start_button.setClickable(false);
                DevStatus = 2;
                Log.e("测试点2","断开连接");
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
//                sendData("aa");
                Log.e("启动","ok");
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String receiveData=intent.getStringExtra(BluetoothLeService.EXTRA_DATA).toLowerCase();
                Log.e("数据",receiveData);
                Toast.makeText(context,receiveData,Toast.LENGTH_SHORT).show();
                byte revData = intent.getByteExtra(BluetoothLeService.EXTRA_DATA,(byte)0);
                water_temp.setText(receiveData);   // tests
                water_temp.setText(revData);      // tests2
                Toast.makeText(context,revData,Toast.LENGTH_SHORT).show();
//                  if(receiveData.length()==17){
//                      dataProcess(receiveData);
//                  }
            }
            Log.d("action",action);
        }
    };

    //接受数据处理函数，字符串解析
    public void dataProcess(String receiveData){
        Log.e("接收的数据",receiveData);

        if(receiveData.compareTo("st") == 0){
            start_button.setBackgroundResource(R.drawable.stop_button_selector);
            start_button.setClickable(true);
            start_button.setText("Stop");
            working_state=true;
        }else  if(receiveData.compareTo("cl") == 0){
            start_button.setBackgroundResource(R.drawable.start_button_normal);
            start_button.setClickable(false);
            start_button.setText("设备未运行");
            working_state=false;
        }

        if (receiveData.compareTo("61") == 0){
            work_state.setText("工作状态：清洗");
        }else if (receiveData.compareTo("62") == 0){
            work_state.setText("工作状态：冲洗");
        }else if (receiveData.compareTo("63") == 0){
            work_state.setText("工作状态：排水");
        }

        water_temp.setText("水温:"+receiveData+"℃");

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
  /*  //替换字符E
    public String replaceE(String data){
        String temp=null;
        pattern = Pattern.compile(REGEX);
        matcher = pattern.matcher(data);
        temp=matcher.replaceAll(REPLACE);
        Log.e("处理后数据",temp);
        return  temp;
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent gattServiceIntent =new Intent(this,BluetoothLeService.class);
        bindService(gattServiceIntent,mServiceConnection,BIND_AUTO_CREATE);
        start_button=(Button)findViewById(R.id.start_btn);
        connect=(Button)findViewById(R.id.connect);
        device_addr=(TextView)findViewById(R.id.device_addr) ;
        connect_state=(TextView)findViewById(R.id.connect_state);
        work_state=(TextView)findViewById(R.id.workState) ;
        water_temp= (TextView) findViewById(R.id.temperature);
        plan_clean=(TextView)findViewById(R.id.time);
        start_button.setOnClickListener(this);
        connect.setOnClickListener(this);

        Log.e("连接设备的地址",mDeviceAddress);
        device_addr.setText("设备地址:"+" "+mDeviceAddress);
    }

    //点击事件处理函数
    @Override
    public void onClick(View v) {
        int id=v.getId();
        switch(id){
            case R.id.connect:{
                mBluetoothLeService.connect(mDeviceAddress);
                break;
            }
            case R.id.start_btn: {
                if(working_state){
                    sendData("s2");    //停止
                    break;
                }
            }
        }
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
             temp = test.get(3).getCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"));
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
             //发送结束符
//             endSend();
         }else{
             Log.e("temp","请重新连接");
         }
     }

    //发送结束符函数
    public void endSend() {
        temp = test.get(3).getCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"));
        int flag = temp.getProperties();
        if(temp != null){
            if ((flag | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                byte[] value = new byte[20];
                value[0] = (byte) 0x00;
                bytes = hex2byte();
                temp.setValue(value[0], BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                temp.setValue(bytes);
                mBluetoothLeService.writeCharacteristic(temp);
                temp.setValue(value[0], BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            }
        }
    }

    //增加16进制的结尾符
    public byte[] hex2byte() {
        byte[] b2 = new byte[4];
        // 两位一组，表示一个字节,把这样表示的16进制字符串，还原成一个进制字节
        b2[0]=(byte)Integer.parseInt("0d",16);
        b2[1] = (byte) Integer.parseInt("0a", 16);
        return b2;
    }
}
