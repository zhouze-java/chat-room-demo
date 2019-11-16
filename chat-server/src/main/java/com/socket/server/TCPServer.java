package com.socket.server;

import com.socket.server.handler.ClientHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 周泽
 * @date Create in 15:42 2019/11/13
 * @Description TCP服务端
 */
@Slf4j
public class TCPServer {

    private final int port;
    private ClientListener clientListener;
    private List<ClientHandler> clientHanlerList = new ArrayList<>();

    public TCPServer(int port) {
        this.port = port;
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

        for (ClientHandler clientHanler : clientHanlerList) {
            clientHanler.exit();
        }
        clientHanlerList.clear();
    }

    /**
     * 给所有客户端发送信息
     * @param str
     */
    public void broadcast(String str) {
        for (ClientHandler clientHanler : clientHanlerList) {
            clientHanler.send(str);
        }
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
                    ClientHandler clientHandler = new ClientHandler(clientScoket, clientHandler1 -> clientHanlerList.remove(clientHandler1));
                    // 读取并打印
                    clientHandler.readToPrint();
                    clientHanlerList.add(clientHandler);
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

