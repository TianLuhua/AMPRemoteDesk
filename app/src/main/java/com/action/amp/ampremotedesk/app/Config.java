package com.action.amp.ampremotedesk.app;

/**
 * Created by Administrator on 2017/8/18 0018.
 */
public class Config {
    /**
     * 调试开关
     */
    public static class DeBug {
        public static final boolean DEBUG = false;

    }

    /**
     * 触摸事件Key值
     */
    public static class TouchKey {
        public static final String KEY_FINGER_DOWN = "fingerdown";
        public static final String KEY_FINGER_UP = "fingerup";
        public static final String KEY_FINGER_MOVE = "fingermove";
        public static final String KEY_EVENT_TYPE = "type";
        public static final String KEY_EVENT_X = "x";
        public static final String KEY_EVENT_Y = "y";
    }

    /**
     * ServerService 的启动和停止的Action值
     */
    public static class ServerServiceActionKey {
        public static final String ACTION_START = "start";
        public static final String ACTION_STOP = "stop";
    }

    public static class MediaProjection {
        public static final int REQUEST_MEDIA_PROJECTION = 1;
    }
}
