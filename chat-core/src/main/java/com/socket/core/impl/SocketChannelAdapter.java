package com.socket.core.impl;

import com.socket.core.core.IoArgs;
import com.socket.core.core.Receiver;
import com.socket.core.core.Sender;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author 周泽
 * @date Create in 14:45 2019/12/23
 * @Description {@link Sender},{@link Receiver} 的具体实现
 */
public class SocketChannelAdapter implements Sender, Receiver, Closeable {
    @Override
    public boolean receiveAsync(IoArgs.IoArgsEventListener listener) throws IOException {
        return false;
    }

    @Override
    public boolean sendAsync(IoArgs ioArgs, IoArgs.IoArgsEventListener listener) throws IOException {
        return false;
    }

    @Override
    public void close() throws IOException {

    }
}
