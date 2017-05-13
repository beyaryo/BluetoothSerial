package id.developer.lynx.taardani;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Bend on 5/13/2017.
 */

public class SendReadBluetoothThread extends Thread {

    private BluetoothSocket mSocket;
    private InputStream mInStream;
    private OutputStream mOutStream;

    public SendReadBluetoothThread(BluetoothSocket socket){
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
//        byte[] buffer = new byte[1024];
//        int begin = 0;
//        int bytes = 0;

//        while(true){
//            try {
//                bytes += mInStream.read(buffer, bytes, buffer.length - bytes);
//
//                for (int i = begin; i < bytes; i++) {
//
//                }
//            }
//        }
    }

    public void send(byte[] bytes){
        try{
            mOutStream.write(bytes);
        }catch (Exception e){
            Log.d(Utils.LOG_TAG, "This is SendReadBluetoothThread => send error : " +e.getMessage());
        }
    }

    public void send(int data){
        try{
            mOutStream.write(data);
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
