#include <jni.h>

#include <android/log.h>

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <sys/time.h>
#include <time.h>
#include "prebuilt/include/st_mobile_common.h"
#include "prebuilt/include/st_mobile_beautify.h"
#include "prebuilt/include/st_mobile_sticker.h"

#include<fcntl.h>

#include "jni_common.h"

#define  LOG_TAG    "STMobileSticker"

extern "C" {
JNIEXPORT jstring JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_generateActiveCode(JNIEnv * env, jobject obj, jstring licensePath);
JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_checkActiveCode(JNIEnv * env, jobject obj, jstring licensePath, jstring activationCode);
JNIEXPORT jstring JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_generateActiveCodeFromBuffer(JNIEnv * env, jobject obj, jstring licenseBuffer, jint licenseSize);
JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_checkActiveCodeFromBuffer(JNIEnv * env, jobject obj, jstring licenseBuffer, jint licenseSize, jstring activationCode);
JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_createInstance(JNIEnv * env, jobject obj, jstring zippath, jstring modelpath, jint config);
//JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_processBuffer(JNIEnv * env, jobject obj, jbyteArray pInputImage, jint rotate, jint imageWidth, jint imageHeight,jboolean needsMirroring,  jint textureOut);
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

static JavaVM *gJavaVM;
static jobject gInterfaceObject;
const char *kInterfacePath = "com/sensetime/stmobile/STMobileStickerNative";


void item_callback(const char* material_name, st_material_status statusCode) {
    LOGI("-->> item_callback: start item callback");

    int status;
    JNIEnv *env;
    bool isAttached = false;
    status = gJavaVM->AttachCurrentThread(&env, NULL);
    if(status<0) {
        LOGE("-->> item_callback: failed to attach current thread!");
        return;
    }
    isAttached = true;

    jclass interfaceClass = env->GetObjectClass(gInterfaceObject);
    if(!interfaceClass) {
        LOGE("-->> item_callback: failed to get class reference");
        if(isAttached) gJavaVM->DetachCurrentThread();
        return;
    }

    /* Find the callBack method ID */
    jmethodID method = env->GetStaticMethodID(interfaceClass, "item_callback", "(Ljava/lang/String;I)V");
    if(!method) {
        LOGE("item_callback: failed to get method ID");
        if(isAttached) gJavaVM->DetachCurrentThread();
        return;
    }

    jstring resultStr = env->NewStringUTF(material_name);
    LOGE("-->> item_callback: resultStr=%s, status=%d",material_name, statusCode);

    //get callback datas

    env->CallStaticVoidMethod(interfaceClass, method, resultStr, (jint)statusCode);
//	env->ReleaseStringChars(resultStr, material_name);
    LOGI("-->> material_name , status_string =&&&&");
//	if(isAttached) gJavaVM->DetachCurrentThread();

}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv *env;
    gJavaVM = vm;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("Failed to get the environment using GetEnv()");
        return -1;
    }

    jclass cls = env->FindClass(kInterfacePath);
    if(!cls) {
        LOGE("initClassHelper: failed to get %s class reference", kInterfacePath);
        return -1;
    }
    jmethodID constr = env->GetMethodID(cls, "<init>", "()V");
    if(!constr) {
        LOGE("initClassHelper: failed to get %s constructor", kInterfacePath);
        return -1;
    }
    jobject obj = env->NewObject(cls, constr);
    if(!obj) {
        LOGE("initClassHelper: failed to create a %s object", kInterfacePath);
        return -1;
    }
    gInterfaceObject = env->NewGlobalRef(obj);

    return JNI_VERSION_1_4;
}


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

JNIEXPORT jstring JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_generateActiveCode(JNIEnv * env, jobject obj, jstring licensePath) {
    LOGI("-->> 111generateActiveCode: start genrate active code");
//    const char *targetProductName = env->GetStringUTFChars(productName, 0);
    const char *targetLicensePath = env->GetStringUTFChars(licensePath, 0);
    char * activationCode = new char[1024];
    memset(activationCode, 0, 1024);
    int len = 1024;
    //	jint *len = (jint*) (env->GetPrimitiveArrayCritical(activeCodeLen, 0));
    LOGI("-->> targetLicensePath=%x, targetActivationCode=%x, activeCodeLen=%x", targetLicensePath, activationCode, len);
    int res = st_mobile_generate_activecode(targetLicensePath, activationCode, &len);
	LOGI("-->> targetLicensePath=%s, targetActivationCode=%s",targetLicensePath, activationCode);
    LOGI("-->> generateActiveCode: res=%d",res);
    jstring targetActivationCode = env->NewStringUTF(activationCode);

    env->ReleaseStringUTFChars(licensePath, targetLicensePath);
    //	env->ReleasePrimitiveArrayCritical(activeCodeLen, len, 0);
    return targetActivationCode;
}

JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_checkActiveCode(JNIEnv * env, jobject obj, jstring licensePath, jstring activationCode) {
    LOGI("-->> checkActiveCode: start check active code");
//    const char *targetProductName = env->GetStringUTFChars(productName, 0);
    const char *targetLicensePath = env->GetStringUTFChars(licensePath, 0);
    const char *targetActivationCode = env->GetStringUTFChars(activationCode, 0);
    //	LOGI("-->> targetProductName=%s, targetLicensePath=%s, targetActivationCode=%s",targetProductName, targetLicensePath, targetActivationCode);
    int res = st_mobile_check_activecode(targetLicensePath, targetActivationCode);
    	LOGI("-->> checkActiveCode: res=%d",res);
    env->ReleaseStringUTFChars(licensePath, targetLicensePath);
    env->ReleaseStringUTFChars(activationCode, targetActivationCode);
    return res;
}

JNIEXPORT jstring JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_generateActiveCodeFromBuffer(JNIEnv * env, jobject obj, jstring licenseBuffer, jint licenseSize) {
    LOGI("-->> 222generateActiveCodeFromBuffer: start genrate active code");
    const char *targetLicenseBuffer = env->GetStringUTFChars(licenseBuffer, 0);
    char * activationCode = new char[1024];
    memset(activationCode, 0, 1024);
    int len = 1024;
    int res = st_mobile_generate_activecode_from_buffer(targetLicenseBuffer, licenseSize, activationCode, &len);
    LOGI("-->> targetLicenseBuffer=%s, license_size=%d, targetActivationCode=%s",targetLicenseBuffer, licenseSize, activationCode);
    LOGI("-->> generateActiveCode: res=%d",res);
    jstring targetActivationCode = env->NewStringUTF(activationCode);

    env->ReleaseStringUTFChars(licenseBuffer, targetLicenseBuffer);
    return targetActivationCode;
}
JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_checkActiveCodeFromBuffer(JNIEnv * env, jobject obj, jstring licenseBuffer, jint licenseSize, jstring activationCode) {
    LOGI("-->> checkActiveCodeFromBuffer: start check active code");
    const char *targetLicenseBuffer = env->GetStringUTFChars(licenseBuffer, 0);
    const char *targetActiveCode = env->GetStringUTFChars(activationCode, 0);
//    int license_size = licenseSize;
    int res = st_mobile_check_activecode_from_buffer(targetLicenseBuffer, licenseSize, targetActiveCode);
    LOGI("-->> checkActiveCodeFromBuffer: res=%d",res);

    env->ReleaseStringUTFChars(licenseBuffer, targetLicenseBuffer);
    env->ReleaseStringUTFChars(activationCode, targetActiveCode);
    return res;
}

JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_createInstance(JNIEnv * env, jobject obj, jstring zippath, jstring modelpath, jint config) {
    LOGE("createInstance");
    st_handle_t  ha_handle = NULL;
    const char *modelpathChars = env->GetStringUTFChars(modelpath, 0);
    const char *zippathChars = env->GetStringUTFChars(zippath, 0);
    LOGI("-->> zippath=%s, modelpath=%s, config=%d", zippathChars, modelpathChars, config);
    int result = st_mobile_human_action_create(modelpathChars, config, &ha_handle);
    if(result != 0){
        LOGE("create handle for human action failed");
        return result;
    }
    setHumanActionHandle(env, obj, ha_handle);

    st_handle_t sticker_handle = NULL;
    result = st_mobile_sticker_create(zippathChars, &sticker_handle);
    if(result != 0)
    {
    LOGE("st_mobile_sticker_create failed");
    return result;
    }
    setStickerHandle(env, obj, sticker_handle);

    env->ReleaseStringUTFChars(zippath, zippathChars);
    env->ReleaseStringUTFChars(modelpath, modelpathChars);
    return result;
}

//JNIEXPORT jint JNICALL Java_com_sensetime_stmobile_STMobileStickerNative_processBuffer(JNIEnv * env, jobject obj, jbyteArray pInputImage, jint rotate, jint imageWidth, jint imageHeight, jboolean needsMirroring, jint textureOut)
//{
//	LOGE("processBuffer, the width is %d, the height is %d, the rotate is %d",imageWidth, imageHeight, rotate);
//	int result = -10;
//	st_handle_t humanActionhandle = getHumanActionHandle(env, obj);
//	st_handle_t stickerhandle = getStickerHandle(env, obj);
//
//	if(humanActionhandle == NULL || stickerhandle == NULL)
//	{
//		LOGE("handle is null");
//	}
//	jbyte *srcdata = (jbyte*) (env->GetPrimitiveArrayCritical(pInputImage, 0));
//
//	int image_stride = imageWidth * 4;
//	int detect_config = 0x0000003F ;
//
//	st_mobile_human_action_t human_action;
//
//	if(humanActionhandle != NULL)
//	{
//		  result =  st_mobile_human_action_detect(humanActionhandle, (unsigned char *)srcdata,  ST_PIX_FMT_RGBA8888,  imageWidth,
//				   	   	   	   	   	   imageHeight, image_stride, (st_rotate_type)rotate, detect_config, &human_action);
//			LOGE("st_mobile_human_action_detect --- result is %d", result);
//	}
//
//	LOGE("the face count is %d", human_action.face_count);
//
//	if(stickerhandle != NULL)
//	{
//		result  = st_mobile_sticker_process_buffer(stickerhandle, (unsigned char *)srcdata, imageWidth, imageHeight,  (st_rotate_type)rotate,needsMirroring, &human_action,textureOut);
//		LOGE("st_mobile_sticker_process_buffer --- result is %d", result);
//	}
//	env->ReleasePrimitiveArrayCritical(pInputImage, srcdata, 0);
//	return result;
//}

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
//	jbyte *srcdata = (jbyte*) (env->GetPrimitiveArrayCritical(pInputImage, 0));
jbyte *srcdata = (jbyte*) (env->GetByteArrayElements(pInputImage, 0));

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
    result  = st_mobile_sticker_process_texture(stickerhandle, textureIn, imageWidth, imageHeight,  (st_rotate_type)rotate,needsMirroring, &human_action, item_callback, textureOut);
    LOGE("-->>st_mobile_sticker_process_texture --- result is %d", result);
}

    long afterStickerTime = getCurrentTime();
    LOGE("process sticker time is %ld", (afterStickerTime - afterdetectTime));
    //	env->ReleasePrimitiveArrayCritical(pInputImage, srcdata, 0);
    env->ReleaseByteArrayElements(pInputImage, srcdata, 0);
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
