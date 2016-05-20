LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := patch
LOCAL_SRC_FILES := 	bsdiff.c \
					bspatch.c  \
					bzlib.c \
					blocksort.c \
					compress.c \
					crctable.c  \
					decompress.c   \
					huffman.c\
					randtable.c
include $(BUILD_SHARED_LIBRARY)