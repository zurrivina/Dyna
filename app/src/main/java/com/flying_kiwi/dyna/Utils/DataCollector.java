package com.flying_kiwi.dyna.Utils;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.util.Log;

import com.flying_kiwi.dyna.TimestampedWeight;

import java.util.Arrays;
import java.util.function.Consumer;

public class DataCollector {
    //List devices
    //select device
    //If direct BT, handshake and connect
    //If advertisement, filter by device name

    Consumer<TimestampedWeight> viewCallback;
    BTManager btManager;
    String deviceName;
    public DataCollector(BluetoothManager bluetoothManager, Consumer<TimestampedWeight> viewCallback) {
        btManager = new BTManager(bluetoothManager);
        this.viewCallback = viewCallback;
        this.deviceName = "IF_B7";
        startScanning();
    }

    private boolean collectingData = false;
    private boolean bleConnectionMode = true;
    private boolean isDeviceFound = false;
    private Runnable onDeviceFoundCallback = null;

    public void setOnDeviceFoundCallback(Runnable callback) {
        this.onDeviceFoundCallback = callback;
    }

    public boolean isDeviceFound() {
        return isDeviceFound;
    }

    //ONLY WH-C06 devices named "IF_B7
    public void startScanning(){
        if("IF_B7".equals(deviceName)){
            bleConnectionMode = true;
            btManager.startBLEScan(scanCallback);
            Log.d("DataCollector", "Started BLE scan for device: " + deviceName);
        } else {
            bleConnectionMode = false;
            btManager.startReadingBTConnData();
        }
    }

    static int cstu(byte i) {
        return (int) i & 0xFF;
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        byte[] last = new byte[17];
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result.getScanRecord() == null) {
                return;
            }
            String deviceName = result.getScanRecord().getDeviceName();
            if (deviceName == null || !deviceName.equals("IF_B7")) {
                return;
            }
            if (!isDeviceFound && onDeviceFoundCallback != null) {
                isDeviceFound = true;
                onDeviceFoundCallback.run();
            }
            byte[] data = result.getScanRecord().getManufacturerSpecificData(256);
            if (data == null || data.length < 15) {
                return;
            }

            if (collectingData) {
                if(data[9] != last[9]){
                    Log.d("Data Last " + collectingData,Arrays.toString(last));
                    Log.d("Data " + collectingData,Arrays.toString(data));
                }
                last = data;
                TimestampedWeight reading = new TimestampedWeight((cstu(data[10]) * 256 + cstu(data[11]))/100f,data[14]==1);
                viewCallback.accept(reading);
            }
        }
    };
    //TODO REMOVE MISSING PERMISSION TAGS
    @SuppressLint("MissingPermission")
    public void stopScanning(){
        stopCollecting();
        btManager.stopBLEScan(scanCallback);
    }
    public void stopCollecting(){
        collectingData = false;
    }

    public void startCollecting(){
        collectingData = true;
        Log.d("DataCollector", "Started collecting data");
    }
    public boolean isCollectingData() {
        return collectingData;
    }
}
