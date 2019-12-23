package com.socket.core.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

/**
 * @author 周泽
 * @date Create in 11:19 2019/12/23
 * @Description
 */
public interface IoProvider extends Closeable {

    /**
     * 注册输入,表示要从socketChannel中读取数据,是一个异步的模式
     * @param channel
     * @param callback
     * @return
     */
    boolean registerInput(SocketChannel channel, HandleInputCallback callback);

    /**
     * 注册输出,异步
     * @param channel
     * @param callback
     * @return
     */
    boolean registerOutput(SocketChannel channel, HandleOutputCallback callback);

    void unRegisterInput(SocketChannel channel);

    void unRegisterOutput(SocketChannel channel);

    /**
     * registerInput 方法的回调
     */
    abstract class HandleInputCallback implements Runnable{
        @Override
        public void run() {
            canProviderInput();
        }

        protected abstract void canProviderInput();
    }

    /**
     * registerOutput 方法的回调
     */
    abstract class HandleOutputCallback implements Runnable{
        private Object attach;

        @Override
        public void run() {
            canProviderOutput(attach);
        }

        public final void setAttach(Object attach){
            this.attach = attach;
        }

        protected abstract void canProviderOutput(Object attach);
    }
}
