LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := ButtonDriver
				Segment
				CLDriver

LOCAL_SRC_FILES := ButtonDriver.c
					CLDriver.c
					Segment.c

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)