package com.example.a00_bt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

//// HERE
import java.io.InputStream;
import java.io.OutputStream;

// ver01 -- sending Direct Command


public class MainActivity extends AppCompatActivity {
    // BT Variables
    private final String CV_ROBOTNAME = "GGWP";
    private BluetoothAdapter cv_btInterface = null;
    private Set<BluetoothDevice> cv_pairedDevices = null;
    private BluetoothDevice cv_btDevice = null;
    private BluetoothSocket cv_btSocket = null;

    // Data stream to/from NXT bluetooth
    private InputStream cv_is = null;
    private OutputStream cv_os = null;

    private TextView cv_label01;
    private TextView cv_label02;
    private TextView ev3Status;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button keyboardImg = findViewById(R.id.keyboardImg);
        Button tuneBtn = findViewById(R.id.tuneBtn);
        Button soundBtn = findViewById(R.id.soundBtn);

        tuneBtn.setOnClickListener(v -> playTune());
        soundBtn.setOnClickListener(v -> playSound());

        keyboardImg.setOnTouchListener((view, event) -> {
            if(event.getAction() == MotionEvent.ACTION_DOWN) {
                float normX = (event.getX() - view.getX()) / view.getWidth();
                float normY = (event.getY() - view.getY()) / view.getHeight();
                int note = noteFromPosition(normX, normY);
                if(note != -1) {
                    playNote(note);
                }
            }
            return false;
        });

        cv_label01 = (TextView) findViewById(R.id.vv_tvOut1);
        cv_label02 = (TextView) findViewById(R.id.vv_tvOut2);
        ev3Status = (TextView) findViewById(R.id.connectionState);

        // Need grant permission once per install
        cpf_checkBTPermissions();
    }

    private void playSound() {
        // todo: ev3 code
    }

    private void playTune() {
        // todo: ev3 code
    }

    // Notes represented in half-steps above the C the keyboard starts on.
    private void playNote(int note) {
        // todo: ev3 code
        System.out.println(note);
    }

    private static final int[] BLACK_NOTES = {1, 3, -1, 6, 8, 10, -1, 13, 15, -1, 18, 20, 22, -1, 25, 27, -1, 30, 32, 34, -1};
    private static final int[] WHITE_NOTES = {0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23, 24, 26, 28, 29, 31, 33, 35};
    private static final int NUM_KEYS = 21;

    private static final float BLACK_KEY_BOTTOM = 115f / 181f;
    private static final float KEY_SIZE = 37f / 778f;
    private static final float BLACK_KEY_SIZE = 27f / 778f;
    private static final float BLACK_KEY_OFFSET = 24f / 778f;
    private int noteFromPosition(float normX, float normY) {
        if(normY < BLACK_KEY_BOTTOM) {
            int blackKey = checkBlackKey(normX);
            if(blackKey != -1) {
                return blackKey;
            }
        }

        int keyIdx = (int) (normX / KEY_SIZE);
        if(keyIdx < NUM_KEYS) {
            return WHITE_NOTES[keyIdx];
        } else {
            return -1;
        }
    }

    private int checkBlackKey(float normX) {
        normX -= BLACK_KEY_OFFSET;
        if(normX >= 0 && normX % KEY_SIZE <= BLACK_KEY_SIZE) {
            // Make a key index
            int keyIdx = (int) (normX / KEY_SIZE);
            // not every key has a corresponding black key
            if(hasBlackKey(keyIdx)) {
                return BLACK_NOTES[keyIdx];
            }
        }
        return -1;
    }

    private boolean hasBlackKey(int keyIdx) {
        return keyIdx < NUM_KEYS && BLACK_NOTES[keyIdx] != -1;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.menu_first) {
            cpf_requestBTPermissions();
            return true;
        } else if (id == R.id.menu_second) {
            cv_btDevice = cpf_locateInPairedBTList(CV_ROBOTNAME);
            return true;
        } else if (id == R.id.menu_third) {
            cpf_connectToEV3(cv_btDevice);
            return true;
        } else if (id == R.id.menu_fourth) {
            cpf_EV3MoveMotor(50, MOTOR_B | MOTOR_C);
            return true;
        } else if (id == R.id.menu_fifth) {
            cpf_EV3PlayTone();
            return true;
        } else if (id == R.id.menu_sixth) {
            cpf_disconnFromEV3(cv_btDevice);
            return true;
        } else {
            return super.onOptionsItemSelected(menuItem);
        }
        /*switch (menuItem.getItemId()) {
            case R.id.menu_first: cpf_requestBTPermissions();
                return true;
            case R.id.menu_second: cv_btDevice = cpf_locateInPairedBTList(CV_ROBOTNAME);
                return true;
            case R.id.menu_third: cpf_connectToEV3(cv_btDevice);
                return true;
            case R.id.menu_fourth: cpf_EV3MoveMotor();
                return true;
            case R.id.menu_fifth: cpf_EV3PlayTone();
                return true;
            case R.id.menu_sixth: cpf_disconnFromEV3(cv_btDevice);
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }*/

    }

    private void cpf_checkBTPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            cv_label01.setText("BLUETOOTH_SCAN already granted.\n");
        } else {
            cv_label01.setText("BLUETOOTH_SCAN NOT granted.\n");
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            cv_label02.setText("BLUETOOTH_CONNECT NOT granted.\n");
        } else {
            cv_label02.setText("BLUETOOTH_CONNECT already granted.\n");
        }
    }

    // https://www.geeksforgeeks.org/android-how-to-request-permissions-in-android-application/
    // https://stackoverflow.com/questions/67722950/android-12-new-bluetooth-permissions
    private void cpf_requestBTPermissions() {
        // We can give any value but unique for each permission.
        final int BLUETOOTH_SCAN_CODE = 100;
        final int BLUETOOTH_CONNECT_CODE = 101;

        // Android version < 12, "android.permission.BLUETOOTH" just fine
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Toast.makeText(MainActivity.this,
                    "BLUETOOTH granted for earlier Android", Toast.LENGTH_SHORT).show();
            return;
        }

        // Android 12+ has to go through the process
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN},
                    BLUETOOTH_SCAN_CODE);
        } else {
            Toast.makeText(MainActivity.this,
                    "BLUETOOTH_SCAN already granted", Toast.LENGTH_SHORT).show();
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    BLUETOOTH_CONNECT_CODE);
        } else {
            Toast.makeText(MainActivity.this,
                    "BLUETOOTH_CONNECT already granted", Toast.LENGTH_SHORT).show();
        }
    }

    // Modify from chap14, pp390 findRobot()
    private BluetoothDevice cpf_locateInPairedBTList(String name) {
        BluetoothDevice lv_bd = null;
        try {
            cv_btInterface = BluetoothAdapter.getDefaultAdapter();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return null;
            }
            cv_pairedDevices = cv_btInterface.getBondedDevices();
            Iterator<BluetoothDevice> lv_it = cv_pairedDevices.iterator();
            while (lv_it.hasNext()) {
                lv_bd = lv_it.next();
                if (lv_bd.getName().equalsIgnoreCase(name)) {
                    cv_label01.setText(name + " is in paired list");
                    return lv_bd;
                }
            }
            cv_label01.setText(name + " is NOT in paired list");
        } catch (Exception e) {
            cv_label01.setText("Failed in findRobot() " + e.getMessage());
        }
        return null;
    }

    // Modify frmo chap14, pp391 connectToRobot()
    private void cpf_connectToEV3(BluetoothDevice bd) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            cv_btSocket = bd.createRfcommSocketToServiceRecord
                    (UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            cv_btSocket.connect();

            //// HERE
            cv_is = cv_btSocket.getInputStream();
            cv_os = cv_btSocket.getOutputStream();
            ev3Status.setText("Connected to " + bd.getName() + " at " + bd.getAddress());
            ev3Status.setTextColor(Color.GREEN);
        } catch (Exception e) {
            ev3Status.setText("Error interacting with remote device [" +
                    e.getMessage() + "]");
            ev3Status.setTextColor(Color.RED);
        }
    }

    private void cpf_disconnFromEV3(BluetoothDevice bd) {
        try {
            cv_btSocket.close();
            cv_is.close();
            cv_os.close();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            cv_label02.setText(bd.getName() + " is disconnect ");
        } catch (Exception e) {
            cv_label02.setText("Error in disconnect -> " + e.getMessage());
        }
    }

    private static final byte MOTOR_A = 0b1;
    private static final byte MOTOR_B = 0b10;
    private static final byte MOTOR_C = 0b100;

    // Communication Developer Kit Page 27
    // 4.2.2 Start motor B & C forward at power 50 for 3 rotation and braking at destination
    private void cpf_EV3MoveMotor(int power, int motors) {
        try {
            byte[] buffer = new byte[20];       // 0x12 command length

            buffer[0] = (byte) (20-2);
            buffer[1] = 0;

            buffer[2] = 34;
            buffer[3] = 12;

            buffer[4] = (byte) 0x80;

            buffer[5] = 0;
            buffer[6] = 0;

            buffer[7] = (byte) 0xad; // OP code
            buffer[8] = 0; //

            buffer[9] = (byte) motors; // Output

            buffer[10] = (byte) 0x81;
            buffer[11] = (byte) power;

            buffer[12] = 0;

            buffer[13] = (byte) 0x82;
            buffer[14] = (byte) 0x84;
            buffer[15] = -1;

            buffer[16] = (byte) 0x82;
            buffer[17] = (byte) 0xB4;
            buffer[18] = (byte) 0x03;

            buffer[19] = 1;

            cv_os.write(buffer);
            cv_os.flush();
        }
        catch (Exception e) {
            new Handler(Looper.getMainLooper()).post(() -> {
                cv_label01.setText("Error in MoveForward(" + e.getMessage() + ")");
            });
        }
    }

    // 4.2.5 Play a 1Kz tone at level 2 for 1 sec.
    private void cpf_EV3PlayTone() {
        try {
            byte[] buffer = new byte[17];       // 0x0f command length

            buffer[0] = (byte) (17-2);
            buffer[1] = 0;

            buffer[2] = 34;
            buffer[3] = 12;

            buffer[4] = (byte) 0x80;

            buffer[5] = 0;
            buffer[6] = 0;

            buffer[7] = (byte) 0x94;
            buffer[8] = 1;

            buffer[9] = (byte) 0x81;
            buffer[10] = (byte) 0x02;

            buffer[11] = (byte) 0x82;
            buffer[12] = (byte) 0xe8;
            buffer[13] = (byte) 0x03;

            buffer[14] = (byte) 0x82;
            buffer[15] = (byte) 0xe8;
            buffer[16] = (byte) 0x03;

            cv_os.write(buffer);
            cv_os.flush();
        }
        catch (Exception e) {
            cv_label02.setText("Error in MoveForward(" + e.getMessage() + ")");
        }
    }
}