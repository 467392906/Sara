package com.sensetime.stmobileapi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.sensetime.stmobileapi.STMobileApiBridge.ResultCode;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class STMobileAuthentification {
	private String TAG = this.getClass().getSimpleName();
    
	private Context mContext;
    
    private static STMobileAuthentification instance = null;
    private AuthCallback authCallback = null;
    private String licenseStr = "";
    
    private STMobileAuthentification(Context context, boolean authFromBuffer, AuthCallback callback) {
    	mContext = context;
    	this.authCallback = callback;
    	
		synchronized(this.getClass()) {
		    STUtils.copyModelIfNeed(STUtils.MODEL_NAME, mContext);
            if(!authFromBuffer) {                   //if authentificate by sdCard
                STUtils.copyModelIfNeed(STUtils.LICENSE_NAME, mContext);
            } else {
            	// 从缓存读取License来认证
                try {
                    InputStreamReader isr = new InputStreamReader(context.getResources().getAssets().open(STUtils.LICENSE_NAME));
                    BufferedReader br = new BufferedReader(isr);
                    String line = "";
                    while((line=br.readLine()) != null) {
                        licenseStr += line;
                        licenseStr += "\n";
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }          	
            }
		}
    }

	public static STMobileAuthentification getInstance(Context context, boolean authFromBuffer, AuthCallback authCallback) {
    	if(instance == null) {
    		instance = new STMobileAuthentification(context, authFromBuffer, authCallback);
    	} 
    	
    	return instance;
    }
	
    // 授权
    // 从Buffer读取License来授权
    public boolean hasAuthentificatedByBuffer(Context context, Pointer generatedActiveCode, IntByReference codeLen) {
        SharedPreferences sp = context.getSharedPreferences("ActiveCodeFile", 0);
        boolean isFirst = sp.getBoolean("isFirst", true);
        int rst = Integer.MIN_VALUE;
        if(isFirst) {
            rst = STMobileApiBridge.FACESDK_INSTANCE.st_mobile_generate_activecode_from_buffer(licenseStr, licenseStr.length(), generatedActiveCode, codeLen);
            if(rst != ResultCode.ST_OK.getResultCode()) {
            	authCallback.authErr("generate active code failed! errCode="+rst);
                return false;
            }

            String activeCode = new String(generatedActiveCode.getByteArray(0, codeLen.getValue()));//            String activeCode = Native.toString(generatedActiveCode);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("activecode", activeCode);
            editor.putBoolean("isFirst", false);
            editor.commit();
        }

        String activeCode = sp.getString("activecode", "null");
        if(activeCode==null || activeCode.length()==0) {
        	authCallback.authErr("activeCode is null in SharedPreference!");
            return false;
        }

        rst = STMobileApiBridge.FACESDK_INSTANCE.st_mobile_check_activecode_from_buffer( licenseStr, licenseStr.length(), activeCode);
        if(rst != ResultCode.ST_OK.getResultCode()) {
        	authCallback.authErr("check activecode failed! errCode="+rst);
            // check失败，也有可能是新的license替换，但是还是用的原来lincense生成的activecode。在这里重新生成一次activecode
            rst = STMobileApiBridge.FACESDK_INSTANCE.st_mobile_generate_activecode_from_buffer(licenseStr, licenseStr.length(), generatedActiveCode, codeLen);

            if(rst != ResultCode.ST_OK.getResultCode()) {
            	authCallback.authErr("again generate active code failed! license may invalide! errCode="+rst);
                return false;
            }
            activeCode = new String(generatedActiveCode.getByteArray(0, codeLen.getValue()));
            rst = STMobileApiBridge.FACESDK_INSTANCE.st_mobile_check_activecode_from_buffer( licenseStr, licenseStr.length(), activeCode);
            if(rst != ResultCode.ST_OK.getResultCode()) {
                authCallback.authErr("again check active code failed, you need a new license! errCode="+rst);
                return false;
            }

            SharedPreferences.Editor editor = sp.edit();
            editor.putString("activecode", activeCode);
            editor.putBoolean("isFirst", false);
            editor.commit();
        }
        
        authCallback.authErr("you have been authorized!");
        
        return true;
    }
    
    // 从SD卡读取License来授权
    public boolean hasAuthentificatd(Context context, Pointer generatedActiveCode, IntByReference codeLen) {
    	String licensePath = STUtils.getModelPath(STUtils.LICENSE_NAME, mContext);
        SharedPreferences sp = context.getSharedPreferences("ActiveCodeFile", 0);
        boolean isFirst = sp.getBoolean("isFirst", true);
        int rst = Integer.MIN_VALUE;
        if(isFirst) {
            rst = STMobileApiBridge.FACESDK_INSTANCE.st_mobile_generate_activecode( licensePath, generatedActiveCode, codeLen);
            if(rst != ResultCode.ST_OK.getResultCode()) {
            	authCallback.authErr("generate active code failed! errCode="+rst);
                return false;
            }
            
            String activeCode = new String(generatedActiveCode.getByteArray(0, codeLen.getValue()));//            String activeCode = Native.toString(generatedActiveCode);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("activecode", activeCode);
            editor.putBoolean("isFirst", false);
            editor.commit();
        }

        String activeCode = sp.getString("activecode", "null");
        if(activeCode==null || activeCode.length()==0) {
        	authCallback.authErr("activeCode is null in SharedPreference!");
            return false;
        }

        rst = STMobileApiBridge.FACESDK_INSTANCE.st_mobile_check_activecode( licensePath, activeCode);
        if(rst != ResultCode.ST_OK.getResultCode()) {
        	authCallback.authErr("check activecode failed! errCode="+rst);
            // check失败，也有可能是新的license替换，但是还是用的原来lincense生成的activecode。在这里重新生成一次activecode
            rst = STMobileApiBridge.FACESDK_INSTANCE.st_mobile_generate_activecode( licensePath, generatedActiveCode, codeLen);

            if(rst != ResultCode.ST_OK.getResultCode()) {
            	authCallback.authErr("again generate active code failed! license may invalide! errCode="+rst);
                return false;
            }
            activeCode = new String(generatedActiveCode.getByteArray(0, codeLen.getValue()));
            rst = STMobileApiBridge.FACESDK_INSTANCE.st_mobile_check_activecode( licensePath, activeCode);
            if(rst != ResultCode.ST_OK.getResultCode()) {
                authCallback.authErr("again check active code failed, you need a new license! errCode="+rst);
                return false;
            }

            SharedPreferences.Editor editor = sp.edit();
            editor.putString("activecode", activeCode);
            editor.putBoolean("isFirst", false);
            editor.commit();
        }

        authCallback.authErr("you have been authorized!");
        return true;
    }
    
}
