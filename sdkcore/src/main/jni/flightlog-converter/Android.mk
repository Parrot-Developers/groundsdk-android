LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := sdkcore-flightlog-converter

LOCAL_STATIC_LIBRARIES := \
    sdkcore-common \
    log2gutma-static

LOCAL_SRC_FILES := \
	flightlog_converter_jni.cpp

include $(BUILD_SDKCORE_MODULE)
