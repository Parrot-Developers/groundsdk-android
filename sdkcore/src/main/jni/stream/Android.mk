LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := sdkcore-stream

LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include

LOCAL_STATIC_LIBRARIES := \
	sdkcore-common \
	sdkcore-pomp \
	libpdraw-static

LOCAL_C_INCLUDES := $(LOCAL_EXPORT_C_INCLUDES)

LOCAL_SRC_FILES := \
	sdkcore_file_source.c \
	sdkcore_frame.c \
	sdkcore_media_info_jni.c \
	sdkcore_overlayer.c \
	sdkcore_overlayer_jni.c \
	sdkcore_renderer.c \
	sdkcore_renderer_jni.c \
	sdkcore_sink.c \
	sdkcore_sink_frame_jni.c \
	sdkcore_sink_jni.c \
	sdkcore_stream.c \
	sdkcore_stream_jni.c \
	sdkcore_texture_loader.c \
	sdkcore_texture_loader_jni.c

include $(BUILD_SDKCORE_MODULE)
