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
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private Button btnLedOn, btnLedOff, btnConnect;
    private TextView textConnected;
    private boolean isConnected = false;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice selectedDevice = null;
    private ConnectBluetoothThread connectThread;

    private ProgressDialog PD;

    private int REQ_ENABLE_BLUETOOTH = 151;
    private int REQ_CONNECT_BLUETOOTH = 713;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            boolean isBluetoothPaired = extras.getBoolean(Utils.PARAM_STATUS_CONNECTED);
            PD.hide();

            if(isBluetoothPaired){
                isConnected = true;
                changeConnectionStatus();
            }else{
                isConnected = false;
                changeConnectionStatus();
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage("You need to pair your device with " +selectedDevice.getName()+ " to continue!")
                        .setPositiveButton("Ok", null)
                        .show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initPD();
        initBluetoothAdapter();
        initView();
    }

    private void initPD(){
        PD = new ProgressDialog(this);
        PD.setCancelable(false);
    }

    private void initBluetoothAdapter(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(bluetoothAdapter == null){
            new AlertDialog.Builder(this).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            }).setMessage("Bluetooth not supported!").show();
        }

        if(!bluetoothAdapter.isEnabled()){
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, REQ_ENABLE_BLUETOOTH);
        }
    }

    private void initView(){
        btnConnect = (Button)findViewById(R.id.btn_connect);
        btnLedOn = (Button)findViewById(R.id.btn_led_on);
        btnLedOff = (Button)findViewById(R.id.btn_led_off);
        textConnected = (TextView)findViewById(R.id.text_connected);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PairingActivity.class);
                startActivityForResult(intent, REQ_CONNECT_BLUETOOTH);
            }
        });

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
            textConnected.setText("Bluetooth connected");
            textConnected.setTextColor(ContextCompat.getColor(this, R.color.colorGreen));
        }else{
            textConnected.setText("Bluetooth not connected");
            textConnected.setTextColor(ContextCompat.getColor(this, R.color.colorRed));
        }
    }

    private void sendData(byte[] data){
        if(!isConnected){
            errorNotConnected();
            return;
        }

        connectThread.send(data);
    }
    
    private void errorNotConnected(){
        Toast.makeText(this, "Please connect your device!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, new IntentFilter(Utils.RECEIVER_INTENT_MAIN));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(connectThread != null){
            connectThread.cancel();
        }
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == REQ_ENABLE_BLUETOOTH){
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
            }else if(requestCode == REQ_CONNECT_BLUETOOTH){
                selectedDevice = data.getExtras().getParcelable(Utils.PARAM_DEVICE);

                PD.setMessage("Connecting to " +selectedDevice.getName());
                PD.show();

                connectThread = new ConnectBluetoothThread(this, selectedDevice, Utils.RECEIVER_INTENT_MAIN);
                connectThread.start();
            }
        }else{
            if(requestCode == REQ_ENABLE_BLUETOOTH){
                finish();
            }else if(requestCode == REQ_CONNECT_BLUETOOTH){
                isConnected = false;
                changeConnectionStatus();
            }
        }
    }
}
