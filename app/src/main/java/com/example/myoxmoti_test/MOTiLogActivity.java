package com.example.myoxmoti_test;

import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.nio.ByteBuffer;
import java.util.Date;

/**
 * Created by Mslab on 2017/7/9.
 */

public class MOTiLogActivity {

    public String acc_x="",acc_y="",acc_z="",gyro_x="",gyro_y="",gyro_z=""/*,msgtime=""*/;
    private double[] mAccData=new double[3];
    private double[] mAccPreData=new double[3];
    private double[] mGyroData=new double[3];
    private double[] mGyroPreData=new double[3];
    private int mCount=0;

    private TimeManager timeManager;

    MOTiLogActivity(TimeManager tM){
        timeManager = tM;
    }


    public void addLogData(byte[] byteArrayExtra, String name){
        if(byteArrayExtra.length == 18) {
//            String data = name;
//            long time = ByteBuffer.wrap(byteArrayExtra, 2, 4).getInt();
//            time *= 1000;
//            data += sdf.format(new Date(time)) + "\n";
//
//            data += "Acc\n";
//            data += "x: " + ByteBuffer.wrap(byteArrayExtra, 6, 2).getShort() + "\n";
//            data += "y: " + ByteBuffer.wrap(byteArrayExtra, 8, 2).getShort() + "\n";
//            data += "z: " + ByteBuffer.wrap(byteArrayExtra, 10, 2).getShort() + "\n";
//
//            data += "Gyro\n";
//            data += "x: " + ByteBuffer.wrap(byteArrayExtra, 12, 2).getShort() + "\n";
//            data += "y: " + ByteBuffer.wrap(byteArrayExtra, 14, 2).getShort() + "\n";
//            data += "z: " + ByteBuffer.wrap(byteArrayExtra, 16, 2).getShort() + "\n";
//
//            TextView tv = new TextView(this);
//            tv.setText(data);
//            llContent.addView(tv, 0);

            MOTiData streamData = new MOTiData(new MOTiCharacteristicData(byteArrayExtra), timeManager);

            ////////////////////////////////////////////////////////////////////////////////////////
            mAccData[0]=RawDataToAccelerometer(ByteBuffer.wrap(byteArrayExtra, 6, 2).getShort());
            mAccData[1]=RawDataToAccelerometer(ByteBuffer.wrap(byteArrayExtra, 8, 2).getShort());
            mAccData[2]=RawDataToAccelerometer(ByteBuffer.wrap(byteArrayExtra, 10, 2).getShort());

            mGyroData[0]=RawDataToGyroscope(ByteBuffer.wrap(byteArrayExtra, 12, 2).getShort());
            mGyroData[1]=RawDataToGyroscope(ByteBuffer.wrap(byteArrayExtra, 14, 2).getShort());
            mGyroData[2]=RawDataToGyroscope(ByteBuffer.wrap(byteArrayExtra, 16, 2).getShort());

            if(mCount>0){
                Acc_lowPass(mAccPreData,mAccData,0.25);
                Gyro_lowPass(mGyroPreData,mGyroData,0.25);
            }
            else{//第一筆資料
                acc_x=acc_x+0+"\n";
                acc_y=acc_y+0+"\n";
                acc_z=acc_z+0+"\n";

                gyro_x=gyro_x+0+"\n";
                gyro_y=gyro_y+0+"\n";
                gyro_z=gyro_z+0+"\n";
            }
            mAccPreData[0]=RawDataToAccelerometer(ByteBuffer.wrap(byteArrayExtra, 6, 2).getShort());
            mAccPreData[1]=RawDataToAccelerometer(ByteBuffer.wrap(byteArrayExtra, 8, 2).getShort());
            mAccPreData[2]=RawDataToAccelerometer(ByteBuffer.wrap(byteArrayExtra, 10, 2).getShort());

            mGyroPreData[0]=RawDataToGyroscope(ByteBuffer.wrap(byteArrayExtra, 12, 2).getShort());
            mGyroPreData[1]=RawDataToGyroscope(ByteBuffer.wrap(byteArrayExtra, 14, 2).getShort());
            mGyroPreData[2]=RawDataToGyroscope(ByteBuffer.wrap(byteArrayExtra, 16, 2).getShort());

            mCount++;
        }

//        if(!devices.contains(name)){
//            devices.add(name);
//            counts.add(1);
//        }else{
//            int c = counts.get(devices.indexOf(name)) + 1;
//            counts.set(devices.indexOf(name), c);
//        }
    }

    private double RawDataToAccelerometer(short data){
        double mdata;
        mdata=(double)(data*56)/(double)23405;
        Log.d("Transform Acc","Raw Data: "+data+" Accelerometer: "+mdata);
        return mdata;
    }
    private double RawDataToGyroscope(short data){
        double mdata;
        mdata=(double)(data*1000)/(double)65535;
        Log.d("Transform Gyro","Raw Data: "+data+" Gyroscope: "+mdata);
        return mdata;
    }

    private void Acc_lowPass( double input[], double output[] ,double ALPHA) {
        for ( int i=0; i<input.length; i++ ){
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }

        acc_x=acc_x+output[0]+"\n";
        acc_y=acc_y+output[1]+"\n";
        acc_z=acc_z+output[2]+"\n";
        //msgtime=msgtime+time+"\n";
    }
    private void Gyro_lowPass( double input[], double output[] ,double ALPHA) {
        for ( int i=0; i<input.length; i++ ){
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }

        gyro_x=gyro_x+output[0]+"\n";
        gyro_y=gyro_y+output[1]+"\n";
        gyro_z=gyro_z+output[2]+"\n";
        //msgtime=msgtime+time+"\n";
    }

//    void showCount(){
//        String count = "";
//        for (int i = 0; i < devices.size(); i++) {
//            count += devices.get(i).trim() + " : " + counts.get(i) + "\n";
////            Log.d("COUNT",devices.get(i).trim() + " : " + counts.get(i));
//        }
////        Log.d("COUNT","-------------------------------------");
//
//        TextView tv = new TextView(this);
//        tv.setText(count);
//        llContent.addView(tv, 0);
//        clean();
//    }
//
//    void clean(){
//        for (int i = 0; i < counts.size(); i++) {
//            counts.set(i, 0);
//        }
//    }

//    public void saveData(View v){
//        Log.d("savebtn","aa");
//        try {
//
//            File mSDFile = null;
//
//            //檢查有沒有SD卡裝置
//            if (Environment.getExternalStorageState().equals(Environment.MEDIA_REMOVED)) {
//                //Toast.makeText(MainActivity.this, "沒有SD卡!!!", Toast.LENGTH_SHORT).show();
//                return;
//            } else {
//                //取得SD卡儲存路徑
//                //mSDFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
//                mSDFile = Environment.getExternalStorageDirectory();
//            }
//
//            //建立文件檔儲存路徑
//            File mFile = new File(mSDFile.getParent() + "/" + mSDFile.getName() + "/MyMotiWrite");
//            //File mFile = new File(mSDFile.getParent() + "/" + mSDFile.getName() + "/MyAndroid");
//
//            //若沒有檔案儲存路徑時則建立此檔案路徑
//            if (!mFile.exists()) {
//                mFile.mkdirs();
//            }
//            FileWriter Acc_x = new FileWriter(mSDFile.getParent() + "/" + mSDFile.getName() + "/MyMotiWrite/Acc_x.txt");
//            Acc_x.write(acc_x);
//            Acc_x.close();
//            FileWriter Acc_y = new FileWriter(mSDFile.getParent() + "/" + mSDFile.getName() + "/MyMotiWrite/Acc_y.txt");
//            Acc_y.write(acc_y);
//            Acc_y.close();
//            FileWriter Acc_z = new FileWriter(mSDFile.getParent() + "/" + mSDFile.getName() + "/MyMotiWrite/Acc_z.txt");
//            Acc_z.write(acc_z);
//            Acc_z.close();
//
//            FileWriter Gyro_x = new FileWriter(mSDFile.getParent() + "/" + mSDFile.getName() + "/MyMotiWrite/Gyro_x.txt");
//            Gyro_x.write(gyro_x);
//            Gyro_x.close();
//            FileWriter Gyro_y = new FileWriter(mSDFile.getParent() + "/" + mSDFile.getName() + "/MyMotiWrite/Gyro_y.txt");
//            Gyro_y.write(gyro_y);
//            Gyro_y.close();
//            FileWriter Gyro_z = new FileWriter(mSDFile.getParent() + "/" + mSDFile.getName() + "/MyMotiWrite/Gyro_z.txt");
//            Gyro_z.write(gyro_z);
//            Gyro_z.close();
//            /*FileWriter t = new FileWriter(mSDFile.getParent() + "/" + mSDFile.getName() + "/MyMotiWrite/time.txt");
//            t.write(msgtime);
//            t.close();*/
//            Log.d("saveSuccess","已儲存文字");
//        } catch (Exception e) {
//        }
//    }
}
