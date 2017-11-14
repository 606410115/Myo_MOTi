package com.example.myoxmoti_test;

import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.LinkedList;

/**
 * Created by Mslab on 2017/7/9.
 */

public class MOTiLogActivity {

    private final static int MOTi_WINDOW_LENGTH = 2;

    private int motiStreamCount = 0;

    public String acc_x="",acc_y="",acc_z="",gyro_x="",gyro_y="",gyro_z=""/*,msgtime=""*/;
    private double[] mAccData=new double[3];
    private double[] mAccPreData=new double[3];
    private double[] mGyroData=new double[3];
    private double[] mGyroPreData=new double[3];
    private int mCount=0;

    private LinkedList<MOTiData> list_moti = new LinkedList<>();
    private LinkedList<MOTiData> list_motiWindow = new LinkedList<>();

    private TimeManager timeManager;

    MOTiLogActivity(TimeManager tM){
        timeManager = tM;
    }


    public void addLogData(byte[] byteArrayExtra, String name){
        if(byteArrayExtra.length == 18) {
            MOTiData streamData = new MOTiData(new MOTiCharacteristicData(byteArrayExtra), timeManager);

            if(motiStreamCount >= MOTi_WINDOW_LENGTH){
                list_motiWindow.removeFirst();
            }
            else{
                motiStreamCount++;
            }

            if(motiStreamCount > 1){
                streamData = motiLowPassFiliter(list_motiWindow.getLast(), streamData, 0.2f);
            }

            list_motiWindow.add(streamData);

            if(MainActivity.addFlag){//收到開始收集的flag
                list_moti.add(streamData);

                //Log.d("MOTi", "list_moti accX: " + list_moti.getLast().getElement(0) + " accY: " + list_moti.getLast().getElement(1) + " accZ: " + list_moti.getLast().getElement(2));
            }

            if(MainActivity.endFlag){//收到停止收集的flag
                if(!MainActivity.motiPreventEndAgain){//防止連續進去
                    MainActivity.motiPreventEndAgain = true;
                    Thread tMoti = new Thread(rMoti);
                    tMoti.start();
                }

            }

            if(MainActivity.cleanListFlag){//收到清空list(clean階段)
                if(!MainActivity.motiHaveCleaned){//當cleanFlag出現時，在大家還沒clean的時候，防止再度clean
                    MainActivity.motiHaveCleaned = true;//已清除過
                    list_moti.clear();
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

    private MOTiData motiLowPassFiliter( MOTiData input, MOTiData output ,double ALPHA) {
        for ( int i = 0; i < 6; i++ ){
            output.setElement(i, output.getElement(i) + ALPHA * (input.getElement(i) - output.getElement(i)));
        }

        return output;
    }

    private Runnable rMoti = new Runnable() {
        @Override
        public void run() {
            Log.d("MOTi", "moti thread");
            LinkedList<MOTiData> moti_motion = new LinkedList<>();
            LinkedList<Double> feature = new LinkedList<>();

            double[] acc_mean = new double[3];


            //moti_motion = list_moti;

            for (MOTiData aList_moti : list_moti){
                moti_motion.add(aList_moti);
            }

            //normalize imu
            for (MOTiData aList_moti : moti_motion) {
                for (int i_moti_num = 0; i_moti_num < 6; i_moti_num++) {
                    if (i_moti_num < 3) {//accelerometer
                        aList_moti.setElement(i_moti_num, (aList_moti.getElement(i_moti_num) + 78.4) / 156.8);
                    }
                    else if (i_moti_num >= 3) {//gyroscope
                        aList_moti.setElement(i_moti_num, (aList_moti.getElement(i_moti_num) + 500) / 1000);
                    }
                }
            }

            //acc 每軸平均值(3 features) => feature[0~2]
            for(int i_axis = 0; i_axis < 3; i_axis++){//MOTi的ACC
                double sum = 0.00, mean;

                for(int i_element = 0; i_element < moti_motion.size(); i_element++){
                    sum = sum + moti_motion.get(i_element).getElement(i_axis);
                }

                mean = sum / moti_motion.size();
                Log.d("MOTi", "mean : " + mean);

//                double normalize_mean = (mean + 78.4) / 156.8;
//                Log.d("MOTi_normalize", "normalize_mean: " + normalize_mean);
//                feature.add(normalize_mean);
                acc_mean[i_axis] = mean;
            }
            //acc 每軸標準差(3 features) => feature[3~5]
            for(int i_axis = 0; i_axis < 3; i_axis++){
                double SD_sum = 0.00, SD;

                for(int i_element = 0; i_element < moti_motion.size(); i_element++){
                    SD_sum = SD_sum + Math.pow( moti_motion.get(i_element).getElement(i_axis) - acc_mean[i_axis] , 2);
                }

                SD = Math.sqrt(SD_sum / moti_motion.size());
                Log.d("MOTi", "SD : " + SD);

//                double normalize_SD = SD / 6146.56;
//                Log.d("MOTi_normalize", "normalize_SD: " + normalize_SD);
                feature.add(SD);
            }

            Classify.getCurrentClassify().motiList(feature);
            Classify.getCurrentClassify().WekaKNN();
        }
    };

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
