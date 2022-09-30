package it.uniroma2.art.semanticturkey.services.core;

import it.uniroma2.art.semanticturkey.properties.STPropertiesManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.support.QueryBuilder;
import it.uniroma2.art.semanticturkey.settings.core.CorePUSettings;
import it.uniroma2.art.semanticturkey.settings.core.CustomTreeSettings;
import it.uniroma2.art.semanticturkey.settings.core.SemanticTurkeyCoreSettingsManager;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@STService
public class CustomTrees extends STServiceAdapter {

    @STServiceOperation
    @Read
    @PreAuthorize("@auth.isAuthorized('rdf(resource, taxonomy)', 'R')")
    public Collection<AnnotatedValue<Resource>> getRoots(IRI cls, IRI childProp,
            @Optional boolean invHierarchyDirection, @Optional boolean includeSubProp, @Optional boolean includeSubclasses) {
        String hierarchyPath = getPathForHierarchy(childProp, invHierarchyDirection, includeSubProp);
        // @formatter:off
        String query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" +
                "PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> \n" +
                "SELECT ?resource ?attr_more " + generateNatureSPARQLSelectPart() + " \n" +
                "WHERE { \n" +
                "   ?resource rdf:type" + (includeSubclasses ? "/rdfs:subClassOf*" : "") + " ?cls . \n" +
                "   FILTER NOT EXISTS { \n" +
                "       ?parent (" + hierarchyPath + ") ?resource . \n" +
                "       ?parent rdf:type"  + (includeSubclasses ? "/rdfs:subClassOf*" : "") + " ?cls . \n" +
                "   } \n" +
                "   BIND(EXISTS { \n" +
                "       ?resource (" + hierarchyPath + ") ?child . \n" +
                "   } as ?attr_more) \n" +
                generateNatureSPARQLWherePart("?resource") +
                "} \n" +
                "GROUP BY ?resource ?attr_more";
        // @formatter:on
        QueryBuilder qb = createQueryBuilder(query);
        qb.setBinding("cls", cls);
        qb.processRendering();
        qb.processQName();
        return qb.runQuery();
    }


    @STServiceOperation
    @Read
    @PreAuthorize("@auth.isAuthorized('rdf(resource, taxonomy)', 'R')")
    public Collection<AnnotatedValue<Resource>> getChildrenResources(IRI cls, IRI parent, IRI childProp,
            @Optional boolean invHierarchyDirection, @Optional boolean includeSubProp, @Optional boolean includeSubclasses) {
        String hierarchyPath = getPathForHierarchy(childProp, invHierarchyDirection, includeSubProp);
        // @formatter:off
        String query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
                "PREFIX owl: <http://www.w3.org/2002/07/owl#> \n" +
                "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> \n" +
                "PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#> \n" +
                "SELECT ?resource ?attr_more " + generateNatureSPARQLSelectPart() + " \n" +
                "WHERE { \n" +
                "   ?resource rdf:type"  + (includeSubclasses ? "/rdfs:subClassOf*" : "") + " ?cls . \n" +
                "   ?parent (" + hierarchyPath + ") ?resource . \n" +
                "   BIND(EXISTS { \n" +
                "       ?resource ("  + hierarchyPath + ") ?child . \n" +
                "   } as ?attr_more) \n" +
                generateNatureSPARQLWherePart("?resource") +
                "} \n" +
                "GROUP BY ?resource ?attr_more";
        // @formatter:on
        QueryBuilder qb = createQueryBuilder(query);
        qb.setBinding("cls", cls);
        qb.setBinding("parent", parent);
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

    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAuthorized('pm(project,_)', 'U')")
    public void storeCustomTreeProjectDefaultSettings(IRI type, IRI hierarchicalProperty)
            throws STPropertyAccessException, STPropertyUpdateException {
        CorePUSettings puSettings = STPropertiesManager.getPUSettingsProjectDefault(CorePUSettings.class, getProject(), SemanticTurkeyCoreSettingsManager.class.getName());
        CustomTreeSettings ctSettings = new CustomTreeSettings();
        ctSettings.type = type;
        ctSettings.hierarchicalProperty = hierarchicalProperty;
        puSettings.customTree = ctSettings;
        STPropertiesManager.setPUSettingsProjectDefault(puSettings, getProject(), SemanticTurkeyCoreSettingsManager.class.getName(), true);
    }

    @STServiceOperation(method = RequestMethod.POST)
    public void storeCustomTreeSettings(IRI type, IRI hierarchicalProperty)
            throws STPropertyAccessException, STPropertyUpdateException {
        CorePUSettings puSettings = STPropertiesManager.getPUSettings(CorePUSettings.class, getProject(), UsersManager.getLoggedUser(), SemanticTurkeyCoreSettingsManager.class.getName());
        CustomTreeSettings ctSettings = new CustomTreeSettings();
        ctSettings.type = type;
        ctSettings.hierarchicalProperty = hierarchicalProperty;
        puSettings.customTree = ctSettings;
        STPropertiesManager.setPUSettings(puSettings, getProject(), UsersManager.getLoggedUser(), SemanticTurkeyCoreSettingsManager.class.getName());
    }

}
