package it.uniroma2.art.semanticturkey.changetracking.sail;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.Statement;

/**
 * A data structure keeping track of quadruples effectively added or removed.
 * 
 * @author <a href="fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class StagingArea {
	private final Set<Statement> addedStatements;
	private final Set<Statement> removedStatements;

	public StagingArea() {
		addedStatements = new HashSet<>();
		removedStatements = new HashSet<>();
	}

	public void clear() {
		addedStatements.clear();
		removedStatements.clear();
	}

	public void stageAddition(Statement st) {
		if (removedStatements.contains(st)) {
			removedStatements.remove(st);
			return;
		}
		addedStatements.add(st);
	}

	public void stageRemoval(Statement st) {
		if (addedStatements.contains(st)) {
			addedStatements.remove(st);
			return;
		}
		removedStatements.add(st);
	}

	public Set<Statement> getAddedStatements() {
		return Collections.unmodifiableSet(addedStatements);
	}

	public Set<Statement> getRemovedStatements() {
		return Collections.unmodifiableSet(removedStatements);
	}

	public boolean isEmpty() {
		return addedStatements.isEmpty() && removedStatements.isEmpty();
	}
}
