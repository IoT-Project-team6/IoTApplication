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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_FINE_LOCATION = 2;
    private static final String TARGET_MAC_ADDRESS = "00:04:3E:9A:B9:BB";
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ActivityResultLauncher<Intent> enableBluetoothLauncher;
    private TextView distanceTextView;
    private String distances = "";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        distanceTextView = findViewById(R.id.activity_main_distance_text);

        // Bluetooth 지원 확인
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth LE를 지원하지 않는 기기입니다.", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Bluetooth 관리자 초기화
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Bluetooth 활성화 확인
        enableBluetoothLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        checkLocationPermission();
                    } else {
                        Toast.makeText(this, "Bluetooth를 활성화해야 합니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(enableBtIntent);
        } else {
            checkLocationPermission();
        }
    }

    private void checkLocationPermission() {
        // Android 6.0 이상에서는 위치 권한 확인 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_FINE_LOCATION);
            } else {
                startBluetoothScan();
            }
        } else {
            startBluetoothScan();
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
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
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
                bluetoothLeScanner.stopScan(scanCallback);
            }
        }, 10000); // 10초 동안 스캔
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            // 비콘과의 거리를 계산하여 출력

            double distance = calculateDistance(result.getRssi());
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    requestPermissions(
                            new String[]{
                                    Manifest.permission.BLUETOOTH_CONNECT
                            },
                            2);
                } else {
                    requestPermissions(
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            2);
                }
            }

            if (result.getDevice().getAddress() == TARGET_MAC_ADDRESS) {
                String info = result.getDevice().getName() + " / " + result.getDevice().getAddress() + " / " + String.format("거리: %.2f 미터", distance);
                distances = distances + info + "\n";
                distanceTextView.setText(distances);
            }

//            if (result.getDevice().getName() != null) {
//                String info = result.getDevice().getName() + " / " + result.getDevice().getAddress() + " / " + String.format("거리: %.2f 미터", distance);
//                names = names + info + "\n";
//                nameTextView.setText(names);
//            }
//
//            if (result.getDevice().getAddress() != null) {
//                macs = macs + " / " + result.getDevice().getAddress();
//                macTextView.setText(macs);
//                distances = distances + " / " + ;
//                distanceTextView.setText(distances);
//            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(MainActivity.this, "Bluetooth 스캔에 실패했습니다.", Toast.LENGTH_SHORT).show();
        }
    };

    private double calculateDistance(int rssi) {
        // RSSI를 사용하여 거리를 추정하는 방법은 여러가지가 있습니다.
        // 여기서는 간단한 모델을 사용하며, 상황에 따라 더 정교한 모델을 사용할 수 있습니다.
        int txPower = -80; // 비콘과 스마트폰 간의 거리가 1 미터일 때의 RSSI 값
        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            return (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
        }
    }

//    private double calculateDistance(int rssi) {
//        // 튜닝 가능한 파라미터
//        int txPower = -80; // 비콘과 스마트폰 간의 거리가 1 미터일 때의 RSSI 값
//        double pathLossExponent = 2.0; // 경로 손실 지수
//        double referenceDistance = 1.0; // 참조 거리 (1 미터)
//
//        // RSSI 값을 이용하여 거리를 추정하는 모델
//        return Math.pow(10, ((double) txPower - rssi) / (10 * pathLossExponent)) * referenceDistance;
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBluetoothScan();
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothLeScanner != null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Bluetooth 스캔에 권한이 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }

}
