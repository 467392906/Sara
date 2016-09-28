package com.pili.pldroid.streaming.camera.demo.gles;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.LinkedList;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

public class CameraInputRender {

	private static final String CAMERA_INPUT_VERTEX_SHADER = ""+
			"attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            "\n" +
            "uniform mat4 textureTransform;\n" +
            "varying vec2 textureCoordinate;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "	textureCoordinate = (textureTransform * inputTextureCoordinate).xy;\n" +
            "	gl_Position = position;\n" +
            "}";

	private static final String CAMERA_INPUT_FRAGMENT_SHADER = ""+
			"#extension GL_OES_EGL_image_external : require\n" +
			"//varying highp vec2 textureCoordinate;\n" +
			"\n" +
	         "precision mediump float;\n" +
	         "varying vec2 textureCoordinate;\n" +
			"uniform samplerExternalOES inputImageTexture;\n" +
			"\n" +
			"void main()\n" +
			"{\n" +
			"	gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
			"}";

    private static final String CAMERA_INPUT_FRAGMENT2_SHADER = ""+
            "precision mediump float;\n" +
            "varying vec2 textureCoordinate;\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "	gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";

	private float[] mTextureTransformMatrix;
    private int mTextureTransformMatrixLocation;

    private final LinkedList<Runnable> mRunOnDraw;
    private final String mVertexShader;
    private final String mFragmentShaderOES;
    private final String mFragmentShader2D;
    protected int mGLProgIdOES;
    protected int mGLProgId2D;
    protected int mGLAttribPosition;
    protected int mGLUniformTexture;
    protected int mGLAttribTextureCoordinate;
    protected int mGLStrengthLocation;
    protected int mOutputWidth;
    protected int mOutputHeight;
    protected boolean mIsInitialized;
    protected FloatBuffer mGLCubeBuffer;
    protected FloatBuffer mGLTextureBuffer;
    protected int mDisplayWidth, mDisplayHeight;

    protected  int[] mFrameBuffers = null;
    protected  int[] mFrameBufferTextures = null;
    private int mFrameWidth = -1;
    private int mFrameHeight = -1;

    private final float vertexPoint[] = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };

    private final float texturePoint[] = {
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
    };

	public CameraInputRender(){
	    mRunOnDraw = new LinkedList<Runnable>();
	    mVertexShader = CAMERA_INPUT_VERTEX_SHADER;
	    mFragmentShaderOES = CAMERA_INPUT_FRAGMENT_SHADER;
        mFragmentShader2D = CAMERA_INPUT_FRAGMENT2_SHADER;

	    mGLCubeBuffer = ByteBuffer.allocateDirect(vertexPoint.length * 4)
	                .order(ByteOrder.nativeOrder())
	                .asFloatBuffer();
	    mGLCubeBuffer.put(vertexPoint).position(0);

	    mGLTextureBuffer = ByteBuffer.allocateDirect(texturePoint.length * 4)
	                .order(ByteOrder.nativeOrder())
	                .asFloatBuffer();
	    mGLTextureBuffer.put(texturePoint).position(0);
	}

    public void init() {
        onInit();
        mIsInitialized = true;
    }

    protected void onInit() {
        mGLProgIdOES = GlUtil.createProgram(mVertexShader, mFragmentShaderOES);
        mGLProgId2D = GlUtil.createProgram(mVertexShader, mFragmentShader2D);
        mIsInitialized = true;
    }

	public void setTextureTransformMatrix(float[] mtx){
		mTextureTransformMatrix = mtx;
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }

    public void onDisplaySizeChanged(final int width, final int height) {
    	mDisplayWidth = width;
    	mDisplayHeight = height;
    }

    public void onOutputSizeChanged(final int width, final int height) {
        mOutputWidth = width;
        mOutputHeight = height;
    }

    public final void destroy() {
        mIsInitialized = false;
        GLES20.glDeleteProgram(mGLProgIdOES);
    }

	public int onDrawFrame(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        GLES20.glUseProgram(mGLProgIdOES);
        if(!mIsInitialized) {
            return -1;
        }
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glUniformMatrix4fv(mTextureTransformMatrixLocation, 1, false, mTextureTransformMatrix, 0);

        if(textureId != -1){
	        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
	        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
	        GLES20.glUniform1i(mGLUniformTexture, 0);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        return 0;
	}

	public int onDrawFrame(int textureId) {
        GLES20.glUseProgram(mGLProgIdOES);
        if(!mIsInitialized) {
            return -1;
        }
        mGLCubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mGLCubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        mGLTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, mGLTextureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glUniformMatrix4fv(mTextureTransformMatrixLocation, 1, false, mTextureTransformMatrix, 0);

        if(textureId != -1){
	        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
	        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
	        GLES20.glUniform1i(mGLUniformTexture, 0);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
   //     GLES20.glDisableVertexAttribArray(mGLAttribPosition);
    //    GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        return 0;
    }

	public int onDrawToTexture(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer, ByteBuffer buffer) {
		if(mFrameBuffers == null
                || !mIsInitialized)
			return -2;

    	GLES20.glUseProgram(mGLProgIdOES);
        GlUtil.checkGlError("glUseProgram");

        mGLAttribPosition = GLES20.glGetAttribLocation(mGLProgIdOES, "position");
        mGLUniformTexture = GLES20.glGetUniformLocation(mGLProgIdOES, "inputImageTexture");
        mTextureTransformMatrixLocation = GLES20.glGetUniformLocation(mGLProgIdOES, "textureTransform");
        mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(mGLProgIdOES,
                "inputTextureCoordinate");

        mGLCubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mGLCubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);

        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);

        GLES20.glUniformMatrix4fv(mTextureTransformMatrixLocation, 1, false, mTextureTransformMatrix, 0);

        if(textureId != -1){
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
            GLES20.glUniform1i(mGLUniformTexture, 0);
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GlUtil.checkGlError("glBindFramebuffer");
        GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        if (buffer != null) {
            GLES20.glReadPixels(0, 0, mOutputWidth, mOutputHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        }

        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
//        GLES20.glViewport(0, 0,mDisplayWidth, mDisplayHeight);
        GLES20.glUseProgram(0);

		return mFrameBufferTextures[0];
	}

    public int onDrawToTexture2(int texutreOut, int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer, ByteBuffer buffer) {
        if(mFrameBuffers == null
                || !mIsInitialized) {
            return -2;
        }

        GLES20.glUseProgram(mGLProgId2D);
        GlUtil.checkGlError("glUseProgram");

        mGLAttribPosition = GLES20.glGetAttribLocation(mGLProgId2D, "position");
        mGLUniformTexture = GLES20.glGetUniformLocation(mGLProgId2D, "inputImageTexture");
        mTextureTransformMatrixLocation = GLES20.glGetUniformLocation(mGLProgId2D, "textureTransform");
        mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(mGLProgId2D,
                "inputTextureCoordinate");

        mGLCubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mGLCubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);

        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);

        GLES20.glUniformMatrix4fv(mTextureTransformMatrixLocation, 1, false, mTextureTransformMatrix, 0);

        if(textureId != -1) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
//            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
            GLES20.glUniform1i(mGLUniformTexture, 0);
        }


        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[1]);
        GlUtil.checkGlError("glBindFramebuffer");
        GLES20.glViewport(0, 0,mOutputHeight, mOutputWidth);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, texutreOut, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

//        if (buffer != null) {
//            GLES20.glReadPixels(0, 0, mOutputHeight, mOutputWidth, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
//        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
//        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, 0, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);

        GLES20.glUseProgram(0);
        return texutreOut;
    }

	public void initCameraFrameBuffer(int width, int height) {
        if(mFrameBuffers != null && (mFrameWidth != width || mFrameHeight != height))
            destroyFramebuffers();

        if (mFrameBuffers == null) {
            mFrameWidth = width;
            mFrameHeight = height;
            mFrameBufferTextures = new int[1];

            GLES20.glGenTextures(1, mFrameBufferTextures, 0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);
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

            mFrameBuffers = new int[2];
            GLES20.glGenFramebuffers(2, mFrameBuffers, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }
	}

	public void destroyFramebuffers() {
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(1, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(2, mFrameBuffers, 0);
            mFrameBuffers = null;
        }

        mFrameWidth = -1;
        mFrameHeight = -1;
    }
}
