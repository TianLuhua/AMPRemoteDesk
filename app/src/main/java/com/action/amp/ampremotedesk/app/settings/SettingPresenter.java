package com.action.amp.ampremotedesk.app.settings;

/**
 * Created by Administrator on 2017/8/17 0017.
 */
public class SettingPresenter implements SettingContract.Presenter {


    private SettingContract.View view;

    public SettingPresenter(SettingContract.View view) {
        this.view = view;
        this.view.setPresenter(this);
    }

    @Override
    public void start() {

    }
}
