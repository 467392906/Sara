package com.sensetime.stmobilebeauty.display;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.sensetime.stmobile.STBeautyParamsType;
import com.sensetime.stmobilebeauty.core.STImageRender;
import com.sensetime.stmobilebeauty.core.STImageRender.GetBitmapFromGLListener;
import com.sensetime.stmobilebeauty.utils.OpenGLUtils;
import com.sensetime.stmobilebeauty.utils.SaveTask;
import com.sensetime.stmobilebeauty.utils.SaveTask.onPictureSaveListener;
import com.sensetime.stmobilebeauty.utils.TextureRotationUtil;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Handler;
import android.os.Message;
import android.text.StaticLayout;
import android.util.Log;

public class ImageDisplay implements Renderer{
   
    private Bitmap mOriginBitmap;
    private String TAG = "ImageDisplay";
    
    private boolean mIsSaving = false;
    
    private STImageRender mRender;
    
	private int mImageWidth;
	private int mImageHeight;
	private GLSurfaceView mGlSurfaceView;
	private int mDisplayWidth;
	private int mDisplayHeight;
	protected SaveTask mSaveTask;
	protected Context mContext;
	protected final FloatBuffer mVertexBuffer;
	protected final FloatBuffer mTextureBuffer;
	private onPictureSaveListener mPictureSaveListener;
	private ImageInputRender mImageInputRender;
	private boolean mInitialized = false;
    private int[] mTextureOutID = null;
	
    private boolean mShowOriginal = false;
	/**
	 * SurfaceTexure����id
	 */
	protected int mTextureId = OpenGLUtils.NO_TEXTURE;
    
    public ImageDisplay(Context context, GLSurfaceView glSurfaceView){
    	mRender = new STImageRender(context, glSurfaceView);
    	mImageInputRender = new ImageInputRender();
    	mGlSurfaceView = glSurfaceView;
    	
    	glSurfaceView.setEGLContextClientVersion(2);
		glSurfaceView.setRenderer(this);
		glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		
    	mContext = context;
		mVertexBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mVertexBuffer.put(TextureRotationUtil.CUBE).position(0);

        mTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mTextureBuffer.put(TextureRotationUtil.TEXTURE_NO_ROTATION).position(0);
    }      
    
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		GLES20.glDisable(GL10.GL_DITHER);
        GLES20.glClearColor(0,0,0,0);
        GLES20.glEnable(GL10.GL_CULL_FACE);
        GLES20.glEnable(GL10.GL_DEPTH_TEST);	
        mImageInputRender.init();
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
		mDisplayWidth = width;
		mDisplayHeight = height;
		int result = mRender.surfaceChanged(width,height);
		if(result != 0){
			mShowOriginal = true;
			Log.e(TAG, "init beautify handle failed , show original");
		}	
		adjustImageDisplaySize();
		setEffectParams(4.0f/7.0f);
		mInitialized = true;
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		if(!mInitialized)
			return;
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		if(mTextureId == OpenGLUtils.NO_TEXTURE)
			mTextureId = OpenGLUtils.loadTexture(mOriginBitmap, OpenGLUtils.NO_TEXTURE);
		
		if(!mShowOriginal)
		{
	        initEffectTexture(mImageWidth, mImageHeight);
	        
			mRender.getEffectFrameTexture(mTextureId, mTextureOutID[0]);
			GLES20.glViewport(0, 0, mDisplayWidth, mDisplayHeight);
			mImageInputRender.onDrawFrame(mTextureOutID[0],mVertexBuffer,mTextureBuffer);
		}
		else
		{
			mImageInputRender.onDisplaySizeChanged(mDisplayWidth,mDisplayHeight);
			mImageInputRender.onDrawFrame(mTextureId,mVertexBuffer,mTextureBuffer);
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
	
	public void setImageBitmap(Bitmap bitmap) {
		if (bitmap == null || bitmap.isRecycled())
			return;
		mImageWidth = bitmap.getWidth();
		mImageHeight = bitmap.getHeight();
		mOriginBitmap = bitmap;
		adjustImageDisplaySize();
		mRender.setImageSize(mImageWidth, mImageHeight);
		refreshDisplay();
	}
	
	public void setShowOriginal(boolean isShow)
	{
		mShowOriginal = isShow;
		refreshDisplay();
	}
	
	private void refreshDisplay(){
		deleteTextures();
		mGlSurfaceView.requestRender();
	}
	
	public void onResume(){
		
	}
	
	public void onPause(){
		if(mSaveTask != null)
			mSaveTask.cancel(true);
	}
	
	public void onDestroy(){
	  	deleteTextures();
	}
	
	public void setEffect(int effectType, float effectValue) {
		mRender.setEffect(effectType, effectValue);
	}
	
	public void setEffectParams(float paramValue)
	{
		mRender.setEffectParam(STBeautyParamsType.ST_BEAUTIFY_CONTRAST_STRENGTH, paramValue);
		mRender.setEffectParam(STBeautyParamsType.ST_BEAUTIFY_SMOOTH_STRENGTH,paramValue);
		refreshDisplay();
	}
	
	private void adjustImageDisplaySize() {
		float ratio1 = (float)mDisplayWidth / mImageWidth;
        float ratio2 = (float)mDisplayHeight / mImageHeight;
        float ratioMax = Math.max(ratio1, ratio2);
        int imageWidthNew = Math.round(mImageWidth * ratioMax);
        int imageHeightNew = Math.round(mImageHeight * ratioMax);

        float ratioWidth = imageWidthNew / (float)mDisplayWidth;
        float ratioHeight = imageHeightNew / (float)mDisplayHeight;

        float[] cube = new float[]{
        		TextureRotationUtil.CUBE[0] / ratioHeight, TextureRotationUtil.CUBE[1] / ratioWidth,
        		TextureRotationUtil.CUBE[2] / ratioHeight, TextureRotationUtil.CUBE[3] / ratioWidth,
        		TextureRotationUtil.CUBE[4] / ratioHeight, TextureRotationUtil.CUBE[5] / ratioWidth,
        		TextureRotationUtil.CUBE[6] / ratioHeight, TextureRotationUtil.CUBE[7] / ratioWidth,
        };
        mVertexBuffer.clear();
        mVertexBuffer.put(cube).position(0);
    }
	
	public void restore(){
		setImageBitmap(mOriginBitmap);
	}
	
	public void savaImage(File output, onPictureSaveListener listener){
		if(mOriginBitmap == null)
			return;
		mIsSaving = true;
		if(mRender != null)
		{
			mPictureSaveListener = listener;
			mRender.getBitmapFromFilter(mOriginBitmap,output, false,mTextureId,mGetbitmapListener);
		}
		else
		{
			mSaveTask = new SaveTask(mContext, output, listener);
			mSaveTask.execute(mOriginBitmap);   
		}
	}
	
	private GetBitmapFromGLListener mGetbitmapListener = new GetBitmapFromGLListener()
	{
		@Override
		public void onGetBitmapFromGL(Bitmap srcBitmap, Bitmap outbitmap,File output){
			mSaveTask = new SaveTask(mContext, output, mPictureSaveListener);
			mSaveTask.execute(outbitmap);
		}
	};
	
	protected void deleteTextures() {
		if(mTextureId != OpenGLUtils.NO_TEXTURE && mTextureOutID != null)
			mGlSurfaceView.queueEvent(new Runnable() {
				
				@Override
				public void run() {
	                GLES20.glDeleteTextures(1, new int[]{
	                        mTextureId
	                }, 0);
	                mTextureId = OpenGLUtils.NO_TEXTURE;  
	        		if(mTextureOutID != null){
	                    GLES20.glDeleteTextures(1, mTextureOutID, 0);
	                    mTextureOutID = null;
	        		}
	            }
	        });
    }
}
