package it.uniroma2.art.semanticturkey.sparql;

/**
 * A builder for the construction of {@link ProjectionElement} objects.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class ProjectionElementBuilder {
	public static ProjectionElement groupConcat(String sourceVariable, String targetVariable) {
		return new ProjectionElement(ProjectionElement.Types.GROUP_CONCAT, sourceVariable, targetVariable);
	}

	public static ProjectionElement min(String sourceVariable, String targetVariable) {
		return new ProjectionElement(ProjectionElement.Types.MIN, sourceVariable, targetVariable);
	}
	
	public static ProjectionElement count(String sourceVariable, String targetVariable) {
		return new ProjectionElement(ProjectionElement.Types.COUNT, sourceVariable, targetVariable);
	}

	public static ProjectionElement variable(String variable) {
		return new ProjectionElement(ProjectionElement.Types.COPY, variable, variable);
	}

}
