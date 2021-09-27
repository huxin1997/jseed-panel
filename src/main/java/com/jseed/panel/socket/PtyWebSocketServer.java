package com.jseed.panel.socket;

import com.pty4j.PtyProcess;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.*;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/socket/{userId}")
@Component
@Log4j2
@Order(10)
public class PtyWebSocketServer {

    /**静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。*/
    private static int onlineCount = 0;
    /**concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。*/
    private static ConcurrentHashMap<String, PtyWebSocketServer> webSocketMap = new ConcurrentHashMap<>();
    /**与某个客户端的连接会话，需要通过它来给客户端发送数据*/
    private Session session;
    /**接收userId*/
    private String userId="";
    private final PtyProcess pty;

    public PtyWebSocketServer() throws IOException {

        // The command to run in a PTY...
        String[] cmd = {"/bin/bash", "-l"};
        // The initial environment to pass to the PTY child process...
        String[] env = {"TERM=xterm"};
        HashMap<String, String> map = new HashMap<>();
        map.put("TERM", "xterm");

//       this.pty = new PtyProcessBuilder(cmd)
//                .setEnvironment(map)
//                .setCygwin(true)
//                .setInitialRows(30)
//       .start();


        this.pty = PtyProcess.exec(cmd, env);
//        WinSize winSize = this.pty.getWinSize();
//        winSize.ws_col =100;
//        winSize.ws_row = 60;
//        this.pty.setWinSize(winSize);
        OutputStream os = this.pty.getOutputStream();
        InputStream is = this.pty.getInputStream();

// ... work with the streams ...

        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader reader = new BufferedReader(isr);
            Thread terminalReaderThread = new Thread(() -> {
                try {
                    int c;
                    while (PtyWebSocketServer.this.pty.isRunning() && (c = reader.read()) != -1) {

                        if(c != 0){
                            sendMessage((Character.toString((char) c)));
    //                        print(Character.toString((char)c));
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        terminalReaderThread.start();
    }

    /**
     * 连接建立成功调用的方法*/
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        this.session = session;
        this.userId=userId;
        if(webSocketMap.containsKey(userId)){
            webSocketMap.remove(userId);
            webSocketMap.put(userId,this);
            //加入set中
        }else{
            webSocketMap.put(userId,this);
            //加入set中
            addOnlineCount();
            //在线数加1
        }

        log.info("用户连接:"+userId+",当前在线人数为:" + getOnlineCount());

        try {
            sendMessage(" \033[32m连接成功！\033[0m \r\b");
        } catch (IOException e) {
            log.error("用户:"+userId+",网络异常!!!!!!");
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        if(webSocketMap.containsKey(userId)){
            webSocketMap.remove(userId);
            //从set中删除
            subOnlineCount();
        }

        log.info("用户退出:"+userId+",当前在线人数为:" + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param data 客户端发送过来的消息*/
    @OnMessage
    public void onMessage(String data, Session session) throws IOException {
        log.info("用户消息:"+userId+",报文:"+ data);
        //可以群发消息
        //消息保存到数据库、redis
        pty.getOutputStream().write(data.getBytes());
//        if(StringUtils.isNotBlank(message)){
//            try {
//                //解析发送的报文
//                JSONObject jsonObject = JSON.parseObject(message);
//                //追加发送人(防止串改)
//                jsonObject.put("fromUserId",this.userId);
//                String toUserId=jsonObject.getString("toUserId");
//                //传送给对应toUserId用户的websocket
//                if(StringUtils.isNotBlank(toUserId)&&webSocketMap.containsKey(toUserId)){
//                    webSocketMap.get(toUserId).sendMessage(jsonObject.toJSONString());
//                }else{
//                    log.error("请求的userId:"+toUserId+"不在该服务器上");
//                    //否则不在这个服务器上，发送到mysql或者redis
//                }
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }
    }

    /**
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("用户错误:"+this.userId+",原因:"+error.getMessage());
        error.printStackTrace();
    }
    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) throws IOException {
        if (this.session!= null) {
            this.session.getBasicRemote().sendText(message);
        }
    }


    /**
     * 发送自定义消息
     * */
    public static void sendInfo(String message,@PathParam("userId") String userId) throws IOException {
        log.info("发送消息到:"+userId+"，报文:"+message);
        // !!
        if(!StringUtils.isEmpty(userId)&&webSocketMap.containsKey(userId)){
            webSocketMap.get(userId).sendMessage(message);
        }else{
            log.error("用户"+userId+",不在线！");
        }
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        PtyWebSocketServer.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        PtyWebSocketServer.onlineCount--;
    }

}
