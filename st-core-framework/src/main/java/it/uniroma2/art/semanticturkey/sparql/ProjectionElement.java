package it.uniroma2.art.semanticturkey.sparql;

import com.google.common.base.Function;
import com.google.common.collect.BiMap;

public class ProjectionElement {

	public static enum Types {
		GROUP_CONCAT, COPY, COUNT
	}

	private Types type;
	private String sourceVariable;
	private String targetVariable;

	public ProjectionElement(Types type, String sourceVariable, String targetVariable) {
		this.type = type;
		this.sourceVariable = sourceVariable;
		this.targetVariable = targetVariable;
	}

	public String getSPARQLFragment() {
		switch (type) {
		case GROUP_CONCAT:
			return "(GROUP_CONCAT(DISTINCT ?" + sourceVariable + "; separator=\",\") AS ?" + targetVariable + ")";
		case COUNT:
			return "(COUNT(DISTINCT ?" + sourceVariable + ") AS ?" + targetVariable + ")";
		case COPY:
			return sourceVariable.equals(targetVariable) ? "?" + sourceVariable
					: "(?" + sourceVariable + " AS ?" + targetVariable + ")";
		default:
			throw new IllegalStateException("Unsupported type: " + type);
		}
	}

	public boolean isAggregate() {
		return type != Types.COPY;
	}

	public String getTargetVariable() {
		return targetVariable;
	}

	public ProjectionElement renamed(Function<String, String> renamingFunction, BiMap<String, String> projected2baseVariableMapping) {
		String newSourceVariable = projected2baseVariableMapping.inverse().get(sourceVariable);
		
		if (newSourceVariable == null) {
			newSourceVariable = renamingFunction.apply(sourceVariable);
			projected2baseVariableMapping.put(newSourceVariable, sourceVariable);
		}
		
		String newTargetVariable = projected2baseVariableMapping.inverse().get(targetVariable);
		
		if (newTargetVariable == null) {
			newTargetVariable = renamingFunction.apply(targetVariable);
			projected2baseVariableMapping.put(newTargetVariable, targetVariable);
		}

		return new ProjectionElement(type, newSourceVariable, newTargetVariable);
	}
}
