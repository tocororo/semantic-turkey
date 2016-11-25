package it.uniroma2.art.semanticturkey.user;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.vocabulary.UserVocabulary;

public class ProjectUserBindingsRepoHelper {
	
	protected static Logger logger = LoggerFactory.getLogger(ProjectUserBindingsRepoHelper.class);
	
	private String BINDING_USER = "user";
	private String BINDING_ROLE = "role";
	private String BINDING_PROJECT = "project";
	
	private Repository repository;
	
	public ProjectUserBindingsRepoHelper(Repository repo) {
		this.repository = repo;
	}
	
	public void loadBindingDetails(File bindingDetailsFile) throws RDFParseException, RepositoryException, IOException {
		RepositoryConnection conn = repository.getConnection();
		conn.add(bindingDetailsFile, UserVocabulary.URI, RDFFormat.TURTLE);
		conn.close();
	}
	
	/**
	 * Insert the given ProjectUserBinding into the repository
	 * @param puBinding
	 */
	public void insertBinding(ProjectUserBinding puBinding) {
		String query = "INSERT DATA {"
				+ " _:binding a <" + UserVocabulary.BINDING + "> ."
				+ " _:binding <" + UserVocabulary.USER_PROP + "> '" + puBinding.getUserEmail() + "' ."
				+ " _:binding <" + UserVocabulary.PROJECT + "> '" + puBinding.getProjectName() + "' .";
		for (String role : puBinding.getRolesName()) {
			query += " _:binding <" + UserVocabulary.ROLE_PROP + "> '" + role + "' .";
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
	 * Returns the binding about the given user and project
	 * @param userEmail
	 * @param projectName
	 * @return
	 */
	public ProjectUserBinding getPUBinding(String userEmail, String projectName) {
		String query = "SELECT * WHERE {"
				+ " ?binding a <" + UserVocabulary.BINDING + "> ."
				+ " BIND('" + userEmail + "' AS ?" + BINDING_USER + ")"
				+ " BIND('" + projectName + "' AS ?" + BINDING_PROJECT + ")"
				+ " ?binding <" + UserVocabulary.USER_PROP + "> ?" + BINDING_USER + " ."
				+ " ?binding <" + UserVocabulary.PROJECT + "> ?" + BINDING_PROJECT + " ."
				+ " ?binding <" + UserVocabulary.ROLE_PROP + "> ?" + BINDING_ROLE + " ."
				+ " }";
		// execute query
		logger.debug(query);
		TupleQueryResult result = null;
		try (RepositoryConnection conn = repository.getConnection()) {
			TupleQuery tq = conn.prepareTupleQuery(query);
			result = tq.evaluate();
			// collect bindings
			Collection<ProjectUserBinding> bindingList = getPUBindingsFromTupleResult(result);
			if (bindingList.isEmpty()) {
				return null;
			} else {
				return bindingList.iterator().next();
			}
		} finally {
			if (result != null) {
				result.close();
			}
		}
	}
	
	/**
	 * Deletes the bindings about the given project
	 * @param projectName
	 */
	public void deletePUBindingOfProject(String projectName) {
		String query = "DELETE { ?binding ?p ?o }"
				+ " WHERE {"
				+ " ?binding a <" + UserVocabulary.BINDING + "> ."
				+ " ?binding <" + UserVocabulary.PROJECT + "> '" + projectName + "' ."
				+ " }";
		logger.debug(query);
		try (RepositoryConnection conn = repository.getConnection()) {
			Update update = conn.prepareUpdate(query);
			update.execute();
		}
	}
	
	/**
	 * Deletes the bindings about the given user. 
	 * @param userEmail
	 */
	public void deletePUBindingOfUser(String userEmail) {
		String query = "DELETE { ?binding ?p ?o }"
				+ " WHERE {"
				+ " ?binding a <" + UserVocabulary.BINDING + "> ."
				+ " ?binding <" + UserVocabulary.USER_PROP + "> '" + userEmail + "' ."
				+ " }";
		logger.debug(query);
		try (RepositoryConnection conn = repository.getConnection()) {
			Update update = conn.prepareUpdate(query);
			update.execute();
		}
	}
	
	private Collection<ProjectUserBinding> getPUBindingsFromTupleResult(TupleQueryResult result) {
		// collect puBindings
		Collection<ProjectUserBinding> list = new ArrayList<>();
		tupleLoop: while (result.hasNext()) {
			BindingSet tuple = result.next();

			String projectName = tuple.getValue(BINDING_PROJECT).stringValue();
			String userEmail = tuple.getValue(BINDING_USER).stringValue();
			String roleName = tuple.getValue(BINDING_ROLE).stringValue();

			ProjectUserBinding puBinding = new ProjectUserBinding(projectName, userEmail);

			// Check if the current tuple is about a binding already fetched (and differs just for a role)
			for (ProjectUserBinding b : list) {
				if (b.getProjectName().equals(projectName) && b.getUserEmail().equals(userEmail)) {
					// binding already in list => add the role to it
					b.addRole(roleName);
					continue tupleLoop;
				}
			}
			//if it reach this point, current binding was not already fetched
			//so add the role to the binding and then the binding to the list
			puBinding.addRole(roleName);
			list.add(puBinding);
		}
		return list;
	}
	
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

}
