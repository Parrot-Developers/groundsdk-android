# Included from Android.mk files to build and register a sdkcore internal module

# Allow usage of internal API
LOCAL_CFLAGS += -DSDKCORE_INTERNAL

# Register module
include build-sdkcore-module.mk