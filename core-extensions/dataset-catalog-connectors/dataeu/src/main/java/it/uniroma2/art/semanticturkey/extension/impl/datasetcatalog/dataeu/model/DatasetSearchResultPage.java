package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.dataeu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DatasetSearchResultPage {
    @JsonProperty(required = false)
    private String message;
    @JsonProperty(required = false)
    private boolean success = true;
    private DatasetSearchResultContainer result;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DatasetSearchResultContainer getResult() {
        return result;
    }

    public void setResult(DatasetSearchResultContainer result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("help", message).add("success", success)
                .add("result", result).toString();
    }
}
