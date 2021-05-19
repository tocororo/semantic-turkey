package it.uniroma2.art.semanticturkey.services.core.resourceview;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Maps;
import it.uniroma2.art.semanticturkey.mdr.bindings.STMetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.settings.core.CoreProjectSettings;
import it.uniroma2.art.semanticturkey.settings.core.ResourceViewCustomSectionSettings;
import it.uniroma2.art.semanticturkey.settings.core.SemanticTurkeyCoreSettingsManager;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.stereotype.Component;

import it.uniroma2.art.semanticturkey.customform.CODACoreProvider;
import it.uniroma2.art.semanticturkey.customform.CustomFormManager;
import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.BroadersStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.ClassAxiomsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.ConstituentsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.DatatypeDefinitionsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.DenotationsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.DomainsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.EquivalentPropertyStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.EvokedLexicalConcepts;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.FormBasedPreviewStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.FormRepresentationsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.InSchemeStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.LabelRelationsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.LexicalFormsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.LexicalSensesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.LexicalizationsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.OntologyImportsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.OtherPropertiesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.PropertyChainStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.PropertyDisjointWithStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.PropertyFacetsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.RDFSMembersStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.RangesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SKOSCollectionMembersStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SKOSNotesStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SKOSOrderedCollectionMembersStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SubPropertyOfStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.SubtermsStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.TopConceptOfStatementConsumer;
import it.uniroma2.art.semanticturkey.services.core.resourceview.consumers.TypesStatementConsumer;

import javax.swing.plaf.nimbus.State;

@Component
public class StatementConsumerProvider implements ApplicationListener<ApplicationEvent> {

    private static final Logger logger = LoggerFactory.getLogger(StatementConsumerProvider.class);

    /**
     * Event handlers used to listens for project events, such as opening and closing
     */
    private ProjectManager.ProjectEventHandler prjEventHandler;

    private CustomFormManager customFormManager;
    private SemanticTurkeyCoreSettingsManager coreSettingsManager;

    /**
     * Statements consumers provided out of the box by Semantic Turkey. The {@code key} is the section name.
     */
    private Map<String, StatementConsumer> factoryStatementConsumers;

    /**
     * Associates an (open) project with its resource view templates
     */
    private Map<String, Map<RDFResourceRole, List<StatementConsumer>>> project2templates;

    @Autowired
    public StatementConsumerProvider(CustomFormManager customFormManager,
                                     ObjectFactory<CODACoreProvider> codaProvider, SemanticTurkeyCoreSettingsManager coreSettingsManager) {
        this.customFormManager = customFormManager;
        this.coreSettingsManager = coreSettingsManager;

        factoryStatementConsumers = new LinkedHashMap<>();
        factoryStatementConsumers.put("types", new TypesStatementConsumer(customFormManager));
        factoryStatementConsumers.put("classaxioms", new ClassAxiomsStatementConsumer(customFormManager));
        factoryStatementConsumers.put("datatypeDefinitions", new DatatypeDefinitionsStatementConsumer(customFormManager));
        factoryStatementConsumers.put("lexicalizations", new LexicalizationsStatementConsumer(customFormManager));
        factoryStatementConsumers.put("broaders", new BroadersStatementConsumer(customFormManager));
        factoryStatementConsumers.put("equivalentProperties", new EquivalentPropertyStatementConsumer(customFormManager));
        factoryStatementConsumers.put("disjointProperties", new PropertyDisjointWithStatementConsumer(customFormManager));
        factoryStatementConsumers.put("superproperties", new SubPropertyOfStatementConsumer(customFormManager));
        factoryStatementConsumers.put("subPropertyChains", new PropertyChainStatementConsumer(customFormManager));
        factoryStatementConsumers.put("facets", new PropertyFacetsStatementConsumer(customFormManager));
        factoryStatementConsumers.put("domains", new DomainsStatementConsumer(customFormManager));
        factoryStatementConsumers.put("ranges", new RangesStatementConsumer(customFormManager));
        factoryStatementConsumers.put("imports", new OntologyImportsStatementConsumer(customFormManager));
        factoryStatementConsumers.put("properties", new OtherPropertiesStatementConsumer(customFormManager));
        factoryStatementConsumers.put("topconceptof", new TopConceptOfStatementConsumer(customFormManager));
        factoryStatementConsumers.put("schemes", new InSchemeStatementConsumer(customFormManager));
        factoryStatementConsumers.put("members", new SKOSCollectionMembersStatementConsumer(customFormManager));
        factoryStatementConsumers.put("membersOrdered", new SKOSOrderedCollectionMembersStatementConsumer(customFormManager));
        factoryStatementConsumers.put("labelRelations", new LabelRelationsStatementConsumer(customFormManager));
        factoryStatementConsumers.put("notes", new SKOSNotesStatementConsumer(customFormManager));
        factoryStatementConsumers.put("lexicalForms", new LexicalFormsStatementConsumer(customFormManager));
        factoryStatementConsumers.put("lexicalSenses", new LexicalSensesStatementConsumer(customFormManager));
        factoryStatementConsumers.put("denotations", new DenotationsStatementConsumer(customFormManager));
        factoryStatementConsumers.put("evokedLexicalConcepts", new EvokedLexicalConcepts(customFormManager));
        factoryStatementConsumers.put("formBasedPreview", new FormBasedPreviewStatementConsumer(customFormManager, codaProvider));
        factoryStatementConsumers.put("subterms", new SubtermsStatementConsumer(customFormManager));
        factoryStatementConsumers.put("constituents", new ConstituentsStatementConsumer(customFormManager));
        factoryStatementConsumers.put("formRepresentations", new FormRepresentationsStatementConsumer(customFormManager));
        factoryStatementConsumers.put("rdfsMembers", new RDFSMembersStatementConsumer(customFormManager));

        project2templates = new HashMap<>();
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextStoppedEvent || event instanceof ContextClosedEvent
                || event instanceof ContextRefreshedEvent) {
            if (prjEventHandler != null) {
                ProjectManager.unregisterProjectEventHandler(prjEventHandler);
                prjEventHandler = null;
            }
        }

        if (event instanceof ContextStartedEvent || event instanceof ContextRefreshedEvent) {
            prjEventHandler = new ProjectManager.ProjectEventHandler() {

                @Override
                public void beforeProjectTearDown(Project project) {
                    unregisterProject(project);
                }

                @Override
                public void afterProjectInitialization(Project project) {
                    registerProject(project);
                }
            };
            ProjectManager.registerProjectEventHandler(prjEventHandler);
        }
    }

    private void registerProject(Project project) {
        try {
            CoreProjectSettings coreProjectSettings = coreSettingsManager.getProjectSettings(project);

            Map<String, ResourceViewCustomSectionSettings> customSectionsSettings = Optional.ofNullable(coreProjectSettings.resourceView).map(s -> s.customSections).orElse(Collections.emptyMap());
            Map<RDFResourceRole, List<String>> templatesSettings = Optional.ofNullable(coreProjectSettings.resourceView).map(s -> s.templates).orElse(Collections.emptyMap());

            Map<String, StatementConsumer> customSections = customSectionsSettings.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> new AbstractPropertyMatchingStatementConsumer(customFormManager, entry.getKey(), entry.getValue().matchedProperties)));

            Map<RDFResourceRole, List<StatementConsumer>> projectTemplates = new HashMap<>();

            for (Map.Entry<RDFResourceRole, List<String>> entry : templatesSettings.entrySet()) {
                RDFResourceRole resourceRole = entry.getKey();
                List<String> roleTemplateSections = entry.getValue();

                // resolve sections names against factory ones and then against custom ones. Skip invalid sections

                List<StatementConsumer> roleTemplate = roleTemplateSections.stream().map(s -> factoryStatementConsumers.getOrDefault(s, customSections.get(s))).filter(Objects::nonNull).collect(Collectors.toList());

                projectTemplates.put(resourceRole, roleTemplate);
            }

            project2templates.put(project.getName(), projectTemplates);
        } catch (Exception e) {
            logger.error("Unable to register the newly opened project: " + project.getName(), e);
        }

    }

    private void unregisterProject(Project project) {
        project2templates.remove(project.getName());
    }


    public List<StatementConsumer> getTemplateForResourceRole(Project project, RDFResourceRole role) {
        // there should always be an entry of (open) project
        Map<RDFResourceRole, List<StatementConsumer>> role2template = project2templates.getOrDefault(project.getName(), Collections.emptyMap());

        List<StatementConsumer> template = null;

        if (role.isProperty()) {
            if (RDFResourceRole.subsumes(RDFResourceRole.objectProperty, role)) {
                template = role2template.get(RDFResourceRole.objectProperty);
            }

            if (template != null && RDFResourceRole.subsumes(RDFResourceRole.datatypeProperty, role)) {
                template = role2template.get(RDFResourceRole.datatypeProperty);
            }
            if (template != null && RDFResourceRole.subsumes(RDFResourceRole.ontologyProperty, role)) {
                template = role2template.get(RDFResourceRole.ontologyProperty);
            }
            if (template != null && RDFResourceRole.subsumes(RDFResourceRole.annotationProperty, role)) {
                template = role2template.get(RDFResourceRole.annotationProperty);
            }
            if (template != null)  {
                template = role2template.get(RDFResourceRole.property);
            }
        } else {
            template = role2template.get(role);
        }

        if (template != null) {
            return template;
        } else {
            // if no template has been found, fallback to the one for individuals. If this is not defined (e.g. corrupted settings), fallback to a simple properties listing
            return role2template.getOrDefault(RDFResourceRole.individual, Arrays.asList(factoryStatementConsumers.get("properties")));
        }
    }
}
