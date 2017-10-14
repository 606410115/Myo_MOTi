package com.example.myoxmoti_test;

/**
 * Created by Mslab on 2017/10/11.
 */

public class TrainingData {
    private static final String TRAININGDATA = "@relation ads\n" +
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
            "@attribute profit {(1),(2)}\n" +
            "\n" +
            "@data\n" +
            /*"10,2,Y\n" +
            "12,3,Y\n" +
            "9,2,Y\n" +
            "0,10,N\n" +
            "1,9,N\n" +*/
            "-8.391137933,1.477727337,-0.654012675,3.738360247,1.940930218,3.092777971,-0.744812466,6.69358337,5.504846736,1.540377163,3.221831624,1.652696418,20.16463415,50.48780488,55,26.85365854,10.95731707,17.25609756,10.65853659,14.56097561,(1)";

    public String getTrainingData(){
        return TRAININGDATA;
    }

}
