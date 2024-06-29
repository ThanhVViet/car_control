package com.example.handcontrol;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AppCompatActivity;
import android.view.MotionEvent;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button btnForward;
    private Button btnBackward;
    private Button btnLeft;
    private Button btnRight;
    private Button btnStop;
    private TextView txtSensor;

    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket = null;
    private OutputStream outputStream = null;
    private InputStream inputStream = null;

    private static final String DEVICE_NAME = "SLAVE";
    private static final UUID UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int REQUEST_CODE_BLUETOOTH_PERMISSION = 1001;

    private Thread listeningThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnForward = findViewById(R.id.btnForward);
        btnBackward = findViewById(R.id.btnBackward);
        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);
        btnStop = findViewById(R.id.btnStop);
        txtSensor = findViewById(R.id.txtSensor);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isConnected()) {
            connectToDevice();
        }

        btnForward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Gửi lệnh khi nút được nhấn
                    sendCommand('F');
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    // Ngừng gửi lệnh khi nút được nhả ra
                    sendCommand('S'); // Gửi lệnh dừng
                }
                return true;
            }
        });

        btnBackward.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Gửi lệnh khi nút được nhấn
                    sendCommand('B');
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    // Ngừng gửi lệnh khi nút được nhả ra
                    sendCommand('S'); // Gửi lệnh dừng
                }
                return true;
            }
        });

        btnLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Gửi lệnh khi nút được nhấn
                    sendCommand('L');
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    // Ngừng gửi lệnh khi nút được nhả ra
                    sendCommand('S'); // Gửi lệnh dừng
                }
                return true;
            }
        });

        btnRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    // Gửi lệnh khi nút được nhấn
                    sendCommand('R');
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    // Ngừng gửi lệnh khi nút được nhả ra
                    sendCommand('S'); // Gửi lệnh dừng
                }
                return true;
            }
        });
    }

    private boolean isConnected() {
        return bluetoothSocket != null && bluetoothSocket.isConnected();
    }

    private void connectToDevice() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= 31) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 100);
                return;
            }
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices != null && !pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {
                if (DEVICE_NAME.equals(device.getName())) {
                    bluetoothDevice = device;
                    break;
                }
            }

            if (bluetoothDevice != null) {
                try {
                    bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID_INSECURE);
                    bluetoothSocket.connect();
                    outputStream = bluetoothSocket.getOutputStream();
                    inputStream = bluetoothSocket.getInputStream();
                    Toast.makeText(this, "Connected to " + bluetoothDevice.getName(), Toast.LENGTH_SHORT).show();
                    startListeningForData();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No paired device found with the name " + DEVICE_NAME, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No paired devices found", Toast.LENGTH_SHORT).show();
        }
    }

    private synchronized void sendCommand(char command) {
        try {
            if (outputStream != null) {
                outputStream.write(command);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to send command", Toast.LENGTH_SHORT).show();
        }
    }

    private void startListeningForData() {
        final Handler handler = new Handler(Looper.getMainLooper());
        final byte delimiter = '\n';

        listeningThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int readBufferPosition = 0;
                byte[] readBuffer = new byte[1024];
                while (!Thread.currentThread().isInterrupted() && !isDestroyed()) {
                    try {
                        int bytesAvailable = inputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            inputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            updateSensorData(data);
                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        break;
                    }
                }
            }
        });

        listeningThread.start();
    }

    private synchronized void updateSensorData(String data) {
        txtSensor.setText(data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (listeningThread != null && listeningThread.isAlive()) {
            listeningThread.interrupt();
        }
    }
}