package com.socket.core.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author 周泽
 * @date Create in 11:08 2019/12/23
 * @Description 发送者
 */
public interface Sender extends Closeable {

    /**
     * 异步发送
     * @param ioArgs
     * @param listener
     * @return
     * @throws IOException
     */
    boolean sendAsync(IoArgs ioArgs, IoArgs.IoArgsEventListener listener) throws IOException;

}
