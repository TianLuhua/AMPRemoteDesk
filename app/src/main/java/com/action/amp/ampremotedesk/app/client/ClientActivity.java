package com.action.amp.ampremotedesk.app.client;


import com.action.amp.ampremotedesk.R;
import com.action.amp.ampremotedesk.app.BaseFragmentActivity;
import com.action.amp.ampremotedesk.app.utils.ActivityUtils;

/**
 * Created by tianluhua on 21/7/17.
 */
public class ClientActivity extends BaseFragmentActivity {


    @Override
    protected void initFragment() {
        ClientFragment clentFragment = (ClientFragment) getSupportFragmentManager()
                .findFragmentById(R.id.contentFrame);
        if (clentFragment == null) {
            clentFragment = ClientFragment.newInstance();
            ActivityUtils.addFragmentToActivity(getSupportFragmentManager(),
                    clentFragment, R.id.contentFrame);
        }
        new ClientPresenter(clentFragment,this);
    }

    @Override
    protected int getContentLayID() {
        return R.layout.client_act;
    }


}
