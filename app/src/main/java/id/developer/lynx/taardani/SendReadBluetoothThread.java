package id.developer.lynx.taardani;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Bend on 5/13/2017.
 */

public class SendReadBluetoothThread extends Thread {

    private String intentFilter;
    private Context context;
    private BluetoothSocket mSocket;
    private InputStream mInStream;
    private OutputStream mOutStream;

    public SendReadBluetoothThread(Context context, BluetoothSocket socket, String intentFilter){
        this.intentFilter = intentFilter;
        this.context = context;
        this.mSocket = socket;
        InputStream tempIn = null;
        OutputStream tempOut = null;

        try{
            tempIn = mSocket.getInputStream();
            tempOut = mSocket.getOutputStream();
        }catch (Exception e){
            Log.d(Utils.LOG_TAG, "This is SendReadBluetoothThread => SendReadBluetoothThread error : " +e.getMessage());
        }

        this.mInStream = tempIn;
        this.mOutStream = tempOut;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes = 0;

        while(true){
            try {
                bytes = mInStream.read(buffer);
                String message = new String(buffer, 0, bytes);
                String arr[] = message.split(":");

                if(arr.length >= 2){
                    Log.d(Utils.LOG_TAG, "This is SendReadBluetoothThread => run message : " +arr[1]+ " length : " +message.length());
                    Intent intent = new Intent(intentFilter);
                    intent.putExtra(Utils.PARAM_SERIAL_VALUE, arr[1]);
                    context.sendBroadcast(intent);
                }
            }catch(IOException e) {
                Log.d(Utils.LOG_TAG, "This is SendReadBluetoothThread => run error : " +e.getMessage());
                break;
            }
        }
    }

    public void send(byte[] bytes){
        try{
            mOutStream.write(bytes);
        }catch (Exception e){
            Log.d(Utils.LOG_TAG, "This is SendReadBluetoothThread => send error : " +e.getMessage());
        }
    }

    public void cancel(){
        try{
            mSocket.close();
        }catch (Exception e){
            Log.d(Utils.LOG_TAG, "This is SendReadBluetoothThread => cancel error : " +e.getMessage());
        }
    }
}
