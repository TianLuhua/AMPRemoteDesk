package com.action.amp.ampremotedesk.app.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.view.InputDeviceCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import com.action.amp.ampremotedesk.R;
import com.action.amp.ampremotedesk.app.Config;
import com.action.amp.ampremotedesk.app.client.ClientFragment;
import com.action.amp.ampremotedesk.app.main.MainActivity;
import com.action.amp.ampremotedesk.app.settings.SettingActivity;
import com.action.amp.ampremotedesk.app.client.ClientActivity;
import com.action.amp.ampremotedesk.app.utils.AddressUtils;
import com.action.amp.ampremotedesk.app.utils.CodecUtils;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tianluhua on 21/7/17.
 */
public class ServerService extends Service {

    private static final String TAG = "ServerService";
    private MediaCodec encoder = null;

    private int serverPort;
    private float bitrateRatio;
    private AsyncHttpServer dataServer;
    private List<WebSocket> _sockets = new ArrayList<WebSocket>();
    private Thread encoderThread = null;
    private Handler mHandler;
    private SharedPreferences preferences;
    static int deviceWidth;
    static int deviceHeight;
    private Point resolution = new Point();

    private static boolean LOCAL_DEBUG = false;
    private VideoWindow videoWindow = null;
    private VirtualDisplay virtualDisplay;

    private class ToastRunnable implements Runnable {
        String mText;

        public ToastRunnable(String text) {
            mText = text;
        }

        @Override
        public void run() {
            Toast.makeText(getApplicationContext(), mText, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Main Entry Point of the server code.
     * Create a WebSocket server and start the encoder.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() == Config.ServerServiceActionKey.ACTION_STOP) {
            dispose();
            return START_NOT_STICKY;
        }
        if (dataServer == null && intent.getAction().equals(Config.ServerServiceActionKey.ACTION_START)) {
            preferences = PreferenceManager.getDefaultSharedPreferences(this);
            LOCAL_DEBUG = preferences.getBoolean("local_debugging", false);
            DisplayMetrics dm = new DisplayMetrics();
            Display mDisplay = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            mDisplay.getMetrics(dm);
            deviceWidth = dm.widthPixels;
            deviceHeight = dm.heightPixels;
            float resolutionRatio = Float.parseFloat(
                    preferences.getString(SettingActivity.KEY_RESOLUTION_PREF, "0.25"));
            mDisplay.getRealSize(resolution);
            resolution.x = (int) (resolution.x * resolutionRatio);
            resolution.y = (int) (resolution.y * resolutionRatio);

            if (!LOCAL_DEBUG) {
                dataServer = new AsyncHttpServer();
                dataServer.websocket("/", null, websocketCallback);
                serverPort = Integer.parseInt(preferences.getString(SettingActivity.KEY_PORT_PREF, "6060"));
                bitrateRatio = Float.parseFloat(preferences.getString(SettingActivity.KEY_BITRATE_PREF, "1"));
                updateNotification("Streaming is live at");
                dataServer.listen(serverPort);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        showToast("Starting main touch server");
//                        new MainStarter(ServerService.this).start();
                        startTouchServer();
                        showToast("started main touch server");
                    }
                }).start();
            } else {
                final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);

                params.gravity = Gravity.TOP | Gravity.LEFT;
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                params.width = WindowManager.LayoutParams.WRAP_CONTENT;

                WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                videoWindow = (VideoWindow) inflater.inflate(R.layout.video_window, null);
                windowManager.addView(videoWindow, params);
                videoWindow.inflateSurfaceView();

                if (encoderThread == null) {
                    encoderThread = new Thread(new EncoderWorker(), "Encoder Thread");
                    encoderThread.start();
                }
            }

            mHandler = new Handler();
        }
        return START_NOT_STICKY;
    }

    private AsyncHttpServer.WebSocketRequestCallback websocketCallback = new AsyncHttpServer.WebSocketRequestCallback() {

        @Override
        public void onConnected(final WebSocket webSocket, AsyncHttpServerRequest request) {
            _sockets.add(webSocket);
            showToast("Someone just connected");
            //Start rendering display on the surface and setting up the encoder
            if (encoderThread == null) {
                startDisplayManager();
                encoderThread = new Thread(new EncoderWorker(), "Encoder Thread");
                encoderThread.start();
            }
            //Use this to clean up any references to the websocket
            webSocket.setClosedCallback(new CompletedCallback() {
                @Override
                public void onCompleted(Exception ex) {
                    try {
                        if (ex != null)
                            ex.printStackTrace();
                    } finally {
                        _sockets.clear();
                    }
                    showToast("Disconnected");
                    dispose();
                }
            });

            webSocket.setStringCallback(new WebSocket.StringCallback() {
                @Override
                public void onStringAvailable(String s) {
                    Log.d(TAG, "String received. No idea what to do with it.");
                }
            });

            webSocket.setDataCallback(new DataCallback() {
                @Override
                public void onDataAvailable(DataEmitter dataEmitter, ByteBufferList byteBufferList) {
                    byteBufferList.recycle();
                }
            });
        }
    };

    /**
     * Create the display surface out of the encoder. The data to encoder will be fed from this
     * Surface itself.
     *
     * @return
     * @throws IOException
     */
    @TargetApi(19)
    private Surface createDisplaySurface() throws IOException {
        MediaFormat mMediaFormat = MediaFormat.createVideoFormat(CodecUtils.MIME_TYPE,
                CodecUtils.WIDTH, CodecUtils.HEIGHT);
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, (int) (1024 * 1024 * 0.5));
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        Log.i(TAG, "Starting encoder");
        encoder = MediaCodec.createEncoderByType(CodecUtils.MIME_TYPE);
        encoder.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        Surface surface = encoder.createInputSurface();
        return surface;
    }

    @TargetApi(19)
    public void startDisplayManager() {
        DisplayManager mDisplayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        Surface encoderInputSurface = null;
        try {
            //获取encoder的数据输入的surface
            encoderInputSurface = createDisplaySurface();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            virtualDisplay = mDisplayManager.createVirtualDisplay("Remote Droid", CodecUtils.WIDTH, CodecUtils.HEIGHT, 50,
                    encoderInputSurface,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC);
        } else {
            if (MainActivity.mMediaProjection != null) {
                virtualDisplay = MainActivity.mMediaProjection.createVirtualDisplay("Remote Droid",
                        CodecUtils.WIDTH, CodecUtils.HEIGHT, 50,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        encoderInputSurface, null, null);
            } else {
                showToast("Something went wrong. Please restart the app.");
            }
        }

        encoder.start();
    }

    @TargetApi(19)
    private class EncoderWorker implements Runnable {

        @Override
        public void run() {
            startDisplayManager();
            ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();

            boolean encoderDone = false;
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            String infoString;
            while (!encoderDone) {
                int encoderStatus;
                try {
                    encoderStatus = encoder.dequeueOutputBuffer(info, CodecUtils.TIMEOUT_USEC);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    break;
                }

                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    Log.d(TAG, "no output from encoder available");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    encoderOutputBuffers = encoder.getOutputBuffers();
                    Log.d(TAG, "encoder output buffers changed");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // not expected for an encoder
                    MediaFormat newFormat = encoder.getOutputFormat();
                    Log.d(TAG, "encoder output format changed: " + newFormat);
                } else if (encoderStatus < 0) {
                    break;
                } else {
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        Log.d(TAG, "============It's NULL. BREAK!=============");
                        return;
                    }
                    if (!LOCAL_DEBUG) {
                        for (WebSocket socket : _sockets) {
                            infoString = info.offset + "," + info.size + "," +
                                    info.presentationTimeUs + "," + info.flags;
                            socket.send(infoString.getBytes());

                            byte[] b = new byte[info.size];
                            try {
                                if (info.size != 0) {
                                    //表示缓冲区的当前终点
                                    encodedData.limit(info.offset + info.size);
                                    //下一个要被读或写的元素的索引，每次读写缓冲区数据时都会改变改值，为下次读写作准备
                                    encodedData.position(info.offset);
                                   //get(byte[] dst, int offset, int length) 从position位置开始相对读，读length个byte，并写入dst下标从offset到offset+length的区域
                                    encodedData.get(b, info.offset, info.offset + info.size);
                                    socket.send(b);
                                }

                            } catch (BufferUnderflowException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        if (info.size != 0) {
                            encodedData.position(info.offset);
                            encodedData.limit(info.offset + info.size);
                        }
                        videoWindow.setData(CodecUtils.clone(encodedData), info);

                        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            Log.w(TAG, "config flag received");
                        }
                    }

                    encoderDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;

                    try {
                        encoder.releaseOutputBuffer(encoderStatus, false);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private AsyncHttpServer touchServer;
    private EventInput input;

    private void startTouchServer() {

        try {
            input = new EventInput();
        } catch (Exception e) {
            e.printStackTrace();
        }

        touchServer = new AsyncHttpServer();
        touchServer.websocket("/", null,
                new AsyncHttpServer.WebSocketRequestCallback() {

                    @Override
                    public void onConnected(WebSocket webSocket,
                                            AsyncHttpServerRequest request) {
                        Log.d(TAG, "Touch client connected");
                        webSocket.setClosedCallback(new CompletedCallback() {
                            @Override
                            public void onCompleted(Exception ex) {
                                if (ex != null) {
                                    ex.printStackTrace();
                                }
                                Log.d(TAG, "Main WebSocket closed");
                            }
                        });
                        webSocket
                                .setStringCallback(new WebSocket.StringCallback() {
                                    @Override
                                    public void onStringAvailable(String s) {
                                        Log.d(TAG, "Received string = " + s);
                                        try {
                                            JSONObject touch = new JSONObject(s);
                                            float x = Float.parseFloat(touch
                                                    .getString(Config.TouchKey.KEY_EVENT_X))
                                                    * ServerService.deviceWidth;
                                            float y = Float.parseFloat(touch
                                                    .getString(Config.TouchKey.KEY_EVENT_Y))
                                                    * ServerService.deviceHeight;
                                            String eventType = touch
                                                    .getString(Config.TouchKey.KEY_EVENT_TYPE);
                                            if (eventType
                                                    .equals(Config.TouchKey.KEY_FINGER_DOWN)) {
                                                input.injectMotionEvent(
                                                        InputDeviceCompat.SOURCE_TOUCHSCREEN,
                                                        0,
                                                        SystemClock
                                                                .uptimeMillis(),
                                                        x, y, 1.0f);
                                            } else if (eventType
                                                    .equals(Config.TouchKey.KEY_FINGER_UP)) {
                                                input.injectMotionEvent(
                                                        InputDeviceCompat.SOURCE_TOUCHSCREEN,
                                                        1,
                                                        SystemClock
                                                                .uptimeMillis(),
                                                        x, y, 1.0f);
                                            } else if (eventType
                                                    .equals(Config.TouchKey.KEY_FINGER_MOVE)) {
                                                input.injectMotionEvent(
                                                        InputDeviceCompat.SOURCE_TOUCHSCREEN,
                                                        2,
                                                        SystemClock
                                                                .uptimeMillis(),
                                                        x, y, 1.0f);
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                    }
                });
        Log.d(TAG, "Touch server listening at port 6059");
        touchServer.listen(6059);

        if (input == null) {
            Log.e(TAG, "THIS SHIT IS NULL");
        } else {
            Log.e(TAG, "THIS SHIT NOT NULL");
        }

        Log.d(TAG, "Waiting for main to finish");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dispose();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void showToast(final String message) {
        mHandler.post(new ToastRunnable(message));
    }

    /**
     * Display the notification
     *
     * @param message
     */
    private void updateNotification(String message) {
        Intent intent = new Intent(this, ServerService.class);
        intent.setAction(Config.ServerServiceActionKey.ACTION_STOP);
        PendingIntent stopServiceIntent = PendingIntent.getService(this, 0, intent, 0);
        Notification.Builder mBuilder =
                new Notification.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setOngoing(true)
                        .addAction(R.drawable.ic_media_stop, "Stop", stopServiceIntent)
                        .setContentTitle(message)
                        .setContentText(AddressUtils.getIPAddress(true) + ":" + serverPort);
        startForeground(6000, mBuilder.build());

    }

    private void dispose() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (virtualDisplay != null)
                virtualDisplay.release();
        }
        if (encoder != null) {
            encoder.signalEndOfInputStream();
            encoder.stop();
            encoder.release();
            encoder = null;
        }
        if (dataServer != null) {
            dataServer.stop();
            dataServer = null;
        }
        if (touchServer != null) {
            touchServer.stop();
            touchServer = null;
        }
        stopForeground(true);
        stopSelf();
    }
}
