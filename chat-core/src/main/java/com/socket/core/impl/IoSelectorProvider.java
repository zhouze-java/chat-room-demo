package com.socket.core.impl;

import com.socket.core.core.IoProvider;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * @author 周泽
 * @date Create in 14:37 2019/12/23
 * @Description {@link IoProvider} 的具体实现
 */
public class IoSelectorProvider implements IoProvider {
    @Override
    public boolean registerInput(SocketChannel channel, HandleInputCallback callback) {
        return false;
    }

    @Override
    public boolean registerOutput(SocketChannel channel, HandleOutputCallback callback) {
        return false;
    }

    @Override
    public void unRegisterInput(SocketChannel channel) {

    }

    @Override
    public void unRegisterOutput(SocketChannel channel) {

    }

    @Override
    public void close() throws IOException {

    }
}
