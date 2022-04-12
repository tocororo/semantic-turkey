package it.uniroma2.art.semanticturkey.settings.core;

import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.STProperty;

import java.util.Objects;
import java.util.Optional;

public class ErrorReportingSettings implements STProperties {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.settings.core.ErrorReportingSettings";

        public static final String shortName = keyBase + ".shortName";

        public static final String reportStackTrace$description = keyBase + ".reportStackTrace.description";
        public static final String reportStackTrace$displayName = keyBase + ".reportStackTrace.displayName";
    }

    public static final Boolean REPORT_STACK_TRACE_DEFAULT = Boolean.TRUE;

    @Override
    public String getShortName() {
        return "{" + MessageKeys.shortName + "}";
    }

    @STProperty(description = "{" + MessageKeys.reportStackTrace$description + "}", displayName = "{"
            + MessageKeys.reportStackTrace$displayName + "}")
    public Boolean reportStackTrace;

    public static boolean shouldReportStackTrace(ErrorReportingSettings errorReportingSettings) {
        return Optional.ofNullable(errorReportingSettings)
                .map(s -> s.reportStackTrace)
                .filter(Objects::nonNull)
                .orElse(REPORT_STACK_TRACE_DEFAULT);
    }
}
