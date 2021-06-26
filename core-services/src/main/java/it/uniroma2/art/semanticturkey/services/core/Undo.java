package it.uniroma2.art.semanticturkey.services.core;

import com.google.common.collect.ImmutableList;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.history.CommitInfo;
import it.uniroma2.art.semanticturkey.services.core.history.ParameterInfo;
import it.uniroma2.art.semanticturkey.services.core.history.SupportRepositoryUtils;
import it.uniroma2.art.semanticturkey.services.tracker.STServiceTracker;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.vocabulary.STCHANGELOG;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.PROV;
import org.eclipse.rdf4j.model.vocabulary.SESAME;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service class related to the management of undo
 */
@STService
public class Undo extends STServiceAdapter {
    private static final Logger logger = LoggerFactory.getLogger(Undo.class);

    @Autowired
    private STServiceTracker stServiceTracker;

    /**
     * Undoes the latest operation operation of the currently logged-in user
     * @return
     */
    @STServiceOperation(method = RequestMethod.POST)
    @Write
    public CommitInfo undo() {
        RepositoryConnection con = getManagedConnection();
        ValueFactory vf = getManagedConnection().getValueFactory();
        con.add(vf.createBNode(), PROV.AGENT, UsersManager.getLoggedUser().getIRI(), CHANGETRACKER.UNDO);
        IRI undoIRI = vf.createIRI(CHANGETRACKER.UNDO.stringValue() + "?nonce=" + System.currentTimeMillis());
        GraphQuery undoMetadataQuery = con.prepareGraphQuery("DESCRIBE " + RenderUtils.toSPARQL(undoIRI) + " FROM <" + CHANGETRACKER.UNDO + ">");
        Model undoMetadata = QueryResults.asModel(undoMetadataQuery.evaluate());
        
        CommitInfo commitInfo = new CommitInfo();

        Models.getPropertyIRI(undoMetadata, CHANGETRACKER.COMMIT_METADATA, PROV.USED).map(v -> {
            AnnotatedValue<IRI> rv = new AnnotatedValue<IRI>(v);
            SupportRepositoryUtils.computeOperationDisplay(stServiceTracker, rv);
            return rv;
        }).ifPresent(op -> {
            commitInfo.setOperation(op);
        });

        commitInfo.setCreated(Models.getPropertyResources(undoMetadata, CHANGETRACKER.COMMIT_METADATA, vf.createIRI("http://semanticturkey.uniroma2.it/ns/st-changelog#created")).stream().collect(Collectors.toList()));
        commitInfo.setModified(Models.getPropertyResources(undoMetadata, CHANGETRACKER.COMMIT_METADATA, vf.createIRI("http://semanticturkey.uniroma2.it/ns/st-changelog#modified")).stream().collect(Collectors.toList()));
        commitInfo.setDeleted(Models.getPropertyResources(undoMetadata, CHANGETRACKER.COMMIT_METADATA, vf.createIRI("http://semanticturkey.uniroma2.it/ns/st-changelog#deleted")).stream().collect(Collectors.toList()));

        Pattern paramPattern = Pattern.compile("^param-(\\d+)-(\\w+)$");
        Models.getPropertyResource(undoMetadata, CHANGETRACKER.COMMIT_METADATA, STCHANGELOG.PARAMETERS).ifPresent(params -> {
            Map<Integer, ParameterInfo> parameters = new TreeMap<>();
            for (Statement st : undoMetadata.filter(params, null, null)) {
                Matcher m = paramPattern.matcher(st.getPredicate().getLocalName());
                if (!m.find()) continue;

                int paramPosition = Integer.parseInt(m.group(1));
                String paramName = m.group(2);

                String valueStr = ImmutableList.of(CHANGELOG.NULL, SESAME.NIL).contains(st.getObject()) ? null : st.getObject().stringValue();

                parameters.put(paramPosition, new ParameterInfo(paramName, valueStr));
            }

            commitInfo.setOperationParameters(new ArrayList<>(parameters.values()));
        });

        return commitInfo;
    }

}
