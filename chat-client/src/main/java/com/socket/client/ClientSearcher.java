package com.socket.client;

import com.socket.client.bean.ServerInfo;
import com.socket.common.constants.UDPConstants;
import com.socket.core.utils.ByteUtils;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author 周泽
 * @date Create in 10:48 2019/11/13
 * @Description
 */
@Slf4j
public class ClientSearcher {
    /**
     * 监听回送端口
     */
    private static final int LISTEN_PORT = UDPConstants.UDP_ACK_PORT;

    public static ServerInfo searchServer(int timeout) throws InterruptedException, IOException {
        log.info("UPDSearcher Started...");
        CountDownLatch receiveCountDownLatch = new CountDownLatch(1);
        // 监听
        Listener listener = null;
        try {
            listener = listen(receiveCountDownLatch);
            // 发送
            send();
            // 等待接收到数据
            receiveCountDownLatch.await(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        log.info("UPDSearcher Done...");

        if (listener == null) {
            return null;
        }
        // 接收到了之后关闭
        List<ServerInfo> serverInfos = listener.getServerAndClose();
        if (CollectionUtils.isEmpty(serverInfos)) {
            return null;
        }
        return serverInfos.get(0);
    }

    private static Listener listen(CountDownLatch receiveLatch) throws InterruptedException {
        log.info("UDP listen started ...");
        CountDownLatch startCountDownLatch = new CountDownLatch(1);
        Listener listener = new Listener(LISTEN_PORT, startCountDownLatch, receiveLatch);
        listener.start();
        startCountDownLatch.await();

        return listener;
    }

    private static void send() throws IOException {
        // 让系统分配端口
        @Cleanup DatagramSocket ds = new DatagramSocket();

        // 构建请求数据
        ByteBuffer byteBuffer = ByteBuffer.allocate(128);
        // 请求头
        byteBuffer.put(UDPConstants.HEADER);
        // 口令
        byteBuffer.putShort((short) 1);
        // 回送端口
        byteBuffer.putInt(LISTEN_PORT);

        // 构建数据包
        DatagramPacket requestDataPacket = new DatagramPacket(byteBuffer.array(), byteBuffer.position() + 1);

        requestDataPacket.setAddress(InetAddress.getByName("255.255.255.255"));
        requestDataPacket.setPort(UDPConstants.UDP_PORT);

        // 发送
        ds.send(requestDataPacket);
        log.info("UDP 广播发送成功...");
    }

    private static class Listener extends Thread {
        private final int listenPort;
        private final CountDownLatch startDownLatch;
        private final CountDownLatch receiveDownLatch;
        private final List<ServerInfo> serverInfoList = new ArrayList<>();
        private final byte[] buffer = new byte[128];
        /**
         * 数据包的最短长度
         */
        private final int minLen = UDPConstants.HEADER.length + 2 + 4;

        private boolean done = false;
        private DatagramSocket ds = null;

        Listener(int listenPort, CountDownLatch startDownLatch, CountDownLatch receiveDownLatch) {
            super();
            this.listenPort = listenPort;
            this.startDownLatch = startDownLatch;
            this.receiveDownLatch = receiveDownLatch;
        }

        @Override
        public void run() {
            super.run();
            // 通知监听器已经启动
            startDownLatch.countDown();

            try {
                ds = new DatagramSocket(listenPort);
                // 接收信息的数据包
                DatagramPacket receicePacket = new DatagramPacket(buffer, buffer.length);

                while (!done) {
                    ds.receive(receicePacket);
                    log.info("接收到来自[{}:{}]的消息", receicePacket.getAddress().getHostAddress(), receicePacket.getPort());
                    byte[] data = receicePacket.getData();
                    // 验证
                    boolean isValid = data.length >= minLen
                            && ByteUtils.startsWith(data, UDPConstants.HEADER);

                    if (!isValid) {
                        continue;
                    }
                    // 去掉请求头,从命令开始读取
                    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, UDPConstants.HEADER.length, receicePacket.getLength());
                    final short cmd = byteBuffer.getShort();
                    final int port = byteBuffer.getInt();
                    if (cmd != 2 || port <= 0) {
                        log.info("口令或端口有误....");
                        continue;
                    }

                    String sn = new String(buffer, minLen, data.length - minLen);
                    ServerInfo serverInfo = new ServerInfo(sn, port, receicePacket.getAddress().getHostAddress());
                    serverInfoList.add(serverInfo);

                    // 成功接收 通知
                    receiveDownLatch.countDown();

                }
            } catch (Exception ignored) {
                // who cares
            } finally {
                close();
            }

            log.info("UDPSearcher listener done...");
        }

        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }

        List<ServerInfo> getServerAndClose() {
            done = true;
            close();
            return serverInfoList;
        }
    }

}
