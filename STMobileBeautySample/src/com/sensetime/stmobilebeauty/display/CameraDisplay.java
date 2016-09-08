package com.sensetime.stmobilebeauty.display;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.sensetime.stmobile.STBeautyParamsType;
import com.sensetime.stmobilebeauty.camera.CameraProxy;
import com.sensetime.stmobilebeauty.core.STImageRender;
import com.sensetime.stmobilebeauty.core.STImageRender.GetBitmapFromGLListener;
import com.sensetime.stmobilebeauty.utils.Exif;
import com.sensetime.stmobilebeauty.utils.OpenGLUtils;
import com.sensetime.stmobilebeauty.utils.Rotation;
import com.sensetime.stmobilebeauty.utils.SaveTask;
import com.sensetime.stmobilebeauty.utils.SaveTask.onPictureSaveListener;
import com.sensetime.stmobilebeauty.utils.TextureRotationUtil;
import com.sensetime.stmobilebeauty.utils.exif.ExifInterface;

import android.R.integer;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.media.midi.MidiDeviceStatus;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.AsyncTask;
import android.text.StaticLayout;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

/**
 * CameraDisplay is used for camera preview
 */
public class CameraDisplay implements Renderer{	

	private String TAG = "CameraDisplay";
	private boolean DEBUG = true; //true;
	
	//this render is used to deal the texture which is bind to camera surfacetexture
	private final CameraInputRender mCameraInputRender;
	//this render is used to render the effect texture
	private ImageInputRender mImageInputRender;
	
	protected int mTextureIdForGpu = OpenGLUtils.NO_TEXTURE;
	
	/**
	 * SurfaceTexure texture id
	 */
	protected int mTextureId = OpenGLUtils.NO_TEXTURE;
	//this is used to add effect to origin 
	private STImageRender mRender = null;
	private int mImageWidth;
	private int mImageHeight;
	private GLSurfaceView mGlSurfaceView;
	private int mSurfaceWidth;
	private int mSurfaceHeight;
	
	protected final FloatBuffer mVertexBuffer;
	protected final FloatBuffer mTextureBuffer;
	private Context mContext;
	private SaveTask mSaveTask;
	
    private int mDisplayRotation;
    private int mPicRotation;
    public CameraProxy mCameraProxy;
    private int mCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;	
    private boolean  mInitialized = false;
    private boolean mPaused = false;
    private onPictureSaveListener mPictureSaveListener;
    private long mStartTime;
    private long mStartTimeForFrame;
    private long mStartPicBeautyTime;
    
    private boolean mShowOriginal = false;
    
    private FpsChangeListener mFpsChangeListener;
    
    private int[] mTextureOutID = null;
	
	private SurfaceTexture mSurfaceTexture;
    
	public CameraDisplay(Context context, GLSurfaceView glSurfaceView){
		mRender = new STImageRender(context, glSurfaceView);
		mCameraProxy = new CameraProxy(context);

		mGlSurfaceView = glSurfaceView;
		mContext = context;
		glSurfaceView.setEGLContextClientVersion(2);
		glSurfaceView.setRenderer(this);
		glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		
		mVertexBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mVertexBuffer.put(TextureRotationUtil.CUBE).position(0);

        mTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);
        
		mCameraInputRender = new CameraInputRender();
		mImageInputRender = new ImageInputRender();
	}
	
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		if(DEBUG) {
		Log.i(TAG, "onSurfaceCreated");
		}
		if(mPaused)
			return;
		GLES20.glEnable(GL10.GL_DITHER);
        GLES20.glClearColor(0,0,0,0);
        GLES20.glEnable(GL10.GL_CULL_FACE);
        GLES20.glEnable(GL10.GL_DEPTH_TEST); 
        
		if(mCameraProxy.getCamera() != null){
			boolean flipHorizontal = mCameraProxy.isFlipHorizontal();
			adjustPosition(mCameraProxy.getOrientation(),flipHorizontal,!flipHorizontal);
			mPicRotation = mCameraProxy.getOrientation();
			setUpCamera();
		}
        mCameraInputRender.init();
        mImageInputRender.init();
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		if(DEBUG) {
			Log.i(TAG, "onSurfaceChanged");
		}
		initEffectTexture(mImageWidth,mImageHeight);
		adjustViewPort(width,height);
		mStartTime =  System.currentTimeMillis();
		int result = mRender.surfaceChanged((int)mSurfaceWidth, (int)mSurfaceHeight);
		if(result != 0){
			mShowOriginal = true;
			Log.e(TAG, "init beautify handle failed , show original");
		}
	    setEffectParams(4.0f/7.0f);
		mInitialized = true;
	}
	
	private void adjustViewPort(int width, int height) {
		float ratio = (float)mImageWidth/(float)mImageHeight;
		mSurfaceHeight = height;
		mSurfaceWidth = width;
	    GLES20.glViewport(0, 0, (int)mSurfaceWidth, (int)mSurfaceHeight);
		adjustImageDisplaySize();
		mCameraInputRender.onDisplaySizeChanged((int)mSurfaceWidth, (int)mSurfaceHeight);
	    mCameraInputRender.initCameraFrameBuffer(mImageWidth, mImageHeight);
	    mImageInputRender.onOutputSizeChanged(mImageWidth, mImageHeight);
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		if(mPaused || !mInitialized)
			return;
		if(mCameraProxy.getCamera() == null)
		    return;
		long dt = System.currentTimeMillis() - mStartTime ;
		mStartTime =  System.currentTimeMillis();
		if(DEBUG) {
			Log.i(TAG, "onDrawFame, the time is "+dt);
		}
		if(mFpsChangeListener != null){
			mFpsChangeListener.onFpsChanged((int)dt);
		}
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);	
		mSurfaceTexture.updateTexImage();
		float[] mtx = new float[16];
		mSurfaceTexture.getTransformMatrix(mtx);
		mCameraInputRender.setTextureTransformMatrix(mtx);
		if(mShowOriginal){
			mCameraInputRender.onDrawFrame(mTextureId, mVertexBuffer, mTextureBuffer);
		}else{
		//	initEffectTexture(mImageWidth,mImageHeight);
		
			int textureID = mCameraInputRender.onDrawToTexture(mTextureId);
            long beginTime = System.currentTimeMillis();
			mRender.getEffectFrameTexture(textureID,mTextureOutID[0]);
			if(DEBUG){
				Log.e(TAG, "onDrawFrame, the time for process texture is "+ (System.currentTimeMillis() - beginTime));
			}
			
			GLES20.glViewport(0, 0, (int)mSurfaceWidth, (int)mSurfaceHeight);

			mImageInputRender.onDrawFrame(mTextureOutID[0], mVertexBuffer, mTextureBuffer);
			
			/*
			GLES20.glViewport(0, 0, (int)mSurfaceWidth, (int)mSurfaceHeight);
			mImageInputRender.onDrawFrame(textureID, mVertexBuffer, mTextureBuffer);
			*/
			
		}
	}
	
	private void initEffectTexture(int width, int height)
	{
		if(mTextureOutID == null)
		{
			mTextureOutID = new int[1];
			GLES20.glGenTextures(1, mTextureOutID, 0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureOutID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
		}
	}
	
	private OnFrameAvailableListener mOnFrameAvailableListener = new OnFrameAvailableListener() {
		
		@Override
		public void onFrameAvailable(SurfaceTexture surfaceTexture) {
			// TODO Auto-generated method stub
			long dt = System.currentTimeMillis()-mStartTimeForFrame;
			mStartTimeForFrame = System.currentTimeMillis();
			if(DEBUG){
			Log.i(TAG, "onFrameAvailable, the time intervals is "+dt);
			}
			mGlSurfaceView.requestRender();
		}
	};
	
	private void setUpCamera(){
       if(mTextureId == OpenGLUtils.NO_TEXTURE){
    	   mTextureId = OpenGLUtils.getExternalOESTextureID();	
    	   mSurfaceTexture = new SurfaceTexture(mTextureId);
    	   mSurfaceTexture.setOnFrameAvailableListener(mOnFrameAvailableListener);   
        }
        Size size = mCameraProxy.getPreviewSize();
        mRender.setImageSize(size.width, size.height);
        mImageWidth = size.width;
        mImageHeight = size.height;
    	mCameraInputRender.onOutputSizeChanged(mImageWidth, mImageHeight);	
        mCameraProxy.startPreview(mSurfaceTexture,null);
    }
	
	protected void onFilterSurfaceChanged(int surfaceWidth, int surfaceHeight){

	}
	
	public void setEffect(int effectType, float effectValue) {
		mRender.setEffect(effectType, effectValue);
	}
	
	public void setEffectParams(float paramValue)
	{
		mRender.setEffectParam(STBeautyParamsType.ST_BEAUTIFY_CONTRAST_STRENGTH, paramValue);
		mRender.setEffectParam(STBeautyParamsType.ST_BEAUTIFY_SMOOTH_STRENGTH,paramValue);
	}
	
	public void setShowOriginal(boolean isShow)
	{
		mShowOriginal = isShow;
	}
	
	public void onResume(){
		if(DEBUG){
		Log.i(TAG, "onResume");
		}
		mPaused = false;
		if(mCameraProxy.getCamera() == null)
        	mCameraProxy.openCamera(mCameraID);
	}
	
	public void onPause(){	
		if(DEBUG){
		Log.i(TAG, "onPause");
		}
		mPaused = true;
		mCameraProxy.releaseCamera();
		if(mInitialized)
		{
			mGlSurfaceView.queueEvent(new Runnable() {
				@Override
				public void run() {
					mCameraInputRender.destroyFramebuffers();
					mCameraInputRender.destroy();
					mImageInputRender.destroy();
					mRender.onDestory();
			}
			});
		}
		deleteTextures();
		mInitialized = false;
	}

	public void onLayoutOrientationChanged(int displayRotation)
	{
		mDisplayRotation = displayRotation;
		int cameraOrientation = mCameraProxy.getOrientation();
		if(mDisplayRotation == 270 || mDisplayRotation == 90)
		{
			mPicRotation = (cameraOrientation+mDisplayRotation-180)%360;
		}
		else
		{
			mPicRotation = (cameraOrientation+mDisplayRotation)%360;
		}
	}
	
	public void onDestroy(){
		
	}
	
	public void changePreviewSize(final int width, final int height){
		if(!mInitialized || mPaused)
			return;
		if(DEBUG)
			Log.d(TAG, "changePreviewSize");
	      mGlSurfaceView.queueEvent(new Runnable() {
				@Override
				public void run() {
					  mCameraProxy.stopPreview();
					  mRender.setImageSize(width, height);
					  mImageWidth = width;
					  mImageHeight = height;
					  adjustViewPort(mSurfaceWidth, mSurfaceHeight);
					  mCameraInputRender.onOutputSizeChanged(mImageWidth, mImageHeight);	
					  mCameraProxy.setPreviewSize(width, height);
				      Size size = mCameraProxy.getPreviewSize();
				      adjustImageDisplaySize();
				      mCameraProxy.startPreview();
			}
		  });  
	}
	
	public void setFpsChangeListener(FpsChangeListener listener){
		mFpsChangeListener = listener;
	}
	
	public interface FpsChangeListener {
		public void onFpsChanged(int value);
	}
	
	private void adjustPosition(int orientation, boolean flipHorizontal,boolean flipVertical) {
        Rotation rotation = Rotation.fromInt(orientation);
        float[] textureCords = TextureRotationUtil.getRotation(rotation, flipHorizontal, flipVertical);
        mTextureBuffer.clear();
        mTextureBuffer.put(textureCords).position(0);
    }
	
	private void adjustImageDisplaySize() {
		int outputHeight = mSurfaceHeight;//mDisplayHeight;
		int outputWidth = mSurfaceWidth; //mDisplayWidth;
        if (mCameraProxy.getOrientation() == 270 || mCameraProxy.getOrientation() == 90) {
            outputWidth = mSurfaceHeight;  //mDisplayHeight;
            outputHeight = mSurfaceWidth;  //mDisplayWidth;
        }
		float ratio1 = (float)outputWidth / mImageWidth;
        float ratio2 = (float)outputHeight / mImageHeight;
        float ratioMax = Math.max(ratio1, ratio2);
        int imageWidthNew = Math.round(mImageWidth * ratioMax);
        int imageHeightNew = Math.round(mImageHeight * ratioMax);

        float ratioWidth = imageWidthNew / (float)outputWidth;
        float ratioHeight = imageHeightNew / (float)outputHeight;
        
        if (mCameraProxy.getOrientation() == 270 || mCameraProxy.getOrientation() == 90) {
        	ratioWidth = imageHeightNew / (float)outputHeight;
        	ratioHeight = imageWidthNew / (float)outputWidth;
        }

        float[] cube = new float[]{
        		TextureRotationUtil.CUBE[0] / ratioHeight, TextureRotationUtil.CUBE[1] / ratioWidth,
        		TextureRotationUtil.CUBE[2] / ratioHeight, TextureRotationUtil.CUBE[3] / ratioWidth,
        		TextureRotationUtil.CUBE[4] / ratioHeight, TextureRotationUtil.CUBE[5] / ratioWidth,
        		TextureRotationUtil.CUBE[6] / ratioHeight, TextureRotationUtil.CUBE[7] / ratioWidth,
        };
        mVertexBuffer.clear();
        mVertexBuffer.put(cube).position(0);
    }

	
	public void capture(ShutterCallback shutterCallback, PictureCallback pictureCallback){
		if(DEBUG){
		Log.i(TAG, "capture");
		}
		mCameraProxy.setRotation(mPicRotation);
		mCameraProxy.takePicture(shutterCallback, null, pictureCallback);
	}
	
	public void onPictureTaken(byte[] data,File file, onPictureSaveListener listener) {
		mStartPicBeautyTime = System.currentTimeMillis();
        ExifInterface exif = Exif.getExif(data);
        int orientation = Exif.getOrientation(exif);
		Bitmap bitmap = null;
		
		Bitmap srcbitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        if(orientation != 0)
        {
            bitmap = adjustPhoneRotation(srcbitmap,orientation);
        }
        else
        {
        	bitmap = srcbitmap;
        }

		if(mShowOriginal){
			mSaveTask = new SaveTask(mContext, file, listener);
			mSaveTask.execute(bitmap); 
		}else{
			mPictureSaveListener = listener;
			mRender.getBitmapFromFilter(bitmap,file, true, mTextureId,mGetbitmapListener);	
		}
		mCameraProxy.startPreview(mSurfaceTexture,null);
	}
	
	private Bitmap  adjustPhoneRotation(Bitmap srcbitmap, int orientation)
	{
		// 下面的方法主要作用是把图片转一个角度，也可以放大缩小等
		 Bitmap rotatebitmap = null;
  		 Matrix m = new Matrix();
  		 int width = srcbitmap.getWidth();
  		 int height = srcbitmap.getHeight();
  		 m.setRotate(orientation); // 旋转angle度
  		 try {
  		 rotatebitmap = Bitmap.createBitmap(srcbitmap, 0, 0, width, height,m, true);// 新生成图片	
  		 } catch(Exception e)
  		 {
  			 Log.e(TAG, "CreateBitmap failed");
  		 }
  		 
  		 if(rotatebitmap == null)
  		 {
  			 rotatebitmap = srcbitmap;
  		 }
  		 
  		 if(srcbitmap != rotatebitmap)
  		 {
  			 srcbitmap.recycle();
  		 }
  		 return rotatebitmap;
	}
	
	private GetBitmapFromGLListener mGetbitmapListener = new GetBitmapFromGLListener()
	{
		@Override
		public void onGetBitmapFromGL(Bitmap srcBitmap, Bitmap outputBitmap,File file){
			if(DEBUG){
			Log.i(TAG, "onGetBitmapFromGL TAKE PICTURE TIME is "+(System.currentTimeMillis()-mStartPicBeautyTime));
			}
			if(srcBitmap != null)
			{
				srcBitmap.recycle();
				srcBitmap = null;
			}
			mSaveTask = new SaveTask(mContext, file, mPictureSaveListener);
			mSaveTask.execute(outputBitmap);
		}
	};
	
	protected void deleteTextures() {
		if(mTextureId != OpenGLUtils.NO_TEXTURE)
			mGlSurfaceView.queueEvent(new Runnable() {
				
				@Override
				public void run() {
					Log.i(TAG, "delete textures");
	                GLES20.glDeleteTextures(1, new int[]{
	                        mTextureId
	                }, 0);
	                mTextureId = OpenGLUtils.NO_TEXTURE;
	            }
	        });
    }
}
