package it.uniroma2.art.semanticturkey.services.core;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.history.CommitInfo;
import it.uniroma2.art.semanticturkey.services.core.history.Page;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersManager;

/**
 * This class provides services related to operation validation.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class Validation extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Validation.class);

	@STServiceOperation
	public Page<CommitInfo> getStagedCommits(@Optional(defaultValue = "100") int limit) {

		Repository supportRepository = getProject().getRepositoryManager().getRepository("support");

		try (RepositoryConnection conn = supportRepository.getConnection()) {
			String queryString =
				// @formatter:off
				" prefix cl: <http://semanticturkey.uniroma2.it/ns/changelog#>                         \n" +
				" prefix prov: <http://www.w3.org/ns/prov#>                                            \n" +
				" prefix dcterms: <http://purl.org/dc/terms/>                                          \n" +
	            "                                                                                      \n" +
				" select * {                                                                           \n" +
	            "    ?commit a cl:Commit .                                                             \n" +
				"    ?commit prov:endedAtTime ?endTime .                                               \n" +
	            "    FILTER NOT EXISTS { ?commit cl:parentCommit [] }                                  \n" +
				"    FILTER NOT EXISTS { cl:MASTER cl:tip ?commit }                                    \n" +
				"    optional {                                                                        \n" +
				"       ?commit prov:used ?operation .                                                 \n" +
				"    }                                                                                 \n" +
				"    optional {                                                                        \n" +
				"       ?commit prov:qualifiedAssociation [                                            \n" +
				"          prov:agent ?agent                                                           \n" +
				"       ]                                                                              \n" +
				"    }                                                                                 \n" +
				"    optional {                                                                        \n" +
				"       ?commit dcterms:subject ?subject                                               \n" +
				"    }                                                                                 \n" +
				" }                                                                                    \n" +
				" order by desc(?endTime)                                                              \n" +
				" limit " + (limit +1)
				// @formatter:on
				;
			
			TupleQuery query = conn.prepareTupleQuery(queryString);
			query.setIncludeInferred(false);
			List<CommitInfo> commitInfos = QueryResults.stream(query.evaluate()).map(bindingSet -> {
				CommitInfo commitInfo = new CommitInfo();

				commitInfo.setCommit((IRI) bindingSet.getValue("commit"));

				AnnotatedValue<IRI> operation = new AnnotatedValue<IRI>(
						(IRI) bindingSet.getValue("operation"));

				if (bindingSet.hasBinding("operation")) {
					commitInfo.setOperation(operation);
				}
				if (bindingSet.hasBinding("agent")) {
					AnnotatedValue<IRI> user = new AnnotatedValue<IRI>((IRI) bindingSet.getValue("agent"));
					STUser userDetails = UsersManager.getUserByIRI(user.getValue());
					if (userDetails != null) {
						String show = new StringBuilder().append(userDetails.getGivenName()).append(" ")
								.append(userDetails.getFamilyName()).append(" <")
								.append(userDetails.getEmail()).append(">").toString();
						user.setAttribute("show", show);
					}
					commitInfo.setUser(user);
				}

				if (bindingSet.hasBinding("subject")) {
					AnnotatedValue<Resource> subject = new AnnotatedValue<Resource>(
							(Resource) bindingSet.getValue("subject"));
					commitInfo.setSubject(subject);
				}
				return commitInfo;
			}).collect(Collectors.toList());

			return Page.build(commitInfos, limit);
		}
	}

	@STServiceOperation(method=RequestMethod.POST)
	@Write
	public void accept(IRI validatableCommit) {
		getManagedConnection().add(CHANGETRACKER.VALIDATION, CHANGETRACKER.ACCEPT, validatableCommit, CHANGETRACKER.VALIDATION);
	}
	
	@STServiceOperation(method=RequestMethod.POST)
	@Write
	public void reject(IRI validatableCommit) {
		getManagedConnection().add(CHANGETRACKER.VALIDATION, CHANGETRACKER.REJECT, validatableCommit, CHANGETRACKER.VALIDATION);
	}
}
