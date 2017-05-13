package id.developer.lynx.taardani;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.UUID;

/**
 * Created by Bend on 5/12/2017.
 */

public class ConnectBluetoothThread extends Thread {

    private BluetoothSocket mSocket;
    private BluetoothDevice mDevice;
    private Context context;
    private SendReadBluetoothThread sendReadThread;

    public ConnectBluetoothThread(Context context, BluetoothDevice device){
        BluetoothSocket temp = null;
        this.context = context;
        this.mDevice = device;

        try{
            temp = device.createRfcommSocketToServiceRecord(device.getUuids()[0].getUuid());
        }catch (Exception e){
            Log.d(Utils.LOG_TAG, "This is ConnectBluetoothThread => ConnectBluetoothThread error : " +e.getMessage());
        }

        this.mSocket = temp;
    }

    @Override
    public void run(){
        Intent intentStatus = new Intent(Utils.RECEIVER_INTENT_MAIN);

        try{
            mSocket.connect();
            Log.d(Utils.LOG_TAG, "This is ConnectBluetoothThread => run connected");
            intentStatus.putExtra(Utils.PARAM_STATUS_CONNECTED, true);
            context.sendBroadcast(intentStatus);

            sendReadThread = new SendReadBluetoothThread(mSocket);
            sendReadThread.start();
        }catch (Exception e){
            try{
                mSocket.close();
            }catch (Exception closeException){}
            Log.d(Utils.LOG_TAG, "This is ConnectBluetoothThread => run error : " +e.getMessage());
            intentStatus.putExtra(Utils.PARAM_STATUS_CONNECTED, false);
            context.sendBroadcast(intentStatus);
            return;
        }
    }

    public void send(int data){
        sendReadThread.send(data);
    }

    public void send(byte[] bytes){
        sendReadThread.send(bytes);
    }

    public void cancel(){
        try{
            mSocket.close();
        }catch (Exception e){
            Log.d(Utils.LOG_TAG, "This is ConnectBluetoothThread => cancel error : " +e.getMessage());
        }
    }
}
