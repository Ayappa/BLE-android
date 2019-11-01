package com.example.ble;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT =001 ;
    private BluetoothAdapter bluetoothAdapter;
    private boolean mScanning;
    private Handler handler =new Handler();
    private static final long SCAN_PERIOD = 10000;
    public final static UUID UUIDBlub= UUID.fromString("df2565d8-6e75-d757-b482-10edbf5377a6");
    public final static UUID Blub= UUID.fromString("FB959362-F26E-43A9-927C-7E17D8FB2D8D");
    public final static UUID Temperature= UUID.fromString("0CED9345-B31F-457D-A6A2-B3DB9B03E39A");
    public final static UUID Beep= UUID.fromString("EC958823-F26E-43A9-927C-7E17D8F32A90");
    BluetoothGatt server;
    Button sound,light;
    TextView temp;
    BluetoothDevice deviceBl;
    BluetoothGatt gattConnect;
    List<BluetoothGattCharacteristic> gattCharacteristics=null;
    Boolean lightBle=false,soundBle=false;
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sound=findViewById(R.id.sound);
        light=findViewById(R.id.light);
        temp=(TextView)findViewById(R.id.temperature);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "ble_not_supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        scanLeDevice(true);


        light.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lightBle=!lightBle;
                int format = BluetoothGattCharacteristic.FORMAT_UINT8;
                if(lightBle){
                    light.setBackgroundResource(R.drawable.nolight);
                    gattCharacteristics.get(1).setValue(1,format,0);
                    server.writeCharacteristic(gattCharacteristics.get(1));
                }else{
                    light.setBackgroundResource(R.drawable.light);
                    gattCharacteristics.get(1).setValue(0,format,0);
                    server.writeCharacteristic(gattCharacteristics.get(1));

                }
            }
        });

        sound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundBle=!soundBle;
                int format = BluetoothGattCharacteristic.FORMAT_UINT8;
                if(soundBle){
                    sound.setBackgroundResource(R.drawable.mute);
                    gattCharacteristics.get(2).setValue(1,format,0);
                }else{
                    sound.setBackgroundResource(R.drawable.sound);
                    gattCharacteristics.get(2).setValue(0,format,0 );
                }
                server.writeCharacteristic(gattCharacteristics.get(2));
            }
        });

    }


    @SuppressLint("NewApi")
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @SuppressLint("NewApi")
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothAdapter.stopLeScan(leScanCallback);
                }
            }, SCAN_PERIOD);
            mScanning = true;
            bluetoothAdapter.startLeScan(new  UUID[]{UUIDBlub},leScanCallback);
        } else {
            mScanning = false;
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
    }
    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    if(device!=null){deviceBl=device; service(device);}
                }
            };

    public void service(BluetoothDevice device){
        BluetoothGattCallback gattCallback=new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                 gattConnect=gatt;
               gatt.discoverServices();
            }
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                List<BluetoothGattService> gattServices=gatt.getServices();
                for(BluetoothGattService gattService : gattServices){
                    if(gattService.getUuid().equals(UUIDBlub)) {
                         gattCharacteristics = gattService.getCharacteristics();
                    }
                }
                       gatt.setCharacteristicNotification(gattCharacteristics.get(0), true);
                       BluetoothGattDescriptor descriptor = gattCharacteristics.get(0).getDescriptor(gattCharacteristics.get(0).getDescriptors().get(0).getUuid());
                       descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                       gatt.writeDescriptor(descriptor);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
               int format = BluetoothGattCharacteristic.FORMAT_UINT8;
                final byte[] data = characteristic.getValue();
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                if (data != null && data.length > 0) {
                    for(byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));

                }
                char[] charArray = new char[stringBuilder.length()];
                stringBuilder.getChars(0, stringBuilder.length(), charArray, 0);
                 StringBuffer sb=new StringBuffer();
                for(int i=1;i<charArray.length;i=i+3){
                    sb.append(charArray[i]);
                }
                final String t=sb.toString();
                final int heartRate1 = characteristic.getIntValue(format, 1);
                // Get a handler that can be used to post to the main thread
                Handler mainHandler = new Handler(getApplicationContext().getMainLooper());

                Runnable myRunnable = new Runnable() {
                    @Override
                    public void run() {
                        temp.setText((t+" F"));
                        sound.setEnabled(true);
                        light.setEnabled(true);

                    } // This is your code
                };
                mainHandler.post(myRunnable);
                Log.d("change", sb.toString()+" F");
            }
        };

         server=device.connectGatt(MainActivity.this,false,gattCallback);
    }

}
