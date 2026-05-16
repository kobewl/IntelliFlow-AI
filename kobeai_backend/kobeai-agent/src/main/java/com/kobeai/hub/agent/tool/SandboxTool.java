package com.kobeai.hub.agent.tool;

import io.agentscope.core.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SandboxTool {

    private static final int TIMEOUT_SECONDS = 10;
    private static final int MAX_OUTPUT_CHARS = 5000;
    private static final Set<String> DANGEROUS_COMMANDS = Set.of(
            "rm", "mkfs", "dd", "shutdown", "reboot", "halt",
            "chmod", "chown", "passwd", "sudo", "su",
            "kill", "pkill", "killall", "fdisk", "format"
    );

    @Tool(name = "run_python", description = "执行 Python 代码。传入要执行的 Python 代码片段。注意：最长执行 10 秒，输出限制 5000 字符")
    public String runPython(String code) {
        if (code == null || code.isBlank()) {
            return "错误：代码不能为空";
        }
        log.info("沙盒执行 Python 代码: {} 字符", code.length());
        return execute(List.of("python3", "-c", code));
    }

    @Tool(name = "run_javascript", description = "执行 JavaScript 代码（通过 Node.js）。传入要执行的 JS 代码片段。注意：最长执行 10 秒，输出限制 5000 字符")
    public String runJavascript(String code) {
        if (code == null || code.isBlank()) {
            return "错误：代码不能为空";
        }
        log.info("沙盒执行 JavaScript 代码: {} 字符", code.length());
        return execute(List.of("node", "-e", code));
    }

    @Tool(name = "run_shell", description = "执行 Shell 命令。注意：包含危险操作的命令会被拒绝，最长执行 10 秒，输出限制 5000 字符")
    public String runShell(String command) {
        if (command == null || command.isBlank()) {
            return "错误：命令不能为空";
        }

        // 安全检查
        String lower = command.toLowerCase();
        for (String dangerous : DANGEROUS_COMMANDS) {
            if (lower.contains(dangerous)) {
                log.warn("沙盒拒绝危险命令: {}", command);
                return "错误：命令包含危险操作 (" + dangerous + ")，已被拒绝";
            }
        }

        log.info("沙盒执行 Shell 命令: {}", command);
        return execute(List.of("bash", "-c", command));
    }

    private String execute(List<String> cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() + line.length() > MAX_OUTPUT_CHARS) {
                        output.append("\n... (输出已截断，超过 ").append(MAX_OUTPUT_CHARS).append(" 字符)");
                        break;
                    }
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "错误：执行超时（" + TIMEOUT_SECONDS + " 秒）";
            }

            int exitCode = process.exitValue();
            String result = output.toString().trim();
            return exitCode == 0
                    ? (result.isEmpty() ? "执行成功（无输出）" : result)
                    : "执行失败 (退出码: " + exitCode + ")\n" + result;

        } catch (Exception e) {
            log.error("沙盒执行失败: {}", e.getMessage());
            return "执行出错: " + e.getMessage();
        }
    }
}
