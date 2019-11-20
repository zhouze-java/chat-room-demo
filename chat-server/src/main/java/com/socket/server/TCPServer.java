package com.socket.server;

import com.socket.core.utils.CloseUtils;
import com.socket.server.handler.ClientHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author 周泽
 * @date Create in 15:42 2019/11/13
 * @Description TCP服务端
 */
@Slf4j
public class TCPServer implements ClientHandler.ClientHandlerCallBack{

    private final int port;
    private ClientListener listener;
    private List<ClientHandler> clientHanlerList = new ArrayList<>();
    private final ExecutorService forwardingThreadPoolExecutor;
    private Selector selector;
    private ServerSocketChannel server;

    public TCPServer(int port) {
        this.port = port;
        this.forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start(){
        try {
            selector = Selector.open();

            ServerSocketChannel server = ServerSocketChannel.open();
            // 设置为非阻塞模式
            server.configureBlocking(false);
            // 绑定本地端口
            server.socket().bind(new InetSocketAddress(port));
            // 创建客户端连接到达的监听
            server.register(selector, SelectionKey.OP_ACCEPT);

            this.server = server;

            log.info("TCP服务端地址为:[{}]", server.getLocalAddress());

            // 启动客户端监听
            ClientListener listener = this.listener = new ClientListener();

            listener.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void stop(){
        if (listener != null) {
            listener.exit();
        }

        CloseUtils.close(server, selector);

        synchronized (TCPServer.this) {
            for (ClientHandler clientHanler : clientHanlerList) {
                clientHanler.exit();
            }

            clientHanlerList.clear();
        }
        // 关闭线程池
        forwardingThreadPoolExecutor.shutdownNow();
    }

    /**
     * 给所有客户端发送信息
     * @param str
     */
    public synchronized void broadcast(String str) {
        for (ClientHandler clientHanler : clientHanlerList) {
            clientHanler.send(str);
        }
    }

    @Override
    public synchronized void onSelfClosed(ClientHandler clientHandler) {
        clientHanlerList.remove(clientHandler);
    }

    @Override
    public void onNewMessageArrived(ClientHandler clientHandler, String msg) {
        // 收到消息
        log.info("收到了{}的消息:{}", clientHandler.getClientInfo(), msg);
        forwardingThreadPoolExecutor.execute(() -> {
            synchronized (TCPServer.this) {
                for (ClientHandler handler : clientHanlerList) {
                    if (handler.equals(clientHandler)) {
                        // 跳过自己
                        continue;
                    }

                    // 调用对应的发送方法
                    handler.send(msg);
                }
            }
        });
    }

    private class ClientListener extends Thread {
        private boolean done;

        @Override
        public void run() {
            super.run();
            log.info("TCP服务端准备就绪....");
            do {
                try {

                    Selector selector = TCPServer.this.selector;
                    // 检查有没有可用的事件,没有的话继续下次循环
                    if (selector.select() == 0) {
                        if (done) {
                            break;
                        }
                        continue;
                    }
                    // 拿到所有的可用的事件,循环
                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                    while (keys.hasNext()) {
                        if (done) {
                            break;
                        }

                        SelectionKey key = keys.next();

                        keys.remove();

                        // 检查当前的key是不是我们关注的
                        if (key.isAcceptable()) {
                            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                            // 非阻塞状态拿到客户端连接
                            SocketChannel socketChannel = serverSocketChannel.accept();

                            // 构建处理消息的线程
                            try {
                                ClientHandler clientHandler = new ClientHandler(socketChannel, TCPServer.this);
                                // 读取并打印
                                clientHandler.readToPrint();
                                synchronized (TCPServer.this) {
                                    clientHanlerList.add(clientHandler);
                                }
                            } catch (IOException e) {
                                log.error("客户端连接异常:{}", e);
                            }

                        }
                    }

                } catch (IOException e) {
                    continue;
                }

            } while (!done);

            log.info("客户端连接关闭...");

        }

        void exit(){
            done = true;
            // 把selector的阻塞状态取消
            selector.wakeup();
        }

    }

}

