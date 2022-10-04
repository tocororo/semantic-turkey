package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.settings.core.CorePUSettings;
import it.uniroma2.art.semanticturkey.settings.core.CustomTreeSettings;
import it.uniroma2.art.semanticturkey.settings.core.SemanticTurkeyCoreSettingsManager;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.queryrender.RenderUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.joining;

@STService
public class CustomTrees extends STServiceAdapter {

    @STServiceOperation
    @Read
    @PreAuthorize("@auth.isAuthorized('rdf(resource, taxonomy)', 'R')")
    public Collection<AnnotatedValue<Resource>> getRoots() throws STPropertyAccessException {

        CorePUSettings puSettings = STPropertiesManager.getPUSettings(CorePUSettings.class, getProject(), UsersManager.getLoggedUser(), SemanticTurkeyCoreSettingsManager.class.getName());
        CustomTreeSettings ctSettings = puSettings.customTree;

        IRI type = ctSettings.type;
        if (type == null) {
            type = RDFS.RESOURCE;
        }
        IRI hierarchicalProperty = ctSettings.hierarchicalProperty;
        boolean includeSubProp = ctSettings.includeSubProp;
        boolean inverseHierarchyDirection = ctSettings.inverseHierarchyDirection;

        String hierarchyPath = getPathForHierarchy(hierarchicalProperty, inverseHierarchyDirection, includeSubProp);
        // @formatter:off
        String query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" +
                "PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> \n" +
                "SELECT ?resource ?attr_more " + generateNatureSPARQLSelectPart() + " \n" +
                "WHERE { \n" + getResourceWhere(ctSettings, hierarchyPath) +
                "   BIND(EXISTS { \n" +
                "       ?resource (" + hierarchyPath + ") ?child . \n" +
                "   } as ?attr_more) \n" +
                generateNatureSPARQLWherePart("?resource") +
                "} \n" +
                "GROUP BY ?resource ?attr_more";
        // @formatter:on
        QueryBuilder qb = createQueryBuilder(query);
        qb.setBinding("cls", type);
        qb.processRendering();
        qb.processQName();
        return qb.runQuery();
    }

    private String getResourceWhere(CustomTreeSettings ctSettings, String hierarchyPath) {
        String rootCriteria = ctSettings.rootCriteria;
        if (rootCriteria == null) {
            rootCriteria = CustomTreeSettings.ROOT_CRITERIA_ALL;
        }
        List<IRI> roots = ctSettings.roots != null ? ctSettings.roots : new ArrayList<>();

        String where = "";
        if (rootCriteria.equals(CustomTreeSettings.ROOT_CRITERIA_STATIC)) {
            //?resource is bound to statically defined values
            where += "   VALUES(?resource) {" + roots.stream().map(iri -> "(" + RenderUtils.toSPARQL(iri) + ")").collect(joining()) + "}";
        } else {
            //?resource is bound to all those resources of the given type
            where += "   ?resource rdf:type" + (ctSettings.includeSubtype ? "/rdfs:subClassOf*" : "") + " ?cls . \n" +
                    "   FILTER NOT EXISTS { \n" +
                    "       ?parent (" + hierarchyPath + ") ?resource . \n" +
                    "       ?parent rdf:type" + (ctSettings.includeSubtype ? "/rdfs:subClassOf*" : "") + " ?cls . \n" +
                    "   } \n";
            //collect only resources with children
            if (rootCriteria.equals(CustomTreeSettings.ROOT_CRITERIA_ONLY_WITH_CHILDREN)) {
                where += "FILTER EXISTS { ?resource (" + hierarchyPath + ") ?child . } \n";
            }
        }
        return where;
    }


    @STServiceOperation
    @Read
    @PreAuthorize("@auth.isAuthorized('rdf(resource, taxonomy)', 'R')")
    public Collection<AnnotatedValue<Resource>> getChildrenResources(IRI resource) throws STPropertyAccessException {

        CorePUSettings puSettings = STPropertiesManager.getPUSettings(CorePUSettings.class, getProject(), UsersManager.getLoggedUser(), SemanticTurkeyCoreSettingsManager.class.getName());
        CustomTreeSettings ctSettings = puSettings.customTree;
        IRI type = ctSettings.type;
        if (type == null) {
            type = RDFS.RESOURCE;
        }
        boolean includeSubtype = ctSettings.includeSubtype;
        IRI hierarchicalProperty = ctSettings.hierarchicalProperty;
        boolean includeSubProp = ctSettings.includeSubProp;
        boolean inverseHierarchyDirection = ctSettings.inverseHierarchyDirection;

        String hierarchyPath = getPathForHierarchy(hierarchicalProperty, inverseHierarchyDirection, includeSubProp);
        // @formatter:off
        String query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" +
                "PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> \n" +
                "SELECT ?resource ?attr_more " + generateNatureSPARQLSelectPart() + " \n" +
                "WHERE { \n" +
                "   ?resource rdf:type"  + (includeSubtype ? "/rdfs:subClassOf*" : "") + " ?cls . \n" +
                "   ?parent (" + hierarchyPath + ") ?resource . \n" +
                "   BIND(EXISTS { \n" +
                "       ?resource ("  + hierarchyPath + ") ?child . \n" +
                "   } as ?attr_more) \n" +
                generateNatureSPARQLWherePart("?resource") +
                "} \n" +
                "GROUP BY ?resource ?attr_more";
        // @formatter:on
        QueryBuilder qb = createQueryBuilder(query);
        qb.setBinding("cls", type);
        qb.setBinding("parent", resource);
        qb.processRendering();
        qb.processQName();
        return qb.runQuery();
    }

    private String getPathForHierarchy(IRI hierarchicalProperty, boolean invHierarchyDirection, boolean includeSubProp) {
        if (includeSubProp) {
            RepositoryConnection conn = getManagedConnection();
            // @formatter:off
            String query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
                    "SELECT ?sub WHERE { \n" +
                    "   ?sub rdfs:subPropertyOf* ?prop . \n" +
                    "}";
            // @formatter:on
            TupleQuery tq = conn.prepareTupleQuery(query);
            tq.setBinding("prop", hierarchicalProperty);
            TupleQueryResult result = tq.evaluate();
            List<String> propsNt = new ArrayList<>();
            while (result.hasNext()) {
                propsNt.add((invHierarchyDirection ? "^" : "") + NTriplesUtil.toNTriplesString(result.next().getValue("sub")));
            }
            return String.join("|", propsNt);
        } else {
            return (invHierarchyDirection ? "^" : "") + NTriplesUtil.toNTriplesString(hierarchicalProperty);
        }
    }


}
