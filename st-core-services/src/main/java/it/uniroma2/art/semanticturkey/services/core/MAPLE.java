package it.uniroma2.art.semanticturkey.services.core;

import java.util.Arrays;
import java.util.UUID;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.springframework.beans.factory.annotation.Autowired;

import it.uniroma2.art.lime.model.vocabulary.LIME;
import it.uniroma2.art.maple.orchestration.MediationFramework;
import it.uniroma2.art.maple.orchestration.ProfilingException;
import it.uniroma2.art.maple.problem.MediationProblem;
import it.uniroma2.art.semanticturkey.data.access.LocalResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.RemoteResourcePosition;
import it.uniroma2.art.semanticturkey.data.access.ResourcePosition;
import it.uniroma2.art.semanticturkey.project.ForbiddenProjectAccessException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectACL.AccessLevel;
import it.uniroma2.art.semanticturkey.project.ProjectACL.LockLevel;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.project.ProjectManager.AccessResponse;
import it.uniroma2.art.semanticturkey.resources.MetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;

/**
 * This class provides access to the capabilities of <a href="http://art.uniroma2.it/maple/">MAPLE</a>
 * (Mapping Architecture based on Linguistic Evidences).
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class MAPLE extends STServiceAdapter {

	@Autowired
	private MediationFramework mediationFramework;

	@Autowired
	private MetadataRegistryBackend metadataRegistryBackend;

	@STServiceOperation
	public MediationProblem profileMediationProblem(ResourcePosition resourcePosition)
			throws ProfilingException, ForbiddenProjectAccessException {

		Project targetProject = null;
		it.uniroma2.art.semanticturkey.resources.DatasetMetadata targetDatasetMetadata = null;

		if (resourcePosition instanceof LocalResourcePosition) {
			Project targetProjectTemp = ((LocalResourcePosition) resourcePosition).getProject();
			AccessResponse accessResponse = ProjectManager.checkAccessibility(getProject(), targetProjectTemp,
					AccessLevel.R, LockLevel.NO);
			if (!accessResponse.isAffirmative()) {
				throw new ForbiddenProjectAccessException(accessResponse.getMsg());
			}

			targetProject = targetProjectTemp;
		} else if (resourcePosition instanceof RemoteResourcePosition) {
			targetDatasetMetadata = ((RemoteResourcePosition) resourcePosition).getDatasetMetadata();
		} else {
			throw new IllegalArgumentException("Unsupported resource position");
		}

		Model targetDatasetProfile;
		IRI dataset;
		if (targetProject != null) {
			throw new RuntimeException("boo");
		} else {
			dataset = targetDatasetMetadata.getIdentity();
		}
		if (targetDatasetMetadata != null) {
			try (RepositoryConnection conn = metadataRegistryBackend.getConnection()) {
				GraphQuery graphQuery = conn.prepareGraphQuery(
						"PREFIX void: <http://rdfs.org/ns/void#> DESCRIBE ?y WHERE {?x void:subset* ?y}");
				graphQuery.setBinding("x", dataset);
				targetDatasetProfile = QueryResults.asModel(graphQuery.evaluate());
			}
		} else {
			throw new RuntimeException("booo");
		}

		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		IRI sourceDataset = vf.createIRI("http://foo/", UUID.randomUUID().toString());
		Model sourceDatasetProfile = new LinkedHashModel();
		sourceDatasetProfile.add(sourceDataset, RDF.TYPE, VOID.DATASET);

		for (String lang : Arrays.asList("en", "it")) {
			IRI lex = vf.createIRI("http://foo/", UUID.randomUUID().toString());

			sourceDatasetProfile.add(sourceDataset, VOID.SUBSET, lex);
			sourceDatasetProfile.add(lex, RDF.TYPE, LIME.LEXICALIZATION_SET);
			sourceDatasetProfile.add(lex, LIME.REFERENCE_DATASET, sourceDataset);
			sourceDatasetProfile.add(lex, LIME.LEXICALIZATION_MODEL, getProject().getLexicalizationModel());
			sourceDatasetProfile.add(lex, LIME.LANGUAGE, vf.createLiteral(lang, XMLSchema.LANGUAGE));
			sourceDatasetProfile.add(lex, LIME.PERCENTAGE, vf.createLiteral(1));
		}

		return mediationFramework.profileProblem(sourceDataset, sourceDatasetProfile, dataset,
				targetDatasetProfile);
	}
}
