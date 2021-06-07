package com.jseed.panel;

import cn.hutool.system.oshi.OshiUtil;
import com.pty4j.PtyProcess;
import org.junit.jupiter.api.Test;
import oshi.hardware.NetworkIF;

import java.io.*;
import java.util.List;

//@SpringBootTest
class JseedPanelApplicationTests {

    @Test
    void test(){
        List<NetworkIF> networkIFs = OshiUtil.getNetworkIFs();
        System.out.println(OshiUtil.getCpuInfo());

    }

    @Test
    void contextLoads() throws IOException, InterruptedException {

        // The command to run in a PTY...
        String[] cmd = {"/bin/bash", "-l"};
// The initial environment to pass to the PTY child process...
        String[] env = {"TERM=xterm"};


        PtyProcess pty = PtyProcess.exec(cmd, env);


        OutputStream os = pty.getOutputStream();
        InputStream is = pty.getInputStream();

// ... work with the streams ...

        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader reader = new BufferedReader(isr);


        System.out.println(pty.getPid());
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
        bw.flush();

        Thread terminalReaderThread = new Thread() {
            public void run() {
                try {
                    int c;
                    while (pty.isRunning() && (c = reader.read()) != -1) {

                        if (c != 0) {
                            print(Character.toString((char) c));
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        };
        terminalReaderThread.start();


// wait until the PTY child process terminates...
        int result = pty.waitFor();

    }
    private void print(String toString) {
        System.out.print(toString);
    }

}
