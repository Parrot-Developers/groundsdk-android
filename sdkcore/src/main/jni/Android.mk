# save local path, include will change it
SDKCORE_PATH := $(call my-dir)

# include alchemy prebuilt
include $(PRODUCT_OUT_DIR)/$(TARGET_ARCH_ABI)/sdk/Android-static.mk

# list of modules to merge into the final sdkcore shared library
SDKCORE_MODULES :=

# allows to declare internal sdkcore modules
BUILD_SDKCORE_MODULE := $(SDKCORE_PATH)/internal-sdkcore-module.mk

# include internal sdkcore modules
include $(SDKCORE_PATH)/common/Android.mk
include $(SDKCORE_PATH)/pomp/Android.mk
include $(SDKCORE_PATH)/ulog/Android.mk
include $(SDKCORE_PATH)/stream/Android.mk
include $(SDKCORE_PATH)/arsdk/Android.mk
include $(SDKCORE_PATH)/flightlog-converter/Android.mk

# allows to declare an sdkcore module: include $(BUILD_SDKCORE_MODULE)
BUILD_SDKCORE_MODULE := $(SDKCORE_PATH)/build-sdkcore-module.mk

# include product specific Android.mk / external sdkcore modules
-include $(PRODUCT_DIR)/Android.mk

# register sdkcore shared library
LOCAL_PATH := $(SDKCORE_PATH)

include $(CLEAR_VARS)

LOCAL_MODULE := sdkcore

LOCAL_WHOLE_STATIC_LIBRARIES := $(SDKCORE_MODULES)

LOCAL_SRC_FILES := empty.cpp

LOCAL_LDLIBS := -llog -lz -lEGL -lGLESv3 -landroid

include $(BUILD_SHARED_LIBRARY)
