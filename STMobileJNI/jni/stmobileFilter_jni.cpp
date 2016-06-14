#include <jni.h>

#include <android/log.h>

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "prebuilt/include/st_mobile.h"
#include "jni_common.h"

#define  LOG_TAG    "STMobileFilter"

jbyteArray as_byte_array(JNIEnv *env, unsigned char* buf, int len) {
	jbyteArray array = env->NewByteArray(len);
	env->SetByteArrayRegion(array, 0, len, reinterpret_cast<jbyte*>(buf));
	return array;
}

//-------------------------------------------------------------------------------------------------
unsigned char* as_unsigned_char_array(JNIEnv *env, jbyteArray array) {
	int len = env->GetArrayLength(array);
	unsigned char* buf = new unsigned char[len];
	env->GetByteArrayRegion(array, 0, len, reinterpret_cast<jbyte*>(buf));
	return buf;
}

extern "C" {
JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STImageFilterNative_initBeautify(JNIEnv * env, jobject obj,jint width, jint height);
JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STImageFilterNative_stColorConvert(JNIEnv * env, jobject obj, jbyteArray imagesrc, jbyteArray imagedst, jint imageWidth, jint imageHeight, jint type);
JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STImageFilterNative_setParam(JNIEnv * env, jobject obj, jint type, jfloat value);
JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STImageFilterNative_processBufferWithCurrentContext(JNIEnv * env, jobject obj,jbyteArray pInputImage, jint informat, jint outputWidth, jint outputHeight, jbyteArray pOutputImage, jint outformat);
JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STImageFilterNative_processBufferWithNewContext(JNIEnv * env, jobject obj,jbyteArray pInputImage, jint informat, jint outputWidth, jint outputHeight, jbyteArray pOutputImage, jint outformat);
JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STImageFilterNative_processTexture(JNIEnv * env, jobject obj,jint textureIn, jint outputWidth, jint outputHeight, jint textureOut);
JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STImageFilterNative_destoryBeautify(JNIEnv * env, jobject obj);
};

JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STImageFilterNative_initBeautify(JNIEnv * env, jobject obj,jint width, jint height)
{
	LOGE("initBeautify Enter");
	st_handle_t handle;
	int result = (int)st_mobile_beautify_create(width, height,&handle);
	if(result != 0)
	{
		LOGE("create handle failed");
		return result;
	}
	setHandle(env, obj, handle);
	LOGE("initBeautify Exit");
	return result;
}

JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STImageFilterNative_stColorConvert(JNIEnv * env, jobject obj, jbyteArray imagesrc, jbyteArray imagedst, jint imageWidth, jint imageHeight, jint type)
{
    jbyte *srcdata = (jbyte*) (env->GetPrimitiveArrayCritical(imagesrc, 0));
    jbyte *dstdata = (jbyte*) env->GetPrimitiveArrayCritical(imagedst, 0);

    int result = (int)st_mobile_color_convert((unsigned char *)srcdata,(unsigned char *)dstdata,imageWidth,imageHeight,(st_color_convert_type)type);

    env->ReleasePrimitiveArrayCritical(imagesrc, srcdata, 0);
    env->ReleasePrimitiveArrayCritical(imagedst, dstdata, 0);
    return result;
}

JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STImageFilterNative_setParam(JNIEnv * env, jobject obj, jint type, jfloat value)
{
	st_handle_t handle = getHandle<st_handle_t>(env, obj);
    if(handle == NULL)
    {
    	return JNI_FALSE;
    }
    LOGE("set Param for %f", value);
	int result = (int)st_mobile_beautify_setparam(handle,(st_beautify_type)type,value);
	return result;
}

JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STImageFilterNative_processBufferWithCurrentContext(JNIEnv * env, jobject obj,jbyteArray pInputImage, jint informat, jint outputWidth, jint outputHeight, jbyteArray pOutputImage, jint outformat)
{
	LOGE("Enter processBuffer");
    st_handle_t handle = getHandle<st_handle_t>(env, obj);

    if(handle == NULL)
    {
    	LOGE("processBuffer---handle is null");
    	return JNI_FALSE;
    }

    jbyte *srcdata = (jbyte*) (env->GetPrimitiveArrayCritical(pInputImage, 0));
    jbyte *dstdata = (jbyte*) env->GetPrimitiveArrayCritical(pOutputImage, 0);

    st_pixel_format pixel_format = (st_pixel_format)informat;
    int stride = 0;
    switch(pixel_format)
    {
		case ST_PIX_FMT_NV21:
			stride = outputWidth;
			break;
		case ST_PIX_FMT_BGRA8888:
		case ST_PIX_FMT_RGBA8888:
			stride = outputWidth *4;
			break;
		default:
			break;
    }

    int result = (int)st_mobile_beautify_process_buffer(handle,(unsigned char *)srcdata, (st_pixel_format)pixel_format, outputWidth, outputHeight, stride,(unsigned char*)dstdata,(st_pixel_format)outformat);
    env->ReleasePrimitiveArrayCritical(pInputImage, srcdata, 0);
    env->ReleasePrimitiveArrayCritical(pOutputImage, dstdata, 0);
	LOGE("Exit processBuffer");
	return result;
}

JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STImageFilterNative_processBufferWithNewContext(JNIEnv * env, jobject obj,jbyteArray pInputImage, jint informat, jint outputWidth, jint outputHeight, jbyteArray pOutputImage, jint outformat)
{
	LOGE("Enter processPicture");
    st_handle_t handle = getHandle<st_handle_t>(env, obj);

    if(handle == NULL)
    {
    	LOGE("processBuffer---handle is null");
    	return JNI_FALSE;
    }

    jbyte *srcdata = (jbyte*) (env->GetPrimitiveArrayCritical(pInputImage, 0));
    jbyte *dstdata = (jbyte*) env->GetPrimitiveArrayCritical(pOutputImage, 0);

    st_pixel_format pixel_format = (st_pixel_format)informat;
    int stride = 0;
    switch(pixel_format)
    {
		case ST_PIX_FMT_NV21:
			stride = outputWidth;
			break;
		case ST_PIX_FMT_BGRA8888:
		case ST_PIX_FMT_RGBA8888:
			stride = outputWidth *4;
			break;
		default:
			break;
    }

    int result = (int)st_mobile_beautify_process_picture(handle,(unsigned char *)srcdata, (st_pixel_format)pixel_format, outputWidth, outputHeight, stride,(unsigned char*)dstdata,(st_pixel_format)outformat);
    env->ReleasePrimitiveArrayCritical(pInputImage, srcdata, 0);
    env->ReleasePrimitiveArrayCritical(pOutputImage, dstdata, 0);
	LOGE("Exit processPicture");
	return result;
}

JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STImageFilterNative_processTexture(JNIEnv * env, jobject obj,jint textureIn, jint outputWidth, jint outputHeight, jint textureOut)
{
	LOGE("Enter processTexture, the texture in ID is %d",textureIn);

    st_handle_t handle = getHandle<st_handle_t>(env, obj);

    if(handle == NULL)
    {
    	LOGE("processTexture---handle is null");
    	return JNI_FALSE;
    }

//	jint *textureId = (jint*) (env->GetPrimitiveArrayCritical(textureOut, 0));

    int stride = outputWidth *4;

	int result = (int)st_mobile_beautify_process_texture(handle, textureIn,
			outputWidth, outputHeight,textureOut);
	LOGE("Exit processTexture, the result is %d", result);

//	env->ReleasePrimitiveArrayCritical(textureOut, textureId, 0);
	return result;
}

JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STImageFilterNative_destoryBeautify(JNIEnv * env, jobject obj)
{
	LOGI("Enter destoryBeautify");
	st_handle_t handle = getHandle<st_handle_t>(env, obj);
    if(handle == NULL)
    {
    	LOGE("destoryBeautify---handle is null");
    	return JNI_FALSE;
    }
	st_mobile_beautify_destroy(handle);
}
