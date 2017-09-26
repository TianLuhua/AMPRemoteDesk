package com.action.amp.ampremotedesk.app.service;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.action.amp.ampremotedesk.R;
import com.action.amp.ampremotedesk.app.client.ClientActivity;
import com.action.amp.ampremotedesk.app.utils.netTools.Pinger;
import com.action.amp.ampremotedesk.app.utils.netTools.model.Device;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tianluhua on 21/7/17.
 */
public class AddressInputDialog extends DialogFragment {

    public static final String KEY_ADDRESS_EXTRA = "address";
    public static final String KEY_LAST_ADDRESS_PREF = "last_address";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        final SharedPreferences prefs = getActivity().getSharedPreferences("MAIN_PREFS", Context.MODE_PRIVATE);
        String lastAddress = prefs.getString(KEY_LAST_ADDRESS_PREF, "");

        final View dialogLayout = inflater.inflate(R.layout.dialog_address_input, null);
        final EditText addressInput = (EditText) dialogLayout.findViewById(R.id.address_input);
        addressInput.setText(lastAddress);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setTitle("Enter server address");
        builder.setView(dialogLayout)
                // Add action buttons
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        String address = addressInput.getText().toString();
                        if (!address.equals("")) {
                            Intent startIntent = new Intent(getActivity(), ClientActivity.class);
                            startIntent.putExtra(KEY_ADDRESS_EXTRA, address);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString(KEY_LAST_ADDRESS_PREF, address);
                            editor.commit();
                            startActivity(startIntent);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AddressInputDialog.this.getDialog().cancel();
                    }
                }).setNeutralButton("Scanner", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                new ScannerIPTask().execute();
            }
        });
        return builder.create();
    }


    public class ScannerIPTask extends AsyncTask<Void, Void, List<Device>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.e("tlh","onPreExecute");
        }

        @Override
        protected List<Device> doInBackground(Void... voids) {
            String ipString = Pinger.getLocalIpv4Address();
            if (ipString == null){
                return new ArrayList<Device>(1);
            }
            int lastdot = ipString.lastIndexOf(".");
            ipString = ipString.substring(0, lastdot);
            List<Device> devices=Pinger.getDevicesOnNetwork(ipString);
            Log.e("tlh","doInBackground__ipString:"+ipString+";devices.size():"+devices.size());
            return devices;
        }

        @Override
        protected void onPostExecute(List<Device> devices) {
            super.onPostExecute(devices);
            Log.e("tlh","onPostExecute__devices.size():"+devices.size());
            for (Device device : devices) {
                Log.e("tlh","device.ip:"+device.getIpAddress());
            }
        }
    }

}
