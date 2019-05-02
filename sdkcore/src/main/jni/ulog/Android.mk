LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := sdkcore-ulog

LOCAL_STATIC_LIBRARIES := sdkcore-common

LOCAL_SRC_FILES := \
	ulog_jni.c \
	ulogtag_jni.c

include $(BUILD_SDKCORE_MODULE)
