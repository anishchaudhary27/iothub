package com.example.combtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import java.util.ArrayList;
import java.util.UUID;

public class SecondFragment extends Fragment {
    private String btName;
    private String qrCode;

    private BluetoothAdapter bluetoothAdapter;

    // Stops scanning after 5 seconds.
    private static final long SCAN_PERIOD = 10000;
    BluetoothGatt gatt;
    private Handler handler;
    ArrayList<BluetoothDevice> devices = new ArrayList<>();
    Boolean scanning = false;

    String ssid = "King_pin";
    String pass = "iubi0792";
    int state = 0;
    int status = 0;
    Boolean connnected = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        btName = getArguments().getString("btName");
        qrCode = getArguments().getString("qrCode");
        state = 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_second, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 123);
        }
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(scanning == true) {
                    scanning = false;
                    bluetoothAdapter.stopLeScan(leScanCallback);
                    Log.i("my","stopped scan");
                    if(devices.size() > 0) {
                        connect(devices.get(0));
                    }
                    else {
                        Toast.makeText(getContext(),"device not found!",Toast.LENGTH_SHORT).show();
                        Bundle b  = new Bundle();
                        b.putString("qrCode",qrCode);
                        Navigation.findNavController(getView()).navigate(R.id.action_SecondFragment_to_optionsFragment,b);
                    }

                }
            }
        }, SCAN_PERIOD);
        scanning = true;
        bluetoothAdapter.startLeScan(leScanCallback);
        Button configBtn = view.findViewById(R.id.configureBtn);
        configBtn.setOnClickListener(v -> {
            if(status > 0 && connnected) {
                configBtn.setEnabled(false);
                TextView tvPass = view.findViewById(R.id.passTextView);
                TextView tvSSID = view.findViewById(R.id.ssidTextView);
                ssid = tvSSID.getText().toString().trim();
                pass = tvPass.getText().toString().trim();
                writeCharacteristic();
            }
            else {
                Toast.makeText(getContext(),"device not found yet!",Toast.LENGTH_SHORT).show();
            }
        });
        TextView textView = view.findViewById(R.id.textView);
        textView.setText(btName);
    }

    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    getActivity().runOnUiThread(() -> {
                        if(device.getName().equals(btName)) {
                            devices.add(device);
                            scanning = false;
                            Log.i("my","scan stopped");
                            bluetoothAdapter.stopLeScan(leScanCallback);
                            connect(devices.get(0));
                        }
                    });
                }
            };

    private void connect(BluetoothDevice bluetoothDevice) {
        gatt = bluetoothDevice.connectGatt(getContext(),false,gattCallback);
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("my","connected");
                gatt.discoverServices();
                connnected = true;
            }
            else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                connnected = false;
                Bundle b  = new Bundle();
                b.putString("qrCode",qrCode);
                Navigation.findNavController(getView()).navigate(R.id.action_SecondFragment_to_optionsFragment,b);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                incrementStatus();
            }
            else {
                Log.i("my","noting discovered!!");
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                writeCharacteristic();
            }
        }
    };

    private void incrementStatus() {
        status = status + 1;
    }

    private void writeCharacteristic() {
        if(state  >= 9) {
            return;
        }
        String uuid;
        String value;
        int a = state%3;
        switch (a) {
            case 0:
                uuid = "306d4f53-5f43-4647-5f6b-65795f5f5f30";
                break;
            case 1:
                uuid = "316d4f53-5f43-4647-5f76-616c75655f31";
                break;
            case 2:
                uuid = "326d4f53-5f43-4647-5f73-6176655f5f32";
                break;
            default:
                uuid = "326d4f53-5f43-4647-5f73-6176655f5f32";
                break;
        }
        switch (state) {
            case 0:
                value = "wifi.sta.ssid";
                break;
            case 1:
                value = ssid;
                break;
            case 2:
                value = "0";
                break;
            case 3:
                value = "wifi.sta.pass";
                break;
            case 4:
                value = pass;
                break;
            case 5:
                value = "0";
                break;
            case 6:
                value = "wifi.sta.enable";
                break;
            case 7:
                value = "true";
                break;
            case 8:
                value = "2";
                break;
            default:
                value = "0";
                break;
        }
        state = state + 1;
        BluetoothGattCharacteristic characteristic = gatt.getService(UUID.fromString("5f6d4f53-5f43-4647-5f53-56435f49445f"))
                .getCharacteristic(UUID.fromString(uuid));
        characteristic.setValue(value);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        gatt.writeCharacteristic(characteristic);
        Log.i("my",uuid);
        Log.i("my",value);
    }
}
