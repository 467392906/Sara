#include <jni.h>

#include <android/log.h>

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <sys/time.h>
#include <time.h>
#include "prebuilt/include/st_common.h"
#include "prebuilt/include/st_mobile_beautify.h"
#include "prebuilt/include/st_mobile_human_action.h"
#include "prebuilt/include/st_mobile_sticker.h"

#include<fcntl.h>

#include "jni_common.h"

#define  LOG_TAG    "STMobileSticker"

extern "C" {
JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_createInstance(JNIEnv * env, jobject obj, jstring zippath, jstring modelpath, jint config);
JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_processBuffer(JNIEnv * env, jobject obj, jbyteArray pInputImage, jint rotate, jint imageWidth, jint imageHeight,jboolean needsMirroring,  jint textureOut);
JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_processTexture(JNIEnv * env, jobject obj, jint textureIn, jbyteArray pInputImage, jint rotate, jint imageWidth, jint imageHeight, jboolean needsMirroring, jint textureOut);
JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_changeSticker(JNIEnv * env, jobject obj, jstring path);
JNIEXPORT void JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_destoryInstance(JNIEnv * env, jobject obj);
};

long getCurrentTime()
{
   struct timeval tv;
   gettimeofday(&tv,NULL);
   return tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

jfieldID    sticker_handle;
jfieldID    humanaction_handle;

static inline jfieldID getHumanActionHandleField(JNIEnv *env, jobject obj)
{
    jclass c = env->GetObjectClass(obj);
    // J is the type signature for long:
    return env->GetFieldID(c, "nativeHumanActionHandle", "J");
}

static inline jfieldID getStickerHandleField(JNIEnv *env, jobject obj)
{
    jclass c = env->GetObjectClass(obj);
    // J is the type signature for long:
    return env->GetFieldID(c, "nativeStickerHandle", "J");
}

void setHumanActionHandle(JNIEnv *env, jobject obj, void * h)
{
    	jlong handle = reinterpret_cast<jlong>(h);
    	env->SetLongField(obj, getHumanActionHandleField(env, obj), handle);
}

void setStickerHandle(JNIEnv *env, jobject obj, void * h)
{
		jlong handle = reinterpret_cast<jlong>(h);
		env->SetLongField(obj, getStickerHandleField(env, obj), handle);
}

void* getHumanActionHandle(JNIEnv *env, jobject obj)
{
    	jlong handle = env->GetLongField(obj, getHumanActionHandleField(env, obj));
    	return reinterpret_cast<void *>(handle);
}

void* getStickerHandle(JNIEnv *env, jobject obj)
{
		jlong handle = env->GetLongField(obj, getStickerHandleField(env, obj));
		return reinterpret_cast<void *>(handle);
}

JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_createInstance(JNIEnv * env, jobject obj, jstring zippath, jstring modelpath, jint config)
{
	LOGE("createInstance");
	st_handle_t  ha_handle = NULL;
	const char *modelpathChars = env->GetStringUTFChars(modelpath, 0);
	int result = st_mobile_human_action_create(modelpathChars, config, &ha_handle);
	if(result != 0)
	{
		 LOGE("create handle for human action failed");
		 return result;
	}
	setHumanActionHandle(env, obj, ha_handle);

	st_handle_t sticker_handle = NULL;
	const char *zippathChars = env->GetStringUTFChars(zippath, 0);
    result = st_mobile_sticker_create(zippathChars, &sticker_handle);
	if(result != 0)
	{
		 LOGE("st_mobile_sticker_create failed");
		 return result;
	}
	setStickerHandle(env, obj, sticker_handle);

	return result;
}

JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_processBuffer(JNIEnv * env, jobject obj, jbyteArray pInputImage, jint rotate, jint imageWidth, jint imageHeight, jboolean needsMirroring, jint textureOut)
{
	LOGE("processBuffer, the width is %d, the height is %d, the rotate is %d",imageWidth, imageHeight, rotate);
	int result = -10;
	st_handle_t humanActionhandle = getHumanActionHandle(env, obj);
	st_handle_t stickerhandle = getStickerHandle(env, obj);

	if(humanActionhandle == NULL || stickerhandle == NULL)
	{
		LOGE("handle is null");
	}
	jbyte *srcdata = (jbyte*) (env->GetPrimitiveArrayCritical(pInputImage, 0));

	int image_stride = imageWidth * 4;
	int detect_config = 0x0000003F ;

	st_mobile_human_action_t human_action;

	if(humanActionhandle != NULL)
	{
		  result =  st_mobile_human_action_detect(humanActionhandle, (unsigned char *)srcdata,  ST_PIX_FMT_RGBA8888,  imageWidth,
				   	   	   	   	   	   imageHeight, image_stride, (st_rotate_type)rotate, detect_config, &human_action);
			LOGE("st_mobile_human_action_detect --- result is %d", result);
	}

	LOGE("the face count is %d", human_action.face_count);

	if(stickerhandle != NULL)
	{
		result  = st_mobile_sticker_process_buffer(stickerhandle, (unsigned char *)srcdata, imageWidth, imageHeight,  (st_rotate_type)rotate,needsMirroring, &human_action,textureOut);
		LOGE("st_mobile_sticker_process_buffer --- result is %d", result);
	}
	env->ReleasePrimitiveArrayCritical(pInputImage, srcdata, 0);
	return result;
}

JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_processTexture(JNIEnv * env, jobject obj, jint textureIn, jbyteArray pInputImage, jint rotate, jint imageWidth, jint imageHeight, jboolean needsMirroring, jint textureOut)
{
	LOGE("processTexture, the width is %d, the height is %d, the rotate is %d",imageWidth, imageHeight, rotate);
	int result = -10;
	st_handle_t humanActionhandle = getHumanActionHandle(env, obj);
	st_handle_t stickerhandle = getStickerHandle(env, obj);

	if(humanActionhandle == NULL || stickerhandle == NULL)
	{
		LOGE("handle is null");
	}
	jbyte *srcdata = (jbyte*) (env->GetPrimitiveArrayCritical(pInputImage, 0));

	int image_stride = imageWidth * 4;
	int detect_config = 0x0000003F ;

	st_mobile_human_action_t human_action;

	long startTime = getCurrentTime();
	if(humanActionhandle != NULL)
	{

		  result =  st_mobile_human_action_detect(humanActionhandle, (unsigned char *)srcdata,  ST_PIX_FMT_RGBA8888,  imageWidth,
				   	   	   	   	   	   imageHeight, image_stride, (st_rotate_type)rotate, detect_config, &human_action);
			LOGE("st_mobile_human_action_detect --- result is %d", result);
	}

	long afterdetectTime = getCurrentTime();
	LOGE("the human action detected time is %ld", (afterdetectTime - startTime));
	LOGE("the face count is %d", human_action.face_count);

	if(stickerhandle != NULL)
	{
	//	result  = st_mobile_sticker_process_buffer(stickerhandle, (unsigned char *)srcdata, imageWidth, imageHeight,  (st_rotate_type)rotate, &human_action,textureOut);
		result  = st_mobile_sticker_process_texture(stickerhandle, textureIn, imageWidth, imageHeight,  (st_rotate_type)rotate,needsMirroring, &human_action,textureOut);
		LOGE("st_mobile_sticker_process_texture --- result is %d", result);
	}

	long afterStickerTime = getCurrentTime();
	LOGE("process sticker time is %ld", (afterStickerTime - afterdetectTime));
	env->ReleasePrimitiveArrayCritical(pInputImage, srcdata, 0);
	return result;
}

JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_changeSticker(JNIEnv * env, jobject obj, jstring path)
{
		int result = JNI_FALSE;
		st_handle_t stickerhandle = getStickerHandle(env, obj);
		const char *pathChars = env->GetStringUTFChars(path, 0);
		if(stickerhandle != NULL)
		{
				result = st_mobile_sticker_change_package(stickerhandle,pathChars);
		}
		return result;
}

JNIEXPORT void JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_destoryInstance(JNIEnv * env, jobject obj)
{
	st_handle_t humanActionhandle = getHumanActionHandle(env, obj);
	if(humanActionhandle != NULL)
	{
			LOGE(" human action destory ");
			st_mobile_human_action_destroy(humanActionhandle);
	}

	st_handle_t stickerhandle = getStickerHandle(env, obj);
	if(stickerhandle != NULL)
	{
			LOGE(" sticker handle destory ");
			st_mobile_sticker_destroy(stickerhandle);
	}
}
