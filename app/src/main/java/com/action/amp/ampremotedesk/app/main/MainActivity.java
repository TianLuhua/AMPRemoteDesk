package com.action.amp.ampremotedesk.app.main;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.action.amp.ampremotedesk.R;
import com.action.amp.ampremotedesk.app.Config;
import com.action.amp.ampremotedesk.app.service.AddressInputDialog;
import com.action.amp.ampremotedesk.app.service.ServerService;
import com.action.amp.ampremotedesk.app.settings.SettingActivity;

/**
 * Created by tianluhua on 21/7/17.
 */
public class MainActivity extends Activity implements MainContract.View {

    public static final String TAG = "MainActivity";

    public static MediaProjection mMediaProjection;
    private MediaProjectionManager mMediaProjectionManager;
    private MainContract.Presenter presenter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMediaProjectionManager = (MediaProjectionManager)
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Config.MediaProjection.REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, "User cancelled the access", Toast.LENGTH_SHORT).show();
                return;
            }
            mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
            Intent startServerIntent = new Intent(MainActivity.this, ServerService.class);
            startServerIntent.setAction(Config.ServerServiceActionKey.ACTION_START);
            startService(startServerIntent);
        }
    }

    public void startClient(View v) {
        new AddressInputDialog().show(getFragmentManager(), "Address Dialog");
    }

    public void startServer(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startScreenCapture();
        } else {
            new StartServerServiceDialog().show(getFragmentManager(), "Start service");
        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startScreenCapture() {
        startActivityForResult(
                mMediaProjectionManager.createScreenCaptureIntent(),
                Config.MediaProjection.REQUEST_MEDIA_PROJECTION);
    }

    @Override
    public void setPresenter(MainContract.Presenter presenter) {
        this.presenter = presenter;

    }

    @SuppressLint("ValidFragment")
    private class StartServerServiceDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Notice");
            builder.setMessage("For using the server mode, the device MUST be rooted and the app MUST be installed " +
                    "to \\system partition");
            builder.setPositiveButton("Start Server", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent startServerIntent = new Intent(MainActivity.this, ServerService.class);
                    startServerIntent.setAction(Config.ServerServiceActionKey.ACTION_START);
                    startService(startServerIntent);
//                    finish();
                }
            });

            return builder.create();
        }
    }
}
