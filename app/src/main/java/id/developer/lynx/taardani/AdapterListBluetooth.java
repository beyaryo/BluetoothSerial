package id.developer.lynx.taardani;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Bend on 5/12/2017.
 */

public class AdapterListBluetooth extends BaseAdapter {
    Context context;
    ArrayList<BluetoothDevice> list;

    public AdapterListBluetooth(Context context, ArrayList<BluetoothDevice> list){
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return list.indexOf(list.get(position));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.layout_paired_device, null, false);
        }

        ((TextView)convertView.findViewById(R.id.text_paired_name)).setText(list.get(position).getName());
        ((TextView)convertView.findViewById(R.id.text_paired_mac)).setText(list.get(position).getAddress());

        return convertView;
    }
}
