package it.uniroma2.art.semanticturkey.services.core;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.history.CommitDelta;
import it.uniroma2.art.semanticturkey.services.core.history.CommitInfo;
import it.uniroma2.art.semanticturkey.services.core.history.Page;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersManager;

/**
 * This class provides services for interacting with the history of a project.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class History extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(History.class);

	@STServiceOperation
	public Page<CommitInfo> getCommits(@Optional IRI parentCommit,
			@Optional(defaultValue = "100") int limit) {

		Repository supportRepository = getProject().getRepositoryManager().getRepository("support");

		try (RepositoryConnection conn = supportRepository.getConnection()) {
			String queryString =
				// @formatter:off
				" prefix cl: <http://semanticturkey.uniroma2.it/ns/changelog#>                         \n" +
				" prefix prov: <http://www.w3.org/ns/prov#>                                            \n" +
				" prefix dcterms: <http://purl.org/dc/terms/>                                          \n" +
	            "                                                                                      \n" +
				" select * {                                                                           \n" +
				"    {select ?commit (COUNT(?successorCommit) as ?count) where {                       \n" +
				( parentCommit != null ?
						"        ?latest cl:parentCommit " + RenderUtils.toSPARQL(parentCommit) + " .\n"
						:
						"        cl:MASTER cl:tip ?latest .\n" ) +
				"        ?latest cl:parentCommit* ?successorCommit .                                   \n" +
				"        ?successorCommit cl:parentCommit* ?commit                                     \n" +
				"     }                                                                                \n" +
				"     group by ?commit                                                                 \n" +
				"     order by asc(?count)                                                             \n" +
				"     limit " + (limit + 1) + "                                                        \n" +
				"    }                                                                                 \n" +
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
				" }                                                                                    \n"
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

	@STServiceOperation
	@Read
	public CommitDelta getCommitDelta(IRI commit) {
		Repository supportRepository = getProject().getRepositoryManager().getRepository("support");

		try (RepositoryConnection conn = supportRepository.getConnection()) {
			TupleQuery query = conn.prepareTupleQuery(
				// @formatter:off
				" prefix cl: <http://semanticturkey.uniroma2.it/ns/changelog#>              \n" +
				" prefix prov: <http://www.w3.org/ns/prov#>                                 \n" +
                "                                                                           \n" +
				" select ?delta ?s ?p ?o ?c {                                               \n" +
				"     cl:MASTER cl:tip ?commit .                                            \n" +
				"     ?commit prov:generated ?m .                                           \n" +
				"     ?m ?deltaProp ?q .                                                    \n" +
				"     ?q cl:subject ?s .                                                    \n" +
				"     ?q cl:predicate ?p .                                                  \n" +
				"     ?q cl:object ?o .                                                     \n" +
				"     ?q cl:context ?c2 .                                                   \n" +
				"     BIND(IF(sameTerm(?c2, sesame:nil), ?c3, ?c2) as ?c)                   \n" +
				" }                                                                         \n" +
				" ORDER BY ?s ?p ?o ?c                                                      \n"
				// @formatter:on
			);

			query.setIncludeInferred(false);

			ValueFactory vf = SimpleValueFactory.getInstance();

			query.setBinding("deltaProp", CHANGELOG.ADDED_STATEMENT);
			List<Statement> addedStatements = QueryResults.stream(query.evaluate()).map(bindingSet -> {
				return vf.createStatement((Resource) bindingSet.getValue("s"), (IRI) bindingSet.getValue("p"),
						bindingSet.getValue("o"), (Resource) bindingSet.getValue("c"));
			}).collect(Collectors.toList());

			query.setBinding("deltaProp", CHANGELOG.REMOVED_STATEMENT);
			List<Statement> removedStatements = QueryResults.stream(query.evaluate()).map(bindingSet -> {
				return vf.createStatement((Resource) bindingSet.getValue("s"), (IRI) bindingSet.getValue("p"),
						bindingSet.getValue("o"), (Resource) bindingSet.getValue("c"));
			}).collect(Collectors.toList());

			CommitDelta commitDelta = new CommitDelta();
			commitDelta.setAdditions(new TreeModel(addedStatements));
			commitDelta.setRemovals(new TreeModel(removedStatements));
			return commitDelta;
		}
	}
}
