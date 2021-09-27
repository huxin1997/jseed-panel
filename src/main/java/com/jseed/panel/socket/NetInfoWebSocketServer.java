package com.jseed.panel.socket;

import cn.hutool.system.oshi.OshiUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import oshi.hardware.NetworkIF;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Component
@EnableScheduling
@ServerEndpoint("/socket/netInfo")
public class NetInfoWebSocketServer {

    /**
     * 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
     */
    private static int onlineCount = 0;
    /**
     * concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
     */
    private static ConcurrentHashMap<String, NetInfoWebSocketServer> webSocketMap = new ConcurrentHashMap<>();
    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;

    private final NetworkIF networkIF;
    private long beforeRecv;

    public NetInfoWebSocketServer() {
        List<NetworkIF> networkIFs = OshiUtil.getNetworkIFs();
        networkIF = networkIFs.get(4);
        beforeRecv = networkIF.getBytesRecv();
    }

    @Scheduled(cron = "*/2 * * * * ?")
    private void scheduleTask() throws IOException {
        networkIF.updateAttributes();
        long bytesRecv = networkIF.getBytesRecv();
        long seceondRecvBytes = (bytesRecv - beforeRecv) / 1024L;
        beforeRecv = bytesRecv;
        sendMessage(seceondRecvBytes);
//        log.info("{}", seceondRecvBytes);
    }

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session) throws IOException {
        this.session = session;
        webSocketMap.put("user", this);
        sendMessage("hello");

    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        log.info("用户退出");
    }

    /**
     * 实现服务器主动推送
     */
    public void sendMessage(Object message) throws IOException {
        webSocketMap.values()
                .forEach(server -> {
                    try {
                        Session session = server.session;
                        if (session != null) {
                            if (message instanceof String) {
                                session.getBasicRemote().sendText(((String) message));
                            } else {
                                session.getBasicRemote().sendObject(message);
                            }
                        }
                    } catch (IOException | EncodeException e) {
                        e.printStackTrace();
                    }
                });
    }


    /**
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("用户错误:,原因:" + error.getMessage());
        error.printStackTrace();
    }

}
