package id.developer.lynx.taardani;

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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

public class PairingActivity extends AppCompatActivity {

    private ListView listPairedDevices;

    private BluetoothAdapter bluetoothAdapter;

    private int REQ_ENABLE_BLUETOOTH = 151;
    private ArrayList<BluetoothDevice> bluetoothDevices;
    private BluetoothDevice selectedDevice = null;

    private BroadcastReceiver discoverReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                bluetoothDevices.add(device);
                Log.d(Utils.LOG_TAG, "This is PairingActivity => onReceive device : " +device.getName());

                synchronized (listPairedDevices.getAdapter()){
                    ((BaseAdapter)listPairedDevices.getAdapter()).notifyDataSetChanged();
                }
            }
        }
    };

    private BroadcastReceiver pairReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    Toast.makeText(context, "Bluetooth paired!", Toast.LENGTH_SHORT).show();

                    bluetoothDevices.clear();
                    bluetoothAdapter.cancelDiscovery();
                    bluetoothAdapter.startDiscovery();

                    synchronized (listPairedDevices.getAdapter()){
                        ((BaseAdapter)listPairedDevices.getAdapter()).notifyDataSetChanged();
                    }
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED){
                    Toast.makeText(context, "Bluetooth not paired!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bluetoothDevices = new ArrayList<>();

        initBluetoothAdapter();
        initView();
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
        listPairedDevices = (ListView)findViewById(R.id.list_paired_devices);

        listPairedDevices.setAdapter(new AdapterListBluetooth(this, bluetoothDevices));
        listPairedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedDevice = bluetoothDevices.get(position);

                if(selectedDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    pairDone();
                }else{
                    pairDevice();
                }
            }
        });
    }

    private void pairDevice(){
        try{
            Method method = selectedDevice.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(selectedDevice, (Object[]) null);
        }catch (Exception e){
            Log.d(Utils.LOG_TAG, "This is PairingActivity => pairDevice error : " +e.getMessage());
        }
    }

    private void pairDone(){
        Intent intent = new Intent();
        intent.putExtra(Utils.PARAM_DEVICE, selectedDevice);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(discoverReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(pairReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        bluetoothAdapter.startDiscovery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(discoverReceiver);
        unregisterReceiver(pairReceiver);
        bluetoothAdapter.cancelDiscovery();
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
