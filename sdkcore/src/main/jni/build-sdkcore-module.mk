# Included from Android.mk files to build and register a sdkcore module

# Default C flags. Module's own flags included last to allow custom override
LOCAL_CFLAGS := \
	-fvisibility=hidden \
	-Wall \
	-Werror \
	-Wextra \
	-Wno-unused-parameter \
	$(LOCAL_CFLAGS)

# Module is a static library merged into sdkcore shared library
include $(BUILD_STATIC_LIBRARY)

# Registers module in sdkcore shared library
SDKCORE_MODULES += $(LOCAL_MODULE)