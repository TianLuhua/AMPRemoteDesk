package com.action.amp.ampremotedesk.app.client;

import android.app.Activity;
import android.graphics.Point;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.action.amp.ampremotedesk.app.Config;
import com.action.amp.ampremotedesk.app.service.AddressInputDialog;
import com.action.amp.ampremotedesk.app.utils.CodecUtils;
import com.action.amp.ampremotedesk.grafika.CircularEncoderBuffer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by tianluhua on 2017/8/17 0017.
 */
public class ClientPresenter implements ClientContract.Presenter {

    private boolean firstIFrameAdded;
    private CircularEncoderBuffer encBuffer = new CircularEncoderBuffer((int) (1024 * 1024 * 0.5), 30, 7);
    private boolean decoderConfigured = false;
    private MediaCodec decoder;
    private MediaFormat format =
            MediaFormat.createVideoFormat(CodecUtils.MIME_TYPE, CodecUtils.WIDTH, CodecUtils.HEIGHT);
    private WebSocket dataSocket;
    private WebSocket touchSocket;
    private MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    private Point videoResolution = new Point();
    private String[] infoStringParts;
    private int i = -1;

    private static final String TAG ="ClientPresenter" ;
    private ClientContract.View view;
    private  Activity activity;
    private String address;

    public ClientPresenter(ClientContract.View view, Activity activity) {
        this.view = view;
        this.activity=activity;
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        try {
            decoder = MediaCodec.createDecoderByType(CodecUtils.MIME_TYPE);
            address = this.activity.getIntent().getStringExtra(AddressInputDialog.KEY_ADDRESS_EXTRA);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    doDecoderThingie();
                }
            }).start();
            AsyncHttpClient.getDefaultInstance().websocket("ws://" + address, null, new AsyncHttpClient
                    .WebSocketConnectCallback() {
                @Override
                public void onCompleted(final Exception ex, WebSocket webSocket) {

                    if (ex != null) {
                        ex.printStackTrace();
                        return;
                    }
                    dataSocket = webSocket;
                    view.showToast("Connection Completed");
                    webSocket.setClosedCallback(new CompletedCallback() {
                        @Override
                        public void onCompleted(Exception e) {
                            dataSocket = null;
                            view. showToast("Closed");
                        }
                    });
                    webSocket.setStringCallback(new WebSocket.StringCallback() {
                        public void onStringAvailable(String s) {
                            String[] parts = s.split(",");
                            try {
                                info.set(Integer.parseInt(parts[0]),
                                        Integer.parseInt(parts[1]),
                                        Long.parseLong(parts[2]),
                                        Integer.parseInt(parts[3]));
                                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                    videoResolution.x = Integer.parseInt(parts[4]);
                                    videoResolution.y = Integer.parseInt(parts[5]);
                                }
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                                Log.d(TAG, "===========Exception = " + e.getMessage() + " =================");
                                view.showToast(e.getMessage());
                            }

                        }
                    });
                    webSocket.setDataCallback(new DataCallback() {
                        @Override
                        public void onDataAvailable(DataEmitter dataEmitter, ByteBufferList byteBufferList) {
                            ++i;
                            ByteBuffer b = byteBufferList.getAll();
                            Log.d("tlh", "Received buffer = " + b);
                            if (i % 2 == 0) {
                                String temp = new String(b.array());
                                Log.d(TAG, "Received String = " + temp);
                                infoStringParts = temp.split(",");
                                info.set(Integer.parseInt(infoStringParts[0]), Integer.parseInt(infoStringParts[1]),
                                        Long.parseLong(infoStringParts[2]), Integer.parseInt(infoStringParts[3]));
                            } else {
                                setData(b,info,view.getSuface());
                            }
                            byteBufferList.recycle();
                        }
                    });
                }
            });
            String ip = address.split(":")[0];
            view.showToast("IP = " + ip);
            AsyncHttpClient.getDefaultInstance().websocket("ws://" + ip + ":6059", null,
                    new AsyncHttpClient.WebSocketConnectCallback() {
                        @Override
                        public void onCompleted(Exception ex, WebSocket tSocket) {
                            if (ex != null) {
                                ex.printStackTrace();
                                view. showToast(ex.getMessage());
                                return;
                            }
                            touchSocket = tSocket;
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     * Main decoder function which reads the encoded frames from the CircularBuffer and renders them
     * on to the Surface
     */
    public void doDecoderThingie() {
        Log.e("tlh", "doDecoderThingie");
        boolean outputDone = false;

        while (!decoderConfigured) {
        }

        if (Config.DeBug.DEBUG) Log.d(TAG, "Decoder Configured");

        while (!firstIFrameAdded) {
        }

        if (Config.DeBug.DEBUG) Log.d(TAG, "Main Body");

        int index = encBuffer.getFirstIndex();
        if (index < 0) {
            Log.e(TAG, "CircularBuffer Error");
            return;
        }
        ByteBuffer encodedFrames;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        while (!outputDone) {
            encodedFrames = encBuffer.getChunk(index, info);
            encodedFrames.limit(info.size + info.offset);
            encodedFrames.position(info.offset);

            try {
                index = encBuffer.getNextIntCustom(index);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            int inputBufIndex = decoder.dequeueInputBuffer(-1);
            if (inputBufIndex >= 0) {
                ByteBuffer inputBuf = decoder.getInputBuffer(inputBufIndex);
                inputBuf.clear();
                inputBuf.put(encodedFrames);
                decoder.queueInputBuffer(inputBufIndex, 0, info.size,
                        info.presentationTimeUs, info.flags);
            }

            if (decoderConfigured) {
                int decoderStatus = decoder.dequeueOutputBuffer(info, CodecUtils.TIMEOUT_USEC);
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (Config.DeBug.DEBUG) Log.d(TAG, "no output from decoder available");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    if (Config.DeBug.DEBUG) Log.d(TAG, "decoder output buffers changed");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // this happens before the first frame is returned
                    MediaFormat decoderOutputFormat = decoder.getOutputFormat();
                    Log.d(TAG, "decoder output format changed: " +
                            decoderOutputFormat);
                } else {
                    decoder.releaseOutputBuffer(decoderStatus, true);
                }
            }
        }
    }


    @Override
    public void setData(ByteBuffer encodedFrame, MediaCodec.BufferInfo info, Surface surface) {
        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            Log.d(TAG, "Configuring Decoder");

            format.setByteBuffer("csd-0", encodedFrame);
            decoder.configure(format, surface,null, 0);
            decoder.start();
            decoderConfigured = true;
            Log.d(TAG, "decoder configured (" + info.size + " bytes)");
            return;
        }

        encBuffer.add(encodedFrame, info.flags, info.presentationTimeUs);
        if (Config.DeBug.DEBUG) Log.d(TAG, "Adding frames to the Buffer");
        if ((info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
            firstIFrameAdded = true;
            if (Config.DeBug.DEBUG) Log.d(TAG, "First I-Frame added");
        }
    }

    @Override
    public void sendtouchData(String toucuString) {
        if (touchSocket != null) {
            touchSocket.send(toucuString);
        } else {
            Log.e(TAG, "Can't send touch events. Socket is null.");
        }
    }
}
