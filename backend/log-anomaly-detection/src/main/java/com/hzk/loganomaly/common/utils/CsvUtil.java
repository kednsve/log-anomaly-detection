package com.hzk.loganomaly.common.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.hzk.loganomaly.entity.AccessLog;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CsvUtil {

    private static final DateTimeFormatter[] TIME_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
    };

    @Data
    public static class ParseResult {
        private int totalRows = 0;
        private List<AccessLog> validLogs = new ArrayList<>();
        private List<String> errors = new ArrayList<>();
    }

    public static ParseResult parse(InputStream inputStream) {
        ParseResult result = new ParseResult();

        EasyExcel.read(inputStream, CsvRow.class, new ReadListener<CsvRow>() {
            @Override
            public void invoke(CsvRow row, AnalysisContext context) {
                result.setTotalRows(result.getTotalRows() + 1);
                int lineNum = context.readRowHolder().getRowIndex() + 1;

                String error = validateAndConvert(row, lineNum, result);
                if (error != null) {
                    result.getErrors().add(error);
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                log.info("CSV解析完成，总行数：{}，成功：{}，失败：{}",
                        result.getTotalRows(), result.getValidLogs().size(), result.getErrors().size());
            }
        }).sheet().doRead();

        return result;
    }

    private static String validateAndConvert(CsvRow row, int lineNum, ParseResult result) {
        if (row == null || isBlank(row.getIp())) return "第" + lineNum + "行：空行或IP为空";

        if (!IpUtil.isValidIpv4(row.getIp())) return "第" + lineNum + "行：IP格式错误 -> " + row.getIp();

        LocalDateTime time = parseTime(row.getAccessTime());
        if (time == null) return "第" + lineNum + "行：时间格式错误 -> " + row.getAccessTime();

        if (isBlank(row.getUrl())) return "第" + lineNum + "行：URL为空";

        Integer status = parseInteger(row.getStatusCode());
        if (status == null || status < 100 || status > 599) return "第" + lineNum + "行：状态码错误";

        AccessLog log = new AccessLog();
        log.setIp(row.getIp().trim());
        log.setAccessTime(time);
        log.setUrl(truncate(row.getUrl(), 500));
        log.setStatusCode(status);
        log.setMethod(isBlank(row.getMethod()) ? "GET" : row.getMethod().trim().toUpperCase());
        log.setResponseTime(parseInteger(row.getResponseTime()) == null ? 0 : parseInteger(row.getResponseTime()));
        log.setUserAgent(truncate(row.getUserAgent(), 500));

        log.setRuleAbnormal(0);
        log.setModelAbnormal(0);
        log.setFinalAbnormal(0);

        result.getValidLogs().add(log);
        return null;
    }

    private static LocalDateTime parseTime(String timeStr) {
        if (isBlank(timeStr)) return null;
        for (DateTimeFormatter f : TIME_FORMATS) {
            try { return LocalDateTime.parse(timeStr.trim(), f); } catch (Exception ignored) {}
        }
        return null;
    }

    private static Integer parseInteger(String str) {
        if (isBlank(str)) return null;
        try { return Integer.parseInt(str.trim()); } catch (Exception e) { return null; }
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static String truncate(String s, int max) { return s != null && s.length() > max ? s.substring(0, max) : s; }

    @Data
    public static class CsvRow {
        @ExcelProperty(index = 0)
        private String ip;
        @ExcelProperty(index = 1)
        private String accessTime;
        @ExcelProperty(index = 2)
        private String url;
        @ExcelProperty(index = 3)
        private String statusCode;
        @ExcelProperty(index = 4)
        private String method;
        @ExcelProperty(index = 5)
        private String responseTime;
        @ExcelProperty(index = 6)
        private String userAgent;
    }
}