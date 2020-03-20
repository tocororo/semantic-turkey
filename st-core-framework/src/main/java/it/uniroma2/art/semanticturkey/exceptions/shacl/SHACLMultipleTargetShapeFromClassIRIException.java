package it.uniroma2.art.semanticturkey.exceptions.shacl;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import java.util.ArrayList;
import java.util.List;

public class SHACLMultipleTargetShapeFromClassIRIException extends SHACLGenericException {
    private List<IRI> targetShapeList;
    private IRI classIRI;

    public SHACLMultipleTargetShapeFromClassIRIException(IRI classIRI, List<IRI> targetShapeList) {
        this.classIRI = classIRI;
        this.targetShapeList = targetShapeList;
    }


    @Override
    public String getMessage() {
        String tsListAsString = "";
        for(IRI targetShape : targetShapeList){
            tsListAsString += " "+NTriplesUtil.toNTriplesString(targetShape)+",";
        }
        tsListAsString = tsListAsString.substring(1, tsListAsString.length()-1);

        return "From the classIRI: "+NTriplesUtil.toNTriplesString(classIRI)+" these TargetShape were obtained: "+
                tsListAsString;
    }

    public List<IRI> getTargetShapeList() {
        return targetShapeList;
    }

    public IRI getClassIRI() {
        return classIRI;
    }
}
