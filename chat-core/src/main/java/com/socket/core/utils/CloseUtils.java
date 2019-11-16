package com.socket.core.utils;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author 周泽
 * @date Create in 10:23 2019/11/14
 * @Description
 */
public class CloseUtils {
    public static void close(Closeable... closeables) {
        if (closeables == null) {
            return;
        }

        for (Closeable closeable : closeables) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
