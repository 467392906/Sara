package com.sensetime.stmobileapi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sensetime.stmobileapi.STMobileApiBridge.ResultCode;
import com.sensetime.stmobileapi.STMobileApiBridge.st_mobile_106_t;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

public class STMobileFaceDetection {
	private Pointer detectHandle;
	private Context mContext;
	private static boolean DEBUG = true;// false;
	private String TAG = "FaceDetection";
	private static final String DETECTION_MODEL_NAME = "face_track_2.0.0.model";
	private static final String LICENSE_NAME = "SENSEME_106.lic";
	public static int ST_MOBILE_DETECT_DEFAULT_CONFIG = 0x00000000;  ///< 榛樿閫夐」
	public static int ST_MOBILE_DETECT_FAST = 0x00000001;  ///< resize鍥惧儚涓洪暱杈�320鐨勫浘鍍忎箣鍚庡啀妫�娴嬶紝缁撴灉澶勭悊涓哄師鍥惧儚瀵瑰簲缁撴灉
	public static int ST_MOBILE_DETECT_BALANCED = 0x00000002;  ///< resize鍥惧儚涓洪暱杈�640鐨勫浘鍍忎箣鍚庡啀妫�娴嬶紝缁撴灉澶勭悊涓哄師鍥惧儚瀵瑰簲缁撴灉
	public static int ST_MOBILE_DETECT_ACCURATE = 0x00000004;
	
    PointerByReference ptrToArray = new PointerByReference();
    IntByReference ptrToSize = new IntByReference();

    public STMobileFaceDetection(Context context, int config) {
        PointerByReference handlerPointer = new PointerByReference();
		mContext = context;
		synchronized(this.getClass())
		{
		   copyModelIfNeed(DETECTION_MODEL_NAME);
		   copyModelIfNeed(LICENSE_NAME);
		}
		String modulePath = getModelPath(DETECTION_MODEL_NAME);
		String licensePath = getModelPath(LICENSE_NAME);

        int memory_size = 1024;
        IntByReference codeLen = new IntByReference(1);
        codeLen.setValue(memory_size);
         Pointer generateActiveCode = new Memory(memory_size);
        generateActiveCode.setMemory(0, memory_size, (byte)0);

        if(hasAuthentificatd(context, licensePath, generateActiveCode, codeLen)) {
        	int rst = STMobileApiBridge.FACESDK_INSTANCE.st_mobile_face_detection_create(modulePath, config, handlerPointer);
        	Log.e(TAG, "-->> create handler rst = "+rst);
        	if(rst != ResultCode.ST_OK.getResultCode())
        	{
        		return;
        	}
            detectHandle = handlerPointer.getValue();
        }
    }
    
 // 授权
    private boolean hasAuthentificatd(Context context, String licensePath,Pointer generatedActiveCode, IntByReference codeLen) {
        SharedPreferences sp = context.getSharedPreferences("ActiveCodeFile", 0);
        boolean isFirst = sp.getBoolean("isFirst", true);
        int rst = Integer.MIN_VALUE;
        if(isFirst) {
            rst = STMobileApiBridge.FACESDK_INSTANCE.st_mobile_generate_activecode("SenseME_106", licensePath, generatedActiveCode, codeLen);
            if(rst != ResultCode.ST_OK.getResultCode()) {
                Log.e(TAG, "-->> generate active code failed!");
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
            Log.e(TAG, "-->> activeCode is null in SharedPreference");
            return false;
        }

        rst = STMobileApiBridge.FACESDK_INSTANCE.st_mobile_check_activecode("SenseME_106", licensePath, activeCode);
        if(rst != ResultCode.ST_OK.getResultCode()) {
            // check失败，也有可能是新的license替换，但是还是用的原来lincense生成的activecode。在这里重新生成一次activecode
            rst = STMobileApiBridge.FACESDK_INSTANCE.st_mobile_generate_activecode("SenseME_106", licensePath, generatedActiveCode, codeLen);

            if(rst != ResultCode.ST_OK.getResultCode()) {
                Log.e(TAG, "-->> again generate active code failed! license may invalide");
                return false;
            }
            activeCode = new String(generatedActiveCode.getByteArray(0, codeLen.getValue()));
            rst = STMobileApiBridge.FACESDK_INSTANCE.st_mobile_check_activecode("SenseME_106", licensePath, activeCode);
            if(rst != ResultCode.ST_OK.getResultCode()) {
                Log.e(TAG, "-->> again invalide active code, you need a new license");
                return false;
            }

            SharedPreferences.Editor editor = sp.edit();
            editor.putString("activecode", activeCode);
            editor.putBoolean("isFirst", false);
            editor.commit();
        }

        return true;
    }
    
	private void copyModelIfNeed(String modelName) {
		String path = getModelPath(modelName);
		if (path != null) {
			File modelFile = new File(path);
			if (!modelFile.exists()) {
				//濡傛灉妯″瀷鏂囦欢涓嶅瓨鍦ㄦ垨鑰呭綋鍓嶆ā鍨嬫枃浠剁殑鐗堟湰璺焥dcard涓殑鐗堟湰涓嶄竴鏍�
				try {
					if (modelFile.exists())
						modelFile.delete();
					modelFile.createNewFile();
					InputStream in = mContext.getApplicationContext().getAssets().open(modelName);
					if(in == null)
					{
						Log.e(TAG, "the src module is not existed");
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
				}
			}
		}
	}
	
	protected String getModelPath(String modelName) {
		String path = null;
		File dataDir = mContext.getApplicationContext().getExternalFilesDir(null);
		if (dataDir != null) {
			path = dataDir.getAbsolutePath() + File.separator + modelName;
		}
		return path;
	}
	
	public void destory()
	{
    	long start_destroy = System.currentTimeMillis();
    	if(detectHandle != null) {
    		STMobileApiBridge.FACESDK_INSTANCE.st_mobile_face_detection_destroy(detectHandle);
    		detectHandle = null;
    	}
        long end_destroy = System.currentTimeMillis();
        Log.i(TAG, "destroy cost "+(end_destroy - start_destroy)+" ms");
	}
	
    /**
     * Given the Image by Bitmap to detect face
     * @param image Input image by Bitmap
     * @param orientation Image orientation
     * @return CvFace array, each one in array is Detected by SDK native API
     */
    public STMobile106[] detect(Bitmap image, int orientation) {
    	if(DEBUG) 
    		Log.d(TAG, "detect bitmap");
    	
        int[] colorImage = STUtils.getBGRAImageByte(image);
        return detect(colorImage, STImageFormat.ST_PIX_FMT_BGRA8888,image.getWidth(), image.getHeight(), image.getWidth() * 4, orientation);
    }
    
    /**
     * Given the Image by Byte Array to detect face
     * @param colorImage Input image by int
     * @param cvImageFormat Image format
     * @param imageWidth Image width
     * @param imageHeight Image height
     * @param imageStride Image stride
     * @param orientation Image orientation
     * @return CvFace array, each one in array is Detected by SDK native API
     */
    public STMobile106[] detect(int[] colorImage,int cvImageFormat, int imageWidth, int imageHeight, int imageStride, int orientation) {
    	if(DEBUG)
    		Log.d(TAG, "detect int array");
    	
    	if(detectHandle == null){
    		return null;
    	}
        long startTime = System.currentTimeMillis();

        int rst = STMobileApiBridge.FACESDK_INSTANCE.st_mobile_face_detection_detect(detectHandle, colorImage, cvImageFormat,imageWidth,
                imageHeight, imageStride, orientation, ptrToArray, ptrToSize);
        long endTime = System.currentTimeMillis();
        
        if(DEBUG)Log.d(TAG, "detect time: "+(endTime-startTime)+"ms");
        
        if (rst != ResultCode.ST_OK.getResultCode()) {
            throw new RuntimeException("Calling st_mobile_face_detection_detect() method failed! ResultCode=" + rst);
        }

        if (ptrToSize.getValue() == 0) {
        	if(DEBUG)Log.d(TAG, "ptrToSize.getValue() == 0");
            return new STMobile106[0];
        }

        st_mobile_106_t arrayRef = new st_mobile_106_t(ptrToArray.getValue());
        arrayRef.read();
        st_mobile_106_t[] array = st_mobile_106_t.arrayCopy((st_mobile_106_t[]) arrayRef.toArray(ptrToSize.getValue()));
        STMobileApiBridge.FACESDK_INSTANCE.st_mobile_face_detection_release_result(ptrToArray.getValue(), ptrToSize.getValue());
        
        STMobile106[] ret = new STMobile106[array.length]; 
        for (int i = 0; i < array.length; i++) {
        	ret[i] = new STMobile106(array[i]);
        }
        
        if(DEBUG)Log.d(TAG, "track : "+ ret);
        
        return ret;
    }
    
    /**
     * Given the Image by Byte to detect face
     * @param image Input image by byte
     * @param orientation Image orientation
     * @param width Image width
     * @param height Image height
     * @return CvFace array, each one in array is Detected by SDK native API
     */
    public STMobile106[] detect(byte[] image, int orientation,int width,int height) {
    	if(DEBUG){
    		Log.d(TAG, "detect byte array");
    	}
    	
        return detect(image, STImageFormat.ST_PIX_FMT_NV21,width, height, width, orientation);
    }

    /**
     * Given the Image by Byte Array to detect face
     * @param colorImage Input image by byte
     * @param cvImageFormat Image format
     * @param imageWidth Image width
     * @param imageHeight Image height
     * @param imageStride Image stride
     * @param orientation Image orientation
     * @return CvFace array, each one in array is Detected by SDK native API
     */
    public STMobile106[] detect(byte[] colorImage,int cvImageFormat, int imageWidth, int imageHeight, int imageStride, int orientation) {
    	if(DEBUG){
    		Log.d(TAG, "detect 111");
    	}
    	
    	if(detectHandle == null){
    		return null;
    	}
        long startTime = System.currentTimeMillis();

        int rst = STMobileApiBridge.FACESDK_INSTANCE.st_mobile_face_detection_detect(detectHandle, colorImage, cvImageFormat,imageWidth,
                imageHeight, imageStride, orientation, ptrToArray, ptrToSize);
        long endTime = System.currentTimeMillis();
        
        if(DEBUG)Log.d(TAG, "detect time: "+(endTime-startTime)+"ms");
        
        if (rst != ResultCode.ST_OK.getResultCode()) {
            throw new RuntimeException("Calling st_mobile_face_detection_detect() method failed! ResultCode=" + rst);
        }

        if (ptrToSize.getValue() == 0) {
            return new STMobile106[0];
        }

        st_mobile_106_t arrayRef = new st_mobile_106_t(ptrToArray.getValue());
        arrayRef.read();
        st_mobile_106_t[] array = st_mobile_106_t.arrayCopy((st_mobile_106_t[]) arrayRef.toArray(ptrToSize.getValue()));
        STMobileApiBridge.FACESDK_INSTANCE.st_mobile_face_detection_release_result(ptrToArray.getValue(), ptrToSize.getValue());
        
        STMobile106[] ret = new STMobile106[array.length]; 
        for (int i = 0; i < array.length; i++) {
        	ret[i] = new STMobile106(array[i]);
        }
        
        if(DEBUG)Log.d(TAG, "track : "+ ret);
        
        return ret;
    }
}
