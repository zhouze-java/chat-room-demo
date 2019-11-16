package com.socket.server;

import com.socket.common.constants.UDPConstants;
import com.socket.core.utils.ByteUtils;
import lombok.extern.slf4j.Slf4j;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * @author 周泽
 * @date Create in 10:01 2019/11/13
 * @Description
 */
@Slf4j
public class UDPProvider {

    private static Provider PROVIDER_INSTANCE;

    static void start(int port){
        stop();
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn, port);
        provider.start();
        PROVIDER_INSTANCE = provider;
    }

    static void stop(){
        if (PROVIDER_INSTANCE != null) {
            PROVIDER_INSTANCE.exit();
            PROVIDER_INSTANCE = null;
        }
    }

    @Slf4j
    private static class Provider extends Thread{
        private final byte[] sn;
        private final int port;
        private boolean done = false;
        private DatagramSocket ds = null;

        // 存放消息的byte数组
        final byte[] buffer = new byte[128];


        Provider(String sn, int port) {
            super();
            this.sn = sn.getBytes();
            this.port = port;
        }

        @Override
        public void run() {
            super.run();
            log.info("UDPProvider Started.");

            try {
                // 监听udp端口
                ds = new DatagramSocket(UDPConstants.UDP_PORT);
                // 接收消息
                DatagramPacket receivePack = new DatagramPacket(buffer,buffer.length);

                while (!done) {
                    // 接收消息
                    ds.receive(receivePack);
                    // 打印接收到的消息和发送者的信息
                    log.info("Server Provider 收到来自[{}:{}] 的消息", receivePack.getAddress().getHostAddress(), receivePack.getPort());

                    byte[] data = receivePack.getData();

                    // 验证是否是需要回送的口令 (数据长度,以及开头是要规定的请求头)
                    boolean isValid = data.length >= (UDPConstants.HEADER.length + 2 + 4)
                            && ByteUtils.startsWith(data, UDPConstants.HEADER);

                    if (!isValid) {
                        continue;
                    }

                    // 解析回送数据
                    int index = UDPConstants.HEADER.length;
                    // 命令
                    short cmd = (short) ((data[index++] << 8) | (data[index++] & 0xff));
                    // 端口
                    int responsePort = (((data[index++]) << 24) |
                            ((data[index++] & 0xff) << 16) |
                            ((data[index++] & 0xff) << 8) |
                            ((data[index] & 0xff)));

                    // 口令和回送端口是合法的
                    if (cmd == 1 && responsePort > 0) {
                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                        byteBuffer.put(UDPConstants.HEADER);
                        byteBuffer.putShort((short) 2);
                        byteBuffer.putInt(port);
                        byteBuffer.put(sn);

                        int len = byteBuffer.position();
                        // 回送数据
                        DatagramPacket responsePacket = new DatagramPacket(
                                buffer, len, receivePack.getAddress(), responsePort);

                        ds.send(responsePacket);

                        log.info("已回送数据到[{}:{}]", receivePack.getAddress().getHostAddress(), responsePort);
                    } else{
                        log.info("接收到的口令不合法....");
                    }
                }

            } catch (Exception ignored) {
                // who cares
            }  finally {
                close();
            }
        }

        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }

        /**
         * 结束退出
         */
        void exit(){
            done = true;
            close();
        }
    }
}
