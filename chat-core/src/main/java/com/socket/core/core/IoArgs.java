package com.socket.core.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author 周泽
 * @date Create in 11:09 2019/12/23
 * @Description 对 ByteBuffer 进行封装, 并提供一些基本的操作
 */
public class IoArgs {

    private byte[] byteBuffer = new byte[256];

    private ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);

    /**
     * 读取数据, 从socketChannel中读取到buffer中
     * @param channel
     * @return
     * @throws IOException
     */
    public int read(SocketChannel channel) throws IOException {
        buffer.clear();
        return channel.read(buffer);
    }

    /**
     * 写数据, buffer里面的数据写到 SocketChannel 中去
     * @param channel
     * @return
     * @throws IOException
     */
    public int write(SocketChannel channel) throws IOException {
        return channel.write(buffer);
    }

    /**
     * 转String
     * @return
     */
    public String bufferString(){
        // 丢弃换行符
        return new String(byteBuffer, 0, buffer.position() - 1);
    }

    /**
     * 监听{@link IoArgs}
     */
    public interface  IoArgsEventListener{
        /**
         * 开始时的回调
         * @param ioArgs
         */
        void onStarted(IoArgs ioArgs);

        /**
         * 结束之后的回调
         * @param ioArgs
         */
        void onCompleted(IoArgs ioArgs);
    }
}
