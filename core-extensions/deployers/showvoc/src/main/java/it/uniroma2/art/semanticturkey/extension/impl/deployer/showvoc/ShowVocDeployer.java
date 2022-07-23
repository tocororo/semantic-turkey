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
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;

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
            RemoteVBConnector vbConnector = new RemoteVBConnector(conf.stHost, conf.vbUrl, conf.username, conf.password);

            // Log in
            vbConnector.loginAdmin();

            String projectName = conf.project;

            String baseURI;

            if (conf instanceof ExistingProjectShowVocDeployerConfiguration) {
                ObjectNode projectInfo = vbConnector.getProjectInfo(projectName); // throws ProjectInexistentException if the project doesn't exist
                boolean isOpen = projectInfo.get("open").asBoolean(false);

                if (!isOpen) {
                    throw new IOException("Project is not open: " + projectName);
                }

                baseURI = projectInfo.get("baseURI").textValue();

            } else if (conf instanceof NewProjectShowVocDeployerConfiguration) {
                NewProjectShowVocDeployerConfiguration newProjectDeployerConf = (NewProjectShowVocDeployerConfiguration) conf;
                baseURI = newProjectDeployerConf.baseURI;
                PluginSpecification coreRepoSailConfigurerSpecification = exptMgr.buildPluginSpecification(RepositoryImplConfigurer.class, newProjectDeployerConf.coreRepoSailConf);
                vbConnector.createProject(projectName, newProjectDeployerConf.baseURI, newProjectDeployerConf.model, newProjectDeployerConf.lexicalizationModel, coreRepoSailConfigurerSpecification);
            } else {
                throw new IllegalArgumentException("Unknown configuration type: " + conf.getClass().getName());
            }

            RDFFormat rdfFormat = RDFFormat.NQUADS;
            File tempFile = File.createTempFile("showoc-deployer", rdfFormat.getDefaultFileExtension());
            source.getSourceRepositoryConnection().export(Rio.createWriter(rdfFormat, new OutputStreamWriter(new FileOutputStream(tempFile))), source.getGraphs());

            PluginSpecification rdfLifterSpec = new PluginSpecification(
                    "it.uniroma2.art.semanticturkey.extension.impl.rdflifter.rdfdeserializer.RDFDeserializingLifter",
                    null, null, null);

            try {
                vbConnector.loadRDF(projectName, baseURI, tempFile, rdfFormat.getName(), rdfLifterSpec, TransitiveImportMethodAllowance.nowhere);
            } finally {
                tempFile.delete();
            }
        } catch(STPropertyAccessException | URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
