package com.socket.client;

import com.socket.client.bean.ServerInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author 周泽
 * @date Create in 14:38 2019/11/13
 * @Description
 */
@Slf4j
public class Client {
    public static void main(String[] args) throws IOException, InterruptedException {
        ServerInfo info = ClientSearcher.searchServer(1000);

        log.info("serverInfo:{}", info);

        if (info != null) {
            try {
                TCPClient.linkWith(info);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
