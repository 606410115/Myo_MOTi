package com.example.myoxmoti_test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by naoki on 15/04/06.
 * 
 * This class help you to read the byte line from Myo.
 * But be carefully to byte array size. There is no limitation of get() method, 
 * so there is a possibilty of overloading the byte buffer.
 * 
 */

public class ByteReader {
    private byte[] byteData;//在setByteData有存入emgdata
    private ByteBuffer bbf;

    public void setByteData(byte[] data){
        this.byteData = data;//byteData get emgdata!!!
        this.bbf = ByteBuffer.wrap(this.byteData);//wraw(包裝) byteData in buffer
        bbf.order(ByteOrder.LITTLE_ENDIAN);//字節序
    }

    public void setMOTiByteData(byte[] data){
        byteData = data;
    }

    public short getShort(int i_MOTi_num){//MOTi getShort
        return ByteBuffer.wrap(this.byteData, i_MOTi_num, 2).getShort();
    }


    public byte[] getByteData() {
        return byteData;
    }

    public short getShort() {
        return this.bbf.getShort();
    }

    public byte getByte(){
        return this.bbf.get();
    }//get buffer's value and increase next postion

    public int getInt(){
        return this.bbf.getInt();
    }

    public String getByteDataString() {
        final StringBuilder stringBuilder = new StringBuilder(byteData.length);
        for (byte byteChar : byteData) {
            stringBuilder.append(String.format("%02X ", byteChar));
        }
        return stringBuilder.toString();
    }

    public String getIntDataString() {
        final StringBuilder stringBuilder = new StringBuilder(byteData.length);
        for (byte byteChar : byteData) {
            stringBuilder.append(String.format("%5d,", byteChar));
        }
        return stringBuilder.toString();
    }
}
