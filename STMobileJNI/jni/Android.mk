LOCAL_PATH := $(call my-dir)

LOCAL_PREBUILT_DIR := prebuilt

include $(CLEAR_VARS)
LOCAL_MODULE := libst_mobile
LOCAL_SRC_FILES := $(LOCAL_PREBUILT_DIR)/lib/$(TARGET_ARCH_ABI)/libst_mobile.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    := stmobilefilter_jni
LOCAL_SRC_FILES := stmobileFilter_jni.cpp\
	               stmobile_getscript.cpp \
				   stmobile_aes.cpp \
				   
LOCAL_LDLIBS := -llog -L$(SYSROOT)/usr/lib/$(TARGET_ARCH_ABI) -lz -ljnigraphics -lGLESv2 
LOCAL_SHARED_LIBRARIES := libst_mobile
LOCAL_C_INCLUDES := $(LOCAL_PATH) $(LOCAL_PATH)/$(LOCAL_PREBUILT_DIR)/include

include $(BUILD_SHARED_LIBRARY)
