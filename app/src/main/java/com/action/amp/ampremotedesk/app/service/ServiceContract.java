package com.action.amp.ampremotedesk.app.service;

import com.action.amp.ampremotedesk.app.BasePresenter;
import com.action.amp.ampremotedesk.app.BaseView;

/**
 * Created by tianluhua on 2017/8/17 0017.
 */
public interface ServiceContract {

    interface View extends BaseView<Presenter> {

    }
    interface Presenter extends BasePresenter {

    }
}
