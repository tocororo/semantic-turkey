package it.uniroma2.art.semanticturkey.sparql;

import org.eclipse.rdf4j.queryrender.RenderUtils;

import com.google.common.base.Function;
import com.google.common.collect.BiMap;

/**
 * An element of the projection associated with a {@link GraphPattern}.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public class ProjectionElement {

	public static enum Types {
		GROUP_CONCAT, COPY, COUNT, MIN
	}

	private Types type;
	private String sourceVariable;
	private String targetVariable;
	private String separator;

	public ProjectionElement(Types type, String sourceVariable, String targetVariable) {
		this(type, sourceVariable, targetVariable, ",");
	}
	
	public ProjectionElement(Types type, String sourceVariable, String targetVariable, String separator) {
		this.type = type;
		this.sourceVariable = sourceVariable;
		this.targetVariable = targetVariable;
		this.separator = separator;
	}


	public String getSPARQLFragment() {
		switch (type) {
		case GROUP_CONCAT:
			return "(GROUP_CONCAT(DISTINCT ?" + sourceVariable + "; separator=\"" + RenderUtils.escape(separator) +"\") AS ?" + targetVariable
					+ ")";
		case MIN:
			return "(MIN(?" + sourceVariable + ") AS ?" + targetVariable + ")";
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

	public ProjectionElement renamed(Function<String, String> renamingFunction,
			BiMap<String, String> projected2baseVariableMapping) {
		String newSourceVariable = projected2baseVariableMapping.get(sourceVariable);

		if (newSourceVariable == null) {
			newSourceVariable = renamingFunction.apply(sourceVariable);
			projected2baseVariableMapping.put(sourceVariable, newSourceVariable);
		}

		String newTargetVariable = projected2baseVariableMapping.get(targetVariable);

		if (newTargetVariable == null) {
			newTargetVariable = renamingFunction.apply(targetVariable);
			projected2baseVariableMapping.put(targetVariable, newTargetVariable);
		}

		return new ProjectionElement(type, newSourceVariable, newTargetVariable);
	}
}
