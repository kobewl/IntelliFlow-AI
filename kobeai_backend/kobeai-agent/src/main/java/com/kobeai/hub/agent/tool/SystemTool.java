package com.kobeai.hub.agent.tool;

import io.agentscope.core.tool.Tool;

import java.lang.management.ManagementFactory;

public class SystemTool {

    @Tool(name = "get_system_info", description = "获取当前系统运行信息，包括 Java 版本、可用内存、系统时间等")
    public String getSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        String javaVersion = System.getProperty("java.version");
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

        return String.format("""
                IntelliFlow AI 系统状态
                - Java 版本: %s
                - 运行时长: %d 秒
                - 内存使用: %d MB / %d MB
                """, javaVersion, uptime, usedMemory, maxMemory);
    }
}
