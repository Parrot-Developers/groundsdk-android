LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := sdkcore-pomp

LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include

LOCAL_STATIC_LIBRARIES := \
	sdkcore-common \
	libpomp-static

LOCAL_C_INCLUDES := $(LOCAL_EXPORT_C_INCLUDES)

LOCAL_SRC_FILES := \
	sdkcore_pomp.c \
	sdkcore_pomp_jni.c

include $(BUILD_SDKCORE_MODULE)
