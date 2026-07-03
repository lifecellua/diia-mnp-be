package com.lifecell.diia.be.configuration.ext;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLogFormatter;

class JsonStructuredLoggingFormatter implements StructuredLogFormatter<ILoggingEvent> {

    private final JsonWriter<ILoggingEvent> writer = JsonWriter.<ILoggingEvent>of((members) -> {
        members.add("timestamp", (event) -> event.getInstant()).whenNotNull();
        members.add("level", (event) -> event.getLevel()).whenNotNull();
        members.add("thread", (event) -> event.getThreadName()).whenNotNull();
        members.add("mdc", (event) -> event.getMDCPropertyMap()).whenNotNull();
        members.add("class", (event) -> event.getLoggerName()).whenNotNull();
        members.add("msg", (event) -> event.getFormattedMessage()).whenNotNull();
        members.add("throwable", (event) -> event.getThrowableProxy()).whenNotNull().usingMembers((submembers) -> {
            writeThrowable(1, submembers);
        });
    }).withNewLineAtEnd();

    private void writeThrowable(int lvl, JsonWriter.Members<IThrowableProxy> members) {
        members.add("class", (throwable) -> throwable.getClassName()).whenNotNull();
        members.add("message", (throwable) -> throwable.getMessage()).whenNotNull();
        if (lvl <= 10) {
            members.add("cause", (throwable) -> throwable.getCause()).whenNotNull().usingMembers((submembers) -> {
                writeThrowable(lvl + 1, submembers);
            });
        }
        members.add("stacktrace", (throwable) -> throwable.getStackTraceElementProxyArray()).whenNotNull();
    }

    @Override
    public String format(ILoggingEvent event) {
        return this.writer.writeToString(event);
    }
}
