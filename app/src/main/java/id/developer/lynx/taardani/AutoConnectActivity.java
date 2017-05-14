package id.developer.lynx.taardani;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Set;

public class AutoConnectActivity extends AppCompatActivity {

    /**
     * View in xml
     */
    private Button btnLedOn, btnLedOff;
    private TextView textStatus;

    /**
     * Added view
     */
    private ProgressDialog PD;

    /**
     * Variable for processing bluetooth
     */
    private BluetoothAdapter bluetoothAdapter;
    private ConnectBluetoothThread connectBluetoothThread;

    /**
     * Local variable
     */
    private static int REQ_ENABLE_BLUETOOTH = 1631;
    private boolean isConnected;

    /**
     * Receive data when bluetooth device discovered
     */
    private BroadcastReceiver discoverReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice discoveredDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(Utils.LOG_TAG, "This is AutoConnectActivity => onReceive discover device : " +discoveredDevice.getName()+ " (" +discoveredDevice.getBondState()+ ")");

                // Check if the name of discovered device is HC-05
                if(discoveredDevice.getName().equals("HC-05")){

                    // Check if HC-05 has been bonded
                    if(discoveredDevice.getBondState() == BluetoothDevice.BOND_BONDED){

                        // Connect device if bonded already
                        connectBluetoothThread = new ConnectBluetoothThread(AutoConnectActivity.this, discoveredDevice, Utils.RECEIVER_CONNECTIVITY_AUTOCONNECT);
                        connectBluetoothThread.start();
                    }else{

                        // Try to bond device
                        try{
                            Method method = discoveredDevice.getClass().getMethod("createBond", (Class[]) null);
                            method.invoke(discoveredDevice, (Object[]) null);
                        }catch (Exception e){
                            Log.d(Utils.LOG_TAG, "This is AutoConnectActivity => pairDevice error : " +e.getMessage());
                            PD.hide();

                            // Show dialog if bluetooth can't be paired
                            new AlertDialog.Builder(AutoConnectActivity.this)
                                    .setMessage("Can't pair device, please pair it manually!")
                                    .setPositiveButton("Setting", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                            // Open bluetooth setting when button "Setting" pressed
                                            Intent intentOpenBluetoothSettings = new Intent();
                                            intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                                            startActivity(intentOpenBluetoothSettings);
                                        }
                                    }).setCancelable(false)
                                    .show();
                        }
                    }
                }
            }
        }
    };

    /**
     * Receive data when bluetooth paired or not paired
     */
    private BroadcastReceiver pairReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                // If bluetooth paired
                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    Toast.makeText(context, "Bluetooth paired!", Toast.LENGTH_SHORT).show();
                    relaunchApps();
                }
                // If bluetooth not paired
                else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    Toast.makeText(context, "Bluetooth not paired!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    /**
     * Receive data when :
     * Bluetooth connect or not connect
     * Arduino send serial data
     */
    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();

            // If data about bluetooth connection
            if(extras.containsKey(Utils.PARAM_STATUS_CONNECTED)){
                isConnected = extras.getBoolean(Utils.PARAM_STATUS_CONNECTED);

                bluetoothAdapter.cancelDiscovery();
                PD.hide();

                if(isConnected){
                    changeConnectionStatus();
                }else{
                    new AlertDialog.Builder(AutoConnectActivity.this)
                            .setMessage("Bluetooth can't connect, please relaunch the apps!")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    relaunchApps();
                                }
                            })
                            .setCancelable(false).show();
                }
            }
            // If receiving serial data
            else if(extras.containsKey(Utils.PARAM_SERIAL_VALUE)){
                String data = extras.getString(Utils.PARAM_SERIAL_VALUE);
                Toast.makeText(context, data, Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_connect);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        isConnected = false;

        initPD();
        initBluetooth();
        initView();
        changeConnectionStatus();

        PD.show();
    }

    private void initPD(){
        PD = new ProgressDialog(this);
        PD.setMessage("Connecting bluetooth");
    }

    private void initBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(bluetoothAdapter == null){
            new AlertDialog.Builder(this).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            }).setMessage("Bluetooth not supported!").setCancelable(false).show();
        }

        if(!bluetoothAdapter.isEnabled()){
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, REQ_ENABLE_BLUETOOTH);
        }
    }

    private void initView(){
        btnLedOn = (Button)findViewById(R.id.btn_led_on);
        btnLedOff = (Button)findViewById(R.id.btn_led_off);
        textStatus = (TextView)findViewById(R.id.text_connected);

        btnLedOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = "1" + Utils.ARDUINO_ID;
                sendData(data.getBytes(Charset.forName("UTF-8")));
            }
        });

        btnLedOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = "0" + Utils.ARDUINO_ID;
                sendData(data.getBytes(Charset.forName("UTF-8")));
            }
        });
    }

    private void changeConnectionStatus(){
        if(isConnected){
            textStatus.setText("Bluetooth connected");
            textStatus.setTextColor(ContextCompat.getColor(this, R.color.colorGreen));
        }else{
            textStatus.setText("Bluetooth not connected");
            textStatus.setTextColor(ContextCompat.getColor(this, R.color.colorRed));
        }
    }

    private void sendData(byte[] data){
        if(!isConnected){
            Toast.makeText(this, "Please connect your device!", Toast.LENGTH_SHORT).show();
            return;
        }

        connectBluetoothThread.send(data);
    }

    private void relaunchApps(){
        startActivity(new Intent(this, AutoConnectActivity.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(discoverReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(pairReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        registerReceiver(bluetoothReceiver, new IntentFilter(Utils.RECEIVER_CONNECTIVITY_AUTOCONNECT));

        if(!bluetoothAdapter.isDiscovering()) bluetoothAdapter.startDiscovery();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(discoverReceiver);
        unregisterReceiver(pairReceiver);
        unregisterReceiver(bluetoothReceiver);
        if(connectBluetoothThread != null) connectBluetoothThread.cancel();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK){
            if(requestCode == REQ_ENABLE_BLUETOOTH){
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
            }
        }else{
            if(requestCode == REQ_ENABLE_BLUETOOTH){
                finish();
            }
        }
    }
}
