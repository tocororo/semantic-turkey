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

public class RolesRepoHelper {
	
	protected static Logger logger = LoggerFactory.getLogger(RolesRepoHelper.class);
	
	private Repository repository;
	
	public static String BINDING_ROLE = "role_name";
	public static String BINDING_CAPABILITY = "capability";
	
	public RolesRepoHelper(Repository repo) {
		this.repository = repo;
	}
	
	public void loadRolesDefinition(File roleCapabilityFile) throws RDFParseException, RepositoryException, IOException {
		RepositoryConnection conn = repository.getConnection();
		conn.add(roleCapabilityFile, UserVocabulary.URI, RDFFormat.TURTLE);
		conn.close();
	}
	
	public void insertRole(STRole role) {
		String query = "INSERT DATA {"
				+ " _:role a <" + UserVocabulary.ROLE + "> ."
				+ " _:role <" + UserVocabulary.ROLE_NAME + "> '" + role.getName() + "' .";
		Collection<UserCapabilitiesEnum> capabilities = role.getCapabilities();
		for (UserCapabilitiesEnum p : capabilities) {
			query += " _:role <" + UserVocabulary.CAPABILITY + "> '" + p.name() + "' .";
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
	 * Returns a list of all the registered users
	 * @return
	 */
	public Collection<STRole> listRoles() {
		String query = "SELECT * WHERE {"
				+ " ?role a <" + UserVocabulary.ROLE + "> ."
				+ " ?role <" + UserVocabulary.ROLE_NAME + "> ?" + BINDING_ROLE + " ."
				+ " ?role <" + UserVocabulary.CAPABILITY + "> ?" + BINDING_CAPABILITY + " ."
				+ "}";
		//execute query
		logger.debug(query);
		TupleQueryResult result = null;
		try (RepositoryConnection conn = repository.getConnection()) {
			TupleQuery tq = conn.prepareTupleQuery(query);
			result = tq.evaluate();
			// collect roles
			return getRolesFromTupleResult(result);
		} finally {
			if (result != null) {
				result.close();
			}
		}
	}
	
	/**
	 * Searches and returns the role with the given name. Null if no user has the given name
	 * @return
	 */
	public STRole searchRole(String roleName) {
		String query = "SELECT * WHERE {"
				+ " ?role a <" + UserVocabulary.ROLE + "> ."
				+ " BIND('" + roleName + "' AS ?" + BINDING_ROLE + ")"
				+ " ?role <" + UserVocabulary.ROLE_NAME + "> ?" + BINDING_ROLE + " ."
				+ " ?role <" + UserVocabulary.CAPABILITY + "> ?" + BINDING_CAPABILITY + " ."
				+ " }";
		// execute query
		logger.debug(query);
		TupleQueryResult result = null;
		try (RepositoryConnection conn = repository.getConnection()) {
			TupleQuery tq = conn.prepareTupleQuery(query);
			result = tq.evaluate();
			// collect roles
			Collection<STRole> roleList = getRolesFromTupleResult(result);
			if (roleList.isEmpty()) {
				return null;
			} else {
				return roleList.iterator().next();
			}
		} finally {
			if (result != null) {
				result.close();
			}
		}
	}
	
	/**
	 * Add a capability to the given role
	 * @param role
	 * @param capability
	 */
	public void addCapability(STRole role, UserCapabilitiesEnum capability) {
		String query = "INSERT {"
				+ " ?role  <" + UserVocabulary.CAPABILITY + "> '" + capability.name() + "' }"
				+ " WHERE {"
				+ " ?role a <" + UserVocabulary.ROLE + "> ."
				+ " ?role <" + UserVocabulary.ROLE_NAME + "> '" + role.getName() + "' ."
				+ " }";
		// execute query
		logger.debug(query);
		try (RepositoryConnection conn = repository.getConnection()) {
			Update update = conn.prepareUpdate(query);
			update.execute();
		}
	}

	/**
	 * Removes the capability associated to the given list
	 * @param role
	 * @param capability
	 */
	public void removeCapability(STRole role, UserCapabilitiesEnum capability) {
		String query = "DELETE {"
				+ " ?role <" + UserVocabulary.CAPABILITY + "> '" + capability.name() + "' }"
				+ " WHERE {"
				+ " ?role a <" + UserVocabulary.ROLE + "> ."
				+ " ?role <" + UserVocabulary.ROLE_NAME + "> '" + role.getName() + "' ."
				+ " }";
		// execute query
		logger.debug(query);
		try (RepositoryConnection conn = repository.getConnection()) {
			Update update = conn.prepareUpdate(query);
			update.execute();
		}
	}
	
	/**
	 * Deletes the given role from the repository
	 * @param role
	 */
	public void deleteRole(STRole role) {
		String query = "DELETE { ?role  ?p ?o }"
				+ " WHERE {"
				+ " ?role a <" + UserVocabulary.ROLE + "> ."
				+ " ?role <" + UserVocabulary.ROLE_NAME + "> '" + role.getName() + "' ."
				+ " ?role ?p ?o ."
				+ " }";
		// execute query
		logger.debug(query);
		try (RepositoryConnection conn = repository.getConnection()) {
			Update update = conn.prepareUpdate(query);
			update.execute();
		}
	}
	
	private Collection<STRole> getRolesFromTupleResult(TupleQueryResult result) {
		
		// collect users
		Collection<STRole> list = new ArrayList<>();
		tupleLoop: while (result.hasNext()) {
			BindingSet tuple = result.next();

			String roleName = tuple.getValue(BINDING_ROLE).stringValue();
			STRole role = new STRole(roleName);

			// Check if the current tuple is about a role already fetched (and differs just for a capability)
			for (STRole r : list) {
				if (r.getName().equals(roleName)) {
					// role already in list => add the capability to it
					// don't check if binding != null, cause it is so for sure, since it is the only value
					// that differs
					r.addCapability(UserCapabilitiesEnum.valueOf(tuple.getValue(BINDING_CAPABILITY).stringValue()));
					continue tupleLoop; // ignore other bindings and go to the following tuple
				}
			}

			if (tuple.getBinding(BINDING_CAPABILITY) != null) {
				role.addCapability(UserCapabilitiesEnum.valueOf(tuple.getValue(BINDING_CAPABILITY).stringValue()));
			}
			list.add(role);
		}
		return list;
	}
	
	public void saveRoleCapabilityFile(File file) throws IOException {
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
	
	/**
	 * For testing
	 */
	@SuppressWarnings("unused")
	private void printRepository() {
		RepositoryResult<Statement> stmt = repository.getConnection().getStatements(null, null, null);
		while (stmt.hasNext()) {
			Statement s = stmt.next();
			System.out.println("S: " + s.getSubject().stringValue() +
					"\nP: " + s.getPredicate().stringValue() +
					"\nO: " + s.getObject().stringValue() + "\n");
		}
	}

}
