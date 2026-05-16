package com.kobeai.hub.agent.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;

import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;

public class DateTimeTool {

    @Tool(name = "get_current_time", description = "获取当前日期和时间，包括星期和时区信息")
    public String getCurrentTime(
            @ToolParam(name = "timezone", description = "时区，例如 Asia/Shanghai, UTC, America/New_York，默认 Asia/Shanghai") String timezone) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss");
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        String[] weekDays = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
        return String.format("%s %s (时区: %s)",
                now.format(fmt), weekDays[dayOfWeek.getValue() - 1],
                timezone != null ? timezone : "Asia/Shanghai");
    }

    @Tool(name = "calculate", description = "执行数学计算，支持加减乘除、乘方、开方、三角函数等。例如: 3^10 + sqrt(144)")
    public String calculate(
            @ToolParam(name = "expression", description = "数学表达式，例如 '3^10 + sqrt(144)'") String expression) {
        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
            if (engine == null) {
                return "计算引擎不可用";
            }
            Object result = engine.eval(expression);
            return expression + " = " + result;
        } catch (Exception e) {
            return "计算失败: " + e.getMessage() + "。请检查表达式是否正确。";
        }
    }

    @Tool(name = "date_diff", description = "计算两个日期之间相差的天数")
    public String dateDiff(
            @ToolParam(name = "date1", description = "第一个日期，格式 yyyy-MM-dd") String date1,
            @ToolParam(name = "date2", description = "第二个日期，格式 yyyy-MM-dd") String date2) {
        try {
            LocalDateTime d1 = LocalDateTime.parse(date1 + "T00:00:00");
            LocalDateTime d2 = LocalDateTime.parse(date2 + "T00:00:00");
            long days = Math.abs(java.time.Duration.between(d1, d2).toDays());
            return String.format("%s 和 %s 相差 %d 天", date1, date2, days);
        } catch (Exception e) {
            return "日期格式错误，请使用 yyyy-MM-dd 格式";
        }
    }
}
