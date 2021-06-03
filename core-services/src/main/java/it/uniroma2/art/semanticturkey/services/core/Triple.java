package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.semanticturkey.data.nature.TripleScopes;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

public class Triple {
    private final AnnotatedValue<Resource> subject;
    private final AnnotatedValue<IRI> predicate;
    private final AnnotatedValue<Value> object;
    private final String graphs;
    private final TripleScopes tripleScope;

    public Triple(AnnotatedValue<Resource> subject, AnnotatedValue<IRI> predicate, AnnotatedValue<Value> object, String graphs, TripleScopes tripleScope) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
        this.graphs = graphs;
        this.tripleScope = tripleScope;
    }

    public AnnotatedValue<Resource> getSubject() {
        return subject;
    }

    public AnnotatedValue<IRI> getPredicate() {
        return predicate;
    }

    public AnnotatedValue<Value> getObject() {
        return object;
    }

    public String getGraphs() {
        return graphs;
    }

    public TripleScopes getTripleScope() {
        return tripleScope;
    }
}
