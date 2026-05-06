package com.flying_kiwi.dyna.Utils;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanSettings;

public class BTManager {
    BluetoothManager bluetoothMgr;
    BluetoothLeScanner bleScanner;
    BTManager(BluetoothManager btMgr){
        this.bluetoothMgr = btMgr;
        BluetoothAdapter adapter = btMgr.getAdapter();
        if (adapter != null) {
            bleScanner = adapter.getBluetoothLeScanner();
        }
    }

    @SuppressLint("MissingPermission")
    public void startBLEScan(ScanCallback scanCallback) {
        if (bleScanner == null) {
            BluetoothAdapter adapter = bluetoothMgr.getAdapter();
            if (adapter != null) {
                bleScanner = adapter.getBluetoothLeScanner();
            }
            if (bleScanner == null) {
                return;
            }
        }
        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        bleScanner.startScan(null, settings, scanCallback);
    }
    @SuppressLint("MissingPermission")
    public void stopBLEScan(ScanCallback scanCallback){
        if (bleScanner != null) {
            bleScanner.stopScan(scanCallback);
        }
    }

    public void startReadingBTConnData(){

    }
}
