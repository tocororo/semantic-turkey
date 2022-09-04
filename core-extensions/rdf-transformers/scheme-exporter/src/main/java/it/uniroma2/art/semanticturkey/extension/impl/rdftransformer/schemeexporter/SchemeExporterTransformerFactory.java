package it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.schemeexporter;

import it.uniroma2.art.semanticturkey.extension.ConfigurableExtensionFactory;
import it.uniroma2.art.semanticturkey.extension.PUScopedConfigurableComponent;
import it.uniroma2.art.semanticturkey.i18n.STMessageSource;

import java.util.Arrays;
import java.util.Collection;

/**
 * Factory for the instantiation of {@link SchemeExporterTransformer}.
 *
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
public class SchemeExporterTransformerFactory implements
        ConfigurableExtensionFactory<SchemeExporterTransformer, SchemeExporterTransformerConfiguration>,
        PUScopedConfigurableComponent<SchemeExporterTransformerConfiguration> {

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.extension.impl.rdftransformer.schemeexporter.SchemeExporterTransformerFactory";
        private static final String name = keyBase + ".name";
        private static final String description = keyBase + ".description";
    }

    @Override
    public String getName() {
        return STMessageSource.getMessage(MessageKeys.name);
    }

    @Override
    public String getDescription() {
        return STMessageSource.getMessage(MessageKeys.description);
    }

    @Override
    public SchemeExporterTransformer createInstance(
            SchemeExporterTransformerConfiguration conf) {
        return new SchemeExporterTransformer(conf);
    }

    @Override
    public Collection<SchemeExporterTransformerConfiguration> getConfigurations() {
        return Arrays.asList(new SchemeExporterTransformerConfiguration());
    }
}
