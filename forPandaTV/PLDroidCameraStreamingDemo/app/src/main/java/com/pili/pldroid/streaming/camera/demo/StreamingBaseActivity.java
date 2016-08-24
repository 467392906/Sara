package com.pili.pldroid.streaming.camera.demo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.pili.pldroid.streaming.CameraStreamingManager;
import com.pili.pldroid.streaming.CameraStreamingSetting;
import com.pili.pldroid.streaming.FrameCapturedCallback;
import com.pili.pldroid.streaming.MicrophoneStreamingSetting;
import com.pili.pldroid.streaming.StreamStatusCallback;
import com.pili.pldroid.streaming.StreamingPreviewCallback;
import com.pili.pldroid.streaming.StreamingProfile;
import com.pili.pldroid.streaming.SurfaceTextureCallback;
import com.pili.pldroid.streaming.camera.demo.gles.CameraInputRender;
import com.pili.pldroid.streaming.camera.demo.gles.FBO;
import com.pili.pldroid.streaming.camera.demo.gles.GlUtil;
import com.pili.pldroid.streaming.camera.demo.ui.RotateLayout;
import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.NetworkInfo;
import com.qiniu.android.dns.http.DnspodFree;
import com.qiniu.android.dns.local.AndroidDnsServer;
import com.qiniu.android.dns.local.Resolver;
import com.sensetime.stmobile.STBeautyParamsType;
import com.sensetime.stmobile.STImageFilterNative;
import com.sensetime.stmobile.STImageFormat;
import com.sensetime.stmobile.STMobileStickerNative;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by jerikc on 15/7/6.
 */
public class StreamingBaseActivity extends Activity implements
        View.OnLayoutChangeListener,
        StreamStatusCallback,
        StreamingPreviewCallback,
        SurfaceTextureCallback,
        CameraPreviewFrameView.Listener,
        CameraStreamingManager.StreamingSessionListener,
        CameraStreamingManager.StreamingStateListener,
        STMobileStickerNative.ItemCallback {

    private static final String TAG = "StreamingBaseActivity";

    private static final int ZOOM_MINIMUM_WAIT_MILLIS = 33; //ms

    private Context mContext;

    protected Button mShutterButton;
    private Button mMuteButton;
    private Button mTorchBtn;
    private Button mCameraSwitchBtn;
    private Button mCaptureFrameBtn;
    private Button mEncodingOrientationSwitcherBtn;
    private RotateLayout mRotateLayout;

    protected TextView mSatusTextView;
    private TextView mLogTextView;
    private TextView mStreamStatus;
    private TextView mcallbackStatus;

    protected boolean mShutterButtonPressed = false;
    private boolean mIsTorchOn = false;
    private boolean mIsNeedMute = false;
    private boolean isEncOrientationPort = true;

    protected static final int MSG_START_STREAMING  = 0;
    protected static final int MSG_STOP_STREAMING   = 1;
    private static final int MSG_SET_ZOOM           = 2;
    private static final int MSG_MUTE               = 3;

    protected String mStatusMsgContent;

    protected String mLogContent = "\n";

    private View mRootView;

    protected CameraStreamingManager mCameraStreamingManager;
    protected CameraStreamingSetting mCameraStreamingSetting;
    protected MicrophoneStreamingSetting mMicrophoneStreamingSetting;
    protected StreamingProfile mProfile;
    protected JSONObject mJSONObject;
    private boolean mOrientationChanged = false;

    protected boolean mIsReady = false;

    private int mCurrentZoom = 0;
    private int mMaxZoom = 0;

    private FBO mFBO = new FBO();

    private STImageFilterNative mStImageFilterNative;
    private STMobileStickerNative mStStickerNative;
    private static final int ST_MATERIAL_BEGIN = 0;      ///< 开始渲染素材
    private static final int ST_MATERIAL_PROCESS = 1;    ///< 素材渲染中
    private static final int ST_MATERIAL_END = 2;         ///< 素材未被渲染

    private int[] mTextureOutId;
    private int[] mMidTextureId;
    private  CameraInputRender mCameraInputRender;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private ByteBuffer mCameraBuffer;
    private Button mChangeStickerBtn;
    private int mPreviewSizeWidth;
    private int mPreviewSizeHeight;

    private Accelerometer mAccelerometer;

    private List<String> mStickerFilesList;
    private int mCurrentStickerNum = 0 ;

    private int mFrontCameraOrientation;
    private int mBackCameraOrientation;

    private String mStickerFolderPath = null;

    private Screenshooter mScreenshooter = new Screenshooter();
    private EncodingOrientationSwitcher mEncodingOrientationSwitcher = new EncodingOrientationSwitcher();

    private int processors = Runtime.getRuntime().availableProcessors();
    ExecutorService executor = Executors.newFixedThreadPool(processors);
    private int initStickerResult = 0;

    protected Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_STREAMING:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // disable the shutter button before startStreaming
                            setShutterButtonEnabled(false);
                            boolean res = mCameraStreamingManager.startStreaming();
                            mShutterButtonPressed = true;
                            Log.i(TAG, "res:" + res);
                            if (!res) {
                                mShutterButtonPressed = false;
                                setShutterButtonEnabled(true);
                            }
                            setShutterButtonPressed(mShutterButtonPressed);
                        }
                    }).start();
                    break;
                case MSG_STOP_STREAMING:
                    // disable the shutter button before stopStreaming
                    setShutterButtonEnabled(false);
                    boolean res = mCameraStreamingManager.stopStreaming();
                    if (!res) {
                        mShutterButtonPressed = true;
                        setShutterButtonEnabled(true);
                    }
                    setShutterButtonPressed(mShutterButtonPressed);
                    break;
                case MSG_SET_ZOOM:
                    mCameraStreamingManager.setZoomValue(mCurrentZoom);
                    break;
                case MSG_MUTE:
                    mIsNeedMute = !mIsNeedMute;
                    mCameraStreamingManager.mute(mIsNeedMute);
                    updateMuteButtonText();
                    break;
                default:
                    Log.e(TAG, "Invalid message");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        } else {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Config.SCREEN_ORIENTATION == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            isEncOrientationPort = true;
        } else if (Config.SCREEN_ORIENTATION == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            isEncOrientationPort = false;
        }
        setRequestedOrientation(Config.SCREEN_ORIENTATION);

        setContentView(R.layout.activity_camera_streaming);
//
//        SharedLibraryNameHelper.getInstance().renameSharedLibrary(
//                SharedLibraryNameHelper.PLSharedLibraryType.PL_SO_TYPE_AAC,
//                getApplicationInfo().nativeLibraryDir + "/libpldroid_streaming_aac_encoder_v7a.so");
//
//        SharedLibraryNameHelper.getInstance().renameSharedLibrary(
//                SharedLibraryNameHelper.PLSharedLibraryType.PL_SO_TYPE_CORE, "pldroid_streaming_core");
//
//        SharedLibraryNameHelper.getInstance().renameSharedLibrary(
//                SharedLibraryNameHelper.PLSharedLibraryType.PL_SO_TYPE_H264, "pldroid_streaming_h264_encoder_v7a");

        //    String streamJsonStrFromServer = getIntent().getStringExtra(Config.EXTRA_KEY_STREAM_JSON);
        String streamJsonStrFromServer = "{\"id\":\"z1.NIU7PS.shangtang-test\",\"createdAt\":\"2016-06-14T03:32:11.534Z\",\"updatedAt\":\"2016-06-14T03:32:11.534Z\",\"title\":\"shangtang-test\",\"hub\":\"NIU7PS\",\"disabled\":false,\"publishKey\":\"efdbc36f-8759-44c2-bdd8-873521b6724a\",\"publishSecurity\":\"static\",\"hosts\":{\"publish\":{\"rtmp\":\"pili-publish.ps.qiniucdn.com\"},\"live\":{\"hdl\":\"pili-live-hdl.ps.qiniucdn.com\",\"hls\":\"pili-live-hls.ps.qiniucdn.com\",\"http\":\"pili-live-hls.ps.qiniucdn.com\",\"rtmp\":\"pili-live-rtmp.ps.qiniucdn.com\",\"snapshot\":\"1000058.live1-snapshot.z1.pili.qiniucdn.com\"},\"playback\":{\"hls\":\"pili-playback.ps.qiniucdn.com\",\"http\":\"pili-playback.ps.qiniucdn.com\"}}}\n";
        Log.i(TAG, "streamJsonStrFromServer:" + streamJsonStrFromServer);

        try {
            mJSONObject = new JSONObject(streamJsonStrFromServer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mFrontCameraOrientation = getIntent().getIntExtra("frontcamera_orientation",3);
        mBackCameraOrientation = getIntent().getIntExtra("backcamera_orientation",3);

        mContext = this;

        StreamingProfile.AudioProfile aProfile = new StreamingProfile.AudioProfile(44100, 96 * 1024);
        StreamingProfile.VideoProfile vProfile = new StreamingProfile.VideoProfile(30, 1000 * 1024, 48);
        StreamingProfile.AVProfile avProfile = new StreamingProfile.AVProfile(vProfile, aProfile);

        mProfile = new StreamingProfile();

        StreamingProfile.Stream stream = new StreamingProfile.Stream(mJSONObject);
        mProfile.setStream(stream);
        mProfile.setVideoQuality(StreamingProfile.VIDEO_QUALITY_HIGH3)
                .setAudioQuality(StreamingProfile.AUDIO_QUALITY_MEDIUM2)
//                .setPreferredVideoEncodingSize(960, 544)
                .setEncodingSizeLevel(Config.ENCODING_LEVEL)
                .setEncoderRCMode(StreamingProfile.EncoderRCModes.QUALITY_PRIORITY)
                .setAVProfile(avProfile)
                .setDnsManager(getMyDnsManager())
                .setStreamStatusConfig(new StreamingProfile.StreamStatusConfig(3))
//                .setEncodingOrientation(StreamingProfile.ENCODING_ORIENTATION.PORT)
                .setSendingBufferProfile(new StreamingProfile.SendingBufferProfile(0.2f, 0.8f, 3.0f, 20 * 1000));

        mCameraStreamingSetting = new CameraStreamingSetting();
        mCameraStreamingSetting.setCameraId(Camera.CameraInfo.CAMERA_FACING_FRONT)
                .setContinuousFocusModeEnabled(true)
                .setRecordingHint(false)
                .setResetTouchFocusDelayInMs(3000)
//                .setFocusMode(CameraStreamingSetting.FOCUS_MODE_CONTINUOUS_PICTURE)
                .setCameraPrvSizeLevel(CameraStreamingSetting.PREVIEW_SIZE_LEVEL.SMALL)
                .setCameraPrvSizeRatio(CameraStreamingSetting.PREVIEW_SIZE_RATIO.RATIO_16_9);
        mMicrophoneStreamingSetting = new MicrophoneStreamingSetting();
        mMicrophoneStreamingSetting.setBluetoothSCOEnabled(false);

        initUIs();

        mChangeStickerBtn = (Button)findViewById(R.id.change_sticker);
        mChangeStickerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(initStickerResult == 0) {
                    executor.execute(new ChangeStickerTask());
                }
            }
        });

        initStickerFiles();
        mStImageFilterNative = new STImageFilterNative();
        mStStickerNative = new STMobileStickerNative();
        STMobileStickerNative.setCallback(this);
        mCameraInputRender = new CameraInputRender();

        initAccelerometer();
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "tid:" + Thread.currentThread().getId());
        try {
            mCameraStreamingManager.resume();
        } catch (Exception e) {
            Toast.makeText(StreamingBaseActivity.this, "Device open error!", Toast.LENGTH_SHORT).show();
        }
        startAccelerometer();
        copyModelIfNeed("face_action.model");
        copyModelIfNeed("MOBILESDK_FD681B7F-82D2-4917-B9B7-E0DB5D8D33ED.lic");
        //    copyModelIfNeed("rabbit.zip");
    }

    @Override
    protected void onPause() {
        super.onPause();

        mIsReady = false;
        mShutterButtonPressed = false;
        mCameraStreamingManager.pause();
        mHandler.removeCallbacksAndMessages(null);

        stopAccelerometer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraStreamingManager.destroy();
        executor.shutdown();
    }

    protected void setShutterButtonPressed(final boolean pressed) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mShutterButtonPressed = pressed;
                mShutterButton.setPressed(pressed);
            }
        });
    }

    protected void setShutterButtonEnabled(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mShutterButton.setFocusable(enable);
                mShutterButton.setClickable(enable);
                mShutterButton.setEnabled(enable);
            }
        });
    }

    protected void startStreaming() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_START_STREAMING), 50);
    }

    protected void stopStreaming() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_STOP_STREAMING), 50);
    }

    @Override
    public boolean onRecordAudioFailedHandled(int err) {
        mCameraStreamingManager.updateEncodingType(CameraStreamingManager.EncodingType.SW_VIDEO_CODEC);
        mCameraStreamingManager.startStreaming();
        return true;
    }

    @Override
    public boolean onRestartStreamingHandled(int err) {
        Log.i(TAG, "onRestartStreamingHandled");
        return mCameraStreamingManager.startStreaming();
    }

    @Override
    public Camera.Size onPreviewSizeSelected(List<Camera.Size> list) {
        Camera.Size size = null;
        if (list != null) {
            for (Camera.Size s : list) {
                size = s;
                Log.i(TAG, "w:" + s.width + ", h:" + s.height);
                //               break;
                if (s.height == 720) {
                    size = s;
                    break;
                } else {
                    continue;
                }
            }
        }
        Log.e(TAG, "selected size :" + size.width + "x" + size.height);
        mPreviewSizeWidth = size.width;
        mPreviewSizeHeight = size.height;
        return size;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.i(TAG, "onSingleTapUp X:" + e.getX() + ",Y:" + e.getY());

        if (mIsReady) {
            setFocusAreaIndicator();
            mCameraStreamingManager.doSingleTapUp((int) e.getX(), (int) e.getY());
            return true;
        }
        return false;
    }

    @Override
    public boolean onZoomValueChanged(float factor) {
        if (mIsReady && mCameraStreamingManager.isZoomSupported()) {
            mCurrentZoom = (int) (mMaxZoom * factor);
            mCurrentZoom = Math.min(mCurrentZoom, mMaxZoom);
            mCurrentZoom = Math.max(0, mCurrentZoom);

            Log.d(TAG, "zoom ongoing, scale: " + mCurrentZoom + ",factor:" + factor + ",maxZoom:" + mMaxZoom);
            if (!mHandler.hasMessages(MSG_SET_ZOOM)) {
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SET_ZOOM), ZOOM_MINIMUM_WAIT_MILLIS);
                return true;
            }
        }
        return false;
    }

    private Switcher mSwitcher = new Switcher();

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        Log.i(TAG, "view!!!!:" + v);
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {

    }

    @Override
    public boolean onPreviewFrame(byte[] bytes, int width, int height) {
//        deal with the yuv data.
//        long start = System.currentTimeMillis();
//        for (int i = 0; i < bytes.length; i++) {
//            bytes[i] = 0x00;
//        }
//        Log.i(TAG, "old onPreviewFrame cost :" + (System.currentTimeMillis() - start));

        Log.i(TAG,"the height11 is "+height+" , the width is "+width);
        mStImageFilterNative.processBufferWithNewContext(bytes, STImageFormat.ST_PIX_FMT_NV21,width,height,bytes,STImageFormat.ST_PIX_FMT_NV21);
        return true;
    }

    @Override
    public void onSurfaceCreated() {
        Log.i(TAG, "onSurfaceCreated");
        mFBO.initialize(this);
        initSticker();
        mCameraInputRender.init();
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged width:" + width + ",height:" + height);
        mFBO.updateSurfaceSize(width, height);
        int result = mStImageFilterNative.initBeautify(mPreviewSizeWidth,mPreviewSizeHeight);
        Log.i(TAG,"the result is for initBeautify "+result);
        mStImageFilterNative.setParam(STBeautyParamsType.ST_BEAUTIFY_CONTRAST_STRENGTH, 7/7);
        mStImageFilterNative.setParam(STBeautyParamsType.ST_BEAUTIFY_SMOOTH_STRENGTH, 7/7);
        mStImageFilterNative.setParam(STBeautyParamsType.ST_BEAUTIFY_TONE_STRENGTH, 7/7);

        mCameraInputRender.onDisplaySizeChanged(width, height);
        mCameraInputRender.onOutputSizeChanged(mPreviewSizeWidth, mPreviewSizeHeight);
        mSurfaceWidth = width;
        mSurfaceHeight = height;

        mCameraInputRender.initCameraFrameBuffer(mPreviewSizeWidth, mPreviewSizeHeight);
    }

    @Override
    public void onSurfaceDestroyed() {
        Log.i(TAG, "onSurfaceDestroyed");
        mFBO.release();

        mCameraInputRender.destroyFramebuffers();

        mStStickerNative.destoryInstance();
        if(mMidTextureId != null){
            GLES20.glDeleteTextures(1, mMidTextureId, 0);
            mMidTextureId = null;
        }

        if(mTextureOutId != null){
            GLES20.glDeleteTextures(1, mTextureOutId, 0);
            mTextureOutId = null;
        }
    }

    @Override
    public int onDrawFrame(int texId, int texWidth, int texHeight, float[] transformMatrix) {
        // newTexId should not equal with texId. texId is from the SurfaceTexture.
        // Otherwise, there is no filter effect.
        /*
        int newTexId = mFBO.drawFrame(texId, texWidth, texHeight);
        Log.i(TAG, "onDrawFrame texId:" + texId + ",newTexId:" + newTexId + ",texWidth:" + texWidth + ",texHeight:" + texHeight);
        return newTexId;
        */
        long startTime = System.currentTimeMillis();
        if(mCameraBuffer == null)
        {
            mCameraBuffer = ByteBuffer.allocate(texWidth*texHeight*4);
        }
        if(mTextureOutId == null)
        {
            mTextureOutId = new int[1];
            GlUtil.initEffectTexture(texWidth,texHeight,mTextureOutId);
        }

        if(mMidTextureId == null)
        {
            mMidTextureId = new int[1];
            GlUtil.initEffectTexture(texWidth,texHeight,mMidTextureId);
        }

        if(mCameraBuffer != null) {
            mCameraBuffer.rewind();
        }
        float[] identityMatrix = new float[16];
        Matrix.setIdentityM(identityMatrix, 0);
        mCameraInputRender.setTextureTransformMatrix(identityMatrix);
        int textureID = mCameraInputRender.onDrawToTexture(texId,mCameraBuffer);
        long afterSrcRenderTime = System.currentTimeMillis();
        Log.i(TAG,"onDrawFrame, the time for camera source data render is "+ (afterSrcRenderTime-startTime));

        int result = -1;
        result = mStImageFilterNative.processTexture(textureID, texWidth, texHeight, mMidTextureId[0]);

        long afterBeautyTime = System.currentTimeMillis();
        Log.i(TAG,"onDrawFrame, the time for Beauty is "+ (afterBeautyTime -afterSrcRenderTime));

        int dir = Accelerometer.getDirection();

        boolean needMirror = false;

        if(mCameraStreamingSetting.getReqCameraId()==Camera.CameraInfo.CAMERA_FACING_FRONT){
            //  if((dir & 1) == 1) {
            if(((mFrontCameraOrientation == 270 && (dir & 1) == 1) ||
                    (mFrontCameraOrientation == 90 && (dir & 1) == 0))){
                dir = (dir ^ 2);
            }
            needMirror = true;
            result = mStStickerNative.processTexture(mMidTextureId[0],mCameraBuffer.array(),dir,texWidth,texHeight,needMirror,mTextureOutId[0]);
        }
        else {
            if(mBackCameraOrientation == 270){
                dir = (dir ^ 2);
            }
            result = mStStickerNative.processTexture(mMidTextureId[0],mCameraBuffer.array(),dir,texWidth,texHeight,needMirror,mTextureOutId[0]);
        }

        long afterStickerTime = System.currentTimeMillis();
        Log.i(TAG,"onDrawFrame, the time for whole operation is "+ (afterStickerTime - startTime));

        Log.i(TAG,"the result is "+result+" the width is "+texWidth+",the height is "+texHeight);
        GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
        //     return mMidTextureId[0];
        return mTextureOutId[0];

/*
       return textureID;
       */
    }

    @Override
    public void notifyStreamStatusChanged(final StreamingProfile.StreamStatus streamStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStreamStatus.setText("bitrate:" + streamStatus.totalAVBitrate / 1024 + " kbps"
                        + "\naudio:" + streamStatus.audioFps + " fps"
                        + "\nvideo:" + streamStatus.videoFps + " fps");
            }
        });
    }

    private boolean copyModelIfNeed(String modelName) {
        String path = getFilePath(modelName);
        if (path != null) {
            File modelFile = new File(path);
            if (!modelFile.exists()) {
                //如果模型文件不存在或者当前模型文件的版本跟sdcard中的版本不一样
                try {
                    if (modelFile.exists())
                        modelFile.delete();
                    modelFile.createNewFile();
                    InputStream in = mContext.getApplicationContext().getAssets().open(modelName);
                    if(in == null)
                    {
                        Log.e("copyMode", "the src module is not existed");
                        return false;
                    }
                    OutputStream out = new FileOutputStream(modelFile);
                    byte[] buffer = new byte[4096];
                    int n;
                    while ((n = in.read(buffer)) > 0) {
                        out.write(buffer, 0, n);
                    }
                    in.close();
                    out.close();
                } catch (IOException e) {
                    modelFile.delete();
                    return false;
                }
            }
        }
        return true;
    }

    private boolean copyFileIfNeed(String fileName) {
        String path = getFilePath(fileName);
        if (path != null) {
            File file = new File(path);
            if (!file.exists()) {
                //如果模型文件不存在或者当前模型文件的版本跟sdcard中的版本不一样
                try {
                    if (file.exists())
                        file.delete();
                    file.createNewFile();
                    InputStream in = mContext.getApplicationContext().getAssets().open(fileName);
                    if(in == null)
                    {
                        Log.e("copyMode", "the src is not existed");
                        return false;
                    }
                    OutputStream out = new FileOutputStream(file);
                    byte[] buffer = new byte[4096];
                    int n;
                    while ((n = in.read(buffer)) > 0) {
                        out.write(buffer, 0, n);
                    }
                    in.close();
                    out.close();
                } catch (IOException e) {
                    file.delete();
                    return false;
                }
            }
        }
        return true;
    }

    protected String getFilePath(String fileName) {
        String path = null;
        File dataDir = mContext.getApplicationContext().getExternalFilesDir(null);
        if (dataDir != null) {
            path = dataDir.getAbsolutePath() + File.separator + fileName;
        }
        return path;
    }

    @Override
    public void processTextureCallback(final String materialName, final int strStatus) {
        StreamingBaseActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String status_string = null;
                switch (strStatus) {
                    case ST_MATERIAL_BEGIN:
                        status_string = "begin";
                        break;
                    case ST_MATERIAL_END:
                        status_string = "end";
                        break;
                    case ST_MATERIAL_PROCESS:
                        status_string = "process";
                        break;
                    default:
                        break;

                }
                mcallbackStatus.setText("curMaterial="+materialName+"\ncurStatus="+status_string);
            }
        });
    }

    private class Switcher implements Runnable {
        @Override
        public void run() {
            mCameraStreamingManager.switchCamera();
            mCameraInputRender.destroyFramebuffers();
            mCameraBuffer = null;
            if(mMidTextureId != null){
                GLES20.glDeleteTextures(1, mMidTextureId, 0);
                mMidTextureId = null;
            }

            if(mTextureOutId != null){
                GLES20.glDeleteTextures(1, mTextureOutId, 0);
                mTextureOutId = null;
            }
        }
    }

    private class EncodingOrientationSwitcher implements Runnable {

        @Override
        public void run() {
            Log.i(TAG, "isEncOrientationPort:" + isEncOrientationPort);
            stopStreaming();
            mOrientationChanged = !mOrientationChanged;
            isEncOrientationPort = !isEncOrientationPort;
            mProfile.setEncodingOrientation(isEncOrientationPort ? StreamingProfile.ENCODING_ORIENTATION.PORT : StreamingProfile.ENCODING_ORIENTATION.LAND);
            mCameraStreamingManager.setStreamingProfile(mProfile);
            setRequestedOrientation(isEncOrientationPort ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            mCameraStreamingManager.notifyActivityOrientationChanged();
            updateOrientationBtnText();
            Toast.makeText(StreamingBaseActivity.this, Config.HINT_ENCODING_ORIENTATION_CHANGED,
                    Toast.LENGTH_SHORT).show();
            Log.i(TAG, "EncodingOrientationSwitcher -");
        }
    }

    private class Screenshooter implements Runnable {
        @Override
        public void run() {
            final String fileName = "PLStreaming_" + System.currentTimeMillis() + ".jpg";
            mCameraStreamingManager.captureFrame(0, 0, new FrameCapturedCallback() {
                private Bitmap bitmap;

                @Override
                public void onFrameCaptured(Bitmap bmp) {
                    bitmap = bmp;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                saveToSDCard(fileName, bitmap);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                if (bitmap != null) {
                                    bitmap.recycle();
                                    bitmap = null;
                                }
                            }
                        }
                    }).start();
                }
            });
        }
    }

    private void setTorchEnabled(final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String flashlight = enabled ? getString(R.string.flash_light_off) : getString(R.string.flash_light_on);
                mTorchBtn.setText(flashlight);
            }
        });
    }

    @Override
    public void onStateChanged(final int state, Object extra) {
        switch (state) {
            case CameraStreamingManager.STATE.PREPARING:
                mStatusMsgContent = getString(R.string.string_state_preparing);
                break;
            case CameraStreamingManager.STATE.READY:
                mIsReady = true;
                mMaxZoom = mCameraStreamingManager.getMaxZoom();
                mStatusMsgContent = getString(R.string.string_state_ready);
                // start streaming when READY
                startStreaming();
                break;
            case CameraStreamingManager.STATE.CONNECTING:
                mStatusMsgContent = getString(R.string.string_state_connecting);
                break;
            case CameraStreamingManager.STATE.STREAMING:
                mStatusMsgContent = getString(R.string.string_state_streaming);
                setShutterButtonEnabled(true);
                setShutterButtonPressed(true);
                break;
            case CameraStreamingManager.STATE.SHUTDOWN:
                mStatusMsgContent = getString(R.string.string_state_ready);
                setShutterButtonEnabled(true);
                setShutterButtonPressed(false);
                if (mOrientationChanged) {
                    mOrientationChanged = false;
                    startStreaming();
                }
                break;
            case CameraStreamingManager.STATE.IOERROR:
                mLogContent += "IOERROR\n";
                mStatusMsgContent = getString(R.string.string_state_ready);
                setShutterButtonEnabled(true);
                break;
            case CameraStreamingManager.STATE.UNKNOWN:
                mStatusMsgContent = getString(R.string.string_state_ready);
                break;
            case CameraStreamingManager.STATE.SENDING_BUFFER_EMPTY:
                break;
            case CameraStreamingManager.STATE.SENDING_BUFFER_FULL:
                break;
            case CameraStreamingManager.STATE.AUDIO_RECORDING_FAIL:
                break;
            case CameraStreamingManager.STATE.OPEN_CAMERA_FAIL:
                Log.e(TAG, "Open Camera Fail. id:" + extra);
                break;
            case CameraStreamingManager.STATE.DISCONNECTED:
                mLogContent += "DISCONNECTED\n";
                break;
            case CameraStreamingManager.STATE.INVALID_STREAMING_URL:
                Log.e(TAG, "Invalid streaming url:" + extra);
                break;
            case CameraStreamingManager.STATE.CAMERA_SWITCHED:
//                mShutterButtonPressed = false;
                if (extra != null) {
                    Log.i(TAG, "current camera id:" + extra);
                }
                Log.i(TAG, "camera switched");
                final int currentCamId = (Integer)extra;
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateCameraSwitcherButtonText(currentCamId);
                    }
                });
                break;
            case CameraStreamingManager.STATE.TORCH_INFO:
                if (extra != null) {
                    final boolean isSupportedTorch = (Boolean) extra;
                    Log.i(TAG, "isSupportedTorch=" + isSupportedTorch);
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isSupportedTorch) {
                                mTorchBtn.setVisibility(View.VISIBLE);
                            } else {
                                mTorchBtn.setVisibility(View.GONE);
                            }
                        }
                    });
                }
                break;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mLogTextView != null) {
                    mLogTextView.setText(mLogContent);
                }
                mSatusTextView.setText(mStatusMsgContent);
            }
        });
    }

    @Override
    public boolean onStateHandled(final int state, Object extra) {
        switch (state) {
            case CameraStreamingManager.STATE.SENDING_BUFFER_HAS_FEW_ITEMS:
                return false;
            case CameraStreamingManager.STATE.SENDING_BUFFER_HAS_MANY_ITEMS:
                return false;
        }
        return false;
    }

    private void initUIs() {
        mRootView = findViewById(R.id.content);
        mRootView.addOnLayoutChangeListener(this);

        mMuteButton = (Button) findViewById(R.id.mute_btn);
        mShutterButton = (Button) findViewById(R.id.toggleRecording_button);
        mTorchBtn = (Button) findViewById(R.id.torch_btn);
        mCameraSwitchBtn = (Button) findViewById(R.id.camera_switch_btn);
        mCaptureFrameBtn = (Button) findViewById(R.id.capture_btn);
        mSatusTextView = (TextView) findViewById(R.id.streamingStatus);

        mLogTextView = (TextView) findViewById(R.id.log_info);
        mStreamStatus = (TextView) findViewById(R.id.stream_status);
        mcallbackStatus = (TextView) findViewById(R.id.callback_status);

        mMuteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mHandler.hasMessages(MSG_MUTE)) {
                    mHandler.sendEmptyMessage(MSG_MUTE);
                }
            }
        });

        mShutterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mShutterButtonPressed) {
                    stopStreaming();
                } else {
                    startStreaming();
                }
            }
        });

        mTorchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (!mIsTorchOn) {
                            mIsTorchOn = true;
                            mCameraStreamingManager.turnLightOn();
                        } else {
                            mIsTorchOn = false;
                            mCameraStreamingManager.turnLightOff();
                        }
                        setTorchEnabled(mIsTorchOn);
                    }
                }).start();
            }
        });

        mCameraSwitchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mHandler.removeCallbacks(mSwitcher);
                mHandler.postDelayed(mSwitcher, 100);
            }
        });

        mCaptureFrameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mHandler.removeCallbacks(mScreenshooter);
                mHandler.postDelayed(mScreenshooter, 100);
            }
        });


        mEncodingOrientationSwitcherBtn = (Button) findViewById(R.id.orientation_btn);
        mEncodingOrientationSwitcherBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHandler.removeCallbacks(mEncodingOrientationSwitcher);
                mHandler.post(mEncodingOrientationSwitcher);
            }
        });
        initButtonText();

    }

    private void initButtonText() {
        updateCameraSwitcherButtonText(mCameraStreamingSetting.getReqCameraId());
        mCaptureFrameBtn.setText("Capture");
        updateMuteButtonText();
        updateOrientationBtnText();
    }

    private void updateOrientationBtnText() {
        if (isEncOrientationPort) {
            mEncodingOrientationSwitcherBtn.setText("Land");
        } else {
            mEncodingOrientationSwitcherBtn.setText("Port");
        }
    }

    protected void setFocusAreaIndicator() {
        if (mRotateLayout == null) {
            mRotateLayout = (RotateLayout)findViewById(R.id.focus_indicator_rotate_layout);
            mCameraStreamingManager.setFocusAreaIndicator(mRotateLayout,
                    mRotateLayout.findViewById(R.id.focus_indicator));
        }
    }

    private void updateMuteButtonText() {
        if (mMuteButton != null) {
            mMuteButton.setText(mIsNeedMute ? "Unmute" : "Mute");
        }
    }

    private void updateCameraSwitcherButtonText(int camId) {
        if (mCameraSwitchBtn == null) {
            return;
        }
        if (camId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mCameraSwitchBtn.setText("Back");
        } else {
            mCameraSwitchBtn.setText("Front");
        }
    }

    private void saveToSDCard(String filename, Bitmap bmp) throws IOException {
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File file = new File(Environment.getExternalStorageDirectory(), filename);
            BufferedOutputStream bos = null;
            try {
                bos = new BufferedOutputStream(new FileOutputStream(file));
                bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
                bmp.recycle();
                bmp = null;
            } finally {
                if (bos != null) bos.close();
            }

            final String info = "Save frame to:" + Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + filename;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, info, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void saveActiveCode(String filename, String activeCode) {
        SharedPreferences sp = getSharedPreferences("activecode", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("activationcode", activeCode);
        editor.commit();
    }

    private String getActiveCode(String filename) {
        SharedPreferences sp = getSharedPreferences(filename, 0);
        String activeCode =sp.getString("activationcode", "");

        return activeCode;
    }

    private static DnsManager getMyDnsManager() {
        IResolver r0 = new DnspodFree();
        IResolver r1 = AndroidDnsServer.defaultResolver();
        IResolver r2 = null;
        try {
            r2 = new Resolver(InetAddress.getByName("119.29.29.29"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return new DnsManager(NetworkInfo.normal, new IResolver[]{r0, r1, r2});
    }

    //fenghx
    private void initAccelerometer() {
        mAccelerometer = new Accelerometer(getApplicationContext());
    }

    private void startAccelerometer() {
        mAccelerometer.start();
    }

    private void stopAccelerometer() {
        mAccelerometer.stop();
    }

    protected void initStickerFiles(){
        String files[] = null;
        mStickerFilesList = new ArrayList<String>();

        try {
            files = this.getAssets().list("");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String folderpath = null;
        File dataDir = getExternalFilesDir(null);
        if (dataDir != null) {
            folderpath = dataDir.getAbsolutePath();
        }
        String unzipFolder = folderpath+File.separator;
        for (int i = 0; i < files.length; i++) {
            String str = files[i];
            if(str.indexOf(".zip") != -1){
                copyFileIfNeed(str);
            }
        }

        List<String> zipfiles = new ArrayList<String>();
        File file = new File(folderpath);
        File[] subFile = file.listFiles();

        for (int i = 0; i < subFile.length; i++) {
            // 判断是否为文件夹
            if (!subFile[i].isDirectory()) {
                String filename = subFile[i].getName();
                String path = subFile[i].getPath();
                // 判断是否为zip结尾
                if (filename.trim().toLowerCase().endsWith(".zip")) {
                    zipfiles.add(filename);
                }
            }
        }

        for (int j = 0; j < zipfiles.size(); j++) {
            String src = zipfiles.get(j);
            File unzipFile = new File(getFilePath(src));
            try {
                ZipUtils.upZipFile(unzipFile, unzipFolder);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            mStickerFilesList.add(unzipFolder + src.substring(0, src.indexOf(".zip")));
        }

    }

    private void initSticker(){
        String modulePath = getFilePath("face_action.model");
        String licensePath =getFilePath("MOBILESDK_FD681B7F-82D2-4917-B9B7-E0DB5D8D33ED.lic");

        int[] codeLen = new int[1];
        codeLen[0] = 1024;

        String activeCode = getActiveCode("activecode");
        int checkRes;

        if(activeCode == null || activeCode.length()==0) {
            activeCode = mStStickerNative.generateActiveCode("MobileSDK", licensePath, codeLen);
            checkRes =  mStStickerNative.checkActiveCode("MobileSDK", licensePath, activeCode);
            if(checkRes != 0) {
                Log.e(TAG, "-->> license is out of date");
                return;
            } else {
                saveActiveCode("active_code.txt", activeCode);
            }
        } else {
            checkRes = mStStickerNative.checkActiveCode("MobileSDK", licensePath, activeCode);
            if(checkRes != 0) {
                Log.e(TAG, "-->> activeCode is out of date, need to generate another one");
                activeCode = mStStickerNative.generateActiveCode("MobileSDK", licensePath, codeLen);
                checkRes = mStStickerNative.checkActiveCode("MobileSDK", licensePath, activeCode);
                if(checkRes != 0) {
                    Log.e(TAG, "-->> license is invalid");
                    return;
                }
            }
        }
//
//        if(checkRes != 0) {
//            Log.e(TAG, "-->> check activeCode failed!");
//            return;
//        }
        //get the first sticker
        mStickerFolderPath = mStickerFilesList.get(0);
        mCurrentStickerNum = 0;
        int result1 =  mStStickerNative.createInstance(mStickerFolderPath, modulePath, 0x0000003F);
        Log.i(TAG,"the result for createInstance for sticker is "+result1);
    }

    public class ChangeStickerTask implements Runnable{

        @Override
        public void run() {
            changeSticker();
        }
    }

    private synchronized int changeSticker(){
        mCurrentStickerNum++;
        if (mCurrentStickerNum == mStickerFilesList.size()) {
            mCurrentStickerNum = 0;
        }
        mStickerFolderPath = mStickerFilesList.get(mCurrentStickerNum);
        int result = mStStickerNative.changeSticker(mStickerFolderPath);
        initStickerResult = result;
        return result;
    }
    //fenghx
}
