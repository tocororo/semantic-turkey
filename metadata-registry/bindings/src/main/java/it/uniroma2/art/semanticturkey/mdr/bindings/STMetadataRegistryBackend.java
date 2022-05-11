package it.uniroma2.art.semanticturkey.mdr.bindings;

import org.eclipse.rdf4j.model.IRI;

import it.uniroma2.art.semanticturkey.mdr.core.MetadataRegistryBackend;
import it.uniroma2.art.semanticturkey.project.Project;

import java.util.Collection;

/**
 * A binding of {@link MetadataRegistryBackend} to Semantic Turkey.
 * 
 */
public interface STMetadataRegistryBackend extends MetadataRegistryBackend {

	/**
	 * Returns metadata about the given project. If no dataset is found, then the method returns
	 * <code>null</code>.
	 * 
	 * @param project
	 * @return
	 */
	IRI findDatasetForProject(Project project);

	/**
	 * Returns metadata about the given project. If no dataset is found, then the method returns
	 * <code>null</code>.
	 *
	 * @param project
	 * @param useDistribution
	 * @return
	 */
	IRI findDatasetForProject(Project project, boolean useDistribution);

	/**
	 * Returns the project associated with the given dataset. If no project is found, then the method returns
	 * <code>null</code>. This is a convenience overload for {@link #findProjectForDataset(IRI, boolean)} with
	 * the second parameter set to <code>false</code>.
	 * 
	 * @param dataset
	 * @return
	 */
	Project findProjectForDataset(IRI dataset);

	/**
	 * Returns the project associated with the given dataset, optionally allowing the lookup of subsets. If no
	 * project is found, then the method returns <code>null</code>.
	 * 
	 * @param dataset
	 * @return
	 */
	Project findProjectForDataset(IRI dataset, boolean allowSubset);

	/**
	 * Registers the metadata associated with a project (see {@link StoredProjectMetadata}). If the project
	 * has been already registered, the metadata are replaced.
	 * 
	 * @param project
	 */
	void registerProject(Project project);

	/**
	 * Removes the metadata associated with a project
	 * 
	 * @param project
	 */
	void unregisterProject(Project project);

}