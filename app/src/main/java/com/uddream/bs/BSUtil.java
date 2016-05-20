package com.uddream.bs;

/**
 * Created by Glen on 2016/5/20.
 */
public class BSUtil {
    public static native int bsdiff(String oldFile, String newFile, String path);

    public static native int bspatch(String oldFile, String newFile, String path);

    static {
        System.loadLibrary("hello-libs");
    }
}
