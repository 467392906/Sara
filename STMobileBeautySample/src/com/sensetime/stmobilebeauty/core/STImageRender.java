package com.sensetime.stmobilebeauty.core;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.logging.Logger;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.sensetime.stmobile.STBeautyParamsType;
import com.sensetime.stmobile.STConvertFormat;
import com.sensetime.stmobile.STImageFilterNative;
import com.sensetime.stmobile.STImageFormat;
import com.sensetime.stmobilebeauty.utils.OpenGLUtils;

import android.R.integer;
import android.R.string;
import android.animation.FloatArrayEvaluator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;

public class STImageRender{
	private String TAG = "STImageRender";
	private boolean DEBUG = true; //false;
	protected final GLSurfaceView mGLSurfaceView;

	protected int mSurfaceWidth, mSurfaceHeight;
	
	protected int mImageWidth, mImageHeight;
	
	protected Context mContext;
	
	protected int mTextureIdForNoGpu = OpenGLUtils.NO_TEXTURE;
	private int mCameraOrietation = 0;
	
	private GetBitmapFromGLListener mGetBitmapListener;
	
    protected static int[] mFrameBuffers = null;
    protected static int[] mFrameBufferTextures = null;
    private int mFrameWidth = -1;
    private int mFrameHeight = -1;  
    
    protected final Queue<Runnable> mRunOnDraw;
    
    private STImageFilterNative mStImageFilterNative;
    /**
     * 
     * @param context 传递应用上下文环境
     * @param glSurfaceView  用以显示图片的view
     */
	public STImageRender(Context context, GLSurfaceView glSurfaceView){
		mContext = context;
		mGLSurfaceView = glSurfaceView;  

	    mRunOnDraw = new LinkedList<Runnable>();
	    mStImageFilterNative = new STImageFilterNative();
	}
	
	/**美颜处理绑定了待处理图片的纹理
	 * 
	 * @param textureID 绑定了待处理图片的纹理ID
	 * @param vertexBuffer  顶点处理器的位置信息
	 * @param textureBuffer  片元处理器的位置信息
	 */
	public int getEffectFrameTexture(int textureIn, int textureOut)
	{
		if(textureIn == -1) {
			Log.e(TAG,"the texture ID is invalid");
			return -1;
		}
		runAll(mRunOnDraw);
        if(mStImageFilterNative != null) {
        	int result = mStImageFilterNative.processTexture(textureIn, mImageWidth, mImageHeight, textureOut);
        	if(DEBUG)
        		Log.d(TAG, "processTexture result is "+result);
        }
        return textureOut;
	}
   
	/**设置处理图片的尺寸
	 * 
	 * @param width  待处理图片的宽度
	 * @param height  待处理图片的高度
	 */
	public void setImageSize(int width, int height)
	{
	     mImageWidth = width;
	     mImageHeight = height;
	}
	
	/**设置美颜效果
	 * 
	 * @param filterType  美颜过滤器的类型,具体参见CVFilterType类
	 */
	public void setEffect(int effectType, float effectValue) {
		mStImageFilterNative.setParam(effectType, effectValue);
    }
	
	/**设置filter对应的参数
	 * 
	 * @param params filter可调整的参数
	 */
	public int setEffectParam(int paramType, float paramValue){
		int result = mStImageFilterNative.setParam(paramType, paramValue);
		return result;
	}
	
	/**根据surface的信息重新设置输出信息和图片信息, 在onSurfaceChange中调用
	 * 
	 * @param surfaceWidth  纹理的输出显示宽度
	 * @param surfaceHeight  纹理的输出显示高度
	 */	
	public int surfaceChanged(int surfaceWidth, int surfaceHeight)
	{
		mSurfaceWidth = surfaceWidth;
		mSurfaceHeight = surfaceHeight;
	    int result = mStImageFilterNative.initBeautify(mImageWidth,mImageHeight);
	    if(DEBUG)
	    	Log.i(TAG, "initBeautify result is "+result);
	    return result;
	}
	
	/**保存GPU美颜之后的图片
	 * 
	 * @param bitmap  待美颜处理的图片
	 * @param file   用来保存处理后图片的文件
	 * @param newTexture  是否需要新创建纹理
	 * @param inputTextureId  绑定了待处理图片的纹理的ID,newTexture为true,这个值设为-1
	 * @param listener 监听获取美颜后的图片, GetBitmapFromGLListener参见接口说明
	 */
	public void getBitmapFromFilter(final Bitmap bitmap,final File file, final boolean newTexture,final int inputTextureId, GetBitmapFromGLListener listener){
		
		setFaceDetectionListener(listener);
		//the process function will create egl context ,so we can call directly
		getBitmapFromGL(bitmap, file);	
		
		/*
		mGLSurfaceView.queueEvent(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				getBitmapFromGL(bitmap, file);
			}
		});
		*/
	}
	
	private void getBitmapFromGL(final Bitmap bitmap,File file)
	{
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
  
		setEffect(STBeautyParamsType.ST_BEAUTIFY_DEFRECKLE, 1.0f);
		
        ByteBuffer srcBuffer = ByteBuffer.allocate(width * height * 4);
        bitmap.copyPixelsToBuffer(srcBuffer);
        
        byte[] pOutputImage = null;
        
    	pOutputImage = new byte[width*height*4];
    	long startTime = System.currentTimeMillis();
        if(mStImageFilterNative != null)
        {
        	int result = mStImageFilterNative.processBufferWithNewContext(srcBuffer.array(),STImageFormat.ST_PIX_FMT_RGBA8888, width, height,pOutputImage,STImageFormat.ST_PIX_FMT_RGBA8888);
        //	int result = mStImageFilterNative.processBufferWithCurrentContext(srcBuffer.array(),STImageFormat.ST_PIX_FMT_RGBA8888, width, height,pOutputImage,STImageFormat.ST_PIX_FMT_RGBA8888);
        	if(DEBUG)
        		Log.d(TAG, "process buffer result is "+result);
        }
        if(DEBUG){
        	Log.d(TAG, "the process beautify time is "+(System.currentTimeMillis()-startTime));
        }
        
        Bitmap outBitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		ByteBuffer buffer = ByteBuffer.wrap(pOutputImage);
		
		outBitmap.copyPixelsFromBuffer(buffer);
		
		setEffect(STBeautyParamsType.ST_BEAUTIFY_DEFRECKLE, 0.0f);

    	mGetBitmapListener.onGetBitmapFromGL(bitmap,outBitmap,file);
	}
	
	public interface GetBitmapFromGLListener
    {
		void onGetBitmapFromGL(Bitmap srcBitmap,Bitmap outputBitmap,File file);
	}
	
    private void setFaceDetectionListener(GetBitmapFromGLListener listener)
    {
    	mGetBitmapListener = listener;
    }
   

    public void onDestory()
    {	
    	if(mStImageFilterNative != null)
    		mStImageFilterNative.destoryBeautify();        
    }

    private void runAll(Queue<Runnable> queue) {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                queue.poll().run();
            }
        }
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.add(runnable);
        }
    }
    
	private void initFrameBuffer(int width, int height) {
		if(mFrameBuffers != null && (mFrameWidth != width || mFrameHeight != height))
			destroyFramebuffers();
        if (mFrameBuffers == null) {
        	mFrameWidth = width;
			mFrameHeight = height;
        	mFrameBuffers = new int[2];
            mFrameBufferTextures = new int[2];

            for (int i = 0; i < 2; i++) {
            GLES20.glGenFramebuffers(1, mFrameBuffers, i);
            
            GLES20.glGenTextures(1, mFrameBufferTextures, i);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[i]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, mFrameBufferTextures[i], 0);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            }
        }
	}	
	
	private void destroyFramebuffers() {
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(mFrameBufferTextures.length, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(mFrameBuffers.length, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
        
        mFrameWidth = -1;
        mFrameHeight = -1;
    }

}
