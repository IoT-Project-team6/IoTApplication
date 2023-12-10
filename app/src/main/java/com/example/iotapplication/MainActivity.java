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
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // 320 이상, 55 이하 일때 이동은 세로 이동으로 판단
    // 225 이상, 320 이하 오른쪽 이동 판단
    // 55 이상 145 이하 왼쪽 이동 판단

    // 강의실 bluetooth 기기 mac 주소
    private static final String TARGET_MAC_ADDRESS = "00:04:3E:9A:B9:BB";

    // bluetooth 측정 센서 setting
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;

    // 걸음 수 측정 센서 setting
    private SensorManager sensorManager;
    private Sensor stepCountSensor;

    private Handler handler = new Handler(Looper.getMainLooper());
    private ActivityResultLauncher<Intent> enableBluetoothLauncher;
    private TextView distanceTextView;
    private Button startButton;
    private Button resetButton;
    private Button submitButton;
    private TextView currentStepTextView;
    private TextView currentTransversTextView;
    private TextView currentLengthTextView;
    private TextView currentAzimuthTextView;
    private TextView locationTextView;
    private String distances = "";
    int currentSteps = 0;
    int currentTransverseStep = 0;
    int currentLengthStep = 0;
    private DirectionSensor directionListener;
    private SensorManager directionSensorManager;
    private float currentAzimuth;
    private List<Double> dis = new ArrayList<>();
    private Button resultButton;
    private boolean flag = false;
    private double lastDirection = 0.0;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        directionListener = new DirectionSensor();
        startDirectionSensor();
        setViews();
//        checkAllPermission();
        setBluetoothSensors();
        setPedometerSensors();
    }

    private void startDirectionSensor() {
        directionSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        Sensor sensor1 = directionSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor sensor2 = directionSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        directionSensorManager.registerListener(directionListener, sensor1, SensorManager.SENSOR_DELAY_UI);
        directionSensorManager.registerListener(directionListener, sensor2, SensorManager.SENSOR_DELAY_UI);
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
        }, 20000); // 10초 동안 스캔
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
//                String info = result.getDevice().getName() + " / " + result.getDevice().getAddress() + " / " + String.format("거리: %.2f 미터", distance);
//                distances = distances + info + "\n";
//                Log.d("hi", distances);

                dis.add(distance);
                Log.d("hi", distance + "");
//                distanceTextView.setText(distances);
            }

            if (dis.size() == 7) {
                Double sum = 0.0;
                for (Double d : dis) {
                    sum += d;
                }

                sum = sum / 7;

                distanceTextView.setText(sum.toString());
                bluetoothLeScanner.stopScan(scanCallback);
                Calculation calculation = new Calculation();
                String location = calculation.calculateLocation(distance, currentTransverseStep, currentLengthStep, lastDirection);
                locationTextView.setText(location);
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
        int txPower = -53; // 비콘과 스마트폰 간의 거리가 1 미터일 때의 RSSI 값
        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            return (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
        }
    }

//    private double calculateDistance(int rssi) {
//        // 튜닝 가능한 파라미터
//        int txPower = -50; // 비콘과 스마트폰 간의 거리가 1 미터일 때의 RSSI 값
////        Log.d("hi", txPower + "");
//        double pathLossExponent = 2.0; // 경로 손실 지수
//        double referenceDistance = 1.0; // 참조 거리 (1 미터)
//
//        // RSSI 값을 이용하여 거리를 추정하는 모델
//        return Math.pow(10, ((double) txPower - rssi) / (10 * pathLossExponent)) * referenceDistance;
//    }

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
                Toast.makeText(MainActivity.this, "Bluetooth 스캔에 권한이 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }

    // 이하 걸음 측정 로직
    private void setViews() {
        distanceTextView = findViewById(R.id.activity_main_distance_text);
        startButton = findViewById(R.id.activity_main_startButton);
        resetButton = findViewById(R.id.activity_main_reset_button);
        submitButton = findViewById(R.id.activity_main_submit_button);
        currentStepTextView = findViewById(R.id.activity_main_current_step_text_view);
        currentTransversTextView = findViewById(R.id.activity_main_current_transverse_step_text_view);
        currentLengthTextView = findViewById(R.id.activity_main_current_length_step_text_view);
        currentAzimuthTextView = findViewById(R.id.activity_main_current_azimuth);
        resultButton = findViewById(R.id.activity_main_result_button);
        locationTextView = findViewById(R.id.activity_main_location_text_view);

        setSubmitButtonListener();
        setStartButtonListener();
        setResetButtonListener();
        setResultButtonListener();

        submitButton.setEnabled(false);
        resetButton.setEnabled(false);
    }

    private void setResultButtonListener() {
        resultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 계산 시
            }
        });
    }

    private void setSubmitButtonListener() {
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetButton.setEnabled(true);
                startBluetoothScan();
            }
        });
    }

    private void setStartButtonListener() {
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reverseResetAndSubmitButtonStatus();
                if (startButton.getText().equals("START")) {
                    startPedometer();
                    startButton.setText("STOP");
                }
                else {
                    stopPedometer();
                    startButton.setText("START");
                }

            }
        });
    }

    private void reverseResetAndSubmitButtonStatus() {
        if (resetButton.isEnabled()) {
            resetButton.setEnabled(false);
            submitButton.setEnabled(false);
        }
        else {
            resetButton.setEnabled(true);
            submitButton.setEnabled(true);
        }
    }

    private void setResetButtonListener() {
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentSteps = 0;
                currentStepTextView.setText(String.valueOf(currentSteps));
            }
        });
    }

    private void setPedometerSensors() {
        if(ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 2);
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
    }
    private void startPedometer() {
        if(stepCountSensor !=null) {
            // 센서 속도 설정
            // * 옵션
            // - SENSOR_DELAY_NORMAL: 20,000 초 딜레이
            // - SENSOR_DELAY_UI: 6,000 초 딜레이
            // - SENSOR_DELAY_GAME: 20,000 초 딜레이
            // - SENSOR_DELAY_FASTEST: 딜레이 없음
            //
            sensorManager.registerListener(this,stepCountSensor,SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    private void stopPedometer() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_STEP_DETECTOR){

            if(sensorEvent.values[0]==1.0f){
                // 센서 이벤트가 발생할때 마다 걸음수 증가
                currentSteps++;

                // 320 이상, 55 이하 일때 이동은 세로 이동으로 판단
                // 225 이상, 320 이하 오른쪽 이동 판단
                // 55 이상 145 이하 왼쪽 이동 판단
                if (225 <= currentAzimuth && currentAzimuth < 320) {
                    if (!flag) {
                        currentTransverseStep++;
                    }
                }
                else if ((320 <= currentAzimuth && currentAzimuth <= 360) || (0 <= currentAzimuth && currentAzimuth <= 55)) {
                    currentLengthStep++;
                }

                if (currentLengthStep > 0) {
                    flag = true;
                }

                currentStepTextView.setText(String.valueOf(currentSteps));
                currentTransversTextView.setText(String.valueOf(currentTransverseStep));
                currentLengthTextView.setText(String.valueOf(currentLengthStep));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public class DirectionSensor implements SensorEventListener {

        float[] accValue = new float[3];
        float[] magValue = new float[3];

        boolean isGetAcc = false;
        boolean isGetMag = false;

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            int type = sensorEvent.sensor.getType();

            switch (type) {
                case Sensor.TYPE_ACCELEROMETER:
                    System.arraycopy(sensorEvent.values, 0, accValue, 0, sensorEvent.values.length);
                    isGetAcc = true;
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    System.arraycopy(sensorEvent.values, 0, magValue, 0, sensorEvent.values.length);
                    isGetMag = true;
                    break;
            }

            if (isGetAcc == true && isGetMag == true) {
                float[] R = new float[9];
                float[] I = new float[9];

                SensorManager.getRotationMatrix(R, I, accValue, magValue);
                float[] values = new float[3];
                SensorManager.getOrientation(R, values);

                float azimuth = (float) Math.toDegrees(values[0]);
                float pitch = (float) Math.toDegrees(values[1]);
                float roll = (float) Math.toDegrees(values[2]);

                if (azimuth < 0) {
                    azimuth += 360;
                }

                currentAzimuth = azimuth;
                currentAzimuthTextView.setText(String.valueOf(currentAzimuth));
                lastDirection = currentAzimuth;
//                Log.d("hi", (currentAzimuth) + ""); // 97~265
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }

        public float getCurrentAzimuth() {
            return currentAzimuth;
        }
    }
}
