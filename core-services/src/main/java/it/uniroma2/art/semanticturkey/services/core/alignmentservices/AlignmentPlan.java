package it.uniroma2.art.semanticturkey.services.core.alignmentservices;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.maple.scenario.ScenarioDefinition;

public class AlignmentPlan {
	@JsonProperty(required = true)
	private ScenarioDefinition scenarioDefinition;
	@JsonProperty(required = false)
	@Nullable
	private ObjectNode settings;
	@JsonProperty(required = false)
	@Nullable
	private MatcherDefinitionDTO matcherDefinition;

	public void setScenarioDefinition(ScenarioDefinition scenarioDefinition) {
		this.scenarioDefinition = scenarioDefinition;
	}

	public ScenarioDefinition getScenarioDefinition() {
		return scenarioDefinition;
	}

	public void setSettings(ObjectNode settings) {
		this.settings = settings;
	}

	public ObjectNode getSettings() {
		return settings;
	}

	public void setMatcherDefinition(MatcherDefinitionDTO matcherDefinition) {
		this.matcherDefinition = matcherDefinition;
	}

	public MatcherDefinitionDTO getMatcherDefinition() {
		return matcherDefinition;
	}
}
