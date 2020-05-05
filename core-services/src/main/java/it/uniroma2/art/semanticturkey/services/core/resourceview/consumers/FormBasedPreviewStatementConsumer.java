package it.uniroma2.art.semanticturkey.services.core.resourceview.consumers;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;

import it.uniroma2.art.coda.core.CODACore;
import it.uniroma2.art.coda.exception.parserexception.PRParserException;
import it.uniroma2.art.semanticturkey.customform.CODACoreProvider;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.services.core.CustomForms;
import it.uniroma2.art.semanticturkey.services.core.resourceview.ResourceViewSection;
import it.uniroma2.art.semanticturkey.services.core.resourceview.StatementConsumer;

public class FormBasedPreviewStatementConsumer implements StatementConsumer {

	private static final Logger logger = LoggerFactory.getLogger(FormBasedPreviewStatementConsumer.class);

	private CustomFormManager cfManager;
	private ObjectFactory<CODACoreProvider> codaProvider;

	public FormBasedPreviewStatementConsumer(CustomFormManager cfManager,
			ObjectFactory<CODACoreProvider> codaProvider) {
		this.cfManager = cfManager;
		this.codaProvider = codaProvider;
	}

	@Override
	public Map<String, ResourceViewSection> consumeStatements(Project project, RepositoryConnection repoConn,
			ResourcePosition resourcePosition, Resource resource, Model statements,
			Set<Statement> processedStatements, Resource workingGraph,
			Map<Resource, Map<String, Value>> resource2attributes,
			Map<IRI, Map<Resource, Literal>> predicate2resourceCreShow, Model propertyModel) {
		if (resourcePosition instanceof LocalResourcePosition
				&& ((LocalResourcePosition) resourcePosition).getProject().equals(project)) {
			Set<IRI> types = Models.objectIRIs(statements.filter(resource, RDF.TYPE, null));

			CODACore codaCore = codaProvider.getObject().getCODACore();
			codaCore.initialize(repoConn);
			try {
				try {
					return CustomForms.getResourceFormPreviewHelper(project, codaCore, cfManager, repoConn,
							resource, types, true);
				} catch (PRParserException e) {
					logger.error("Unexpected error", e);
				}
			} finally {
				codaCore.setRepositoryConnection(null);
				codaCore.stopAndClose();
			}
		}

		return Collections.emptyMap();
	}

}
