package com.socket.server.handler;

import com.socket.core.utils.CloseUtils;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author 周泽
 * @date Create in 10:15 2019/11/14
 * @Description
 */
@Slf4j
public class ClientHandler {
    private final SocketChannel socketChannel;
    private final ClientReadHandler readHandler;
    private final ClientWriteHandler writeHandler;
    private final ClientHandlerCallBack clientHandlerCallBack;
    private final String clientInfo;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallBack clientHandlerCallBack) throws IOException {
        this.socketChannel = socketChannel;
        // 设置为非阻塞模式
        socketChannel.configureBlocking(false);

        Selector readSelector = Selector.open();
        socketChannel.register(readSelector, SelectionKey.OP_READ);
        readHandler = new ClientReadHandler(readSelector);

        Selector writeSelector = Selector.open();
        socketChannel.register(writeSelector, SelectionKey.OP_WRITE);
        writeHandler = new ClientWriteHandler(writeSelector);

        this.clientHandlerCallBack = clientHandlerCallBack;
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        log.info("得到新的客户端连接,客户端地址:[{}]", socketChannel.getRemoteAddress());
    }

    public String getClientInfo(){
        return clientInfo;
    }

    public void send(String str) {
        writeHandler.send(str);
    }

    public void exit() {
        readHandler.exit();
        writeHandler.exit();
        CloseUtils.close(socketChannel);
        log.info("客户端:[{}]已退出...", clientInfo);
    }

    public void readToPrint() {
        readHandler.start();
    }

    private void exitBySelf() {
        exit();
        // 通知外层 自己已经关闭
        clientHandlerCallBack.onSelfClosed(this);
    }

    public interface ClientHandlerCallBack {
        /**
         * 自身关闭的通知
         *
         * @param clientHandler
         */
        void onSelfClosed(ClientHandler clientHandler);

        /**
         * 新消息到达的通知
         * @param clientHandler
         * @param msg
         */
        void onNewMessageArrived(ClientHandler clientHandler, String msg);
    }

    class ClientWriteHandler {
        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;
        private final ExecutorService executorService;

        ClientWriteHandler(Selector selector) {
            this.selector = selector;
            this.executorService = Executors.newSingleThreadExecutor();
            this.byteBuffer = ByteBuffer.allocate(256);
        }



        void exit(){
            done = true;
            CloseUtils.close(selector);
            executorService.shutdownNow();
        }

        void send(String str) {
            if (done) {
                return;
            }
            executorService.execute(new WriteRunnable(str));
        }

        class WriteRunnable implements Runnable {
            private final String msg;
            WriteRunnable(String msg) {
                // readline 方法会把 \n 换行符去掉
                this.msg = msg + '\n';
            }

            @Override
            public void run() {
                if (ClientWriteHandler.this.done) {
                    return;
                }
                byteBuffer.clear();
                byteBuffer.put(msg.getBytes());

                // 反转操作,这里是重点
                byteBuffer.flip();

                while (!done && byteBuffer.hasRemaining()) {

                    try {
                        int write = socketChannel.write(byteBuffer);
                        // < 0 表示没有成功
                        if (write < 0) {
                            log.info("客户端已无法发送数据...");
                            ClientHandler.this.exitBySelf();
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    class ClientReadHandler extends Thread {
        private boolean done = false;
        private final Selector selector;
        private final ByteBuffer byteBuffer;

        ClientReadHandler(Selector selector) {
            this.selector = selector;
            this.byteBuffer = ByteBuffer.allocate(256);
        }

        @Override
        public void run() {
            super.run();

            try {
                do {
                    if (selector.select() == 0) {
                        if (done) {
                            break;
                        }
                        continue;
                    }

                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                    while (keys.hasNext()) {
                        SelectionKey key = keys.next();
                        keys.remove();

                        if (key.isReadable()) {
                            SocketChannel client = (SocketChannel) key.channel();
                            // 先清空ByteBuffer
                            byteBuffer.clear();

                            int read = client.read(byteBuffer);
                            if (read > 0) {
                                // 这里 -1 是要丢弃换行符
                                String str = new String(byteBuffer.array(), 0, byteBuffer.position() - 1);

                                clientHandlerCallBack.onNewMessageArrived(ClientHandler.this, str);
                            } else {
                                log.error("客户端已无法读取数据.");
                                // 退出
                                ClientHandler.this.exitBySelf();
                                break;
                            }
                        }
                    }
                } while (!done);
            } catch (Exception e) {
                if (!done){
                    log.info("客户端连接异常断开...");
                    ClientHandler.this.exitBySelf();
                }
            } finally {
                // 关闭读取流
                CloseUtils.close(selector);
            }
        }

        void exit(){
            done = true;
            // 唤醒selector
            selector.wakeup();
            CloseUtils.close(selector);
        }

    }

}
