package com.action.amp.ampremotedesk.app;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * Created by tianluhua on 2017/8/18 0018.
 */
public abstract class BaseFragment<T extends BasePresenter> extends Fragment {


    protected Context mContext;
    protected View rootView;
    protected T mPresenter;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //绑定View
        rootView = inflater.inflate(getContentlayoutId(), container, false);
        init();
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        this.mContext = context;
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mContext = null;
    }

    public void showToast(final String message) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
            }
        });
    }


    protected abstract int getContentlayoutId();

    protected abstract void init();
}
