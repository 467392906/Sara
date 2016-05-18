package com.sensetime.stmobileapi;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public interface STMobileApiBridge extends Library {
    class cv_rect_t extends Structure {
    	
        public static class ByValue extends cv_rect_t implements Structure.ByValue {
        }

        public int left;
        public int top;
        public int right;
        public int bottom;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[]{"left", "top", "right", "bottom"});
        }

        @Override
        public cv_rect_t clone() {
        	cv_rect_t copy = new cv_rect_t();
            copy.left = this.left;
            copy.top = this.top;
            copy.right = this.right;
            copy.bottom = this.bottom;
            return copy;
        }

        /**
         * The jna.Structure class is passed on by reference by default,
         * however, in some cases, we need it by value.
         */
        public cv_rect_t.ByValue copyToValue() {
        	cv_rect_t.ByValue retObj = new cv_rect_t.ByValue();
            retObj.left = this.left;
            retObj.top = this.top;
            retObj.right = this.right;
            retObj.bottom = this.bottom;
            return retObj;
        }
    }
    
    class st_mobile_106_t extends Structure {
    	public cv_rect_t rect;
    	public float score;
    	public float[] points_array = new float[212];
    	public int yaw;
    	public int pitch;
    	public int roll;
    	public int eye_dist;
    	public int ID;

        public st_mobile_106_t() {
            super();
        }

        public st_mobile_106_t(Pointer p) {
            super(p);
        }

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[]{"rect", "score", "points_array", 
            		"yaw", "pitch", "roll", "eye_dist", "ID"});
        }

        @Override
        public st_mobile_106_t clone() {
        	st_mobile_106_t copy = new st_mobile_106_t();
            copy.rect = this.rect.clone();
            copy.score = this.score;
            copy.points_array = this.points_array;
            copy.yaw = this.yaw;
            copy.pitch = this.pitch;
            copy.roll = this.roll;
            copy.eye_dist = this.eye_dist;
            copy.ID = this.ID;
            return copy;
        }

        public static st_mobile_106_t[] arrayCopy(st_mobile_106_t[] origin) {
        	st_mobile_106_t[] copy = new st_mobile_106_t[origin.length];
            for (int i = 0; i < origin.length; ++i) {
                copy[i] = origin[i].clone();
            }
            return copy;
        }
    }

    enum ResultCode {
        CV_OK(0),
        CV_E_INVALIDARG(-1),
        CV_E_HANDLE(-2),
        CV_E_OUTOFMEMORY(-3),
        CV_E_FAIL(-4),
        CV_E_DELNOTFOUND(-5);

        private final int resultCode;

        ResultCode(int resultCode) {
            this.resultCode = resultCode;
        }

        public int getResultCode() {
            return resultCode;
        }
    }

    /**
     * The instance of stmobile DLL
     */
    STMobileApiBridge FACESDK_INSTANCE = (STMobileApiBridge) Native.loadLibrary("st_mobile", STMobileApiBridge.class);

    //cv_mobile_face_106
    int st_mobile_tracker_106_create(String model_path, int config, PointerByReference handle);
    
    int st_mobile_tracker_106_set_facelimit(Pointer handle,int max_facecount);
    
    int st_mobile_tracker_106_set_detectinternal(Pointer handle,int val);
    
    int st_mobile_tracker_106_track(
    	Pointer handle,
    	byte[] image,
    	int pixel_format,
    	int image_width,
    	int image_height,
    	int image_stride,
    	int orientation,
    	PointerByReference p_faces_array,
    	IntByReference p_faces_count
    );
    
    int st_mobile_tracker_106_track(
        	Pointer handle,
        	int[] image,
        	int pixel_format,
        	int image_width,
        	int image_height,
        	int image_stride,
        	int orientation,
        	PointerByReference p_faces_array,
        	IntByReference p_faces_count
        );
    
    void st_mobile_tracker_106_release_result(Pointer faces_array, int faces_count);
    
    void st_mobile_tracker_106_destroy(Pointer handle);
    
}
