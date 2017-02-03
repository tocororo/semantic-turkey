package it.uniroma2.art.semanticturkey.sparql;

public class ProjectionElementBuilder {
	public static ProjectionElement groupConcat(String sourceVariable, String targetVariable) {
		return new ProjectionElement(ProjectionElement.Types.GROUP_CONCAT, sourceVariable, targetVariable);
	}
	
	public static ProjectionElement count(String sourceVariable, String targetVariable) {
		return new ProjectionElement(ProjectionElement.Types.COUNT, sourceVariable, targetVariable);
	}

	public static ProjectionElement variable(String variable) {
		return new ProjectionElement(ProjectionElement.Types.COPY, variable, variable);
	}

}
