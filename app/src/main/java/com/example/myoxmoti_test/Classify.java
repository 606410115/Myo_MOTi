package com.example.myoxmoti_test;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import weka.classifiers.Classifier;
import weka.classifiers.lazy.IBk;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Created by Mslab on 2017/10/10.
 */

public class Classify {
    static LinkedList<Double> emg_feature = new LinkedList<>();
    static LinkedList<Double> imu_feature = new LinkedList<>();
    static LinkedList<Double> moti_feature = new LinkedList<>();

    static boolean Myo_EMG = false, Myo_IMU = false, MOTi = false;

    public static void emgList(LinkedList<Double> emgF){
        emg_feature = emgF;
        Myo_EMG = true;
    }

    public static void imuList(LinkedList<Double> imuF){
        imu_feature = imuF;
        Myo_IMU = true;
    }

    public static void motiList(LinkedList<Double> motiF){
        moti_feature = motiF;
        MOTi = true;
    }

    public static void WekaKNN(){
        if(Myo_EMG && Myo_IMU && MOTi){//three devices have data
            LinkedList<Double> all_feature = new LinkedList<>();

            for(int i = 0; i < moti_feature.size(); i++){
                all_feature.add(moti_feature.get(i));
            }
            for(int i = 0; i < imu_feature.size(); i++){
                all_feature.add(imu_feature.get(i));
            }
            for(int i = 0; i < emg_feature.size(); i++){
                all_feature.add(emg_feature.get(i));
            }

            /*String testData = "@relation ads\n" +
                    "\n" +
                    "@attribute moti_x_acc_mean numeric\n" +
                    "@attribute moti_y_acc_mean numeric\n" +
                    "@attribute moti_z_acc_mean numeric\n" +
                    "@attribute moti_x_acc_SD numeric\n" +
                    "@attribute moti_y_acc_SD numeric\n" +
                    "@attribute moti_z_acc_SD numeric\n" +
                    "@attribute imu_x_acc_mean numeric\n" +
                    "@attribute imu_y_acc_mean numeric\n" +
                    "@attribute imu_z_acc_mean numeric\n" +
                    "@attribute imu_x_acc_SD numeric\n" +
                    "@attribute imu_y_acc_SD numeric\n" +
                    "@attribute imu_z_acc_SD numeric\n" +
                    "@attribute emg_0_mean numeric\n" +
                    "@attribute emg_1_mean numeric\n" +
                    "@attribute emg_2_mean numeric\n" +
                    "@attribute emg_3_mean numeric\n" +
                    "@attribute emg_4_mean numeric\n" +
                    "@attribute emg_5_mean numeric\n" +
                    "@attribute emg_6_mean numeric\n" +
                    "@attribute emg_7_mean numeric\n" +
                    "@attribute profit {Y, N}\n" +
                    "\n" +
                    "@data\n" +
                    all_feature+","+all_feature+","+all_feature+","+all_feature+","+all_feature+","+all_feature+","+all_feature+","+all_feature+","+all_feature+","+all_feature+","+
            all_feature+","+all_feature+","+all_feature+","+all_feature+","+all_feature+","+all_feature+","+all_feature+","+all_feature+","+all_feature+","+all_feature+",?";*/

            String testData = "@relation ads\n" +
                    "\n" +
                    "@attribute moti_x_acc_mean numeric\n" +
                    "@attribute moti_y_acc_mean numeric\n" +
                    "@attribute moti_z_acc_mean numeric\n" +
                    "@attribute moti_x_acc_SD numeric\n" +
                    "@attribute moti_y_acc_SD numeric\n" +
                    "@attribute moti_z_acc_SD numeric\n" +
                    "@attribute imu_x_acc_mean numeric\n" +
                    "@attribute imu_y_acc_mean numeric\n" +
                    "@attribute imu_z_acc_mean numeric\n" +
                    "@attribute imu_x_acc_SD numeric\n" +
                    "@attribute imu_y_acc_SD numeric\n" +
                    "@attribute imu_z_acc_SD numeric\n" +
                    "@attribute emg_0_mean numeric\n" +
                    "@attribute emg_1_mean numeric\n" +
                    "@attribute emg_2_mean numeric\n" +
                    "@attribute emg_3_mean numeric\n" +
                    "@attribute emg_4_mean numeric\n" +
                    "@attribute emg_5_mean numeric\n" +
                    "@attribute emg_6_mean numeric\n" +
                    "@attribute emg_7_mean numeric\n" +
                    "@attribute profit {Y, N}\n" +
                    "\n" +
                    "@data\n";
            for (int i_feature = 0; i_feature < all_feature.size(); i_feature++){
                testData = testData + all_feature.get(i_feature) +",";
            }
            testData = testData + "?";

            try {

                File mSDFile = null;

                //檢查有沒有SD卡裝置
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_REMOVED)) {
                    //Toast.makeText(MainActivity.this, "沒有SD卡!!!", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    //取得SD卡儲存路徑
                    //mSDFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                    mSDFile = Environment.getExternalStorageDirectory();
                }

                //建立文件檔儲存路徑
                File mFile = new File(mSDFile.getParent() + "/" + mSDFile.getName() + "/MYOxMOTi/TestData");
                //File mFile = new File(mSDFile.getParent() + "/" + mSDFile.getName() + "/MyAndroid");

                //若沒有檔案儲存路徑時則建立此檔案路徑
                if (!mFile.exists()) {
                    if(!mFile.mkdirs()){
                        throw new Error("mkdirs error");
                    }
                }

                FileWriter test = new FileWriter(mSDFile.getParent() + "/" + mSDFile.getName() + "/MYOxMOTi/TestData/TestData.txt");
                test.write(testData);
                test.close();

                //Log.d("saveSuccess","已儲存文字");
            } catch (Exception e) {
                Log.e("save data error", e.getLocalizedMessage());
            }

            //start classify
            Thread classify = new Thread(rClassify);
            classify.start();
        }
    }

    public static BufferedReader readDataFile(String filename) {
        BufferedReader inputReader = null;

        try {
            inputReader = new BufferedReader(new FileReader(filename));
        } catch (FileNotFoundException ex) {
            System.err.println("File not found: " + filename);
        }

        return inputReader;
    }

    private static Runnable rClassify = new Runnable() {
        @Override
        public void run() {

            try {
                File mSDFile = null;

                //檢查有沒有SD卡裝置
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_REMOVED)) {
                    //Toast.makeText(MainActivity.this, "沒有SD卡!!!", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    //取得SD卡儲存路徑
                    //mSDFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                    mSDFile = Environment.getExternalStorageDirectory();
                }

                //建立文件檔儲存路徑
                File mTrainingFile = new File(mSDFile.getParent() + "/" + mSDFile.getName() + "/MYOxMOTi/TrainingData/TrainingData.txt");

                BufferedReader trainingData = readDataFile(mTrainingFile.toString());

                Instances training = new Instances(trainingData);
                training.setClassIndex(training.numAttributes() - 1);


                File mTestFile = new File(mSDFile.getParent() + "/" + mSDFile.getName() + "/MYOxMOTi/TestData/TestData.txt");

                BufferedReader testData = readDataFile(mTestFile.toString());
                Instances test = new Instances(testData);
                test.setClassIndex(test.numAttributes() - 1);

                Classifier ibk = new IBk();
                ibk.buildClassifier(training);

                for (int i = 0; i < test.numInstances(); i++) {
                    double clsLabel = ibk.classifyInstance(test.instance(i));
                    test.instance(i).setClassValue(clsLabel);
                }
                Instance mTest = test.instance(0);
                String[] mGesture = mTest.toString().split(",");

                String result = mGesture[test.numAttributes()-1];

            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    };


}