package it.uniroma2.art.semanticturkey.services.core.export;

import com.fasterxml.jackson.annotation.JsonProperty;

import it.uniroma2.art.semanticturkey.services.core.PluginSpecification;

/**
 * A filtering step consists of a {@link PluginSpecification} and a collection of graphs it applies to.
 *  
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class FilteringStep {
	private PluginSpecification filter;
	private int[] graphs;

	public FilteringStep(@JsonProperty("filter")PluginSpecification filter, @JsonProperty("graphs")int [] graphs) {
		this.filter = filter;
		this.graphs = graphs;
	}

	public PluginSpecification getFilter() {
		return filter;
	}

	public int[] getGraphs() {
		return graphs;
	}
}
