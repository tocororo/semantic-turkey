package it.uniroma2.art.semanticturkey.exceptions.shacl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

import java.util.List;
import java.util.stream.Collectors;

public class SHACLMultipleTargetShapeFromClassIRIException extends SHACLGenericException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7254378249836109563L;
	
	private List<IRI> targetShapeList;
	private IRI classIRI;

	public SHACLMultipleTargetShapeFromClassIRIException(IRI classIRI, List<IRI> targetShapeList) {
		super(SHACLMultipleTargetShapeFromClassIRIException.class.getName() + ".message",
				new Object[] { NTriplesUtil.toNTriplesString(classIRI), targetShapeList.stream()
						.map(NTriplesUtil::toNTriplesString).collect(Collectors.joining(",")) });
		this.classIRI = classIRI;
		this.targetShapeList = targetShapeList;
	}

	public List<IRI> getTargetShapeList() {
		return targetShapeList;
	}

	public IRI getClassIRI() {
		return classIRI;
	}
}
