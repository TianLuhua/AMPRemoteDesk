package com.action.amp.ampremotedesk.app;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.action.amp.ampremotedesk.app.utils.HideSystemUIUtils;


/**
 * Created by tianluhua on 2017/8/18 0018.
 */
public abstract class BaseFragmentActivity extends FragmentActivity implements View.OnClickListener {

    private boolean mAllowFullScreen = true;
    private boolean isAllowScreenRoate = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mAllowFullScreen) {
            HideSystemUIUtils.hideSystemUI(this);
        }
        setContentView(getContentLayID());
        if (!isAllowScreenRoate) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        initFragment();
    }

    /**
     * [初始工作]
     */
    protected abstract void initFragment();

    /**
     * [设置视图]
     */
    protected abstract int getContentLayID();

    /**
     * [设置监听]
     */
    protected void widgetClick(View v) {
    }

    /**
     * [绑定控件]
     *
     * @param resId
     * @return T extends View
     */
    protected <T extends View> T $(int resId) {
        return (T) super.findViewById(resId);
    }


    @Override
    public void onClick(View v) {
        widgetClick(v);
    }


    /**
     * [页面跳转]
     *
     * @param clz
     */
    public void startActivity(Class<?> clz) {
        startActivity(new Intent(BaseFragmentActivity.this, clz));
    }


    /**
     * [携带数据的页面跳转]
     *
     * @param clz
     * @param bundle
     */
    public void startActivity(Class<?> clz, Bundle bundle) {
        Intent intent = new Intent();
        intent.setClass(this, clz);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        startActivity(intent);
    }

    /**
     * [启动服务]
     *
     * @param clz
     * @param action
     */
    public void startService(Class<?> clz, String action) {
        Intent startServerIntent = new Intent(BaseFragmentActivity.this, clz);
        if (action != null) {
            startServerIntent.setAction(action);
        }
        startService(startServerIntent);
    }

    /**
     * [是否允许全屏]
     *
     * @param mAllowFullScreen
     */
    public void setmAllowFullScreen(boolean mAllowFullScreen) {
        this.mAllowFullScreen = mAllowFullScreen;
    }

    /**
     * [是否允许屏幕旋转]
     *
     * @param isAllowScreenRoate
     */
    public void setScreenRoate(boolean isAllowScreenRoate) {
        this.isAllowScreenRoate = isAllowScreenRoate;
    }

    public void showToast(final String message) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BaseFragmentActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

}
