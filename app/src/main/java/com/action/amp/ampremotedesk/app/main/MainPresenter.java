package com.action.amp.ampremotedesk.app.main;

/**
 * Created by Administrator on 2017/8/17 0017.
 */
public class MainPresenter implements MainContract.Presenter {


    private MainContract.View view;


    public MainPresenter(MainContract.View view) {
        this.view = view;
        this.view.setPresenter(this);
    }

    @Override
    public void start() {

    }
}
