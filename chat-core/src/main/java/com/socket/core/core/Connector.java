package com.socket.core.core;

import java.nio.channels.SocketChannel;
import java.util.UUID;

/**
 * @author 周泽
 * @date Create in 11:02 2019/12/23
 * @Description 抽象连接, 客户端与服务端的所有操作,都是通过一个连接开始的
 */
public class Connector {

    /**
     * 连接的唯一标识
     */
    private UUID key = UUID.randomUUID();

    /**
     * channel
     */
    private SocketChannel channel;

    /**
     * 发送者
     */
    private Sender sender;

    /**
     * 接收者
     */
    private Receiver receiver;

    /**
     * 初始化 socketChannel
     * @param socketChannel
     */
    private void setUp(SocketChannel socketChannel) {
        this.channel = socketChannel;
    }

}
