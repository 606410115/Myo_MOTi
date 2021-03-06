package com.example.myoxmoti_test;

import android.app.Activity;
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
import java.util.ListIterator;
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

    private final static int EMG_WINDOW_LENGTH = 5;
    private final static int EMG_MIN_LENGTH = 30;
    private final static int EMG_START_THRESHOLD = 8;
    private final static int EMG_END_THRESHOLD = 6;

    private final static int IMU_WINDOW_LENGTH = 2;

    private Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<BluetoothGattDescriptor>();
    private Queue<BluetoothGattCharacteristic> readCharacteristicQueue = new LinkedList<BluetoothGattCharacteristic>();

    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mCharacteristic_command;
    private BluetoothGattCharacteristic mCharacteristic_emg0;
    private BluetoothGattCharacteristic mCharacteristic_imu;

    private MyoCommandList commandList = new MyoCommandList();

    private String TAG = "MyoGatt";

    //count motion textView
    private TextView countText1;
    private TextView countText2;
    private TextView countText3;
    private TextView countText4;
    private TextView countText5;
    private TextView countText6;

    //private TextView dataView;
    private TextView myoStatusView;
    private String callback_msg;
    //private Handler mHandler;
    private int[] emgDatas = new int[16];

    //private Button mybtn;
    byte[] emg_data;
    public String EMG_data="";

    private int emgStreamCount = 0;
    private EmgData emgStreamingMaxData;// emg取stream取0.2秒內最大

    private int imuStreamCount = 0;

//    private static final float MYOHW_ORIENTATION_SCALE = 16384.0f;
//    private static final float MYOHW_ACCELEROMETER_SCALE = 2048.0f;
//    private static final float MYOHW_GYROSCOPE_SCALE = 16.0f;
//    private static final float G=9.8f;

    private LinkedList<EmgData> list_emg = new LinkedList<>();
    private LinkedList<ImuData> list_imu = new LinkedList<>();

    private LinkedList<EmgData> list_emgWindow = new LinkedList<>();
    private LinkedList<ImuData> list_imuWindow = new LinkedList<>();

    private TimeManager timeManager;
    private Activity activity;

    public MyoGattCallback(HashMap<String,View> views, TimeManager tM, Activity mainActivity){
        //dataView = (TextView) views.get("result");
        myoStatusView = (TextView) views.get("myoStatus");

        countText1= (TextView) views.get("motion1");
        countText2= (TextView) views.get("motion2");
        countText3= (TextView) views.get("motion3");
        countText4= (TextView) views.get("motion4");
        countText5= (TextView) views.get("motion5");
        countText6= (TextView) views.get("motion6");

        timeManager = tM;
        activity = mainActivity;

        Classify.getCurrentClassify().setActivity(mainActivity);
        Classify.getCurrentClassify().setTextView(views);
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
        boolean checkEmgCommand = false, checkImuCommand = false;

        Log.d(TAG, "onServicesDiscovered received: " + status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            // Find GATT Service
            BluetoothGattService service_emg = gatt.getService(UUID.fromString(MYO_EMG_DATA_ID));
            if (service_emg == null) {
                Log.d(TAG,"No Myo EMG-Data Service !!");
            } else {
                Log.d(TAG, "Find Myo EMG-Data Service !!");
                checkEmgCommand = true;
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
                checkImuCommand = true;
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

            if(checkEmgCommand && checkImuCommand){
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        myoStatusView.setText("Find");
                        //init count
                        countText1.setText("0");
                        countText2.setText("0");
                        countText3.setText("0");
                        countText4.setText("0");
                        countText5.setText("0");
                        countText6.setText("0");
                    }
                });
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
    //long pretime=0;
    final static long NEVER_SLEEP_SEND_TIME = 10000;  // Milli Second
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (EMG_0_ID.equals(characteristic.getUuid().toString())) {
            long systemTime_ms = System.currentTimeMillis();
            /*byte[] */emg_data = characteristic.getValue();
            /////GestureDetectModelManager.getCurrentModel().event(systemTime_ms,emg_data);//GestureSaveModel use it!!
            /*Log.d("timeee","pre: "+pretime+" now: "+systemTime_ms);
            pretime=systemTime_ms;*/


            EmgData streamData = new EmgData(new EmgCharacteristicData(emg_data), timeManager);//get current emgDataRows and then 16->8 in other class

            if(emgStreamCount >= EMG_WINDOW_LENGTH){
                list_emgWindow.removeFirst();
            }
            else{
                emgStreamCount++;
            }

            if(emgStreamCount > 1){
                streamData = emgLowPassFiliter(list_emgWindow.getLast(), streamData, 0.2f);
            }

            list_emgWindow.add(streamData);

            if (emgStreamCount == EMG_WINDOW_LENGTH){//5
                emgStreamingMaxData = list_emgWindow.getFirst();

                for(int i_window = 0; i_window < list_emgWindow.size(); i_window++){//window內的全部data
                    for (int i_element = 0; i_element < 8; i_element++) {
                        if (list_emgWindow.get(i_window).getElement(i_element) > emgStreamingMaxData.getElement(i_element)) {
                            emgStreamingMaxData.setElement(i_element, streamData.getElement(i_element));
                        }
                    }
                }

                double sum = 0.00, mean;

                for (int i = 0; i < 8; i++) {
                    sum = sum + emgStreamingMaxData.getElement(i);
                }
                mean = sum / 8;

                if(mean > EMG_START_THRESHOLD){//有意義的動作
                    //Log.d("MyoEMG", "cross start threshold" + " mean: " + mean + " Flag: " + MainActivity.addFlag);
                    if(!MainActivity.addFlag){//避免正在用力中還在多存
                        //Log.d("MyoEMG", "start save data");
                        MainActivity.addFlag = true;

                        for(int i = 0; i < EMG_WINDOW_LENGTH-1; i++){//把window裡的data存起來 最後一個留給下面add
                            list_emg.add(list_emgWindow.get(i));
                        }
                    }

                }
                else if (mean < EMG_END_THRESHOLD && MainActivity.addFlag){//正在存且低於結束門檻
                    Log.d("MyoEMG", "lower than end threshold");
                    if(list_emg.size() < EMG_MIN_LENGTH){//太短
                        Log.d("MyoEMG", "data too short");
                        MainActivity.cleanListFlag = true;
                    }
                    else{//蒐集完成
                        MainActivity.endFlag = true;
                    }

                    MainActivity.addFlag = false;

                }

                if(MainActivity.addFlag){
                    list_emg.add(streamData);
                }

                if(MainActivity.endFlag){
                    if(!MainActivity.myoEmgPreventEndAgain){//防止連續進去
                        MainActivity.myoEmgPreventEndAgain = true;
                        Thread tEmg = new Thread(rEmg);
                        tEmg.start();
                    }
                }

                if(MainActivity.cleanListFlag){
                    if(!MainActivity.myoEmgHaveCleaned){//當cleanFlag出現時，在大家還沒clean的時候，防止再度clean
                        MainActivity.myoEmgHaveCleaned = true;//已清除過
                        list_emg.clear();
                    }

                    if(MainActivity.myoEmgHaveCleaned && MainActivity.myoImuHaveCleaned && MainActivity.motiHaveCleaned){//當大家在清空狀態時，都已清空，將Flag轉為不需要清空
                        MainActivity.cleanListFlag = false;

                        MainActivity.myoEmgHaveCleaned= false;
                        MainActivity.myoImuHaveCleaned = false;
                        MainActivity.motiHaveCleaned = false;
                    }
                }
            }
        }


        /********************************************************************************************************************/
        if (IMU_DATA_ID.equals(characteristic.getUuid().toString())){
            byte[] imu_data = characteristic.getValue();
            //Log.d("Mode","IMU : "+imu_data);
            ImuData streamData = new ImuData(new ImuCharacteristicData(imu_data), timeManager);

            if(imuStreamCount >= IMU_WINDOW_LENGTH){
                list_imuWindow.removeFirst();
            }
            else{
                imuStreamCount++;
            }

            if(imuStreamCount > 1){
                streamData = imuLowPassFiliter(list_imuWindow.getLast(), streamData, 0.2f);
            }

            list_imuWindow.add(streamData);

            if(MainActivity.addFlag){//收到開始收集的flag
                list_imu.add(streamData);
            }

            if(MainActivity.endFlag){//收到停止收集的flag
                if(!MainActivity.myoImuPreventEndAgain){//防止連續進去
                    MainActivity.myoImuPreventEndAgain = true;//已收集完
                    Thread tImu = new Thread(rImu);
                    tImu.start();
                }

            }

            if(MainActivity.cleanListFlag){//收到清空list
                if(!MainActivity.myoImuHaveCleaned){//當cleanFlag出現時，在大家還沒clean的時候，防止再度clean
                    MainActivity.myoImuHaveCleaned = true;//已清除過
                    list_imu.clear();
                }

                if(MainActivity.myoEmgHaveCleaned && MainActivity.myoImuHaveCleaned && MainActivity.motiHaveCleaned){//當大家在清空狀態時，都已清空，將Flag轉為不需要清空
                    MainActivity.cleanListFlag = false;

                    MainActivity.myoEmgHaveCleaned= false;
                    MainActivity.myoImuHaveCleaned = false;
                    MainActivity.motiHaveCleaned = false;
                }
            }

            /*if(imuStreamCount >= IMU_WINDOW_LENGTH){
                list_imuWindow.removeFirst();
            }
            else{
                imuStreamCount++;
            }

            list_imuWindow.add(streamData);

            if (imuStreamCount == 1){//window
                imuStreamingMaxData = streamData;
            } else {
                if (imuStreamCount == IMU_WINDOW_LENGTH){

                    //要做什麼事

                    if(mean > IMU_START_THRESHOLD){
                        MainActivity.addFlag = true;

                        for(int i = 0; i < IMU_WINDOW_LENGTH; i++){//把window裡的data存起來
                            list_imu.add(list_imuWindow.get(i));
                        }
                    }
                    else if (mean < IMU_END_THRESHOLD && MainActivity.addFlag){//正在存且低於結束門檻

                        if(list_imu.size() < IMU_MIN_LENGTH){//太短
                            list_imu.clear();
                        }
                        else{//蒐集完成
                            Thread tEmg = new Thread(rEmg);
                            tEmg.start();
                        }

                        MainActivity.addFlag = false;
                    }

                    if(MainActivity.addFlag){
                        list_imu.add(streamData);
                    }

                    //streamCount = 0;
                }
            }*/
        }
    }

    private EmgData emgLowPassFiliter( EmgData input, EmgData output ,double ALPHA) {
        for ( int i = 0; i < 8; i++ ){
            output.setElement(i, output.getElement(i) + ALPHA * (input.getElement(i) - output.getElement(i)));
        }

        return output;
    }

    private ImuData imuLowPassFiliter( ImuData input, ImuData output ,double ALPHA) {
        for ( int i = 0; i < 10; i++ ){
            output.setElement(i, output.getElement(i) + ALPHA * (input.getElement(i) - output.getElement(i)));
        }

        return output;
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

    private Runnable rEmg = new Runnable() {
        @Override
        public void run() {
            Log.d("MyoEMG", "emg thread");
            LinkedList<EmgData> emg_motion = new LinkedList<>();
            LinkedList<Double> feature = new LinkedList<>();

            //emg_motion = list_emg;

            synchronized(this) {
                for (EmgData aList_emg : list_emg) {
                    emg_motion.add(aList_emg);
                }
            }

            //normalize emg
            for (EmgData aList_emg : emg_motion) {
                for (int i_emg8 = 0; i_emg8 < 8; i_emg8++) {
                    aList_emg.setElement(i_emg8, aList_emg.getElement(i_emg8) / 256);
                }
            }

            //emg 每個sensor的平均值特徵(8 features)
            for(int j_sensor = 0; j_sensor < 8; j_sensor++){//MYO EMG的哪個sensor
                double sum =0.00, mean;

                for(int i_element = 0; i_element < emg_motion.size(); i_element++){//蒐集的數量
                    sum = sum + emg_motion.get(i_element).getElement(j_sensor);
                }

                mean = sum / emg_motion.size();
                Log.d("Myo", "emg_mean : " + mean);
//                double normalize_mean = mean / 256;
//                Log.d("MYO_normalize", "EMG normalize_mean: " + normalize_mean);
//                feature.add(normalize_mean);

                feature.add(mean);
            }
            Classify.getCurrentClassify().emgList(feature);
            Classify.getCurrentClassify().WekaKNN();
        }
    };

    private Runnable rImu = new Runnable() {
        @Override
        public void run() {
            Log.d("MyoIMU", "imu thread");
            LinkedList<ImuData> imu_motion = new LinkedList<>();
            LinkedList<Double> feature = new LinkedList<>();

            double[] acc_mean = new double[3];


            //imu_motion = list_imu;

            synchronized(this) {
                for (ImuData aList_imu : list_imu) {
                    imu_motion.add(aList_imu);
                }
            }

            //normalize imu
            for (ImuData aList_imu : imu_motion) {
                for (int i_imu_num = 0; i_imu_num < 10; i_imu_num++) {
                    if (i_imu_num < 4) {//quaternion
                        aList_imu.setElement(i_imu_num, aList_imu.getElement(i_imu_num) / 65536);
                    }
                    else if (i_imu_num >= 4 && i_imu_num < 7) {//accelerometer
                        aList_imu.setElement(i_imu_num, (aList_imu.getElement(i_imu_num) + 156.8) / 313.6);
                    }
                    else if (i_imu_num >= 7) {//gyroscope
                        aList_imu.setElement(i_imu_num, (aList_imu.getElement(i_imu_num) + 2000) / 4000);
                    }
                }
            }

            //acc 每軸平均值(3 features) => feature[0~2]
            for(int i_axis = 4; i_axis < 7; i_axis++){//IMU的ACC
                double sum = 0.00, mean;

                for(int i_element = 0; i_element < imu_motion.size(); i_element++){
                    sum = sum + imu_motion.get(i_element).getElement(i_axis);
                }

                mean = sum / imu_motion.size();
                Log.d("Myo", "Imu_mean : " + mean);
//                double normalize_mean = (mean + 156.8) / 313.6;
//                Log.d("MYO_normalize", "IMU normalize_mean: " + normalize_mean);
//                feature.add(normalize_mean);

                feature.add(mean);

                switch (i_axis){
                    case 4:
                        acc_mean[0] = mean;//x mean
                        break;
                    case 5:
                        acc_mean[1] = mean;//y mean
                        break;
                    case 6:
                        acc_mean[2] = mean;//z mean
                        break;
                }
            }
            //acc 每軸標準差(3 features) => feature[3~5]
            for(int i_axis = 4; i_axis < 7; i_axis++){
                double SD_sum = 0.00, SD;

                for(int i_element = 0; i_element < imu_motion.size(); i_element++){
                    SD_sum = SD_sum + Math.pow( imu_motion.get(i_element).getElement(i_axis) - acc_mean[i_axis - 4] , 2);
                }

                SD = Math.sqrt(SD_sum / imu_motion.size());
                Log.d("Myo", "Imu_SD : " + SD);
//                double normalize_SD = SD / 24586.24;
//                Log.d("MYO_normalize", "IMU normalize_SD: " + normalize_SD);
//                feature.add(normalize_SD);

                feature.add(SD);
            }

            Classify.getCurrentClassify().imuList(feature);
            Classify.getCurrentClassify().WekaKNN();
        }
    };

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
