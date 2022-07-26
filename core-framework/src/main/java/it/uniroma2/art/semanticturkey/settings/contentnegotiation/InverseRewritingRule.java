package it.uniroma2.art.semanticturkey.settings.contentnegotiation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InverseRewritingRule {

    private String sourceRDFresURIregExp;
    private Map<String, String> formatMap;
    private String targetResURIExp;

    public InverseRewritingRule() {}

    public InverseRewritingRule(String sourceRDFresURIregExp, Map<String, String> formatMap, String targetResURIExp) {
        this.sourceRDFresURIregExp = sourceRDFresURIregExp;
        this.formatMap = formatMap;
        this.targetResURIExp = targetResURIExp;
    }

    public String getSourceRDFresURIregExp() {
        return sourceRDFresURIregExp;
    }

    public void setSourceRDFresURIregExp(String sourceRDFresURIregExp) {
        this.sourceRDFresURIregExp = sourceRDFresURIregExp;
    }

    public Map<String, String> getFormatMap() {
        return formatMap;
    }

    public void setFormatMap(Map<String, String> formatMap) {
        this.formatMap = formatMap;
    }

    public String getTargetResURIExp() {
        return targetResURIExp;
    }

    public void setTargetResURIExp(String targetResURIExp) {
        this.targetResURIExp = targetResURIExp;
    }
}
