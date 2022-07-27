package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.NoSuchSettingsManager;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.properties.STPropertyUpdateException;
import it.uniroma2.art.semanticturkey.properties.WrongPropertiesException;
import it.uniroma2.art.semanticturkey.resources.Scope;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Produces;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.settings.contentnegotiation.ContentNegotiationFormat;
import it.uniroma2.art.semanticturkey.settings.contentnegotiation.ContentNegotiationManager;
import it.uniroma2.art.semanticturkey.settings.contentnegotiation.ContentNegotiationSettings;
import it.uniroma2.art.semanticturkey.settings.contentnegotiation.InverseRewritingRule;
import it.uniroma2.art.semanticturkey.settings.contentnegotiation.RewritingRule;
import it.uniroma2.art.semanticturkey.settings.uri2projectresolution.Uri2ProjectResolutionManager;
import it.uniroma2.art.semanticturkey.settings.uri2projectresolution.Uri2ProjectResolutionSettings;
import it.uniroma2.art.semanticturkey.tx.RDF4JRepositoryUtils;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@STService
public class HttpResolution extends STServiceAdapter {

    @Autowired
    private ExtensionPointManager exptManager;

    private static final String PH_FORMAT = "${format}";
    private static final String PH_SOURCE_URI = "${sourceURI}";

    private static final List<RDFFormat> supportedSerializationRdfFormat = Arrays.asList(
            RDFFormat.RDFXML, RDFFormat.NTRIPLES, RDFFormat.N3, RDFFormat.TURTLE, RDFFormat.JSONLD);


    @STServiceOperation
    @PreAuthorize("@auth.isAdmin()")
    public ContentNegotiationSettings getContentNegotiationSettings(String projectName)
            throws IllegalStateException, STPropertyAccessException, NoSuchSettingsManager,
            ProjectAccessException, ProjectInexistentException, InvalidProjectNameException {
        Project project = ProjectManager.getProject(projectName, true);
        return (ContentNegotiationSettings) exptManager.getSettings(
                project, null, null, ContentNegotiationManager.class.getName(), Scope.PROJECT);
    }

    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAdmin()")
    public void storeContentNegotiationSettings(ObjectNode settings, String projectName) throws NoSuchSettingsManager, STPropertyAccessException, STPropertyUpdateException, WrongPropertiesException, ProjectAccessException, ProjectInexistentException, InvalidProjectNameException {
        Project project = ProjectManager.getProject(projectName, true);
        exptManager.storeSettings(ContentNegotiationManager.class.getName(), project, null, null, Scope.PROJECT, settings);
    }


    /**
     * Returns the mapping
     * @return
     * @throws IllegalStateException
     * @throws STPropertyAccessException
     * @throws NoSuchSettingsManager
     * @throws STPropertyUpdateException
     * @throws WrongPropertiesException
     */
    @STServiceOperation
    @PreAuthorize("@auth.isAdmin()")
    public Map<String, String> getUri2ProjectSettings() throws IllegalStateException, STPropertyAccessException,
            NoSuchSettingsManager, STPropertyUpdateException, WrongPropertiesException {
        Uri2ProjectResolutionSettings settings = (Uri2ProjectResolutionSettings) exptManager.getSettings(
                null, null, null, Uri2ProjectResolutionManager.class.getName(), Scope.SYSTEM);
        //clears/removes any mapping to not existing project
        boolean updated = false;
        for (Map.Entry<String, String> mapping: settings.uri2ProjectMap.entrySet()) {
            try {
                ProjectManager.getProject(mapping.getValue(), true);
            } catch (InvalidProjectNameException | ProjectInexistentException | ProjectAccessException e) {
                settings.uri2ProjectMap.remove(mapping.getKey());
                updated = true;
            }
        }
        if (updated) {
            storeUri2ProjectSettingsInner(settings);
        }

        return settings.uri2ProjectMap;
    }

    @STServiceOperation(method = RequestMethod.POST)
    @PreAuthorize("@auth.isAdmin()")
    public void storeUri2ProjectSettings(Map<String, String> uri2ProjectMap) throws NoSuchSettingsManager, STPropertyAccessException, STPropertyUpdateException, WrongPropertiesException {
        Uri2ProjectResolutionSettings uri2ProjSettings = (Uri2ProjectResolutionSettings) exptManager.getSettings(
                null, null, null, Uri2ProjectResolutionManager.class.getName(), Scope.SYSTEM);
        uri2ProjSettings.uri2ProjectMap = uri2ProjectMap;
        storeUri2ProjectSettingsInner(uri2ProjSettings);
    }

    private void storeUri2ProjectSettingsInner(Uri2ProjectResolutionSettings uri2ProjSettings)
            throws STPropertyUpdateException, NoSuchSettingsManager, WrongPropertiesException, STPropertyAccessException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNode = mapper.valueToTree(uri2ProjSettings);
        exptManager.storeSettings(Uri2ProjectResolutionManager.class.getName(), null, null, null, Scope.SYSTEM, objectNode);
    }


    /**
     * Given a resource URI (e.g. http://example#c_123, http://dbpedia.org/resource/Rome), find the project
     * mapped to the URI and, according the Accept header and the rewriting rules configured for the project,
     * returns a 303 with the proper location or that redirects to
     * the resource description serialization (rdfResURI) or to the html resource page (htmlResURI)
     * An {@link IllegalStateException} is thrown if:
     * - no project is mapped to resURI
     * - no rewriting rule matches resURI
     * If Accept header is not provided, or if it's one not supported, by default the 303 is to the html page
     * @param oReq
     * @param oRes
     * @param resURI
     * @throws IOException
     * @throws NoSuchSettingsManager
     * @throws STPropertyAccessException
     * @throws ProjectAccessException
     * @throws ProjectInexistentException
     * @throws InvalidProjectNameException
     */
    @STServiceOperation(methods = { RequestMethod.GET, RequestMethod.HEAD })
    @Produces({"*/*"})
    public void contentNegotiation(HttpServletRequest oReq, HttpServletResponse oRes, String resURI)
            throws IOException, NoSuchSettingsManager, STPropertyAccessException, ProjectAccessException,
            ProjectInexistentException, InvalidProjectNameException {

        //Find the Project mapped to the input URI
        Project project = resolveProjectFromUri(resURI, false);

        //Find the rewriting rule that matches the input URI and compute the 303 location

        // - according to Accept header mime-type, detect the desired content-negotiation format and the response content-type
        String acceptHeader = oReq.getHeader("Accept");
        ContentNegotiationFormat contNegFormat;
        String responseContentType;

        //accept mime-type => RDFFormat => extension/ContentNegotiationFormat
        if (acceptHeader != null) {
            //get the RDFFormat corresponding to the mime-type
            java.util.Optional<RDFFormat> acceptHeaderFormat = supportedSerializationRdfFormat.stream()
                    .filter(format -> acceptHeader.contains(format.getDefaultMIMEType())).findFirst();
            if (acceptHeaderFormat.isPresent()) { //if found, get the extension and the response content type
                contNegFormat = ContentNegotiationFormat.valueOf(acceptHeaderFormat.get().getDefaultFileExtension());
                responseContentType = acceptHeaderFormat.get().getDefaultMIMEType();
            } else { //accept mime type not supported => fallback to html
                contNegFormat = ContentNegotiationFormat.html;
                responseContentType = "text/html";
            }
        } else { //no Accept header -> fallback to html
            contNegFormat = ContentNegotiationFormat.html;
            responseContentType = "text/html";
        }

        // - Look for the rule that matches the input URI and covers the desired format
        ContentNegotiationSettings contentNegotiationSettings = (ContentNegotiationSettings) exptManager.getSettings(
                project, null, null, ContentNegotiationManager.class.getName(), Scope.PROJECT);

        RewritingRule rule = null;
        if (contentNegotiationSettings.rewritingRules != null) {
            for (RewritingRule r : contentNegotiationSettings.rewritingRules) {
                if (r.getFormat().covers(contNegFormat)) { //if format handled by the rule covers the required
                    if (resURI.matches(r.getSourceURIRegExp())) { //and resource URI matches the source RegExp
                        rule = r; //=> rule has been found
                        break;
                    }
                }
            }
        }

        if (rule == null) {
            throw new IllegalStateException("No rewriting rule configured in " + project.getName() + " that matches URI " + resURI);
        }

        String sourceRegExp = rule.getSourceURIRegExp();
        String targetRegExp = rule.getTargetURIExp();
        if (targetRegExp.contains(PH_FORMAT)) { //replace ${format} with the extension for the desired format
            targetRegExp = targetRegExp.replace(PH_FORMAT, contNegFormat.toString());
        }
        if (targetRegExp.contains(PH_SOURCE_URI)) { //replace ${sourceURI} with the resource URI
            targetRegExp = targetRegExp.replace(PH_SOURCE_URI, resURI);
        }
        String location = resURI.replaceAll(sourceRegExp, targetRegExp);

//        System.out.println("303 " + location);

        oRes.setStatus(HttpServletResponse.SC_SEE_OTHER);
        oRes.setHeader("Location", location);
        oRes.addHeader("Content-Type", responseContentType);
        oRes.flushBuffer();

    }

    /**
     * Given a (RDF) resource URI (e.g. http://example#c_123.ttl, http://dbpedia.org/data/Rome.ttl)
     * - detects the project mapped to such URI
     * - then according its inverse rewriting rules retrieves the original resource IRI and the format
     * - finally returns the serialization of the resource description in the above format
     * An {@link IllegalStateException} is thrown if
     * - no project is mapped to rdfResURI
     * - no inverse rewriting rule matches rdfResURI
     * In case an unsupported format is retrieved, a not-acceptable (406) response is returned
     *
     * @param oRes
     * @param rdfResURI
     * @throws IOException
     * @throws NoSuchSettingsManager
     * @throws ProjectAccessException
     * @throws ProjectInexistentException
     * @throws InvalidProjectNameException
     * @throws STPropertyAccessException
     */
    @STServiceOperation(methods = { RequestMethod.GET, RequestMethod.HEAD })
    @Produces({"*/*"})
    public void rdfProvider(HttpServletResponse oRes, String rdfResURI) throws IOException, NoSuchSettingsManager, ProjectAccessException, ProjectInexistentException, InvalidProjectNameException, STPropertyAccessException {

        //Find the Project mapped to the input URI
        Project project = resolveProjectFromUri(rdfResURI, true);

        ContentNegotiationSettings contentNegotiationSettings = (ContentNegotiationSettings) exptManager.getSettings(
                project, null, null, ContentNegotiationManager.class.getName(), Scope.PROJECT);

        InverseRewritingRule rule = null;
        if (contentNegotiationSettings.inverseRewritingRules != null) {
            for (InverseRewritingRule r : contentNegotiationSettings.inverseRewritingRules) {
                if (rdfResURI.matches(r.getSourceRDFresURIregExp())) {
                    rule = r;
                    break;
                }
            }
        }

        if (rule == null) {
            throw new IllegalStateException("No inverse rewriting rule configured in " + project.getName() + " that matches URI " + rdfResURI);
        }

        String sourceRegExp = rule.getSourceRDFresURIregExp();
        String targetRegExp = rule.getTargetResURIExp();
        Map<String, String> formatMap = rule.getFormatMap();

        String resourceURI = rdfResURI.replaceAll(sourceRegExp, targetRegExp);

        Pattern pattern = Pattern.compile(sourceRegExp);
        Matcher matcher = pattern.matcher(rdfResURI);
        matcher.find();
        String detectedFormat = matcher.group("format");
        String format = formatMap.getOrDefault(detectedFormat, detectedFormat);

        Repository repo = project.getRepository();
        RepositoryConnection conn = RDF4JRepositoryUtils.getConnection(repo);
        GraphQuery gq = conn.prepareGraphQuery("DESCRIBE <" + resourceURI + ">");

        Optional<RDFFormat> rdfFormat = supportedSerializationRdfFormat.stream()
                .filter(f -> f.getFileExtensions().stream().anyMatch(e -> e.equals(format)))
                .findFirst();
        if (rdfFormat.isPresent()) {
            oRes.setContentType(rdfFormat.get().getDefaultMIMEType());
            gq.evaluate(Rio.createWriter(rdfFormat.get(), oRes.getOutputStream()));
        } else {
            oRes.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
            oRes.flushBuffer();
        }
    }

    /**
     * Given an (html) resource URI (e.g. http://example#c_123.html, http://dbpedia.org/page/Rome)
     * returns a response with the project name and the inverseRewritingRules for such project.
     * In case no project is mapped to such URI, an {@link IllegalStateException} is thrown.
     * @param htmlResURI
     * @return
     * @throws NoSuchSettingsManager
     * @throws ProjectAccessException
     * @throws ProjectInexistentException
     * @throws InvalidProjectNameException
     * @throws STPropertyAccessException
     */
    @STServiceOperation
    public JsonNode getBrowsingInfo(String htmlResURI) throws NoSuchSettingsManager, ProjectAccessException, ProjectInexistentException, InvalidProjectNameException, STPropertyAccessException {

        JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
        ObjectNode respNode = jsonFactory.objectNode();

        //Find the Project mapped to the input URI
        Project project = resolveProjectFromUri(htmlResURI, false);

        ContentNegotiationSettings contentNegotiationSettings = (ContentNegotiationSettings) exptManager.getSettings(
                project, null, null, ContentNegotiationManager.class.getName(), Scope.PROJECT);

        respNode.set("project", jsonFactory.textNode(project.getName()));
        respNode.set("inverseRewritingRules", new ObjectMapper().valueToTree(contentNegotiationSettings.inverseRewritingRules));

        return respNode;
    }

    /**
     * Returns the Project mapped to the input URI. In case of no mapping found, throw an unchecked {@link IllegalStateException}
     * @param resURI
     * @return
     * @throws IllegalStateException
     * @throws STPropertyAccessException
     * @throws NoSuchSettingsManager
     * @throws ProjectAccessException
     * @throws ProjectInexistentException
     * @throws InvalidProjectNameException
     */
    @STServiceOperation
    public String getMappedProject(String resURI) throws IllegalStateException, STPropertyAccessException,
            NoSuchSettingsManager, ProjectAccessException, ProjectInexistentException, InvalidProjectNameException {
        return resolveProjectFromUri(resURI, false).getName();
    }

    /**
     * Find the Project mapped to the input URI. In case of no mapping found, throw an unchecked {@link IllegalStateException}
     * @param resURI
     * @return
     */
    private Project resolveProjectFromUri(String resURI, boolean onlyOpen) throws NoSuchSettingsManager, STPropertyAccessException, ProjectAccessException, ProjectInexistentException, InvalidProjectNameException {
        Uri2ProjectResolutionSettings uri2ProjSettings = (Uri2ProjectResolutionSettings) exptManager.getSettings(
                null, null, null, Uri2ProjectResolutionManager.class.getName(), Scope.SYSTEM);

        String projectName = null;
        for (Map.Entry<String, String> mapping : uri2ProjSettings.uri2ProjectMap.entrySet()) {
            String uriRegexp = mapping.getKey();
            if (resURI.matches(uriRegexp)) {
                projectName = mapping.getValue();
            }
        }

        if (projectName == null) {
            throw new IllegalStateException("No mapped dataset found for URI " + resURI);
        }

        boolean descriptionAllowed = !onlyOpen;
        Project project = ProjectManager.getProject(projectName, descriptionAllowed);

        if (project == null) {
            throw new IllegalStateException(resURI + " belongs to a not an open project: " + projectName);
        }

        return project;
    }

}
