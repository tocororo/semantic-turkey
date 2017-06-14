package it.uniroma2.art.semanticturkey.services.core;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.datatypes.XMLDatatypeUtil;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.history.CommitDelta;
import it.uniroma2.art.semanticturkey.services.core.history.CommitInfo;
import it.uniroma2.art.semanticturkey.services.core.history.Page;
import it.uniroma2.art.semanticturkey.services.core.history.PaginationInfo;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersManager;

/**
 * This class provides services for interacting with the history of a project.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class History extends STServiceAdapter {

	private static final String DEFAULT_PAGE_SIZE = "100";

	private static Logger logger = LoggerFactory.getLogger(History.class);

	public static enum SortingDirection {
		Ascending, Descending, Unordered
	};

	@STServiceOperation
	@Read
	public PaginationInfo getCommitSummary(@Optional(defaultValue = "") IRI[] operationFilter,
			@Optional String timeLowerBound, @Optional String timeUpperBound,
			@Optional(defaultValue = DEFAULT_PAGE_SIZE) long limit) {
		IRI historyGraph = obtainHistoryGraph(getManagedConnection());

		String timeBoundsSPARQLFilter = computeTimeBoundsSPARQLFilter(timeLowerBound, timeUpperBound);
		String operationSPARQLFilter = computeOperationSPARQLFilter(operationFilter);

		Repository supportRepository = getProject().getRepositoryManager().getRepository("support");
		try (RepositoryConnection conn = supportRepository.getConnection()) {
			String queryString =
					// @formatter:off
					" PREFIX cl: <http://semanticturkey.uniroma2.it/ns/changelog#>                 \n" +
					" PREFIX prov: <http://www.w3.org/ns/prov#>                                    \n" +
					" PREFIX dcterms: <http://purl.org/dc/terms/>                                  \n" +
					" SELECT (MAX(?revisionNumber) as ?tipRevisionNumber) (COUNT(?commit) as ?commitCount) \n" +
					" FROM " + RenderUtils.toSPARQL(historyGraph ) + "\n" +
					" {                                                                            \n" +
					"     ?commit a cl:Commit .                                                    \n" +
					"     ?commit cl:revisionNumber ?revisionNumber .                              \n" +
					"     ?commit prov:startedAtTime ?startTime .                                  \n" +
					"     ?commit prov:endedAtTime ?endTime .                                      \n" +
					timeBoundsSPARQLFilter +
					"     OPTIONAL {                                                               \n" +
					"         ?commit prov:used ?operation .                                       \n" +
					"     }                                                                        \n" +
					operationSPARQLFilter +
					" }                                                                            \n" 
					// @formatter:on
			;

			TupleQuery tupleQuery = conn.prepareTupleQuery(queryString);
			tupleQuery.setIncludeInferred(false);
			BindingSet bindingSet = QueryResults.singleResult(tupleQuery.evaluate());
			long tipRevisionNumber = ((Literal) bindingSet.getValue("tipRevisionNumber")).longValue();
			long commitCount = ((Literal) bindingSet.getValue("commitCount")).longValue();

			return new PaginationInfo(tipRevisionNumber,
					(commitCount / limit) + (commitCount % limit == 0 ? 0 : 1));
		}
	}

	@STServiceOperation
	@Read
	public Collection<CommitInfo> getCommits2(long tipRevisionNumber,
			@Optional(defaultValue = "") IRI[] operationFilter, @Optional String timeLowerBound,
			@Optional String timeUpperBound,
			@Optional(defaultValue = "Unordered") SortingDirection operationSorting,
			@Optional(defaultValue = "Descending") SortingDirection timeSorting,
			@Optional(defaultValue = "0") long page, @Optional(defaultValue = DEFAULT_PAGE_SIZE) long limit) {

		IRI historyGraph = obtainHistoryGraph(getManagedConnection());

		String operationSPARQLFilter = computeOperationSPARQLFilter(operationFilter);

		String orderBySPARQLFragment = computeOrderBySPARQLFragment(operationSorting, timeSorting);

		String timeBoundsSPARQLFilter = computeTimeBoundsSPARQLFilter(timeLowerBound, timeUpperBound);

		Repository supportRepository = getProject().getRepositoryManager().getRepository("support");
		try (RepositoryConnection conn = supportRepository.getConnection()) {
			String queryString =
				// @formatter:off
				" PREFIX cl: <http://semanticturkey.uniroma2.it/ns/changelog#>                 \n" +
				" PREFIX prov: <http://www.w3.org/ns/prov#>                                    \n" +
				" PREFIX dcterms: <http://purl.org/dc/terms/>                                  \n" +
				" SELECT *                                                                     \n" +
				" FROM " + RenderUtils.toSPARQL(historyGraph) + "\n" +
				" {                                                                            \n" +
				"     ?commit a cl:Commit .                                                    \n" +
				"     ?commit cl:revisionNumber ?revisionNumber .                              \n" +
				"     FILTER(?revisionNumber <= ?tipRevisionNumber)                            \n" +
				"     ?commit prov:startedAtTime ?startTime .                                  \n" +
				"     ?commit prov:endedAtTime ?endTime .                                      \n" +
				timeBoundsSPARQLFilter +
				"     OPTIONAL {                                                               \n" +
				"         ?commit dcterms:subject ?subject .                                   \n" +
				"     }                                                                        \n" +
				"     OPTIONAL {                                                               \n" +
				"         ?commit prov:used ?operation .                                       \n" +
				"     }                                                                        \n" +
				operationSPARQLFilter +
				"     OPTIONAL {                                                               \n" +
				"         ?commit prov:qualifiedAssociation [                                  \n" +
				"             prov:agent ?agent                                                \n" +
				"         ]                                                                    \n" +
				"     }                                                                        \n" +
				" }                                                                            \n" +
				orderBySPARQLFragment +
				" OFFSET " + (page * limit) + "                                                \n" +
				" LIMIT " + limit + "                                                          \n";
				// @formatter:on
			;

			TupleQuery tupleQuery = conn.prepareTupleQuery(queryString.toString());
			tupleQuery.setIncludeInferred(false);
			tupleQuery.setBinding("tipRevisionNumber",
					conn.getValueFactory().createLiteral(BigInteger.valueOf(tipRevisionNumber)));

			return QueryResults.stream(tupleQuery.evaluate()).map(bindingSet -> {
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

				if (bindingSet.hasBinding("startTime")) {
					commitInfo.setStartTime(
							Literals.getCalendarValue((Literal) bindingSet.getValue("startTime"), null)
									.toGregorianCalendar());
				}

				if (bindingSet.hasBinding("endTime")) {
					commitInfo.setEndTime(
							Literals.getCalendarValue((Literal) bindingSet.getValue("endTime"), null)
									.toGregorianCalendar());
				}

				return commitInfo;
			}).collect(Collectors.toList());
		}
	}

	protected static String computeTimeBoundsSPARQLFilter(String timeLowerBound, String timeUpperBound)
			throws IllegalArgumentException {
		String timeLowerBoundSPARQLFilter;
		if (timeLowerBound != null) {
			if (!XMLDatatypeUtil.isValidDateTime(timeLowerBound)) {
				throw new IllegalArgumentException(
						"Time lower bound is not a valid xsd:dateTime lexical form: " + timeLowerBound);
			}

			timeLowerBoundSPARQLFilter = "FILTER(?endTime >= " + RenderUtils.toSPARQL(
					SimpleValueFactory.getInstance().createLiteral(timeLowerBound, XMLSchema.DATETIME))
					+ ")\n";

		} else {
			timeLowerBoundSPARQLFilter = "";
		}

		String timeUpperBoundSPARQLFilter;
		if (timeUpperBound != null) {
			if (!XMLDatatypeUtil.isValidDateTime(timeUpperBound)) {
				throw new IllegalArgumentException(
						"Time lower bound is not a valid xsd:dateTime lexical form: " + timeUpperBound);
			}

			timeUpperBoundSPARQLFilter = "FILTER(?endTime <= " + RenderUtils.toSPARQL(
					SimpleValueFactory.getInstance().createLiteral(timeUpperBound, XMLSchema.DATETIME))
					+ ")\n";

		} else {
			timeUpperBoundSPARQLFilter = "";
		}

		return timeLowerBoundSPARQLFilter + timeUpperBoundSPARQLFilter;
	}

	protected String computeOrderBySPARQLFragment(SortingDirection operationSorting,
			SortingDirection timeSorting) {
		String orderBy = "";

		switch (operationSorting) {
		case Ascending:
			orderBy += " ASC(?operation)";
			break;
		case Descending:
			orderBy += " DESC(?operation)";
			break;
		default:
		}

		switch (timeSorting) {
		case Ascending:
			orderBy += " ASC(?revisionNumber)";
			break;
		case Descending:
			orderBy += " DESC(?revisionNumber)";
			break;
		default:
		}

		if (!orderBy.isEmpty()) {
			orderBy = "ORDER BY " + orderBy + "\n";
		}
		return orderBy;
	}

	protected static String computeOperationSPARQLFilter(IRI[] operationFilter) {
		String operationSPARQLFilter = operationFilter.length != 0
				? "FILTER(?operation IN " + Arrays.stream(operationFilter).map(RenderUtils::toSPARQL)
						.collect(Collectors.joining(", ", "(", ")")) + ")\n"
				: "";
		return operationSPARQLFilter;
	}

	protected static IRI obtainHistoryGraph(RepositoryConnection coreRepoConnection)
			throws IllegalStateException, QueryEvaluationException, RepositoryException {
		IRI historyGraph = Models
				.objectIRI(
						QueryResults.asModel(coreRepoConnection.getStatements(CHANGETRACKER.GRAPH_MANAGEMENT,
								CHANGETRACKER.HISTORY_GRAPH, null, CHANGETRACKER.GRAPH_MANAGEMENT)))
				.orElseThrow(() -> new IllegalStateException(
						"Could not obtain the history graph. Perhaps this project is without history"));
		return historyGraph;
	}

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
				"    optional {                                                                        \n" +
				"       ?commit prov:startedAtTime ?startTime                                          \n" +
				"    }                                                                                 \n" +
				"    optional {                                                                        \n" +
				"       ?commit prov:endedAtTime ?endTime                                              \n" +
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

				if (bindingSet.hasBinding("startTime")) {
					commitInfo.setStartTime(
							Literals.getCalendarValue((Literal) bindingSet.getValue("startTime"), null)
									.toGregorianCalendar());
				}

				if (bindingSet.hasBinding("endTime")) {
					commitInfo.setEndTime(
							Literals.getCalendarValue((Literal) bindingSet.getValue("endTime"), null)
									.toGregorianCalendar());
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

			query.setBinding("commit", commit);

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
