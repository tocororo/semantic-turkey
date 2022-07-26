package it.uniroma2.art.semanticturkey.settings.contentnegotiation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RewritingRule {

    private String sourceURIRegExp;
    private ContentNegotiationFormat format;
    private String targetURIExp;

    public RewritingRule() {}

    public RewritingRule(String sourceURIRegExp, ContentNegotiationFormat format, String targetURIRegExp) {
        this.sourceURIRegExp = sourceURIRegExp;
        this.format = format;
        this.targetURIExp = targetURIRegExp;
    }

    public String getSourceURIRegExp() {
        return sourceURIRegExp;
    }

    public void setSourceURIRegExp(String sourceURIRegExp) {
        this.sourceURIRegExp = sourceURIRegExp;
    }

    public ContentNegotiationFormat getFormat() {
        return format;
    }

    public void setFormat(ContentNegotiationFormat format) {
        this.format = format;
    }

    public String getTargetURIExp() {
        return targetURIExp;
    }

    public void setTargetURIExp(String targetURIExp) {
        this.targetURIExp = targetURIExp;
    }

}
