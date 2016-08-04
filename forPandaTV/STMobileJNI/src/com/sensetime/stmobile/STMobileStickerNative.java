package com.sensetime.stmobile;

import android.R.integer;

public class STMobileStickerNative {
	static
	{
		System.loadLibrary("st_mobile");
		System.loadLibrary( "stmobile_jni" );
	}
	
	private long nativeStickerHandle;
	
	private long nativeHumanActionHandle;
	
	public native int createInstance(String zippath, String modelpath,int config);
	
	public native int processBuffer(byte[] pInputImage, int rotate, int imageWidth, int imageHeight,boolean needsMirroring, int textureOut);
	
	public native int processTexture(int textureIn, byte[] pInputImage, int rotate, int imageWidth, int imageHeight, boolean needsMirroring, int textureOut);
	
	public native int changeSticker(String path);
	
	public native void  destoryInstance();
}
