package it.uniroma2.art.semanticturkey.changetracking.sail;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.PROV;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.util.Values;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;

public class UndoStack {
    public static final int MAX_DEPTH = 10;

    private LinkedList<StagingArea> storage;

    public UndoStack() {
        this.storage = new LinkedList<>();
    }

    public void push(StagingArea data) {
        if (storage.size() == MAX_DEPTH) {
            storage.clear();
        }

        Model commitMetadataModel = data.getCommitMetadataModel();
        Optional<IRI> performer = getPerformer(commitMetadataModel);

        if (performer.isPresent()) {
            /* @Nullable */ StagingArea stackTip = storage.peek();
            if (stackTip != null) {
                Optional<IRI> tipPerformer = getPerformer(stackTip.getCommitMetadataModel());
                if (!tipPerformer.filter(performer.get()::equals).isPresent()) {
                    storage.clear();
                }
            }

            storage.push(data);
        } else {
            storage.clear();
        }
    }

    public Optional<StagingArea> peek() {
        return Optional.ofNullable(storage.peek());
    }

    public StagingArea pop() {
        return storage.pop();
    }

    public static Optional<IRI> getPerformer(Model commitMetadataModel) {
        Optional<IRI> performer = Models.objectResources(commitMetadataModel.filter(CHANGETRACKER.COMMIT_METADATA, PROV.QUALIFIED_ASSOCIATION, null))
                .stream()
                .filter(ass -> commitMetadataModel.contains(ass, PROV.HAD_ROLE, Values.iri("http://semanticturkey.uniroma2.it/ns/st-changelog#performer")))
                .map(ass -> Models.getPropertyIRI(commitMetadataModel, ass, PROV.AGENT).orElse(null))
                .filter(Objects::nonNull)
                .findAny();
        return performer;
    }

}
