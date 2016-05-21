### 系统结构
自动更新系统的示意图如下：
![](http://blog.qiniu.uddream.cn/update_zengliang_introduce.jpg)
图中手机代表客户端。服务端的各个模块描述如下：
- WebConsole:提供上传更新包的网站操作界面。
- FS：文件系统，存储apk文件和增量更新包，增量更新的原理后文会提到。
- DB：用于存储文件的属性，例如版本号，更新描述，文件的md5等。
- Server: 接收客户端请求，返回文件下载链接。
- CDN：提供文件下载服务(友盟用到的是阿里云的CDN服务)。

### 基本流程
1.  用户在WebConsole上传更新包、填写更新版本和更新日志，程序将更新包存储到FS，版本(version-code)、更新日志、文件md5及其他配置信息存储到DB。
2. 客户端请求Server,传入客户端的appkey、版本号、md5等信息。Server与DB存储的信息比较，如果需要更新则返回更新包的url，否则返回不更新。
3. 客户端收到服务器端的返回结果后判断是否需要更新，如果需要更新，则弹窗提示用户有新版本更新，用户确认后下载安装包，安装新版本。

### 服务端处理流程
流程图如下：
![](http://blog.qiniu.uddream.cn/update_zengliang_flow.png)
1. 客户端发送请求至服务端，请求内容除了必要的验证信息以外，最重要的信息就是version_code，或者类似的用于比较版本号以判断是否需要升级的字段信息。
2. 服务端接收到请求后，验证请求的有效性。
3. 若请求有效，则对比请求中的version_code是否是最新的。
4. 若不是最新的version_code，则说明需要进行更新，此时需要首先判断是否能够进行增量更新。如果请求中version_code对应的apk文件在服务端存在且md5一致，则可以进行增量更新，否则不能。
5 如果不能增量更新，则直接返回apk文件的CDN下载链接。
6 如果能进行增量更新，首先判断对应的patch文件是否存在，如果不存在则调用bsdiff命令生成patch文件后返回patch文件的CDN链接；如果存在就直接返回patch文件CDN链接。

### 客户端处理流程
1.客户端首先请求server，获得是否有新版本的更新信息。
2. 如果没有更新，则客户端不进行更新动作。
3. 如果服务端返回的是有更新，客户端会根据全量更新和增量更新两种情况来处理:  如果服务器端返回的是全量更新，则会开启service下载完整版的apk文件； 如果服务器端返回的是增量更新，则会开启service下载patch文件到本地，然后使用JNI进行bspatch，给原apk文件打补丁，生成新版本的apk文件，生成的apk文件要进行MD5校验，如果与后台上传的apk文件的MD5值相等，则认为bspatch成功。
4. 客户端在下载完成后，会提示安装，若用户忽略，则会在下次检测更新的时候，首先判断本地是否已经存在最新版的apk文件，若已存在，则会提示安装。

### 一些经验
1. 客户端与服务器端之间的数据传输要进行加密处理，推荐使用https协议。
2. 建议使用全量更新，目前已知增量更新方案在部分系统厂商上不能正常工作。
3. 当遇到apk有较大改动时，可能会出现差分包和新apk大小相差不大的情况。这种情况下，建议进行全量升级。因为合并差分包的耗费的时间可能会超过全量升级所花费的时间。
4. 当apk本身较小时，全量更新更加合适
5. 若系统内置的apk文件无法获取到，则无法进行增量更新（bspatch是根据系统内置的apk文件与patch文件来合并生成新版本的apk文件）。
6. 为防止增量更新合成的apk文件有误，需要对合成的apk文件和最新版的apk文件进行MD5校验。

### 增量升级技术方案
#### 服务端生成增量包
工具下载：[bsdiff和 bspatch](http://www.daemonology.net/bsdiff/)
[备用下载地址](http://blog.qiniu.uddream.cn/bsdiff-4.3.tar.gz)
服务端操作系统：centos6.5，理论上上只要是linux都可以，安装步骤：
1. 解压bsdiff-4.3.tar.gz压缩包，`tar zxvf bsdiff-4.3.tar.gz`
2. 进入解压目录，修改Makefile，倒数第一行和倒数第三行 加TAB
3. 安装依赖库 `yum install bzip2-devel`， 然后执行make命令编译源码文件
4.  将生成的bsdiff和bspatch复制到/usr/local/sbin/下
5. 生成增量包 `bsdiff -h oldfile newfile patchfile`
6. 合并增量包 `bspatch -h oldfile newfile patchfile`
7. 进行md5值校验，使用命令md5sum file-name.apk

#### 客户端合并增量包
在客户端合并增量文件，需要使用上述工具包，因此我们需要将上述的bsdiff源码编译成so文件，供客户端调用，在编译so文件时，需要依赖bzip2库，下载地址：[bzip2](http://www.bzip.org/downloads.html)，备用下载地址：[bzip2-download](http://blog.qiniu.uddream.cn/bzip2-1.0.6.tar.gz)
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
- 编写测试java类，命名为BSUtil.java，调用该jni接口即可，为方便工程使用，需要将此java文件打包成jar包，方便后期使用，打包命令(在com文件夹所在目录执行命令，必须建立包名目录结构，否则打出jar包无法使用)：
`javac com\uddream\bs\BSUtil.java`
`jar cvf  patch.jar com\uddream\bs\BSUtil.class`
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
- 将so文件和jar包文件导入工程，调用jar包方法即可。

> GitHub地址：https://github.com/share-sdk/ClientUpdateSdk
