package com.socket.core.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author 周泽
 * @date Create in 11:09 2019/12/23
 * @Description 接收者
 */
public interface Receiver extends Closeable {

    /**
     * 异步接收
     * @param listener
     * @return
     * @throws IOException
     */
    boolean receiveAsync(IoArgs.IoArgsEventListener listener) throws IOException;

}
