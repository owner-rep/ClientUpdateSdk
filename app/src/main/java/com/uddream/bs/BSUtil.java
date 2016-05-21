package com.uddream.bs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;

/**
 * Created by Glen on 2016/5/20.
 */
public class BSUtil {
    public static native int bsdiff(String oldFile, String newFile, String path);

    public static native int bspatch(String oldFile, String newFile, String path);

    public static String md5sum(String path) {
        String value = null;
        FileInputStream in = null;
        try {
            File file = new File(path);
            in = new FileInputStream(file);
            MappedByteBuffer byteBuffer = in.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteBuffer);
            BigInteger bi = new BigInteger(1, md5.digest());
            value = bi.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return value;
    }

    static {
        System.loadLibrary("patch");
    }
}
