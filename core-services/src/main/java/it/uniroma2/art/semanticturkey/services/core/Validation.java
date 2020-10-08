package it.uniroma2.art.semanticturkey.services.core;

import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.stream.Collectors;

import it.uniroma2.art.semanticturkey.exceptions.DeniedOperationException;
import it.uniroma2.art.semanticturkey.user.UserException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.util.Literals;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.history.OperationMetadata;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.OmitHistoryMetadata;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.SkipTermValidation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.History.SortingDirection;
import it.uniroma2.art.semanticturkey.services.core.history.CommitInfo;
import it.uniroma2.art.semanticturkey.services.core.history.SupportRepositoryUtils;
import it.uniroma2.art.semanticturkey.services.core.history.ValidationPaginationInfo;
import it.uniroma2.art.semanticturkey.services.tracker.STServiceTracker;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.vocabulary.STCHANGELOG;

/**
 * This class provides services related to operation validation.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class Validation extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(Validation.class);

	private static final String DEFAULT_PAGE_SIZE = "100";

	@Autowired
	private STServiceTracker stServiceTracker;

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(code)', 'V')")
	public ValidationPaginationInfo getStagedCommitSummary(@Optional(defaultValue = "") IRI[] operationFilter,
			@Optional(defaultValue = "") IRI[] performerFilter, @Optional String timeLowerBound,
			@Optional String timeUpperBound, @Optional(defaultValue = DEFAULT_PAGE_SIZE) long limit) {
		return getStagedCommitSummaryInternal(operationFilter, performerFilter, timeLowerBound, timeUpperBound, limit);
	}

	@STServiceOperation
	@Read
	public ValidationPaginationInfo getCurrentUserStagedCommitSummary(@Optional(defaultValue = "") IRI[] operationFilter,
			@Optional String timeLowerBound, @Optional String timeUpperBound,
			@Optional(defaultValue = DEFAULT_PAGE_SIZE) long limit) {
		IRI[] performerFilter = { UsersManager.getLoggedUser().getIRI() };
		return getStagedCommitSummaryInternal(operationFilter, performerFilter, timeLowerBound, timeUpperBound, limit);
	}

	private ValidationPaginationInfo getStagedCommitSummaryInternal(IRI[] operationFilter, IRI[] performerFilter,
			String timeLowerBound, String timeUpperBound, long limit) {
		IRI validationGraph = SupportRepositoryUtils.obtainValidationGraph(getManagedConnection());

		String timeBoundsSPARQLFilter = SupportRepositoryUtils.computeTimeBoundsSPARQLFilter(timeLowerBound,
				timeUpperBound, "?startTimeT", "?endTimeT");
		String operationSPARQLFilter = SupportRepositoryUtils.computeInCollectionSPARQLFilter(operationFilter,
				"operationT");
		String performerSPARQLFilter = SupportRepositoryUtils.computeInCollectionSPARQLFilter(performerFilter,
				"performerT");

		Repository supportRepository = getProject().getRepositoryManager().getRepository("support");
		try (RepositoryConnection conn = supportRepository.getConnection()) {
			String queryString =
					// @formatter:off
					" PREFIX cl: <http://semanticturkey.uniroma2.it/ns/changelog#>					\n" +
					" PREFIX prov: <http://www.w3.org/ns/prov#>										\n" +
					" PREFIX dcterms: <http://purl.org/dc/terms/>									\n" +
					" SELECT (MAX(?endTimeT) as ?tipTime) (COUNT(?commit) as ?commitCount)			\n" +
					//" FROM " + RenderUtils.toSPARQL(validationGraph ) + "\n" +
					" WHERE {																		\n" +
					" GRAPH "+  RenderUtils.toSPARQL(validationGraph ) + "\n {" +
					"     ?commit a cl:Commit .														\n" +
					"     ?commit prov:startedAtTime ?startTimeT .									\n" +
					"     ?commit prov:endedAtTime ?endTimeT .										\n" +
					timeBoundsSPARQLFilter +
					SupportRepositoryUtils.conditionalOptional(operationSPARQLFilter.isEmpty(),
					"     ?commit prov:used ?operationT .											\n"
					) +
					operationSPARQLFilter +
					SupportRepositoryUtils.conditionalOptional(performerSPARQLFilter.isEmpty(),
					"     ?commit prov:qualifiedAssociation [										\n" +
					"         prov:agent ?performerT ;												\n" +
					"         prov:hadRole <" + STCHANGELOG.PERFORMER + ">							\n" +
					"     ]																			\n"
					) +
					performerSPARQLFilter +
					" } \n }";
					// @formatter:on

			TupleQuery tupleQuery = conn.prepareTupleQuery(queryString);
			tupleQuery.setIncludeInferred(false);
			BindingSet bindingSet = QueryResults.singleResult(tupleQuery.evaluate());
			long commitCount = ((Literal) bindingSet.getValue("commitCount")).longValue();
			GregorianCalendar tipTime = commitCount != 0
					? ((Literal) bindingSet.getValue("tipTime")).calendarValue().toGregorianCalendar()
					: null;

			return new ValidationPaginationInfo(tipTime,
					(commitCount / limit) + (commitCount % limit == 0 ? 0 : 1));
		}
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(code)', 'V')")
	public Collection<CommitInfo> getCommits(@Optional(defaultValue = "") IRI[] operationFilter,
			@Optional(defaultValue = "") IRI[] performerFilter, @Optional String timeLowerBound,
			String timeUpperBound, @Optional(defaultValue = "Unordered") SortingDirection operationSorting,
			@Optional(defaultValue = "Descending") SortingDirection timeSorting,
			@Optional(defaultValue = "0") long page, @Optional(defaultValue = DEFAULT_PAGE_SIZE) long limit) {
		return getCommitsInternal(operationFilter, performerFilter, timeLowerBound, timeUpperBound,
				operationSorting, timeSorting, page, limit);
	}

	@STServiceOperation
	@Read
	public Collection<CommitInfo> getCurrentUserCommits(@Optional(defaultValue = "") IRI[] operationFilter,
			@Optional String timeLowerBound, String timeUpperBound,
			@Optional(defaultValue = "Unordered") SortingDirection operationSorting,
			@Optional(defaultValue = "Descending") SortingDirection timeSorting,
			@Optional(defaultValue = "0") long page, @Optional(defaultValue = DEFAULT_PAGE_SIZE) long limit) {
		IRI[] performerFilter = { UsersManager.getLoggedUser().getIRI() };
		return getCommitsInternal(operationFilter, performerFilter, timeLowerBound, timeUpperBound,
				operationSorting, timeSorting, page, limit);
	}

	private Collection<CommitInfo> getCommitsInternal(IRI[] operationFilter, IRI[] performerFilter,
				String timeLowerBound, String timeUpperBound, SortingDirection operationSorting,
				SortingDirection timeSorting, long page, long limit) {

		IRI validationGraph = SupportRepositoryUtils.obtainValidationGraph(getManagedConnection());

		String operationSPARQLFilter = SupportRepositoryUtils.computeInCollectionSPARQLFilter(operationFilter,
				"operationT");

		String performerSPARQLFilter = SupportRepositoryUtils.computeInCollectionSPARQLFilter(performerFilter,
				"performerT");

		String orderBySPARQLFragment = SupportRepositoryUtils.computeOrderBySPARQLFragment(operationSorting,
				timeSorting, false);

		String timeBoundsSPARQLFilter = SupportRepositoryUtils.computeTimeBoundsSPARQLFilter(timeLowerBound,
				timeUpperBound, "?startTimeT", "?endTimeT");

		Repository supportRepository = getProject().getRepositoryManager().getRepository("support");
		try (RepositoryConnection conn = supportRepository.getConnection()) {
			String queryString =
			// @formatter:off
				" PREFIX cl: <http://semanticturkey.uniroma2.it/ns/changelog#>                 \n" +
			    " PREFIX stcl: <http://semanticturkey.uniroma2.it/ns/st-changelog#>            \n" +
				" PREFIX prov: <http://www.w3.org/ns/prov#>                                    \n" +
				" PREFIX dcterms: <http://purl.org/dc/terms/>                                  \n" +
				" PREFIX blacklist: <http://semanticturkey.uniroma2.it/ns/blacklist#>          \n" +
				" SELECT ?commit                                                               \n" +
				"        (MAX(?startTimeT) as ?startTime)                                      \n" +
				"        (MAX(?endTimeT) as ?endTime)                                          \n" +
				"        (MAX(?operationT) as ?operation)                                      \n" +
				"        (GROUP_CONCAT(DISTINCT CONCAT(STR(?param), \"$\", REPLACE(REPLACE(STR(?paramValue), \"\\\\\\\\\", \"$0$0\"), \"\\\\$\", \"\\\\\\\\$0\")); separator=\"$\") as ?parameters)\n" + 
				"        (MAX(?performerT) as ?agent)                                          \n" +
				"        (MAX(?commentAllowedT) as ?commentAllowed)                            \n" +
				//" FROM " + RenderUtils.toSPARQL(validationGraph) + "\n" +
				" WHERE {                                                                      \n" +
				" GRAPH "+  RenderUtils.toSPARQL(validationGraph ) + "\n {" +
				"     ?commit a cl:Commit .                                                    \n" +
				"     ?commit prov:startedAtTime ?startTimeT .                                 \n" +
				"     ?commit prov:endedAtTime ?endTimeT .                                     \n" +
				timeBoundsSPARQLFilter +
				SupportRepositoryUtils.conditionalOptional(operationSPARQLFilter.isEmpty(),
				"     ?commit prov:used ?operationT .                                          \n"
				) +
			    "     OPTIONAL {                                                               \n" +
			    "         ?commit stcl:parameters ?params .                                    \n" +
			    "         ?params ?param ?paramValue .                                         \n" +
			    "         FILTER(STRSTARTS(STR(?param), STR(?operationT)))                     \n" +
			    "     }                                                                        \n" +
				operationSPARQLFilter +
				SupportRepositoryUtils.conditionalOptional(performerSPARQLFilter.isEmpty(),
				"     ?commit prov:qualifiedAssociation [                                  \n" +
				"         prov:agent ?performerT ;                                         \n" +
				"         prov:hadRole <" + STCHANGELOG.PERFORMER + ">\n" +
				"     ]                                                                    \n"
				) +
				performerSPARQLFilter +
				"     BIND(EXISTS{?commit blacklist:template [] } AS ?commentAllowedT)         \n" +
				" } \n}                                                                        \n" +
				" GROUP BY ?commit                                                             \n" +
				" HAVING(BOUND(?commit))                                                       \n" +
				orderBySPARQLFragment +
				" OFFSET " + (page * limit) + "                                                \n" +
				" LIMIT " + limit + "                                                          \n";
				// @formatter:on

			TupleQuery tupleQuery = conn.prepareTupleQuery(queryString);
			tupleQuery.setIncludeInferred(false);

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

				commitInfo.setCommentAllowed(
						Literals.getBooleanValue(bindingSet.getValue("commentAllowed"), false));

				return commitInfo;
			}).collect(Collectors.toList());
		}
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@OmitHistoryMetadata
	@PreAuthorize("@auth.isAuthorized('rdf(code)', 'V')")
	public void accept(@SkipTermValidation IRI validatableCommit) {
		OperationMetadata operationMetadata = new OperationMetadata();
		operationMetadata.setUserIRI(UsersManager.getLoggedUser().getIRI(), STCHANGELOG.VALIDATOR);
		RepositoryConnection conn = getManagedConnection();

		conn.add(operationMetadata.toRDF(), CHANGETRACKER.COMMIT_METADATA);
		conn.prepareBooleanQuery("ASK {}").evaluate(); // flush commit metadata

		conn.add(CHANGETRACKER.VALIDATION, CHANGETRACKER.ACCEPT,
				conn.getValueFactory().createLiteral(validatableCommit.stringValue()),
				CHANGETRACKER.VALIDATION);
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@OmitHistoryMetadata
	@PreAuthorize("@auth.isAuthorized('rdf(code)', 'V')")
	public void reject(@SkipTermValidation IRI validatableCommit, @Optional String comment) {
		RepositoryConnection conn = getManagedConnection();
		rejectInternal(conn, validatableCommit, comment);
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@OmitHistoryMetadata
	public void rejectCurrentUserCommit(@SkipTermValidation IRI validatableCommit, @Optional String comment) throws DeniedOperationException {
		STUser user = UsersManager.getLoggedUser();

		RepositoryConnection conn = getManagedConnection();
		IRI validationGraph = SupportRepositoryUtils.obtainValidationGraph(conn);

		Repository supportRepository = getProject().getRepositoryManager().getRepository("support");
		try (RepositoryConnection supportConn = supportRepository.getConnection()) {
			String query = "PREFIX prov: <http://www.w3.org/ns/prov#>		\n" +
					"ASK {													\n" +
					" 	GRAPH %graph% {										\n" +
					"		%commit% prov:qualifiedAssociation [			\n" +
					"			prov:agent %performer%;						\n" +
					"		]												\n" +
					"	}													\n" +
					"}";
			query = query.replace("%graph%", RenderUtils.toSPARQL(validationGraph));
			query = query.replace("%commit%", RenderUtils.toSPARQL(validatableCommit));
			query = query.replace("%performer%", RenderUtils.toSPARQL(user.getIRI()));
			BooleanQuery bq = supportConn.prepareBooleanQuery(query);
			boolean isAllowed = bq.evaluate();
			if (isAllowed) {
				rejectInternal(conn, validatableCommit, comment);
			} else {
				throw new DeniedOperationException("You cannot reject commit about operation performed by other users");
			}
		}
	}

	private void rejectInternal(RepositoryConnection conn, IRI validatableCommit, String comment) {
		conn.add(CHANGETRACKER.VALIDATION, CHANGETRACKER.REJECT,
				conn.getValueFactory().createLiteral(validatableCommit.stringValue()),
				CHANGETRACKER.VALIDATION);
		if (comment != null) {
			conn.add(CHANGETRACKER.VALIDATION, RDFS.COMMENT, conn.getValueFactory().createLiteral(comment),
					CHANGETRACKER.VALIDATION);
		}
	}

}
