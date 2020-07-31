package it.uniroma2.art.semanticturkey.services.core;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import it.uniroma2.art.semanticturkey.user.UserException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGELOG;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.SkipTermValidation;
import it.uniroma2.art.semanticturkey.services.core.history.CommitDelta;
import it.uniroma2.art.semanticturkey.services.core.history.CommitInfo;
import it.uniroma2.art.semanticturkey.services.core.history.HistoryPaginationInfo;
import it.uniroma2.art.semanticturkey.services.core.history.SupportRepositoryUtils;
import it.uniroma2.art.semanticturkey.services.tracker.STServiceTracker;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.vocabulary.STCHANGELOG;

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
	}

	@Autowired
	private STServiceTracker stServiceTracker;

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf', 'R')")
	public HistoryPaginationInfo getCommitSummary(@Optional(defaultValue = "") IRI[] operationFilter,
			@Optional(defaultValue = "") IRI[] performerFilter,
			@Optional(defaultValue = "") IRI[] validatorFilter, @Optional String timeLowerBound,
			@Optional String timeUpperBound, @Optional(defaultValue = DEFAULT_PAGE_SIZE) long limit) {
		IRI historyGraph = SupportRepositoryUtils.obtainHistoryGraph(getManagedConnection());

		String timeBoundsSPARQLFilter = SupportRepositoryUtils.computeTimeBoundsSPARQLFilter(timeLowerBound,
				timeUpperBound);
		String operationSPARQLFilter = SupportRepositoryUtils.computeInCollectionSPARQLFilter(operationFilter,
				"operationT");
		String performerSPARQLFilter = SupportRepositoryUtils.computeInCollectionSPARQLFilter(performerFilter,
				"performerT");
		String validatorSPARQLFilter = SupportRepositoryUtils.computeInCollectionSPARQLFilter(validatorFilter,
				"validatorT");

		Repository supportRepository = getProject().getRepositoryManager().getRepository("support");
		try (RepositoryConnection conn = supportRepository.getConnection()) {
			String queryString =
			// @formatter:off
					" PREFIX cl: <http://semanticturkey.uniroma2.it/ns/changelog#>                 \n" +
					" PREFIX prov: <http://www.w3.org/ns/prov#>                                    \n" +
					" PREFIX dcterms: <http://purl.org/dc/terms/>                                  \n" +
					" SELECT (MAX(?revisionNumberT) as ?tipRevisionNumber) (COUNT(?commit) as ?commitCount) \n" +
					//" FROM " + RenderUtils.toSPARQL(historyGraph ) + "\n" +
					" WHERE {                                                                    \n" +
					" GRAPH "+  RenderUtils.toSPARQL(historyGraph ) + "{ \n" +
					"     ?commit a cl:Commit .                                                    \n" +
					"     ?commit cl:revisionNumber ?revisionNumberT .                             \n" +
					"     ?commit prov:startedAtTime ?startTimeT .                                 \n" +
					"     ?commit prov:endedAtTime ?endTimeT .                                     \n" +
					timeBoundsSPARQLFilter;
					/*SupportRepositoryUtils.conditionalOptional(operationSPARQLFilter.isEmpty(),
					"     ?commit prov:used ?operationT .                                          \n"
					) +*/
			if(!operationSPARQLFilter.isEmpty()) {
					queryString+="     ?commit prov:used ?operationT .                         \n" +
								operationSPARQLFilter;
					
					}
			if(!performerSPARQLFilter.isEmpty()) {
				queryString+= "     ?commit prov:qualifiedAssociation [                        \n" +
					"         prov:agent ?performerT ;                                         \n" +
					"         prov:hadRole <" + STCHANGELOG.PERFORMER + ">\n" +
					"     ]                                                                    \n" +
					performerSPARQLFilter;
			}
			if(!validatorSPARQLFilter.isEmpty()) {
				queryString+="     ?commit prov:qualifiedAssociation [                         \n" +
					"         prov:agent ?validatorT ;                                         \n" +
					"         prov:hadRole <" + STCHANGELOG.VALIDATOR + ">\n" +
					"     ]                                                                    \n" +
					validatorSPARQLFilter ;
					}
			queryString+=" }\n}                                                                 \n" 
					// @formatter:on
			;
			logger.debug("query: " + queryString);
			TupleQuery tupleQuery = conn.prepareTupleQuery(queryString);
			tupleQuery.setIncludeInferred(false);
			BindingSet bindingSet = QueryResults.singleResult(tupleQuery.evaluate());
			long commitCount = ((Literal) bindingSet.getValue("commitCount")).longValue();
			long tipRevisionNumber = commitCount != 0
					? ((Literal) bindingSet.getValue("tipRevisionNumber")).longValue()
					: -1;

			return new HistoryPaginationInfo(tipRevisionNumber,
					(commitCount / limit) + (commitCount % limit == 0 ? 0 : 1));
		}
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf', 'R')")
	public Collection<CommitInfo> getCommits(long tipRevisionNumber,
			@Optional(defaultValue = "") IRI[] operationFilter,
			@Optional(defaultValue = "") IRI[] performerFilter,
			@Optional(defaultValue = "") IRI[] validatorFilter, @Optional String timeLowerBound,
			@Optional String timeUpperBound,
			@Optional(defaultValue = "Unordered") SortingDirection operationSorting,
			@Optional(defaultValue = "Descending") SortingDirection timeSorting,
			@Optional(defaultValue = "0") long page, @Optional(defaultValue = DEFAULT_PAGE_SIZE) long limit) {

		IRI historyGraph = SupportRepositoryUtils.obtainHistoryGraph(getManagedConnection());


		String orderBySPARQLFragment = SupportRepositoryUtils.computeOrderBySPARQLFragment(operationSorting,
				timeSorting, true);

		String timeBoundsSPARQLFilter = SupportRepositoryUtils.computeTimeBoundsSPARQLFilter(timeLowerBound,
				timeUpperBound);

		String operationSPARQLFilter = SupportRepositoryUtils.computeInCollectionSPARQLFilter(operationFilter,
				"operationT");
		String performerSPARQLFilter = SupportRepositoryUtils.computeInCollectionSPARQLFilter(performerFilter,
				"performerT");
		String validatorSPARQLFilter = SupportRepositoryUtils.computeInCollectionSPARQLFilter(validatorFilter,
				"validatorT");


		String innerPatternOperation = 	" ?commit prov:used ?operationT . 				\n" +
										operationSPARQLFilter;
		String innerPatternPerformer = 	" ?commit prov:qualifiedAssociation [			\n" +
										" prov:agent ?performerT ;						\n" +
										" prov:hadRole <" + STCHANGELOG.PERFORMER + ">	\n" +
										" ]                                           	\n" +
										performerSPARQLFilter;
		String innerPatternValidator = 	" ?commit prov:qualifiedAssociation [			\n" +
										" prov:agent ?validatorT ;						\n" +
										"  prov:hadRole <" + STCHANGELOG.VALIDATOR + ">	\n" +
										" ]												\n" +
										validatorSPARQLFilter;


		Repository supportRepository = getProject().getRepositoryManager().getRepository("support");
		try (RepositoryConnection conn = supportRepository.getConnection()) {
			String queryString =
			// @formatter:off
				" PREFIX cl: <http://semanticturkey.uniroma2.it/ns/changelog#>                 \n" +
			    " PREFIX stcl: <http://semanticturkey.uniroma2.it/ns/st-changelog#>            \n" +
				" PREFIX prov: <http://www.w3.org/ns/prov#>                                    \n" +
				" PREFIX dcterms: <http://purl.org/dc/terms/>                                  \n" +
				" SELECT ?commit                                                               \n" +
				"        (MAX(?revisionNumberT) as ?revisionNumber)                            \n" +
				"        (MAX(?startTimeT) as ?startTime)                                      \n" +
				"        (MAX(?endTimeT) as ?endTime)                                          \n" +
				"        (MAX(?operationT) as ?operation)                                      \n" +
				"        (GROUP_CONCAT(DISTINCT CONCAT(STR(?param), \"$\", REPLACE(REPLACE(STR(?paramValue), \"\\\\\\\\\", \"$0$0\"), \"\\\\$\", \"\\\\\\\\$0\")); separator=\"$\") as ?parameters)\n" + 
				"        (MAX(?performerT) as ?agent)                                          \n" +
				"        (MAX(?validatorT) as ?validator)                                      \n" +
				//" FROM " + RenderUtils.toSPARQL(historyGraph) + "\n" +
				" WHERE {                                                                      \n" +
				// INNER QUERY STARTED
				"{																				\n" +
				"SELECT ?commit ?revisionNumberT ?startTimeT ?endTimeT ?operationT ?performerT ?validatorT \n" +
				"WHERE { 																		\n" +
				" GRAPH "+  RenderUtils.toSPARQL(historyGraph ) + "\n {" +
				"     ?commit a cl:Commit .                                                    \n" +
				"     ?commit cl:revisionNumber ?revisionNumberT .                             \n" +
				"     FILTER(str(?revisionNumberT) <= '"+tipRevisionNumber+"')                   \n" +
				"     ?commit prov:startedAtTime ?startTimeT .                                 \n" +
				"     ?commit prov:endedAtTime ?endTimeT .                                     \n" +
				//add all the FILTER that are specified by the input parameters
				SupportRepositoryUtils.addInnerPatter(!operationSPARQLFilter.isEmpty(), innerPatternOperation) +
				SupportRepositoryUtils.addInnerPatter(!performerSPARQLFilter.isEmpty(), innerPatternPerformer) +
				SupportRepositoryUtils.addInnerPatter(!validatorSPARQLFilter.isEmpty(), innerPatternValidator) +
				"}																				\n" + // closing GRAPH
				"}																				\n" +
				orderBySPARQLFragment +
				" OFFSET " + (page * limit) + "                                                \n" +
				" LIMIT " + limit + "                                                          \n" +
				"} 																				\n" +
				//INNER QUERY ENDED
				timeBoundsSPARQLFilter +
				SupportRepositoryUtils.addInnerPatter(operationSPARQLFilter.isEmpty(),
						SupportRepositoryUtils.conditionalOptional(operationSPARQLFilter.isEmpty(),
							innerPatternOperation)) +
			    "     OPTIONAL {                                                               \n" +
			    "         ?commit stcl:parameters ?params .                                    \n" +
			    "         ?params ?param ?paramValue .                                         \n" +
			    "         FILTER(STRSTARTS(STR(?param), STR(?operationT)))                     \n" +
			    "     }                                                                        \n" +
				operationSPARQLFilter +
				SupportRepositoryUtils.addInnerPatter(performerSPARQLFilter.isEmpty(),
						SupportRepositoryUtils.conditionalOptional(performerSPARQLFilter.isEmpty(),
							innerPatternPerformer)) +
				SupportRepositoryUtils.addInnerPatter(validatorSPARQLFilter.isEmpty(),
					SupportRepositoryUtils.conditionalOptional(validatorSPARQLFilter.isEmpty(),
						innerPatternValidator)) +
				"}                                                                        \n" +
				" GROUP BY ?commit                                                             \n" +
				" HAVING(BOUND(?commit))                                                       \n";
				// @formatter:on

			logger.debug("query: " + queryString);
			TupleQuery tupleQuery = conn.prepareTupleQuery(queryString);
			tupleQuery.setIncludeInferred(false);
			//tupleQuery.setBinding("tipRevisionNumber",
			//		conn.getValueFactory().createLiteral(BigInteger.valueOf(tipRevisionNumber)));

			return QueryResults.stream(tupleQuery.evaluate()).map(bindingSet -> {
				CommitInfo commitInfo = new CommitInfo();

				commitInfo.setCommit((IRI) bindingSet.getValue("commit"));

				AnnotatedValue<IRI> operation = new AnnotatedValue<IRI>(
						(IRI) bindingSet.getValue("operation"));

				if (bindingSet.hasBinding("operation")) {
					commitInfo.setOperation(operation);
					SupportRepositoryUtils.computeOperationDisplay(stServiceTracker, operation);
				}

				if (bindingSet.hasBinding("parameters")) {
					commitInfo.setOperationParameters(SupportRepositoryUtils
							.deserializeOperationParameters(bindingSet.getValue("parameters").stringValue()));
				}

				if (bindingSet.hasBinding("agent")) {
					AnnotatedValue<IRI> user = new AnnotatedValue<IRI>((IRI) bindingSet.getValue("agent"));
					try {
						STUser userDetails = UsersManager.getUser(user.getValue());
						if (userDetails != null) {
							String show = new StringBuilder().append(userDetails.getGivenName()).append(" ")
									.append(userDetails.getFamilyName()).append(" <")
									.append(userDetails.getEmail()).append(">").toString();
							user.setAttribute("show", show);
						}
					} catch (UserException e) {}
					commitInfo.setUser(user);
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

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf', 'R')")
	public CommitDelta getCommitDelta(@SkipTermValidation IRI commit, @Optional(defaultValue = "100") int limit) {
		Repository supportRepository = getProject().getRepositoryManager().getRepository("support");

		try (RepositoryConnection conn = supportRepository.getConnection()) {

			Function<IRI, TupleQuery> prepareQuery = deltaProp -> {
				TupleQuery query = conn.prepareTupleQuery(
				// @formatter:off
						" prefix cl: <http://semanticturkey.uniroma2.it/ns/changelog#>              \n" +
						" prefix prov: <http://www.w3.org/ns/prov#>                                 \n" +
		                "                                                                           \n" +
						" select ?s ?p ?o ?c {                                                      \n" +
		                "     select * {                                                            \n" +
						"     	" + RenderUtils.toSPARQL(commit) + " prov:generated ?m .            \n" +
						"     	?m " + RenderUtils.toSPARQL(deltaProp) + " ?q .                     \n" +
						"     	?q cl:subject ?s .                                                  \n" +
						"    	?q cl:predicate ?p .                                                \n" +
						"     	?q cl:object ?o .                                                   \n" +
						"     	?q cl:context ?c2 .                                                 \n" +
						"    	BIND(IF(sameTerm(?c2, sesame:nil), ?c3, ?c2) as ?c)                 \n" +
						"     }                                                                     \n" +
						(limit != 0 ? "     LIMIT " + (limit + 1) +"\n": "") +
						" }                                                                         \n" +
						" ORDER BY ?s ?p ?o ?c                                                      \n"
						// @formatter:on
				);
				query.setIncludeInferred(false);
				return query;
			};

			ValueFactory vf = SimpleValueFactory.getInstance();

			TupleQuery additionsQuery = prepareQuery.apply(CHANGELOG.ADDED_STATEMENT);
			List<Statement> addedStatements = QueryResults.stream(additionsQuery.evaluate())
					.map(bindingSet -> {
						return vf.createStatement((Resource) bindingSet.getValue("s"),
								(IRI) bindingSet.getValue("p"), bindingSet.getValue("o"),
								(Resource) bindingSet.getValue("c"));
					}).collect(Collectors.toList());

			TupleQuery removalsQuery = prepareQuery.apply(CHANGELOG.REMOVED_STATEMENT);
			List<Statement> removedStatements = QueryResults.stream(removalsQuery.evaluate())
					.map(bindingSet -> {
						return vf.createStatement((Resource) bindingSet.getValue("s"),
								(IRI) bindingSet.getValue("p"), bindingSet.getValue("o"),
								(Resource) bindingSet.getValue("c"));
					}).collect(Collectors.toList());

			int additionsTruncated;
			int removalsTruncated;

			if (limit > 0) {
				if (addedStatements.size() > limit) {
					additionsTruncated = limit;
					addedStatements = addedStatements.subList(0, limit);
				} else {
					additionsTruncated = 0;
				}

				if (removedStatements.size() > limit) {
					removalsTruncated = limit;
					removedStatements = removedStatements.subList(0, limit);
				} else {
					removalsTruncated = 0;
				}
			} else {
				additionsTruncated = 0;
				removalsTruncated = 0;
			}

			CommitDelta commitDelta = new CommitDelta();
			commitDelta.setAdditions(new TreeModel(addedStatements));
			commitDelta.setRemovals(new TreeModel(removedStatements));
			commitDelta.setAdditionsTruncated(additionsTruncated);
			commitDelta.setRemovalsTruncated(removalsTruncated);
			return commitDelta;
		}
	}
}
