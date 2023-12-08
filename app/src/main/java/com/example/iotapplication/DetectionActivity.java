package com.example.iotapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DetectionActivity extends AppCompatActivity {

    private Button nextActivityButton;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ZonedDateTime lastDetectionTime;

    private Handler handler = new Handler(Looper.getMainLooper());
    private ActivityResultLauncher<Intent> enableBluetoothLauncher;
    private static final String TARGET_MAC_ADDRESS = "00:04:3E:9A:B9:BB";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            lastDetectionTime = ZonedDateTime.now();
        }

        nextActivityButton = findViewById(R.id.activity_detection_next_activity_button);
        nextActivityButton.setEnabled(false);
        nextActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DetectionActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        setBluetoothSensors();
        startBluetoothScan();
    }

    private void checkAllPermission() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth LE를 지원하지 않는 기기입니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        String[] permissions = new String[] {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACTIVITY_RECOGNITION
        };
        List<String> needPermissions = new ArrayList<>(Arrays.asList(permissions));

        for (int i = 0; i < permissions.length; i++) {
            String currentPermission = permissions[i];
            int checkPermission = ContextCompat.checkSelfPermission(this, currentPermission);

            if (checkPermission == PackageManager.PERMISSION_DENIED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, currentPermission)) {
                    needPermissions.add(currentPermission);
                }
            }
        }

        String[] targets = new String[needPermissions.size()];
        needPermissions.toArray(targets);

        ActivityCompat.requestPermissions(this, targets, 101);
    }

    private void setBluetoothSensors() {
        // Bluetooth 지원 확인
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth LE를 지원하지 않는 기기입니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Bluetooth 관리자 초기화
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        enableBluetoothLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        checkAllPermission();
                    } else {
                        Toast.makeText(this, "Bluetooth를 활성화해야 합니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(enableBtIntent);
        } else {
            checkAllPermission();
        }
    }

    private void startBluetoothScan() {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(
                        new String[]{
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT
                        },
                        2);
            } else {
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        2);
            }
        }
        bluetoothLeScanner.startScan(scanCallback);

        // 일정 시간 후에 스캔 종료
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (ActivityCompat.checkSelfPermission(DetectionActivity.this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                        requestPermissions(
//                                new String[]{
//                                        Manifest.permission.BLUETOOTH_SCAN,
//                                        Manifest.permission.BLUETOOTH_CONNECT
//                                },
//                                2);
//                    } else {
//                        requestPermissions(
//                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                                2);
//                    }
//                }
//                bluetoothLeScanner.stopScan(scanCallback);
//            }
//        }, 20000); // 10초 동안 스캔
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            // 비콘과의 거리를 계산하여 출력

            if (ActivityCompat.checkSelfPermission(DetectionActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestPermissions(
                            new String[]{
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH_CONNECT
                            },
                            2);
                } else {
                    requestPermissions(
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            2);
                }
            }

            if (result.getDevice().getAddress().equals(TARGET_MAC_ADDRESS)) {
                nextActivityButton.setEnabled(true);
            }
            else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    ZonedDateTime currentTime = ZonedDateTime.now();

                    if (currentTime.isAfter(lastDetectionTime.plusSeconds(5))) {
                        nextActivityButton.setEnabled(false);
                    }
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(DetectionActivity.this, "Bluetooth 스캔에 실패했습니다.", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != 0) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults.length > 0 && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "권한 설정이 필요합니다.", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothLeScanner != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(DetectionActivity.this, "Bluetooth 스캔에 권한이 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }
}
