package it.uniroma2.art.semanticturkey.user;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.project.AbstractProject;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.vocabulary.UserVocabulary;

public class ProjectGroupBindingsRepoHelper {
	
	protected static Logger logger = LoggerFactory.getLogger(ProjectGroupBindingsRepoHelper.class);
	
	private static String BINDING_GROUP = "group";
	private static String BINDING_PROJECT = "project";
	private static String BINDING_OWNED_SCHEME = "scheme";
	
	private Repository repository;
	
	public ProjectGroupBindingsRepoHelper() {
		MemoryStore memStore = new MemoryStore();
		memStore.setPersist(false);
		repository = new SailRepository(memStore);
		repository.initialize();
	}
	
	public void loadBindingDetails(File bindingDetailsFile) throws RDFParseException, RepositoryException, IOException {
		RepositoryConnection conn = repository.getConnection();
		conn.add(bindingDetailsFile, UserVocabulary.BASEURI, RDFFormat.TURTLE);
		conn.close();
	}
	
	/**
	 * Insert the given ProjectGroupBinding into the repository
	 * @param pgBinding
	 */
	public void insertBinding(ProjectGroupBinding pgBinding) {
		String query = "INSERT DATA {"
				+ " _:binding a " + NTriplesUtil.toNTriplesString(UserVocabulary.BINDING) + " ."
				+ " _:binding " + NTriplesUtil.toNTriplesString(UserVocabulary.GROUP_PROP) + " " 
				+ NTriplesUtil.toNTriplesString(pgBinding.getGroup().getIRI()) + " ."
				+ " _:binding " + NTriplesUtil.toNTriplesString(UserVocabulary.PROJECT_PROP) + " " 
				+ NTriplesUtil.toNTriplesString(getProjectIRI(pgBinding.getProject())) + " .";
		for (IRI scheme : pgBinding.getOwnedSchemes()) {
			query += " _:binding " + NTriplesUtil.toNTriplesString(UserVocabulary.OWNED_SCHEME_PROP) + " " 
					+ NTriplesUtil.toNTriplesString(scheme) + " .";
		}
		query += " }";
		//execute query
		logger.debug(query);
		try (RepositoryConnection conn = repository.getConnection()) {
			Update update = conn.prepareUpdate(query);
			update.execute();
		}
	}
	
	/**
	 * Returns the bindings
	 * @return
	 */
	public Collection<ProjectGroupBinding> listPGBindings() {
		String query = "SELECT * WHERE {"
				+ " ?binding a " + NTriplesUtil.toNTriplesString(UserVocabulary.BINDING) + " ."
				+ " ?binding " + NTriplesUtil.toNTriplesString(UserVocabulary.GROUP_PROP) + " ?" + BINDING_GROUP + " ."
				+ " ?binding " + NTriplesUtil.toNTriplesString(UserVocabulary.PROJECT_PROP) + " ?" + BINDING_PROJECT + " ."
				+ " OPTIONAL { ?binding " + NTriplesUtil.toNTriplesString(UserVocabulary.OWNED_SCHEME_PROP) + " ?" + BINDING_OWNED_SCHEME + " . }"
				+ " }";
		// execute query
		logger.debug(query);
		TupleQueryResult result = null;
		try (RepositoryConnection conn = repository.getConnection()) {
			TupleQuery tq = conn.prepareTupleQuery(query);
			result = tq.evaluate();
			// collect bindings
			return getPGBindingsFromTupleResult(result);
		} finally {
			if (result != null) {
				result.close();
			}
		}
	}
	
	private Collection<ProjectGroupBinding> getPGBindingsFromTupleResult(TupleQueryResult result) {
		// collect puBindings
		Collection<ProjectGroupBinding> list = new ArrayList<>();
		tupleLoop: while (result.hasNext()) {
			BindingSet tuple = result.next();

			IRI projIRI = (IRI) tuple.getValue(BINDING_PROJECT);
			AbstractProject project = getProjectFromIRI(projIRI);
			if (project == null) { //there is a binding that references a no more existing project
				logger.warn("Invalid binding: IRI " + projIRI.stringValue() + " references to a not existing project."
						+ " Project-User bindings with this project will be ignored");
				continue; // => binding ignored
			}
			
			IRI groupIRI = (IRI) tuple.getValue(BINDING_GROUP);
			UsersGroup group = UsersGroupsManager.getGroupByIRI(groupIRI);
			if (group == null) { //there is a binding that references a no more existing user
				logger.warn("Invalid binding: IRI " + groupIRI.stringValue() + " references to a not existing group."
						+ " Project-Group bindings with this group will be ignored");
				continue; // => binding ignored
			}
			
			ProjectGroupBinding pgBinding = new ProjectGroupBinding(project, group);
			
			IRI scheme = null;
			if (tuple.getBinding(BINDING_OWNED_SCHEME) != null) {
				scheme = (IRI) tuple.getValue(BINDING_OWNED_SCHEME);
			}
			// Check if the current tuple is about a binding already fetched (and so it differs just for scheme)
			for (ProjectGroupBinding b : list) {
				if (b.getProject().getName().equals(project.getName()) && b.getGroup().getIRI().equals(group.getIRI())) {
					// binding already in list => add the scheme to it
					if (scheme != null) {
						b.addScheme(scheme);
					}
					continue tupleLoop;
				}
			}
			//if it reach this point, current binding was not already fetched so add the scheme to the binding
			if (scheme != null) {
				pgBinding.addScheme(scheme);
			}
			
			list.add(pgBinding);
		}
		return list;
	}
	
	/**
	 * Serializes the binding in the given file
	 * @param file
	 * @throws IOException
	 */
	public void saveBindingDetailsFile(File file) throws IOException {
		RepositoryConnection conn = repository.getConnection();
		try {
			RepositoryResult<Statement> stats = conn.getStatements(null, null, null, false);
			Model model = Iterations.addAll(stats, new LinkedHashModel());
			try (FileOutputStream out = new FileOutputStream(file)) {
				RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, out);
				writer.startRDF();
				for (Statement st : model) {
					writer.handleStatement(st);
				}
				writer.endRDF();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			conn.close();
		}
	}
	
	public void shutDownRepository() {
		repository.shutDown();
	}
	
	private IRI getProjectIRI(AbstractProject project) {
		return SimpleValueFactory.getInstance().createIRI(UserVocabulary.PROJECTSBASEURI, encodeProjectName(project));
	}
	
	private AbstractProject getProjectFromIRI(IRI projectIRI) {
		try {
			String projectName = "";
			try {
				projectName = URLDecoder.decode(projectIRI.getLocalName(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return ProjectManager.getProjectDescription(projectName);
		} catch (InvalidProjectNameException | ProjectInexistentException | ProjectAccessException e) {
			logger.warn("Invalid binding: IRI " + projectIRI.stringValue()
				+ " references to a not existing project");
			return null;
		}
	}
	
	public static String encodeProjectName(AbstractProject project) {
		String encodedProjName = "";
		try {
			encodedProjName = URLEncoder.encode(project.getName(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return encodedProjName;
	}

}
