package it.uniroma2.art.semanticturkey.services.core;

import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
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

import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;
import it.uniroma2.art.semanticturkey.services.core.History.SortingDirection;
import it.uniroma2.art.semanticturkey.services.core.history.CommitInfo;
import it.uniroma2.art.semanticturkey.services.core.history.SupportRepositoryUtils;
import it.uniroma2.art.semanticturkey.services.core.history.ValidationPaginationInfo;
import it.uniroma2.art.semanticturkey.services.tracker.STServiceTracker;
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

	private static final String DEFAULT_PAGE_SIZE = "100";

	@Autowired
	private STServiceTracker stServiceTracker;

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf(validation)', 'V')")
	public ValidationPaginationInfo getStagedCommitSummary(@Optional(defaultValue = "") IRI[] operationFilter,
			@Optional String timeLowerBound, @Optional String timeUpperBound,
			@Optional(defaultValue = DEFAULT_PAGE_SIZE) long limit) {
		IRI validationGraph = SupportRepositoryUtils.obtainValidationGraph(getManagedConnection());

		String timeBoundsSPARQLFilter = SupportRepositoryUtils.computeTimeBoundsSPARQLFilter(timeLowerBound,
				timeUpperBound);
		String operationSPARQLFilter = SupportRepositoryUtils.computeOperationSPARQLFilter(operationFilter);

		Repository supportRepository = getProject().getRepositoryManager().getRepository("support");
		try (RepositoryConnection conn = supportRepository.getConnection()) {
			String queryString =
					// @formatter:off
					" PREFIX cl: <http://semanticturkey.uniroma2.it/ns/changelog#>                 \n" +
					" PREFIX prov: <http://www.w3.org/ns/prov#>                                    \n" +
					" PREFIX dcterms: <http://purl.org/dc/terms/>                                  \n" +
					" SELECT (MAX(?endTime) as ?tipTime) (COUNT(?commit) as ?commitCount) \n" +
					" FROM " + RenderUtils.toSPARQL(validationGraph ) + "\n" +
					" {                                                                            \n" +
					"     ?commit a cl:Commit .                                                    \n" +
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
			long commitCount = ((Literal) bindingSet.getValue("commitCount")).longValue();
			GregorianCalendar tipTime = commitCount != 0
					? ((Literal) bindingSet.getValue("tipTime")).calendarValue().toGregorianCalendar() : null;

			return new ValidationPaginationInfo(tipTime,
					(commitCount / limit) + (commitCount % limit == 0 ? 0 : 1));
		}
	}

	@STServiceOperation
	@Read
	@PreAuthorize("@auth.isAuthorized('rdf', 'V')")
	public Collection<CommitInfo> getCommits(
			@Optional(defaultValue = "") IRI[] operationFilter, @Optional String timeLowerBound,
			String timeUpperBound,
			@Optional(defaultValue = "Unordered") SortingDirection operationSorting,
			@Optional(defaultValue = "Descending") SortingDirection timeSorting,
			@Optional(defaultValue = "0") long page, @Optional(defaultValue = DEFAULT_PAGE_SIZE) long limit) {

		IRI validationGraph = SupportRepositoryUtils.obtainValidationGraph(getManagedConnection());

		String operationSPARQLFilter = SupportRepositoryUtils.computeOperationSPARQLFilter(operationFilter);

		String orderBySPARQLFragment = SupportRepositoryUtils.computeOrderBySPARQLFragment(operationSorting,
				timeSorting, false);

		String timeBoundsSPARQLFilter = SupportRepositoryUtils.computeTimeBoundsSPARQLFilter(timeLowerBound,
				timeUpperBound);

		Repository supportRepository = getProject().getRepositoryManager().getRepository("support");
		try (RepositoryConnection conn = supportRepository.getConnection()) {
			String queryString =
				// @formatter:off
				" PREFIX cl: <http://semanticturkey.uniroma2.it/ns/changelog#>                 \n" +
				" PREFIX prov: <http://www.w3.org/ns/prov#>                                    \n" +
				" PREFIX dcterms: <http://purl.org/dc/terms/>                                  \n" +
				" SELECT *                                                                     \n" +
				" FROM " + RenderUtils.toSPARQL(validationGraph) + "\n" +
				" {                                                                            \n" +
				"     ?commit a cl:Commit .                                                    \n" +
				"     ?commit prov:startedAtTime ?startTime .                                  \n" +
				"     ?commit prov:endedAtTime ?endTime .                                      \n" +
				timeBoundsSPARQLFilter +
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

			return QueryResults.stream(tupleQuery.evaluate()).map(bindingSet -> {
				CommitInfo commitInfo = new CommitInfo();

				commitInfo.setCommit((IRI) bindingSet.getValue("commit"));

				AnnotatedValue<IRI> operation = new AnnotatedValue<IRI>(
						(IRI) bindingSet.getValue("operation"));

				if (bindingSet.hasBinding("operation")) {
					commitInfo.setOperation(operation);
					SupportRepositoryUtils.computeOperationDisplay(stServiceTracker, operation);
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

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf', 'V')")
	public void accept(IRI validatableCommit) {
		getManagedConnection().add(CHANGETRACKER.VALIDATION, CHANGETRACKER.ACCEPT, validatableCommit,
				CHANGETRACKER.VALIDATION);
	}

	@STServiceOperation(method = RequestMethod.POST)
	@Write
	@PreAuthorize("@auth.isAuthorized('rdf', 'V')")
	public void reject(IRI validatableCommit) {
		getManagedConnection().add(CHANGETRACKER.VALIDATION, CHANGETRACKER.REJECT, validatableCommit,
				CHANGETRACKER.VALIDATION);
	}
}
