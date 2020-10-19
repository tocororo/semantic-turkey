package it.uniroma2.art.semanticturkey.alignment;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;

import java.util.ArrayList;
import java.util.List;

public class AlignmentUtils {

    /**
     * Converts the given relation to a property. The conversion depends on:
     * <ul>
     * <li>Relation: =, >, <, %, InstanceOf, HasInstance</li>
     * <li>Type of entity: property, class, instance, concept</li>
     * </ul>
     *
     * @param role
     * @param relation
     *
     * @return
     * @throws InvalidAlignmentRelationException
     */
    public static List<IRI> suggestPropertiesForRelation(RDFResourceRole role, String relation) throws InvalidAlignmentRelationException {
        List<IRI> suggested = new ArrayList<>();

        if (RDFResourceRole.isProperty(role)) {
            if (relation.equals("=")) {
                suggested.add(OWL.EQUIVALENTPROPERTY);
                suggested.add(OWL.SAMEAS);
            } else if (relation.equals(">")) {
                throw new InvalidAlignmentRelationException("a rdfs:subProperty alignment would "
                        + "require to assert a triple with the target resource as the subject, "
                        + "which is advisable not to do");
            } else if (relation.equals("<")) {
                suggested.add(RDFS.SUBPROPERTYOF);
            } else if (relation.equals("%")) {
                suggested.add(OWL.PROPERTYDISJOINTWITH);
            } else if (relation.equals("InstanceOf")) {
                suggested.add(RDF.TYPE);
            } else if (relation.equals("HasInstance")) {
                throw new InvalidAlignmentRelationException("a rdf:type alignment would require to "
                        + "assert a triple with the target resource as the subject, "
                        + "which is advisable not to do");
            }
        } else if (role.equals(RDFResourceRole.concept)) {
            if (relation.equals("=")) {
                suggested.add(SKOS.EXACT_MATCH);
                suggested.add(SKOS.CLOSE_MATCH);
            } else if (relation.equals(">")) {
                suggested.add(SKOS.NARROW_MATCH);
            } else if (relation.equals("<")) {
                suggested.add(SKOS.BROAD_MATCH);
//			} else if (relation.equals("%")) { //not foreseen
            } else if (relation.equals("InstanceOf")) {
                suggested.add(SKOS.BROAD_MATCH);
                suggested.add(RDF.TYPE);
            } else if (relation.equals("HasInstance")) {
                suggested.add(SKOS.NARROW_MATCH);
            }
        } else if (role.equals(RDFResourceRole.cls)) {
            if (relation.equals("=")) {
                suggested.add(OWL.EQUIVALENTCLASS);
                suggested.add(OWL.SAMEAS);
            } else if (relation.equals(">")) {
                throw new InvalidAlignmentRelationException("a rdfs:subClass alignment would "
                        + "require to assert a triple with the target resource as the subject, "
                        + "which is advisable not to do");
            } else if (relation.equals("<")) {
                suggested.add(RDFS.SUBCLASSOF);
            } else if (relation.equals("%")) {
                suggested.add(OWL.DISJOINTWITH);
            } else if (relation.equals("InstanceOf")) {
                suggested.add(RDF.TYPE);
            } else if (relation.equals("HasInstance")) {
                throw new InvalidAlignmentRelationException("a rdf:type alignment would require "
                        + "to assert a triple with the target resource as the subject, "
                        + "which is advisable not to do");
            }
        } else if (role.equals(RDFResourceRole.individual)) {
            if (relation.equals("=")) {
                suggested.add(OWL.SAMEAS);
            } else if (relation.equals(">") || relation.equals("<")) {
                throw new InvalidAlignmentRelationException(
                        "not possible to state a class subsumption on a individual");
            } else if (relation.equals("%")) {
                suggested.add(OWL.DIFFERENTFROM);
            } else if (relation.equals("InstanceOf")) {
                suggested.add(RDF.TYPE);
            } else if (relation.equals("HasInstance")) {
                throw new InvalidAlignmentRelationException(
                        "not possible to state a " + "class denotation on an individual");
            }
        }
        return suggested;
    }
}
