package it.uniroma2.art.semanticturkey.extension.impl.deployer.showvoc;

import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.extension.ExtensionPointManager;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.Deployer;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.RepositorySource;
import it.uniroma2.art.semanticturkey.extension.extpts.deployer.RepositorySourcedDeployer;
import it.uniroma2.art.semanticturkey.extension.extpts.repositoryimplconfigurer.RepositoryImplConfigurer;
import it.uniroma2.art.semanticturkey.ontology.TransitiveImportMethodAllowance;
import it.uniroma2.art.semanticturkey.plugin.PluginSpecification;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.showvoc.RemoteVBConnector;
import it.uniroma2.art.semanticturkey.showvoc.ShowVocConstants;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.util.Arrays;

/**
 * Implementation of the {@link Deployer} extension point targeting ShowVoc. This implementation can deploy data
 * provided by a {@link RepositorySource}.
 *
 * <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
public class ShowVocDeployer implements RepositorySourcedDeployer {

    private final ExtensionPointManager exptMgr;
    private final ShowVocDeployerConfiguration conf;

    public ShowVocDeployer(ExtensionPointManager exptMgr, ShowVocDeployerConfiguration conf) {
        this.exptMgr = exptMgr;
        this.conf = conf;
    }

    @Override
    public void deploy(RepositorySource source) throws IOException {
        try {
            // Creates a connector to the remote VB
            RemoteVBConnector vbConnector = new RemoteVBConnector(conf.stHost, conf.stPath, conf.username, conf.password);

            // Log in
            vbConnector.loginAdmin();

            String projectName = conf.project;

            String baseURI;

            if (conf instanceof ExistingProjectShowVocDeployerConfiguration) {
                ObjectNode projectInfo = vbConnector.getProjectInfo(projectName); // throws ProjectInexistentException if the project doesn't exist
                boolean isOpen = projectInfo.findValue("open").asBoolean(false);

                if (!isOpen) {
                    throw new IOException("Project is not open: " + projectName);
                }

                baseURI = projectInfo.findValue("baseURI").textValue();

            } else if (conf instanceof NewProjectShowVocDeployerConfiguration) {
                IRI model = source.getProject().getModel();
                IRI lexicalizationModel = source.getProject().getLexicalizationModel();
                baseURI = source.getProject().getBaseURI();

                NewProjectShowVocDeployerConfiguration newProjectDeployerConf = (NewProjectShowVocDeployerConfiguration) conf;
                PluginSpecification coreRepoSailConfigurerSpecification = exptMgr.buildPluginSpecification(RepositoryImplConfigurer.class, newProjectDeployerConf.coreRepoSailConf);
                vbConnector.createProject(projectName, baseURI, model, lexicalizationModel, coreRepoSailConfigurerSpecification);
                vbConnector.addRolesToUser(projectName, ShowVocConstants.SHOWVOC_VISITOR_EMAIL, Arrays.asList(ShowVocConstants.ShowVocRole.STAGING));
            } else {
                throw new IllegalArgumentException("Unknown configuration type: " + conf.getClass().getName());
            }

            // we use TRIG instead of NQUADS to also export namespaces
            RDFFormat rdfFormat = RDFFormat.TRIG;
            File tempFile = File.createTempFile("showvoc-deployer", rdfFormat.getDefaultFileExtension());
            RDFWriter rdfWriter = Rio.createWriter(rdfFormat, new OutputStreamWriter(new FileOutputStream(tempFile)));
            rdfWriter.set(BasicWriterSettings.PRETTY_PRINT, false);
            rdfWriter.set(BasicWriterSettings.INLINE_BLANK_NODES, false);

            // we ignore source.getGraphs() and always load all graphs!
            source.getSourceRepositoryConnection().export(rdfWriter);

            PluginSpecification rdfLifterSpec = new PluginSpecification(
                    "it.uniroma2.art.semanticturkey.extension.impl.rdflifter.rdfdeserializer.RDFDeserializingLifter",
                    null, null, null);

            try {
                // delete existing data
                vbConnector.clearData(projectName);

                // we require that imports are not resolved, as we are loading all named graphs in the same graph
                vbConnector.loadRDF(projectName, baseURI, tempFile, rdfFormat.getName(), rdfLifterSpec, TransitiveImportMethodAllowance.nowhere, true);
            } finally {
                tempFile.delete();
            }
        } catch(STPropertyAccessException | URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
