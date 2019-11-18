package com.socket.server.handler;

import com.socket.core.utils.CloseUtils;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author 周泽
 * @date Create in 10:15 2019/11/14
 * @Description
 */
@Slf4j
public class ClientHandler {
    private final Socket socket;
    private final ClientReadHandler readHandler;
    private final ClientWriteHandler writeHandler;
    private final ClientHandlerCallBack clientHandlerCallBack;
    private final String clientInfo;

    public ClientHandler(Socket socket, ClientHandlerCallBack clientHandlerCallBack) throws IOException {
        this.socket = socket;
        readHandler = new ClientReadHandler(socket.getInputStream());
        writeHandler = new ClientWriteHandler(socket.getOutputStream());
        this.clientHandlerCallBack = clientHandlerCallBack;
        this.clientInfo = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        log.info("得到新的客户端连接,客户端地址:[{}:{}]", socket.getInetAddress(), socket.getPort());
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
        CloseUtils.close(socket);
        log.info("客户端:[{}:{}]已退出...",socket.getInetAddress(),socket.getPort());
    }

    public void readToPrint() {
        readHandler.start();
    }

    private void exitBySelf(){
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
        private final PrintStream printStream;
        private final ExecutorService executorService;

        ClientWriteHandler(OutputStream outputStream) {
            this.printStream = new PrintStream(outputStream);
            this.executorService = Executors.newSingleThreadExecutor();
        }



        void exit(){
            done = true;
            CloseUtils.close(printStream);
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
                this.msg = msg;
            }

            @Override
            public void run() {
                if (ClientWriteHandler.this.done) {
                    return;
                }
                ClientWriteHandler.this.printStream.println(msg);
            }
        }

    }

    class ClientReadHandler extends Thread {
        private boolean done = false;
        private final InputStream inputStream;

        ClientReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            super.run();

            try {
                // 得到输入流，用于接收数据
                @Cleanup BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));

                do {
                    // 客户端拿到一条数据
                    String str = socketInput.readLine();
                    if (str == null) {
                        log.error("客户端已无法读取数据.");
                        // 退出
                        ClientHandler.this.exitBySelf();
                        break;
                    }

                    clientHandlerCallBack.onNewMessageArrived(ClientHandler.this, str);

                } while (!done);
            } catch (Exception e) {
                if (!done){
                    log.info("客户端连接异常断开...");
                    ClientHandler.this.exitBySelf();
                }
            } finally {
                // 关闭读取流
                CloseUtils.close(inputStream);
            }
        }

        void exit(){
            done = true;
            CloseUtils.close(inputStream);
        }

    }

}
