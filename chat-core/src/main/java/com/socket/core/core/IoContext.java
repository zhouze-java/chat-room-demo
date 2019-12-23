package com.socket.core.core;

import java.io.IOException;

/**
 * @author 周泽
 * @date Create in 11:18 2019/12/23
 * @Description 对 {@link IoProvider 做一个提供}
 */
public class IoContext {
    private static IoContext INSTANCE;
    private final IoProvider ioProvider;

    public IoContext(IoProvider ioProvider) {
        this.ioProvider = ioProvider;
    }

    public IoProvider getIoProvider(){return ioProvider;}

    public static IoContext get(){
        return INSTANCE;
    }

    public static StartedBoot setUp(){
        return new StartedBoot();
    }

    public static void close() throws IOException {
        if (INSTANCE != null) {
            INSTANCE.callClose();
        }
    }

    private void callClose() throws IOException {
        ioProvider.close();
    }

    public static class StartedBoot{
        private IoProvider ioProvider;

        private StartedBoot(){}

        public StartedBoot ioProvider(IoProvider ioProvider) {
            this.ioProvider = ioProvider;
            return this;
        }

        public IoContext start(){
            INSTANCE = new IoContext(ioProvider);
            return INSTANCE;
        }
    }
}
