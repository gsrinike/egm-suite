package eu.egm.srv.iidm.transformer.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import eu.egm.data.iidm.common.IidmDiagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.LoggerFactory;

/**
 * Captures bounded transform diagnostics for persistence in Elasticsearch.
 */
public class IidmTransformDiagnosticsCollector implements AutoCloseable {
    private static final int MAX_LOG_MESSAGES = 100;
    private static final String POWSYBL_CGMES_LOGGER = "com.powsybl.cgmes.conversion";

    private final List<IidmDiagnostic> diagnostics = new ArrayList<>();
    private final AtomicInteger warningCount = new AtomicInteger();
    private final AtomicInteger errorCount = new AtomicInteger();
    private final Logger logger;
    private final DiagnosticAppender appender;

    public IidmTransformDiagnosticsCollector(String sourceId) {
        org.slf4j.Logger candidate = LoggerFactory.getLogger(POWSYBL_CGMES_LOGGER);
        if (candidate instanceof Logger logbackLogger) {
            this.logger = logbackLogger;
            this.appender = new DiagnosticAppender(sourceId);
            this.appender.setContext(logbackLogger.getLoggerContext());
            this.appender.start();
            this.logger.addAppender(appender);
        } else {
            this.logger = null;
            this.appender = null;
        }
    }

    public List<IidmDiagnostic> successDiagnostics(List<IidmDiagnostic> baseDiagnostics) {
        List<IidmDiagnostic> values = new ArrayList<>(baseDiagnostics == null ? List.of() : baseDiagnostics);
        values.addAll(capturedDiagnostics());
        addSummary(values, "WARN", "POWSYBL_WARNING_COUNT", warningCount.get());
        addSummary(values, "ERROR", "POWSYBL_ERROR_COUNT", errorCount.get());
        return values;
    }

    public List<IidmDiagnostic> failureDiagnostics(Exception exception, String sourceId) {
        List<IidmDiagnostic> values = new ArrayList<>();
        Throwable root = rootCause(exception);
        values.add(new IidmDiagnostic(
                "ERROR",
                root.getClass().getSimpleName(),
                root.getMessage() == null || root.getMessage().isBlank() ? root.toString() : root.getMessage(),
                sourceId));
        values.addAll(capturedDiagnostics());
        addSummary(values, "WARN", "POWSYBL_WARNING_COUNT", warningCount.get());
        addSummary(values, "ERROR", "POWSYBL_ERROR_COUNT", errorCount.get());
        return values;
    }

    private List<IidmDiagnostic> capturedDiagnostics() {
        return appender == null ? List.of() : appender.diagnostics();
    }

    private void addSummary(List<IidmDiagnostic> values, String severity, String code, int count) {
        if (count > 0) {
            values.add(new IidmDiagnostic(severity, code, String.valueOf(count), ""));
        }
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    @Override
    public void close() {
        if (logger != null && appender != null) {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private final class DiagnosticAppender extends AppenderBase<ILoggingEvent> {
        private final String sourceId;
        private final List<IidmDiagnostic> values = new ArrayList<>();

        private DiagnosticAppender(String sourceId) {
            this.sourceId = sourceId;
        }

        @Override
        protected synchronized void append(ILoggingEvent event) {
            if (event.getLevel().isGreaterOrEqual(Level.WARN)) {
                if (event.getLevel().isGreaterOrEqual(Level.ERROR)) {
                    errorCount.incrementAndGet();
                } else {
                    warningCount.incrementAndGet();
                }
                if (values.size() < MAX_LOG_MESSAGES) {
                    values.add(new IidmDiagnostic(
                            event.getLevel().levelStr,
                            "POWSYBL_CGMES_" + event.getLevel().levelStr,
                            event.getFormattedMessage(),
                            sourceId));
                }
            }
        }

        private synchronized List<IidmDiagnostic> diagnostics() {
            return List.copyOf(values);
        }
    }
}
