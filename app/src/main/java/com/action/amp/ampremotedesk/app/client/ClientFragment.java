package com.action.amp.ampremotedesk.app.client;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.action.amp.ampremotedesk.R;
import com.action.amp.ampremotedesk.app.BaseFragment;
import com.action.amp.ampremotedesk.app.Config;

import org.json.JSONException;
import org.json.JSONObject;



/**
 * Created by tianluhua on 2017/8/18 0018.
 */
public class ClientFragment extends BaseFragment implements ClientContract.View, SurfaceHolder.Callback, View.OnTouchListener {

    private static final String TAG = "ClientFragment";
    private static ClientFragment instance;
    private ClientContract.Presenter presenter;

    private SurfaceView surfaceView;
    private int deviceWidth;
    private int deviceHeight;


    public static ClientFragment newInstance() {
        if (instance == null) {
            instance = new ClientFragment();
        }
        return instance;
    }

    @Override
    public void setPresenter(ClientContract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    protected int getContentlayoutId() {
        return R.layout.client_frag;
    }

    @Override
    protected void init() {
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        deviceWidth = dm.widthPixels;
        deviceHeight = dm.heightPixels;
        surfaceView = (SurfaceView) rootView.findViewById(R.id.main_surface_view);
        surfaceView.getHolder().addCallback(this);
        surfaceView.setOnTouchListener(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (presenter!=null){
            Log.e("tlh", "surfaceCreated");
            presenter.start();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.e("tlh", "surfaceChanged");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.e("tlh", "surfaceDestroyed");
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        JSONObject touchData = new JSONObject();
        try {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchData.put(Config.TouchKey.KEY_EVENT_TYPE, Config.TouchKey.KEY_FINGER_DOWN);
                    break;
                case MotionEvent.ACTION_MOVE:
                    touchData.put(Config.TouchKey.KEY_EVENT_TYPE, Config.TouchKey.KEY_FINGER_MOVE);
                    break;
                case MotionEvent.ACTION_UP:
                    touchData.put(Config.TouchKey.KEY_EVENT_TYPE, Config.TouchKey.KEY_FINGER_UP);
                    break;
                default:
                    return true;
            }
            touchData.put(Config.TouchKey.KEY_EVENT_X, motionEvent.getX() / deviceWidth);
            touchData.put(Config.TouchKey.KEY_EVENT_Y, motionEvent.getY() / deviceHeight);
            Log.d(TAG, "Sending = " + touchData.toString());
            if (presenter != null) {
                presenter.sendtouchData(touchData.toString());
            } else {
                Log.e(TAG, "Can't send touch events. presenter is null.");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }


    @Override
    public  void showToast(final String msg){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
            }
        });

    }

    @Override
    public Surface getSuface() {
        return surfaceView.getHolder().getSurface();
    }


}
