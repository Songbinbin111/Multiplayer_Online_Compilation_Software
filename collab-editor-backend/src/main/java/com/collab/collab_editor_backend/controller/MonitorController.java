package com.collab.collab_editor_backend.controller;

import com.collab.collab_editor_backend.util.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统监控控制器
 */
@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.profiles.active}")
    private String activeProfile;

    /**
     * 获取系统信息
     */
    @GetMapping("/system")
    public Result<Map<String, Object>> getSystemInfo() {
        Map<String, Object> systemInfo = new LinkedHashMap<>();
        OperatingSystemMXBean osMxBean = ManagementFactory.getOperatingSystemMXBean();
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();

        // 应用信息
        systemInfo.put("applicationName", applicationName);
        systemInfo.put("activeProfile", activeProfile);
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("javaHome", System.getProperty("java.home"));
        systemInfo.put("osName", System.getProperty("os.name"));
        systemInfo.put("osVersion", System.getProperty("os.version"));
        systemInfo.put("osArch", System.getProperty("os.arch"));
        systemInfo.put("startTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
                new Date(runtimeMxBean.getStartTime())));
        systemInfo.put("uptime", formatUptime(runtimeMxBean.getUptime()));

        return Result.success(systemInfo);
    }

    /**
     * 获取JVM内存信息
     */
    @GetMapping("/memory")
    public Result<Map<String, Object>> getMemoryInfo() {
        Map<String, Object> memoryInfo = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();

        // 内存信息（字节转换为MB）
        memoryInfo.put("totalMemory", runtime.totalMemory() / 1024 / 1024);
        memoryInfo.put("maxMemory", runtime.maxMemory() / 1024 / 1024);
        memoryInfo.put("freeMemory", runtime.freeMemory() / 1024 / 1024);
        memoryInfo.put("usedMemory", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        memoryInfo.put("usedMemoryPercentage", Math.round(((double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory()) * 100));

        return Result.success(memoryInfo);
    }

    /**
     * 获取CPU使用率信息
     */
    @GetMapping("/cpu")
    public Result<Map<String, Object>> getCpuInfo() {
        Map<String, Object> cpuInfo = new LinkedHashMap<>();
        OperatingSystemMXBean osMxBean = ManagementFactory.getOperatingSystemMXBean();

        cpuInfo.put("availableProcessors", osMxBean.getAvailableProcessors());
        
        // 获取系统负载平均值（如果支持）
        if (osMxBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsMxBean = (com.sun.management.OperatingSystemMXBean) osMxBean;
            cpuInfo.put("systemCpuLoad", Math.round(sunOsMxBean.getSystemCpuLoad() * 100));
            cpuInfo.put("processCpuLoad", Math.round(sunOsMxBean.getProcessCpuLoad() * 100));
        }

        return Result.success(cpuInfo);
    }

    /**
     * 获取磁盘使用情况
     */
    @GetMapping("/disk")
    public Result<Map<String, Object>> getDiskInfo() {
        Map<String, Object> diskInfo = new LinkedHashMap<>();
        File[] roots = File.listRoots();
        
        List<Map<String, Object>> diskList = java.util.Arrays.stream(roots)
            .map(root -> {
                Map<String, Object> disk = new LinkedHashMap<>();
                disk.put("path", root.getPath());
                disk.put("totalSpace", root.getTotalSpace() / 1024 / 1024 / 1024); // GB
                disk.put("freeSpace", root.getFreeSpace() / 1024 / 1024 / 1024); // GB
                disk.put("usableSpace", root.getUsableSpace() / 1024 / 1024 / 1024); // GB
                disk.put("usedSpace", (root.getTotalSpace() - root.getFreeSpace()) / 1024 / 1024 / 1024); // GB
                disk.put("usedPercentage", Math.round(((double)(root.getTotalSpace() - root.getFreeSpace()) / root.getTotalSpace()) * 100));
                return disk;
            })
            .collect(Collectors.toList());
        
        diskInfo.put("disks", diskList);
        return Result.success(diskInfo);
    }

    /**
     * 获取线程信息
     */
    @GetMapping("/threads")
    public Result<Map<String, Object>> getThreadInfo() {
        Map<String, Object> threadInfo = new LinkedHashMap<>();
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        
        threadInfo.put("threadCount", threadMxBean.getThreadCount());
        threadInfo.put("peakThreadCount", threadMxBean.getPeakThreadCount());
        threadInfo.put("daemonThreadCount", threadMxBean.getDaemonThreadCount());
        threadInfo.put("totalStartedThreadCount", threadMxBean.getTotalStartedThreadCount());
        threadInfo.put("deadlockedThreadCount", threadMxBean.findDeadlockedThreads() != null ? threadMxBean.findDeadlockedThreads().length : 0);
        
        return Result.success(threadInfo);
    }

    /**
     * 获取系统健康报告
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> getHealthReport() {
        Map<String, Object> healthReport = new LinkedHashMap<>();
        
        // 系统信息
        healthReport.put("system", getSystemInfo().getData());
        
        // 内存信息
        healthReport.put("memory", getMemoryInfo().getData());
        
        // CPU信息
        healthReport.put("cpu", getCpuInfo().getData());
        
        // 磁盘信息
        healthReport.put("disk", getDiskInfo().getData());
        
        // 线程信息
        healthReport.put("threads", getThreadInfo().getData());
        
        // 应用状态
        Map<String, Object> appStatus = new LinkedHashMap<>();
        appStatus.put("status", "UP");
        appStatus.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        
        healthReport.put("appStatus", appStatus);
        
        return Result.success(healthReport);
    }

    /**
     * 格式化运行时间
     */
    private String formatUptime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "天 " + (hours % 24) + "小时 " + (minutes % 60) + "分钟";
        } else if (hours > 0) {
            return hours + "小时 " + (minutes % 60) + "分钟 " + (seconds % 60) + "秒";
        } else if (minutes > 0) {
            return minutes + "分钟 " + (seconds % 60) + "秒";
        } else {
            return seconds + "秒";
        }
    }
}
