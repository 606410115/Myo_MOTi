package com.example.myoxmoti_test;

import java.util.ArrayList;

/**
 * Created by Mslab on 2017/10/4.
 */

public class ImuData {
    private ArrayList<Double> imuData = new ArrayList<>();

    private double time;

    double w, x, y, z;
    double accX, accY, accZ;
    double gryoX, gryoY, gryoZ;

    public ImuData() {
    }

    public ImuData(ImuCharacteristicData characteristicData, TimeManager timeManager) {
        this.imuData = new ArrayList<>(characteristicData.covertRawData().getImuArray() );
        //time = timeManager.getTime();
    }

    public void addElement(double element) {
        imuData.add(element);
    }

    public Double getElement(int index) {
        if (index < 0 || index > imuData.size() - 1) {
            return null;
        } else {
            return imuData.get(index);
        }
    }

    public ArrayList<Double> getImuArray() {
        return this.imuData;
    }

    public double getTime(){
        return time;
    }

    public void logImu(){
        w=imuData.get(0);
        x=imuData.get(1);
        y=imuData.get(2);
        z=imuData.get(3);
        accX=imuData.get(4);
        accY=imuData.get(5);
        accZ=imuData.get(6);
        gryoX=imuData.get(7);
        gryoY=imuData.get(8);
        gryoZ=imuData.get(9);
    }

}
