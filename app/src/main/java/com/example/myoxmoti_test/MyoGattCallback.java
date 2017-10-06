package com.example.myoxmoti_test;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.graphics.drawable.ColorDrawable;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

/**
 * Created by naoki on 15/04/15.
 */
 
public class MyoGattCallback extends BluetoothGattCallback {
    /** Service ID */
    private static final String MYO_CONTROL_ID  = "d5060001-a904-deb9-4748-2c7f4a124842";
    private static final String MYO_EMG_DATA_ID = "d5060005-a904-deb9-4748-2c7f4a124842";
    private static final String MYO_IMU_DATA_ID = "d5060002-a904-deb9-4748-2c7f4a124842";
    /** Characteristics ID */
    private static final String MYO_INFO_ID = "d5060101-a904-deb9-4748-2c7f4a124842";
    private static final String FIRMWARE_ID = "d5060201-a904-deb9-4748-2c7f4a124842";
    private static final String COMMAND_ID  = "d5060401-a904-deb9-4748-2c7f4a124842";
    private static final String EMG_0_ID    = "d5060105-a904-deb9-4748-2c7f4a124842";
    private static final String IMU_DATA_ID    = "d5060402-a904-deb9-4748-2c7f4a124842";
    /** android Characteristic ID (from Android Samples/BluetoothLeGatt/SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG) */
    private static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    private Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<BluetoothGattDescriptor>();
    private Queue<BluetoothGattCharacteristic> readCharacteristicQueue = new LinkedList<BluetoothGattCharacteristic>();

    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mCharacteristic_command;
    private BluetoothGattCharacteristic mCharacteristic_emg0;
    private BluetoothGattCharacteristic mCharacteristic_imu;

    private MyoCommandList commandList = new MyoCommandList();

    private String TAG = "MyoGatt";

    private TextView dataView;
    private String callback_msg;
    //private Handler mHandler;
    private int[] emgDatas = new int[16];

    //private Button mybtn;
    byte[] emg_data;
    public String EMG_data="";

    private int nowGraphIndex = 0;
    private Button nowButton;

    int[][] dataList1_a = new int[8][50];
    int[][] dataList1_b = new int[8][50];

    private static final float MYOHW_ORIENTATION_SCALE = 16384.0f;
    private static final float MYOHW_ACCELEROMETER_SCALE = 2048.0f;
    private static final float MYOHW_GYROSCOPE_SCALE = 16.0f;
    private static final float G=9.8f;

    private LinkedList<EmgData> list_emg = new LinkedList<>();
    private LinkedList<ImuData> list_imu = new LinkedList<>();

    private TimeManager timeManager;

    public MyoGattCallback(TextView view, TimeManager tM){
        dataView = view;
        timeManager = tM;
    }


    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        Log.d(TAG, "onConnectionStateChange: " + status + " -> " + newState);
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            // GATT Connected
            // Searching GATT Service
            gatt.discoverServices();

        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            // GATT Disconnected
            stopCallback();
            Log.d(TAG,"Bluetooth Disconnected");
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        Log.d(TAG, "onServicesDiscovered received: " + status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            // Find GATT Service
            BluetoothGattService service_emg = gatt.getService(UUID.fromString(MYO_EMG_DATA_ID));
            if (service_emg == null) {
                Log.d(TAG,"No Myo EMG-Data Service !!");
            } else {
                Log.d(TAG, "Find Myo EMG-Data Service !!");
                // Getting CommandCharacteristic
                mCharacteristic_emg0 = service_emg.getCharacteristic(UUID.fromString(EMG_0_ID));
                if (mCharacteristic_emg0 == null) {
                    callback_msg = "Not Found EMG-Data Characteristic";
                } else {
                    // Setting the notification
                    boolean registered_0 = gatt.setCharacteristicNotification(mCharacteristic_emg0, true);
                    if (!registered_0) {
                        Log.d(TAG,"EMG-Data Notification FALSE !!");
                    } else {
                        Log.d(TAG,"EMG-Data Notification TRUE !!");
                        // Turn ON the Characteristic Notification
                        BluetoothGattDescriptor descriptor_0 = mCharacteristic_emg0.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
                        if (descriptor_0 != null ){
                            descriptor_0.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                            writeGattDescriptor(descriptor_0);

                            Log.d(TAG,"Set descriptor");

                        } else {
                            Log.d(TAG,"No descriptor");
                        }
                    }
                }
            }

            // Find GATT Service(mIMU)
            BluetoothGattService service_imu = gatt.getService(UUID.fromString(MYO_IMU_DATA_ID));
            if (service_imu == null) {
                Log.d(TAG,"No Myo IMU-Data Service !!");
            } else {
                Log.d(TAG, "Find Myo IMU-Data Service !!");
                // Getting CommandCharacteristic
                mCharacteristic_imu = service_imu.getCharacteristic(UUID.fromString(IMU_DATA_ID));
                if (mCharacteristic_imu == null) {
                    callback_msg = "Not Found IMU-Data Characteristic";
                } else {
                    // Setting the notification
                    boolean registered_imu = gatt.setCharacteristicNotification(mCharacteristic_imu, true);
                    if (!registered_imu) {
                        Log.d(TAG,"IMU-Data Notification FALSE !!");
                    } else {
                        Log.d(TAG,"IMU-Data Notification TRUE !!");
                        // Turn ON the Characteristic Notification
                        BluetoothGattDescriptor descriptor_imu = mCharacteristic_imu.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
                        if (descriptor_imu != null ){
                            descriptor_imu.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                            writeGattDescriptor(descriptor_imu);

                            Log.d(TAG,"Set descriptor");

                        } else {
                            Log.d(TAG,"No descriptor");
                        }
                    }
                }
            }

            BluetoothGattService service = gatt.getService(UUID.fromString(MYO_CONTROL_ID));
            if (service == null) {
                Log.d(TAG,"No Myo Control Service !!");
            } else {
                Log.d(TAG, "Find Myo Control Service !!");
                // Get the MyoInfoCharacteristic
                BluetoothGattCharacteristic characteristic =
                        service.getCharacteristic(UUID.fromString(MYO_INFO_ID));
                if (characteristic == null) {
                } else {
                    Log.d(TAG, "Find read Characteristic !!");
                    //put the characteristic into the read queue
                    readCharacteristicQueue.add(characteristic);
                    //if there is only 1 item in the queue, then read it.  If more than 1, we handle asynchronously in the callback above
                    //GIVE PRECEDENCE to descriptor writes.  They must all finish first.
                    if((readCharacteristicQueue.size() == 1) && (descriptorWriteQueue.size() == 0)) {
                        mBluetoothGatt.readCharacteristic(characteristic);
                    }
/*                        if (gatt.readCharacteristic(characteristic)) {
                            Log.d(TAG, "Characteristic read success !!");
                        }
*/
                }

                // Get CommandCharacteristic
                mCharacteristic_command = service.getCharacteristic(UUID.fromString(COMMAND_ID));
                if (mCharacteristic_command == null) {
                } else {
                    Log.d(TAG, "Find command Characteristic !!");
                }
            }
        }
    }

    public void writeGattDescriptor(BluetoothGattDescriptor d){
        //put the descriptor into the write queue
        descriptorWriteQueue.add(d);
        //if there is only 1 item in the queue, then write it.  If more than 1, we handle asynchronously in the callback above
        if(descriptorWriteQueue.size() == 1){
            mBluetoothGatt.writeDescriptor(d);
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "Callback: Wrote GATT Descriptor successfully.");
        }
        else{
            Log.d(TAG, "Callback: Error writing GATT Descriptor: "+ status);
        }
        descriptorWriteQueue.remove();  //pop the item that we just finishing writing
        //if there is more to write, do it!
        if(descriptorWriteQueue.size() > 0)
            mBluetoothGatt.writeDescriptor(descriptorWriteQueue.element());
        else if(readCharacteristicQueue.size() > 0)
            mBluetoothGatt.readCharacteristic(readCharacteristicQueue.element());
    }

    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        readCharacteristicQueue.remove();
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (UUID.fromString(FIRMWARE_ID).equals(characteristic.getUuid())) {
                // Myo Firmware Infomation
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    ByteReader byteReader = new ByteReader();
                    byteReader.setByteData(data);

                    Log.d(TAG, String.format("This Version is %d.%d.%d - %d",
                            byteReader.getShort(), byteReader.getShort(),
                            byteReader.getShort(), byteReader.getShort()));

                }
                if (data == null) {
                    Log.d(TAG,"Characteristic String is " + characteristic.toString());
                }
            } else if (UUID.fromString(MYO_INFO_ID).equals(characteristic.getUuid())) {
                // Myo Device Information
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    ByteReader byteReader = new ByteReader();
                    byteReader.setByteData(data);

                    callback_msg = String.format("Serial Number     : %02x:%02x:%02x:%02x:%02x:%02x",
                            byteReader.getByte(), byteReader.getByte(), byteReader.getByte(),
                            byteReader.getByte(), byteReader.getByte(), byteReader.getByte()) +
                            '\n' + String.format("Unlock            : %d", byteReader.getShort()) +
                            '\n' + String.format("Classifier builtin:%d active:%d (have:%d)",
                            byteReader.getByte(), byteReader.getByte(), byteReader.getByte()) +
                            '\n' + String.format("Stream Type       : %d", byteReader.getByte());
//                    mHandler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            dataView.setText(callback_msg);
//                        }
//                    });

                }
            }
        }
        else{
            Log.d(TAG, "onCharacteristicRead error: " + status);
        }

        if(readCharacteristicQueue.size() > 0)
            mBluetoothGatt.readCharacteristic(readCharacteristicQueue.element());
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "onCharacteristicWrite success");
        } else {
            Log.d(TAG, "onCharacteristicWrite error: " + status);
        }
    }

    long last_send_never_sleep_time_ms = System.currentTimeMillis();
    long pretime=0;
    final static long NEVER_SLEEP_SEND_TIME = 10000;  // Milli Second
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (EMG_0_ID.equals(characteristic.getUuid().toString())) {
            long systemTime_ms = System.currentTimeMillis();
            /*byte[] */emg_data = characteristic.getValue();
            /////GestureDetectModelManager.getCurrentModel().event(systemTime_ms,emg_data);//GestureSaveModel use it!!
            Log.d("timeee","pre: "+pretime+" now: "+systemTime_ms);
            pretime=systemTime_ms;


            EmgData streamData = new EmgData(new EmgCharacteristicData(emg_data), timeManager);//get current emgDataRows and then 16->8 in other class

            list_emg.add(streamData);

            for (int i = 0; i < 8; i++) {
                EMG_data=EMG_data+streamData.getElement(i)+"    ";
            }
            EMG_data=EMG_data+"\n";
            Log.d("myEMGmsg",EMG_data);


            ///////////////////////////////////////////////////////////////////////////////////////////
            ByteReader emg_br = new ByteReader();
            emg_br.setByteData(emg_data);//for nopModel use SaveModel has the same one

            final String callback_msg = String.format("emg %5d,%5d,%5d,%5d,%5d,%5d,%5d,%5d\n" +
                            "    %5d,%5d,%5d,%5d,%5d,%5d,%5d,%5d",
                    emg_br.getByte(),emg_br.getByte(),emg_br.getByte(),emg_br.getByte(),
                    emg_br.getByte(),emg_br.getByte(),emg_br.getByte(),emg_br.getByte(),
                    emg_br.getByte(),emg_br.getByte(),emg_br.getByte(),emg_br.getByte(),
                    emg_br.getByte(),emg_br.getByte(),emg_br.getByte(),emg_br.getByte());
            Log.d("MYOEMG",callback_msg);
            emg_br = new ByteReader();
            emg_br.setByteData(emg_data);
            for(int emgInputIndex = 0;emgInputIndex<16;emgInputIndex++) {
                emgDatas[emgInputIndex] = emg_br.getByte();
                Log.d("MYOEMG_br",emgDatas[emgInputIndex]+"");
            }

            if (systemTime_ms > last_send_never_sleep_time_ms + NEVER_SLEEP_SEND_TIME) {
                // set Myo [Never Sleep Mode]
                setMyoControlCommand(commandList.sendUnSleep());
                last_send_never_sleep_time_ms = systemTime_ms;
            }
        }


        /////////////////////////////IMU///////////////////////////////
        if (IMU_DATA_ID.equals(characteristic.getUuid().toString())){
            byte[] imu_data = characteristic.getValue();
            //Log.d("Mode","IMU : "+imu_data);
            ImuData streamData = new ImuData(new ImuCharacteristicData(imu_data), timeManager);

            list_imu.add(streamData);
            //list_imu.get(id).getImuArray().get(0~9);
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ByteReader imu_br = new ByteReader();
            imu_br.setByteData(imu_data);//for nopModel use SaveModel has the same one

            /*final String callback_imu = String.format("orientation %5f,%5f,%5f,%5f\n" +
                            "accelerometer %5f,%5f,%5f\n" +
                            "gyroscope %5f,%5f,%5f",
                    imu_br.getShort()/MYOHW_ORIENTATION_SCALE,imu_br.getShort()/MYOHW_ORIENTATION_SCALE,imu_br.getShort()/MYOHW_ORIENTATION_SCALE,imu_br.getShort()/MYOHW_ORIENTATION_SCALE,
                    (imu_br.getShort()/MYOHW_ACCELEROMETER_SCALE)*G,(imu_br.getShort()/MYOHW_ACCELEROMETER_SCALE)*G,(imu_br.getShort()/MYOHW_ACCELEROMETER_SCALE)*G,
                    imu_br.getShort()/MYOHW_GYROSCOPE_SCALE,imu_br.getShort()/MYOHW_GYROSCOPE_SCALE,imu_br.getShort()/MYOHW_GYROSCOPE_SCALE);
            Log.d("MYOIMU",callback_imu);*/
            float w=imu_br.getShort()/MYOHW_ORIENTATION_SCALE, x=imu_br.getShort()/MYOHW_ORIENTATION_SCALE, y=imu_br.getShort()/MYOHW_ORIENTATION_SCALE, z=imu_br.getShort()/MYOHW_ORIENTATION_SCALE;
            float accX=(imu_br.getShort()/MYOHW_ACCELEROMETER_SCALE)*G, accY=(imu_br.getShort()/MYOHW_ACCELEROMETER_SCALE)*G, accZ=(imu_br.getShort()/MYOHW_ACCELEROMETER_SCALE)*G;
            float gryoX=imu_br.getShort()/MYOHW_GYROSCOPE_SCALE, gryoY=imu_br.getShort()/MYOHW_GYROSCOPE_SCALE, gryoZ=imu_br.getShort()/MYOHW_GYROSCOPE_SCALE;

            String q="w:"+w+"x:"+x+" y:"+y+" z:"+z;
            String acc="x:"+accX+" y:"+accY+" z:"+accZ;
            String gyo="x:"+gryoX+" y:"+gryoY+" z:"+gryoZ;

            float roll = (float) Math.toDegrees(Math.atan2(2.0D * (w * x + y * z), 1.0D - 2.0D * (x * x + y * y)));
            float pitch = (float) Math.toDegrees(Math.asin(2.0D * (w * y - z * x)));
            float yaw = (float) Math.toDegrees(Math.atan2(2.0D * (w * z + x * y), 1.0D - 2.0D * (y * y + z * z)));

            dataView.setRotation(roll);
            dataView.setRotationX(pitch);
            dataView.setRotationY(yaw);

            Log.d("mQ",q);
            Log.d("mAccelerometer",acc);
            Log.d("mGyroscopeData",gyo);


        }
    }

    public void setBluetoothGatt(BluetoothGatt gatt) {
        mBluetoothGatt = gatt;
    }

    public boolean setMyoControlCommand(byte[] command) {
        if ( mCharacteristic_command != null) {
            mCharacteristic_command.setValue(command);
            int i_prop = mCharacteristic_command.getProperties();
            if (i_prop == BluetoothGattCharacteristic.PROPERTY_WRITE) {
                if (mBluetoothGatt.writeCharacteristic(mCharacteristic_command)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void stopCallback() {
        // Before the closing GATT, set Myo [Normal Sleep Mode].
        setMyoControlCommand(commandList.sendNormalSleep());
        descriptorWriteQueue = new LinkedList<BluetoothGattDescriptor>();
        readCharacteristicQueue = new LinkedList<BluetoothGattCharacteristic>();
        if (mCharacteristic_command != null) {
            mCharacteristic_command = null;
        }
        if (mCharacteristic_emg0 != null) {
            mCharacteristic_emg0 = null;
        }
        if (mCharacteristic_imu != null) {
            mCharacteristic_imu = null;
        }
        if (mBluetoothGatt != null) {
            mBluetoothGatt = null;
        }
    }
}
