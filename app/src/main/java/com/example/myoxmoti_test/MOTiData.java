package com.example.myoxmoti_test;

import java.util.ArrayList;

/**
 * Created by Mslab on 2017/10/6.
 */

public class MOTiData{
    private ArrayList<Double> MOTiData = new ArrayList<>();

    private double time;

    double accX, accY, accZ;
    double gryoX, gryoY, gryoZ;

    public MOTiData() {
    }

    public MOTiData(MOTiCharacteristicData characteristicData, TimeManager timeManager) {
        this.MOTiData = new ArrayList<>(characteristicData.covertRawData().getMOTiArray() );
        time = timeManager.getTime();
    }

    public void addElement(double element) {
        MOTiData.add(element);
    }

    public ArrayList<Double> getMOTiArray() {
        return this.MOTiData;
    }

    public double getTime(){
        return time;
    }

    public void logImu(){
        accX=MOTiData.get(0);
        accY=MOTiData.get(1);
        accZ=MOTiData.get(2);
        gryoX=MOTiData.get(3);
        gryoY=MOTiData.get(4);
        gryoZ=MOTiData.get(5);
    }
}
