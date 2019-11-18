package com.socket.server;

import com.socket.server.handler.ClientHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
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
    private ClientListener clientListener;
    private List<ClientHandler> clientHanlerList = new ArrayList<>();
    private final ExecutorService forwardingThreadPoolExecutor;

    public TCPServer(int port) {
        this.port = port;
        this.forwardingThreadPoolExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start(){
        try {
            ClientListener listener = new ClientListener(port);
            clientListener = listener;
            listener.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void stop(){
        if (clientListener != null) {
            clientListener.exit();
        }

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
        private ServerSocket serverSocket;
        private boolean done;

        private ClientListener(int port) throws IOException {
            serverSocket = new ServerSocket(port);
            log.info("TCP服务端地址为:[{}:{}]", serverSocket.getInetAddress(),serverSocket.getLocalPort());
        }

        @Override
        public void run() {
            super.run();
            log.info("TCP服务端准备就绪....");
            do {
                Socket clientScoket;
                try {
                    // 得到客户端连接
                    clientScoket = serverSocket.accept();
                } catch (IOException e) {
                    continue;
                }

                // 构建处理消息的线程
                try {
                    ClientHandler clientHandler = new ClientHandler(clientScoket, TCPServer.this);
                    // 读取并打印
                    clientHandler.readToPrint();
                    synchronized (TCPServer.this) {
                        clientHanlerList.add(clientHandler);
                    }
                } catch (IOException e) {
                    log.error("客户端连接异常:{}", e);
                }

            } while (!done);

            log.info("客户端连接关闭...");

        }

        void exit(){
            done = true;
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}

