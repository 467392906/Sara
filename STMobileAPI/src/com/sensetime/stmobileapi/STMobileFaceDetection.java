package com.sensetime.stmobileapi;

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
	private static boolean DEBUG = true;// false;
	private String TAG = "FaceDetection";
	private boolean authFromBuffer = true;                  //默认从缓存读取license来认证
    public STMobileAuthentification stMobileAuth = null;
	public static int ST_MOBILE_DETECT_DEFAULT_CONFIG = 0x00000000;  ///< 榛樿閫夐」
	public static int ST_MOBILE_DETECT_FAST = 0x00000001;  ///< resize鍥惧儚涓洪暱杈�320鐨勫浘鍍忎箣鍚庡啀妫�娴嬶紝缁撴灉澶勭悊涓哄師鍥惧儚瀵瑰簲缁撴灉
	public static int ST_MOBILE_DETECT_BALANCED = 0x00000002;  ///< resize鍥惧儚涓洪暱杈�640鐨勫浘鍍忎箣鍚庡啀妫�娴嬶紝缁撴灉澶勭悊涓哄師鍥惧儚瀵瑰簲缁撴灉
	public static int ST_MOBILE_DETECT_ACCURATE = 0x00000004;
	
    PointerByReference ptrToArray = new PointerByReference();
    IntByReference ptrToSize = new IntByReference();

    public STMobileFaceDetection(Context context, int config,  AuthCallback authCallback) {
        PointerByReference handlerPointer = new PointerByReference();
        stMobileAuth = new STMobileAuthentification(context, authFromBuffer, authCallback);
		
        int memory_size = 1024;
        IntByReference codeLen = new IntByReference(1);
        codeLen.setValue(memory_size);
        Pointer generateActiveCode = new Memory(memory_size);
        generateActiveCode.setMemory(0, memory_size, (byte)0);

        if(authFromBuffer) {
        	if(stMobileAuth.hasAuthentificatedByBuffer(context, generateActiveCode, codeLen)) {
                int rst = STMobileApiBridge.FACESDK_INSTANCE.st_mobile_face_detection_create(STUtils.getModelPath(STUtils.MODEL_NAME, context), config, handlerPointer);
                Log.e(TAG, "-->> create handler rst = " + rst);
                if (rst != ResultCode.ST_OK.getResultCode()) {
                    return;
                }
                detectHandle = handlerPointer.getValue();
            }

        } else {          
            if (stMobileAuth.hasAuthentificatd(context, generateActiveCode, codeLen)) {
                int rst = STMobileApiBridge.FACESDK_INSTANCE.st_mobile_face_detection_create(STUtils.getModelPath(STUtils.MODEL_NAME, context), config, handlerPointer);
                Log.e(TAG, "-->> create handler rst = " + rst);
                if (rst != ResultCode.ST_OK.getResultCode()) {
                    return;
                }
                detectHandle = handlerPointer.getValue();
            }
        }
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
        Log.e(TAG, "-->> detect array ="+array);
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
