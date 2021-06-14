package it.uniroma2.art.semanticturkey.extension.impl.datasetcatalog.dataeu.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DatasetShowPage {
	private String help;
	private boolean success;
	private DatasetShowResultContainer result;

	public String getHelp() {
		return help;
	}

	public void setHelp(String help) {
		this.help = help;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public DatasetShowResultContainer getResult() {
		return result;
	}

	public void setResult(DatasetShowResultContainer result) {
		this.result = result;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("help", help).add("success", success)
				.add("result", result).toString();
	}
}
