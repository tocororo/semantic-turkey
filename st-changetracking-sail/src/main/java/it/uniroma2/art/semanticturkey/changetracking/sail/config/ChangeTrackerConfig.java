package it.uniroma2.art.semanticturkey.changetracking.sail.config;

import static it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema.EXCLUDE_GRAPH;
import static it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema.HISTORY_GRAPH;
import static it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema.METADATA_NS;
import static it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema.SUPPORT_REPOSITORY_ID;
import static it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema.INCLUDE_GRAPH;
import static it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema.INTERACTIVE_NOTIFICATIONS;
import static it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema.SERVER_URL;
import static it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema.HISTORY_ENABLED;
import static it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema.VALIDATION_ENABLED;
import static it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema.VALIDATION_GRAPH;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
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

	private String supportRepositoryID;
	private String metadataNS;
	private String serverURL;
	private IRI historyGraph;
	private Set<IRI> includeGraph;
	private Set<IRI> excludeGraph;
	private Boolean interactiveNotifications;
	private boolean historyEnabled;
	private boolean validationEnabled;
	private IRI validationGraph;

	public ChangeTrackerConfig() {
		this(null);
	}

	public ChangeTrackerConfig(SailImplConfig delegate) {
		super(ChangeTrackerFactory.SAIL_TYPE, delegate);
		serverURL = null;
		supportRepositoryID = null;
		metadataNS = null;
		historyGraph = null;
		includeGraph = Collections.emptySet();
		excludeGraph = Collections.singleton(SESAME.NIL);
		interactiveNotifications = null;
		validationGraph = null;
		historyEnabled = true;
		validationEnabled = false;
	}

	public String getSupportRepositoryID() {
		return supportRepositoryID;
	}

	public void setSupportRepositoryID(String supportRepositoryID) {
		this.supportRepositoryID = supportRepositoryID;
	}

	public String getMetadataNS() {
		return metadataNS;
	}

	public void setMetadataNS(String metadataNS) {
		this.metadataNS = metadataNS;
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

	public Optional<Boolean> isInteractiveNotifications() {
		return Optional.ofNullable(interactiveNotifications);
	}

	public void setInteractiveNotifications(/* @Nullable */ Boolean interactiveNotifications) {
		this.interactiveNotifications = interactiveNotifications;
	}

	public boolean isHistoryEnabled() {
		return historyEnabled;
	}

	public boolean isValidationEnabled() {
		return validationEnabled;
	}

	public void setHistoryEnabled(boolean historyEnabled) {
		this.historyEnabled = historyEnabled;
	}

	public void setValidationEnabled(boolean validationEnabled) {
		this.validationEnabled = validationEnabled;
	}

	public IRI getValidationGraph() {
		return validationGraph;
	}

	public void setValidationGraph(IRI validationGraph) {
		this.validationGraph = validationGraph;
	}

	@Override
	public Resource export(Model graph) {
		Resource implNode = super.export(graph);

		ValueFactory vf = SimpleValueFactory.getInstance();

		if (serverURL != null) {
			graph.add(implNode, SERVER_URL, vf.createLiteral(serverURL));
		}

		if (supportRepositoryID != null) {
			graph.add(implNode, SUPPORT_REPOSITORY_ID, vf.createLiteral(supportRepositoryID));
		}

		if (metadataNS != null) {
			graph.add(implNode, METADATA_NS, vf.createLiteral(metadataNS));
		}

		if (historyGraph != null) {
			graph.add(implNode, HISTORY_GRAPH, historyGraph);
		} else {
			graph.add(implNode, HISTORY_GRAPH, SESAME.NIL);
		}

		if (validationGraph != null) {
			graph.add(implNode, VALIDATION_GRAPH, validationGraph);
		} else {
			graph.add(implNode, VALIDATION_GRAPH, SESAME.NIL);
		}

		for (IRI g : includeGraph) {
			graph.add(implNode, INCLUDE_GRAPH, g);
		}

		for (IRI g : excludeGraph) {
			graph.add(implNode, EXCLUDE_GRAPH, g);
		}

		graph.add(implNode, HISTORY_ENABLED, vf.createLiteral(historyEnabled));
		graph.add(implNode, VALIDATION_ENABLED, vf.createLiteral(validationEnabled));

		if (interactiveNotifications != null) {
			graph.add(implNode, INTERACTIVE_NOTIFICATIONS, vf.createLiteral(interactiveNotifications));
		}

		return implNode;
	}

	@Override
	public void parse(Model graph, Resource implNode) throws SailConfigException {
		super.parse(graph, implNode);

		Models.objectString(graph.filter(implNode, SERVER_URL, null)).ifPresent(this::setServerURL);
		Models.objectString(graph.filter(implNode, SUPPORT_REPOSITORY_ID, null))
				.ifPresent(this::setSupportRepositoryID);
		Models.objectString(graph.filter(implNode, METADATA_NS, null)).ifPresent(this::setMetadataNS);
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
		Models.objectIRI(graph.filter(implNode, VALIDATION_GRAPH, null))
				.map(v -> SESAME.NIL.equals(v) ? null : v).ifPresent(this::setValidationGraph);

		historyEnabled = graph
				.filter(implNode, HISTORY_ENABLED, SimpleValueFactory.getInstance().createLiteral(false))
				.isEmpty() ? true : false;
		validationEnabled = graph
				.filter(implNode, VALIDATION_ENABLED, SimpleValueFactory.getInstance().createLiteral(true))
				.isEmpty() ? false : true;

		Set<Value> knownValuesForInteractiveModifications = graph
				.filter(implNode, INTERACTIVE_NOTIFICATIONS, null).objects();

		interactiveNotifications = knownValuesForInteractiveModifications
				.contains(SimpleValueFactory.getInstance().createLiteral(true))
						? Boolean.TRUE
						: (knownValuesForInteractiveModifications.contains(
								SimpleValueFactory.getInstance().createLiteral(false)) ? Boolean.FALSE
										: null);
	}

	@Override
	public void validate() throws SailConfigException {
		super.validate();

		if (supportRepositoryID == null) {
			throw new SailConfigException("No support repository set for " + getType() + " Sail.");
		}

		if (metadataNS == null) {
			throw new SailConfigException("No metadata namespace set for " + getType() + " Sail.");
		}

		if (includeGraph == null) {
			throw new SailConfigException("Null include graph for " + getType() + " Sail.");
		}

		if (excludeGraph == null) {
			throw new SailConfigException("Null exclude graph for " + getType() + " Sail.");
		}

		if (historyEnabled && historyGraph == null) {
			throw new SailConfigException("History enabled but no history graph is specified");
		}

		if (validationEnabled && validationGraph == null) {
			throw new SailConfigException("Validation enabled but no validation graph is specified");
		}
	}

	public String getServerURL() {
		return serverURL;
	}

	public void setServerURL(String serverURL) {
		this.serverURL = serverURL;
	}
}
