package it.uniroma2.art.semanticturkey.config.resourcemetadata;

import it.uniroma2.art.semanticturkey.extension.ProjectScopedConfigurableComponent;
import it.uniroma2.art.sheet2rdf.cfg.GraphApplicationConfigurationLoader;
import org.apache.poi.util.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class ResourceMetadataPatternStore implements ProjectScopedConfigurableComponent<ResourceMetadataPattern> {

	@Override
	public String getId() {
		return ResourceMetadataPatternStore.class.getName();
	}

	public List<String> getFactoryConfigurations() {
		return Arrays.asList("resource");
	}


	public File getFactoryConfigurationFile(String fileName) throws IOException {
//		return new File(ResourceMetadataPatternStore.class.getResource(fileName).getFile());
		InputStream is = ResourceMetadataPatternStore.class.getResourceAsStream(fileName);
		File cfgFile = File.createTempFile("configFile", ".cfg");
		try (OutputStream os = new FileOutputStream(cfgFile)) {
			IOUtils.copy(is, os);
		}
		return cfgFile;
	}

}
