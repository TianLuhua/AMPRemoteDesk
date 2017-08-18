package com.action.amp.ampremotedesk.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;


/**
 * Created by tianluhua on 2017/8/18 0018.
 */
public abstract class BaseActivity extends Activity implements View.OnClickListener {

    private boolean mAllowFullScreen=true;
    private boolean isAllowScreenRoate=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mAllowFullScreen) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        setContentView(getContentLayID());
        if (!isAllowScreenRoate) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    /**
     * [设置视图]
     */
    protected abstract int getContentLayID();
    /**
     * [设置监听]
     */
    protected abstract void widgetClick(View v);

    /**
     * [绑定控件]
     *
     * @param resId
     *
     * @return T extends View
     */
    protected    <T extends View> T $(int resId) {
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
        startActivity(new Intent(BaseActivity.this,clz));
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
    public  void startService(Class<?> clz,String action){
        Intent startServerIntent = new Intent(BaseActivity.this, clz);
        if(action!=null){
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

}
