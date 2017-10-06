package com.example.myoxmoti_test;

import java.nio.ByteBuffer;

/**
 * Created by Mslab on 2017/10/6.
 */

public class MOTiCharacteristicData {

    private ByteReader MOTiData = new ByteReader();

    public MOTiCharacteristicData(byte[] byteData) {
        MOTiData.setMOTiByteData(byteData);
    }

    public MOTiData covertRawData(){
        MOTiData MOTiCovert = new MOTiData();

        for (int i_MOTi_num = 6; i_MOTi_num <= 16; i_MOTi_num = i_MOTi_num + 2){
            double temp =MOTiData.getShort(i_MOTi_num);

            if(i_MOTi_num <= 10){//Accelerometer
                temp = (temp * 56) / 23405;
            }
            else if(i_MOTi_num >10 && i_MOTi_num <= 16){//Gyroscope
                temp = (temp * 1000) / 5535;
            }

            MOTiCovert.addElement(temp);
        }

        return MOTiCovert;
    }

}
