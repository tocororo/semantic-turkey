package it.uniroma2.art.semanticturkey.services.core.alignmentservices.backend;

import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.maple.problem.MatchingProblem;

public class TaskSubmission {
	private MatchingProblem taskReport;
	private ObjectNode systemConfiguration;
	private String matcher;
	private ObjectNode matcherConfiguration;

	public MatchingProblem getTaskReport() {
		return taskReport;
	}

	public void setTaskReport(MatchingProblem taskReport) {
		this.taskReport = taskReport;
	}

	public ObjectNode getSystemConfiguration() {
		return systemConfiguration;
	}

	public void setSystemConfiguration(ObjectNode systemConfiguration) {
		this.systemConfiguration = systemConfiguration;
	}

	public String getMatcher() {
		return matcher;
	}

	public void setMatcher(String matcher) {
		this.matcher = matcher;
	}

	public ObjectNode getMatcherConfiguration() {
		return matcherConfiguration;
	}

	public void setMatcherConfiguration(ObjectNode matcherConfiguration) {
		this.matcherConfiguration = matcherConfiguration;
	}
}
