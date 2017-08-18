package com.action.amp.ampremotedesk.app.client;

import android.media.MediaCodec;
import android.view.Surface;

import com.action.amp.ampremotedesk.app.BasePresenter;
import com.action.amp.ampremotedesk.app.BaseView;

import java.nio.ByteBuffer;

/**
 * Created by tianluhua on 2017/8/17 0017.
 */
public interface ClientContract {


    interface View extends BaseView<Presenter> {
        void showToast(String msg);
        Surface getSuface();
    }

    interface Presenter extends BasePresenter {
        void setData(ByteBuffer encodedFrame, MediaCodec.BufferInfo info, Surface surface);
        void sendtouchData(String toucuString);
    }

}
