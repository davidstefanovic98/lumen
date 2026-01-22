package io.lumen.core.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

class LumenLogFormatter extends Formatter {
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS");

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        LocalDateTime time = LocalDateTime.ofInstant(
                record.getInstant(),
                java.time.ZoneId.systemDefault()
        );
        sb.append(time.format(TIME_FORMATTER));
        sb.append("  ");

        String level = record.getLevel().getName();
        sb.append(String.format("%-5s", level));
        sb.append(" ");

        sb.append(String.format("%-15s",
                Thread.currentThread().getName().substring(0,
                        Math.min(15, Thread.currentThread().getName().length()))));
        sb.append(" ");

        String loggerName = record.getLoggerName();
        sb.append(shortenLoggerName(loggerName));
        sb.append(" : ");

        sb.append(formatMessage(record));
        sb.append("\n");

        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            record.getThrown().printStackTrace(pw);
            sb.append(sw);
        }

        return sb.toString();
    }

    private String shortenLoggerName(String name) {
        if (name.length() <= 40) {
            return String.format("%-40s", name);
        }

        String[] parts = name.split("\\.");
        StringBuilder shortened = new StringBuilder();

        for (int i = 0; i < parts.length - 1; i++) {
            shortened.append(parts[i].charAt(0)).append(".");
        }
        shortened.append(parts[parts.length - 1]);

        return String.format("%-40s", shortened);
    }
}
