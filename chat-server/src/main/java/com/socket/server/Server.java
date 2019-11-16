package com.socket.server;

import com.socket.common.constants.TCPConstants;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author 周泽
 * @date Create in 10:43 2019/11/13
 * @Description 启动类
 */
@Slf4j
@Data
public class Server {

    public static void main(String[] args) throws IOException {

        TCPServer tcpServer = new TCPServer(TCPConstants.SERVER_PORT);
        boolean successed = tcpServer.start();
        if (!successed) {
            log.info("TCP 服务端启动失败...");
        }

        UDPProvider.start(TCPConstants.SERVER_PORT);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String str;
        do {
            str = bufferedReader.readLine();
            // 给所有的客户端发送消息
            tcpServer.broadcast(str);
        } while (!"00bye00".equalsIgnoreCase(str));

        UDPProvider.stop();
        tcpServer.stop();
    }
}
