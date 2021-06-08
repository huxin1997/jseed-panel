package com.jseed.panel.web.controller;


import cn.hutool.system.OsInfo;
import cn.hutool.system.SystemUtil;
import cn.hutool.system.oshi.CpuInfo;
import cn.hutool.system.oshi.OshiUtil;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import oshi.hardware.GlobalMemory;
import oshi.hardware.NetworkIF;

import java.io.File;
import java.util.List;

@Controller
public class RouteController {

    @GetMapping("/")
    public String toDashboard(Model model) {

        List<NetworkIF> networkIFs = OshiUtil.getNetworkIFs();
        NetworkIF networkIF = networkIFs.get(2);
//        model.addAttribute("info", networkIF.getSpeed());
//        model.addAttribute("networkIF", networkIF);
        model.addAttribute("msg", "hello");
        CpuInfo cpuInfo = OshiUtil.getCpuInfo();
        GlobalMemory memory = OshiUtil.getMemory();
        long available = memory.getAvailable();
        model.addAttribute("cpu", cpuInfo);
        float m = 1024F * 1024;
        model.addAttribute("memory", (memory.getTotal() - memory.getAvailable()) / m / (memory.getTotal() / m) *100);
        ApplicationHome applicationHome = new ApplicationHome();
        File homeDir = applicationHome.getDir();
        model.addAttribute("files", homeDir.listFiles());
        model.addAttribute("net", networkIF.getBytesSent()/m);
        OsInfo osInfo = SystemUtil.getOsInfo();
        model.addAttribute("OSInfo", osInfo);
        return "dashboard";
    }


}
