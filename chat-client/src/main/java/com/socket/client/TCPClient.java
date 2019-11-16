package com.socket.client;

import com.socket.client.bean.ServerInfo;
import com.socket.core.utils.CloseUtils;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * @author 周泽
 * @date Create in 16:39 2019/11/13
 * @Description TCP客户端
 */
@Slf4j
public class TCPClient {

    public static void linkWith(ServerInfo info) throws IOException {
        Socket socket = new Socket();
        // 设置超时时间
        socket.setSoTimeout(3000);

        // 绑定服务端端口
        socket.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()), 3000);

        log.info("连接TCP服务器...");
        log.info("客户端地址:[{}:{}]", socket.getLocalAddress(), socket.getLocalPort());
        log.info("服务端信息:[{}:{}]", socket.getInetAddress().getHostAddress(), socket.getPort());

        try {
            // 启动读取线程
            ReadHandler readHandler = new ReadHandler(socket.getInputStream());
            readHandler.start();
            // 发送数据
            write(socket);
        } catch (Exception e) {
            log.info("异常关闭");
        }

        // 关闭退出
        socket.close();
        log.info("TCP客户端结束退出");

    }

    private static void write(Socket client) throws IOException {
        // 构建键盘输入流
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        // 得到Socket输出流，并转换为打印流
        OutputStream outputStream = client.getOutputStream();
        @Cleanup PrintStream socketPrintStream = new PrintStream(outputStream);

        do {
            // 键盘读取一行
            String str = input.readLine();
            // 发送到服务器
            socketPrintStream.println(str);

            if ("00bye00".equalsIgnoreCase(str)) {
                break;
            }
        } while (true);
    }

    @Slf4j
    static class ReadHandler extends Thread {
        private boolean done = false;
        private final InputStream inputStream;

        ReadHandler(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            super.run();

            try {
                // 得到输入流，用于接收数据
                @Cleanup BufferedReader socketInput = new BufferedReader(new InputStreamReader(inputStream));

                String str;
                do {
                    // 客户端拿到一条数据
                    try {
                        str = socketInput.readLine();

                    } catch (SocketTimeoutException e) {
                        continue;
                    }
                    if (str == null) {
                        log.info("服务端连接已关闭,无法读取数据");
                        break;
                    }
                    log.info("接收到的数据:{}", str);
                } while (!done);
            } catch (Exception e) {
                if (!done){
                    log.info("客户端连接异常断开...");
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
