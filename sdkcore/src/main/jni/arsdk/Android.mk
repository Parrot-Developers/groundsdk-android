LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := sdkcore-arsdk

# prefix for module private vars
MY := MY_$(LOCAL_MODULE)

LOCAL_STATIC_LIBRARIES := \
	sdkcore-common \
	sdkcore-pomp \
	sdkcore-stream \
	libarsdkctrl-static \
	libmux-static \
	libpuf-static

# all sdkcore jni source files (excluding generated sources)
$(MY)_SRC_FILES := \
	arsdk_backend_ble_jni.c \
	arsdk_backend_mux_jni.c \
	arsdk_backend_net_jni.c \
	arsdk_blackbox_request_jni.c \
	arsdk_ble_connection.c \
	arsdk_ble_connection_jni.c \
	arsdk_command_jni.c \
	arsdk_core_jni.c \
	arsdk_crashml_download_request_jni.c \
	arsdk_device_jni.c \
	arsdk_discovery_jni.c \
	arsdk_firmware_upload_request_jni.c \
	arsdk_firmware_version_jni.c \
	arsdk_flightlog_download_request_jni.c \
	arsdk_stream_jni.c \
	arsdk_tcp_proxy_jni.c \
	arsdkcore.c \
	arsdkcore_command.c \
	arsdkcore_device.c \
	arsdkcore_media.c \
	arsdkcore_source.c \
	arsdkcore_tcp_proxy.c \
	arsdkctrl_backend_ble.c

# rules to produce generated sources
# avoid defining the rule multiple time as this Android.mk is included for each eabi
ifndef $(MY)_GEN_OUT

# directory containing generated files
$(MY)_GEN_OUT := $(NDK_OUT)/arsdk-jni-gen

# done file for the code generation rule
$(MY)_GEN_DONE := $($(MY)_GEN_OUT)/arsdkgen.done

# list of generated files, with absolute path
$(MY)_GEN_FILES := $(addprefix $($(MY)_GEN_OUT)/, arsdkgen.c)

# arsdkgen.py in current arch product sdk
$(MY)_ARSDKGEN := $(PRODUCT_OUT_DIR)/$(TARGET_ARCH_ABI)/sdk/host/usr/lib/arsdkgen/arsdkgen.py

# arsdkgenjni.py in sdkcore tools directory
$(MY)_ARSDKGEN_JNI := $(LOCAL_PATH)/../../../../tools/arsdkgenjni.py

# all sdkcore jni (non-generated) sources require generated code to be produced first
$(addprefix $(LOCAL_PATH)/,$($(MY)_SRC_FILES)): | $($(MY)_GEN_DONE)

# generated files depend on code generation rule
$($(MY)_GEN_FILES): $($(MY)_GEN_DONE)
	$(empty)

# code generation rule
.PHONY: .FORCE
$($(MY)_GEN_DONE): .FORCE
	@mkdir -p $(dir $@)
	$($(MY)_ARSDKGEN) $($(MY)_ARSDKGEN_JNI) -o $(dir $@)
	@touch $@

clean-arsdk-jni-$(TARGET_ARCH_ABI):: clean-arsdk-generated

clean-arsdk-generated:
	rm $($(MY)_GEN_OUT)/*

endif

# include generated headers
LOCAL_C_INCLUDES := $($(MY)_GEN_OUT)

# built sources: sdkcore jni sources plus generated sources
LOCAL_SRC_FILES := $($(MY)_SRC_FILES) $(filter %.c,$($(MY)_GEN_FILES))

include $(BUILD_SDKCORE_MODULE)
