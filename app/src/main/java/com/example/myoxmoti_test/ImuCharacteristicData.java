package com.example.myoxmoti_test;

import java.util.ArrayList;

/**
 * Created by Mslab on 2017/10/4.
 */

public class ImuCharacteristicData {
    private static final float MYOHW_ORIENTATION_SCALE = 16384.0f;
    private static final float MYOHW_ACCELEROMETER_SCALE = 2048.0f;
    private static final float MYOHW_GYROSCOPE_SCALE = 16.0f;
    private static final float G=9.8f;

    private ByteReader imuData = new ByteReader();

    public ImuCharacteristicData(byte[] byteData) {
        imuData.setByteData(byteData);
    }

    public ImuData covertRawData(){
        ImuData imuCovert = new ImuData();

        for (int i_imu_num = 0; i_imu_num < 10; i_imu_num++) {
            double temp = imuData.getShort();
            if(i_imu_num < 4){//quaternion
                temp = temp / MYOHW_ORIENTATION_SCALE;
            }
            else if(i_imu_num >= 4 && i_imu_num < 7){//accelerometer
                temp = (temp / MYOHW_ACCELEROMETER_SCALE) * G;
            }
            else if(i_imu_num >= 7){//gyroscope
                temp = temp / MYOHW_GYROSCOPE_SCALE;
            }

            //temp = normalize(temp, i_imu_num);

            imuCovert.addElement(temp);
        }

        return imuCovert;
    }

    public double normalize(double temp, int i_imu_num){
        if(i_imu_num < 4){//quaternion
            temp = temp / 65536;
        }
        else if(i_imu_num >= 4 && i_imu_num < 7){//accelerometer
            temp = (temp + 156.8) / 313.6;
        }
        else if(i_imu_num >= 7){//gyroscope
            temp = (temp + 2000) / 4000;
        }

        return temp;
    }


}
