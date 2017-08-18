package com.action.amp.ampremotedesk.app.client;

import android.support.v4.app.Fragment;


/**
 * Created by tianluhua on 2017/8/18 0018.
 */
public class ClientFragment extends Fragment implements ClientContract.View {


    private ClientContract.Presenter presenter;

    @Override
    public void setPresenter(ClientContract.Presenter presenter) {

        this.presenter = presenter;
    }


}
