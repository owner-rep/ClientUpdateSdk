#### 客户端合并增量包
在客户端合并增量文件，需要使用上述工具包，因此我们需要将上述的bsdiff源码编译成so文件，供客户端调用，在编译so文件时，需要依赖bzip2库，下载地址：[bzip2](http://www.bzip.org/downloads.html)，备用下载地址：[bzip2-download](http://static.blog.uddream.cn/bzip2-1.0.6.tar.gz)
- 将bsdiff-4.3.tar.gz和bzip.tar.gz的源码文件解压到同一个目录下，目录名为jni
- 在jni目录下，编写Android.mk和Application.mk文件

Android.mk
```
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
```
Application.mk
```
APP_ABI := armeabi armeabi-v7a arm64-v8a
```
- bspatch.c bsdiff.c在其中找到main方法，我们只需要编写一个jni接口，调用其main方法即可实现生成和合并增量包的功能，首先将main方法声明改为static修饰，然后编写一个jni接口，调用该静态方法，在这两个.c文件的最后追加一下代码即可。
```
JNIEXPORT jint JNICALL Java_com_uddream_bs_BSUtil_bspatch(JNIEnv *env,  
        jobject obj, jstring old, jstring new , jstring patch){  
  const char * argv[4];  
  argv[0]="bspatch";  
  argv[1]=(*env)->GetStringUTFChars(env,old, 0);  
  argv[2]=(*env)->GetStringUTFChars(env,new, 0);  
  argv[3]=(*env)->GetStringUTFChars(env,patch, 0);  
    
  int ret=main(4, argv);  
    
   (*env)->ReleaseStringUTFChars(env,old,argv[1]);  
   (*env)->ReleaseStringUTFChars(env,new,argv[2]);  
   (*env)->ReleaseStringUTFChars(env,patch,argv[3]);  
   return ret;  
}
```
- 执行ndk-build将生成的so文件导入工程
- 编写测试java类，命名为BSUtil.java，调用该jni接口即可，为方便工程使用，需要将此java文件打包成jar包，方便后期使用，打包命令：`jar cvf  patch.jar com\uddream\bs\BSUtil.java`
- 注意：必须建立好java包路径，否则生成的jar包有问题
```
package com.uddream.bs;

public class BSUtil {
    public static native int bsdiff(String oldFile, String newFile, String path);

    public static native int bspatch(String oldFile, String newFile, String path);

    static {
        System.loadLibrary("patch");
    }
}
```
- 将so文件和jar包文件导入工程，调用jar包方法即可，编译好的文件下载地址：[源码+编译好so和jar文件](http://static.blog.uddream.cn/bspatch.rar)
