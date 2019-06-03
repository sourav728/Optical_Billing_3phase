package com.example.srv.optical_billing_threephase;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.srv.optical_billing.R;

import java.io.File;
import java.io.FileWriter;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    /*
     * Notifications from UsbService will be received here.

     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            switch (intent.getAction()) {

                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private UsbService usbService;
    private TextView display;
    //private EditText editText;
    private MyHandler mHandler;
    private ClipboardManager myClipboard;
    private ClipData myClip;
    private ArrayList arrayList;
    private String data = "";
    int i = 0;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arrayList = new ArrayList();
        mHandler = new MyHandler(this);
        display = findViewById(R.id.textView1);
        display.setMovementMethod(new ScrollingMovementMethod());
        //Send button
        Button sendButton = findViewById(R.id.buttonSend);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (usbService != null) { // if UsbService was correctly binded, Send data
                    char data[] = new char[12];

                    data[0] = 0x3A;//3A

                    data[1] = 0x3F;//?
                    data[2] = 0x21;//!
                    data[3] = 0x0D;//CTRL+M
                    data[4] = 0x0A;//CTRL+J

                    data[5] = 0x06;//CTRL+F
                    data[6] = 0x30;//0
                    data[7] = 0x34;//4
                    data[8] = 0x30;//0
                    data[9] = 0x0D;//CTRL+M
                    data[10] = 0x0A;//CTRL+J
                    data[11] = 0x10;///CTRL+P
                    for (int i = 0; i <= 11; i++) {

                        switch (i) {
                            case 0:
                                for (int j = 0; j < 2; j++) {

                                    // display.append(Character.toString(data[0]));

                                    try {
                                        usbService.write(Character.toString(data[0]).getBytes("US-ASCII"));
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                }
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case 1:
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case 5:
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                break;
                        }
                        //display.append(Character.toString(data[i]));
                        try {
                            usbService.write(Character.toString(data[i]).getBytes("US-ASCII"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        // copy button
        Button copyText = findViewById(R.id.buttonCopy);
        myClipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        copyText.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                String text = display.getText().toString();
                myClip = ClipData.newPlainText("text", text);
                myClipboard.setPrimaryClip(myClip);
                Toast.makeText(getApplicationContext(), "Text Copied", Toast.LENGTH_SHORT).show();
            }
        });

        //clear button
        Button clrButton = findViewById(R.id.buttonClear);
        clrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                display.setText("");

            }
        });


    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    public class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    data = (String) msg.obj;
                    //Toast.makeText(MainActivity.this, "Data Received "+ data, Toast.LENGTH_SHORT).show();
                    mActivity.get().display.append(data);
                    // arrayList.add(data);
                    storevalue();
                    break;
            }
        }
    }

    public void storevalue() {
        arrayList.add(data);
        String display_text="";
        i++;
        if (i == 1614) {
            display_text = display.getText().toString();
            writeToFile(display_text);
            String meter_serial_no = display_text.substring(display_text.indexOf("/") + 5, display_text.indexOf("H"));
            Toast.makeText(MainActivity.this, "Meter Serial No: " + meter_serial_no, Toast.LENGTH_SHORT).show();
            //usbService.stopSelf();
        }

    }

    //Below code is for writing the Opticals port output in text file
    private void writeToFile(String currentStacktrace) {
        try {

            //Gets the Android external storage directory & Create new folder Crash_Reports
            File dir = new File(Environment.getExternalStorageDirectory(),
                    "Opticals_3Phase");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String filename = "Optical" + ".txt";
            File reportFile = new File(dir, filename);
            FileWriter fileWriter = new FileWriter(reportFile);
            fileWriter.append(currentStacktrace);
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {
            Log.e("ExceptionHandler", e.getMessage());
        }
    }
}