package it.uniroma2.art.semanticturkey.changetracking.sail.config;

import static it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema.EXCLUDE_GRAPH;
import static it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema.HISTORY_GRAPH;
import static it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema.HISTORY_NS;
import static it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema.SERVER_URL;
import static it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema.HISTORY_REPOSITORY_ID;
import static it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema.INCLUDE_GRAPH;
import static it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema.VALIDATION_ENABLED;
import static it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema.INTERACTIVE_NOTIFICATIONS;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.sail.config.AbstractDelegatingSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailImplConfig;

import it.uniroma2.art.semanticturkey.changetracking.sail.ChangeTracker;

/**
 * A configuration class for the {@link ChangeTracker} sail.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ChangeTrackerConfig extends AbstractDelegatingSailImplConfig {

	private String historyRepositoryID;
	private String historyNS;
	private String serverURL;
	private IRI historyGraph;
	private Set<IRI> includeGraph;
	private Set<IRI> excludeGraph;
	private boolean interactiveNotifications;
	private boolean validationEnabled;
	
	public ChangeTrackerConfig() {
		this(null);
	}

	public ChangeTrackerConfig(SailImplConfig delegate) {
		super(ChangeTrackerFactory.SAIL_TYPE, delegate);
		serverURL = null;
		historyRepositoryID = null;
		historyNS = null;
		historyGraph = null;
		includeGraph = Collections.emptySet();
		excludeGraph = Collections.singleton(SESAME.NIL);
		interactiveNotifications = false;
	}

	public String getHistoryRepositoryID() {
		return historyRepositoryID;
	}

	public void setHistoryRepositoryID(String historyRepositoryID) {
		this.historyRepositoryID = historyRepositoryID;
	}

	public String getHistoryNS() {
		return historyNS;
	}

	public void setHistoryNS(String metadataNS) {
		this.historyNS = metadataNS;
	}

	public IRI getHistoryGraph() {
		return historyGraph;
	}

	public void setHistoryGraph(IRI historyGraph) {
		this.historyGraph = historyGraph;
	}

	public Set<IRI> getIncludeGraph() {
		return Collections.unmodifiableSet(includeGraph);
	}

	public void setIncludeGraph(Set<IRI> includeGraph) {
		this.includeGraph = new HashSet<>(includeGraph);
	}

	public Set<IRI> getExcludeGraph() {
		return Collections.unmodifiableSet(excludeGraph);
	}

	public void setExcludeGraph(Set<IRI> excludeGraph) {
		this.excludeGraph = new HashSet<>(excludeGraph);
	}

	public boolean isInteractiveNotifications() {
		return interactiveNotifications;
	}

	public void setInteractiveNotifications(boolean interactiveNotifications) {
		this.interactiveNotifications = interactiveNotifications;
	}

	public boolean isValidationEnabled() {
		return validationEnabled;
	}

	public void setValidationEnabled(boolean validationEnabled) {
		this.validationEnabled = validationEnabled;
	}

	@Override
	public Resource export(Model graph) {
		Resource implNode = super.export(graph);

		ValueFactory vf = SimpleValueFactory.getInstance();

		if (serverURL != null) {
			graph.add(implNode, SERVER_URL, vf.createLiteral(serverURL));
		}

		if (historyRepositoryID != null) {
			graph.add(implNode, HISTORY_REPOSITORY_ID, vf.createLiteral(historyRepositoryID));
		}

		if (historyNS != null) {
			graph.add(implNode, HISTORY_NS, vf.createLiteral(historyNS));
		}

		if (historyGraph != null) {
			graph.add(implNode, HISTORY_GRAPH, historyGraph);
		} else {
			graph.add(implNode, HISTORY_GRAPH, SESAME.NIL);
		}

		for (IRI g : includeGraph) {
			graph.add(implNode, INCLUDE_GRAPH, g);
		}

		for (IRI g : excludeGraph) {
			graph.add(implNode, EXCLUDE_GRAPH, g);
		}

		graph.add(implNode, VALIDATION_ENABLED, vf.createLiteral(validationEnabled));
		
		graph.add(implNode, INTERACTIVE_NOTIFICATIONS, vf.createLiteral(interactiveNotifications));

		return implNode;
	}

	@Override
	public void parse(Model graph, Resource implNode) throws SailConfigException {
		super.parse(graph, implNode);

		Models.objectString(graph.filter(implNode, SERVER_URL, null)).ifPresent(this::setServerURL);
		Models.objectString(graph.filter(implNode, HISTORY_REPOSITORY_ID, null))
				.ifPresent(this::setHistoryRepositoryID);
		Models.objectString(graph.filter(implNode, HISTORY_NS, null)).ifPresent(this::setHistoryNS);
		Models.objectIRI(graph.filter(implNode, HISTORY_GRAPH, null))
				.map(v -> SESAME.NIL.equals(v) ? null : v).ifPresent(this::setHistoryGraph);
		Set<IRI> newIncludeGraph = new HashSet<IRI>();
		graph.filter(implNode, INCLUDE_GRAPH, null).stream().forEach(st -> {
			Value obj = st.getObject();
			if (obj instanceof IRI) {
				newIncludeGraph.add((IRI) obj);
			}
		});
		includeGraph = newIncludeGraph;

		Set<IRI> newExcludeGraph = new HashSet<IRI>();
		graph.filter(implNode, EXCLUDE_GRAPH, null).stream().forEach(st -> {
			Value obj = st.getObject();
			if (obj instanceof IRI) {
				newExcludeGraph.add((IRI) obj);
			}
		});
		excludeGraph = newExcludeGraph;

		validationEnabled = graph.filter(implNode, VALIDATION_ENABLED,
				SimpleValueFactory.getInstance().createLiteral(true)).isEmpty() ? false : true;

		interactiveNotifications = graph.filter(implNode, INTERACTIVE_NOTIFICATIONS,
				SimpleValueFactory.getInstance().createLiteral(true)).isEmpty() ? false : true;
	}

	@Override
	public void validate() throws SailConfigException {
		super.validate();

		if (historyRepositoryID == null) {
			throw new SailConfigException("No history repository set for " + getType() + " Sail.");
		}

		if (historyNS == null) {
			throw new SailConfigException("No history namespace set for " + getType() + " Sail.");
		}

		if (includeGraph == null) {
			throw new SailConfigException("Null include graph for " + getType() + " Sail.");
		}

		if (excludeGraph == null) {
			throw new SailConfigException("Null exclude graph for " + getType() + " Sail.");
		}
	}

	public String getServerURL() {
		return serverURL;
	}

	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}
}
