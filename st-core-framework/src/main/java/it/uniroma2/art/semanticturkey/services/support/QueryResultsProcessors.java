package it.uniroma2.art.semanticturkey.services.support;

/**
 * A factory class for instatiating common {@link QueryResultsProcessor}s.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public abstract class QueryResultsProcessors {

	/**
	 * Returns a new instance of {@link AnnotatedResourcesQueryResultsProcessor}.
	 * 
	 * @return
	 */
	public static AnnotatedResourcesQueryResultsProcessor toAnnotatedResources() {
		return new AnnotatedResourcesQueryResultsProcessor();
	}
	
	/**
	 * Returns a new instance of {@link BindingSetsQueryResultsProcessor}.
	 * 
	 * @return
	 */
	public static BindingSetsQueryResultsProcessor toBindingSets() {
		return new BindingSetsQueryResultsProcessor();
	}

}
