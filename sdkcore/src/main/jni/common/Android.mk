LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := sdkcore-common

LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include

LOCAL_STATIC_LIBRARIES := libulog-static

LOCAL_C_INCLUDES := $(LOCAL_EXPORT_C_INCLUDES)

LOCAL_SRC_FILES := \
	sdkcore_jni.c \
	sdkcore_rect_jni.c

include $(BUILD_SDKCORE_MODULE)
