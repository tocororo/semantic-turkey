package it.uniroma2.art.semanticturkey.user;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
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
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.uniroma2.art.semanticturkey.vocabulary.UserVocabulary;

public class UsersGroupsRepoHelper {
	
	protected static Logger logger = LoggerFactory.getLogger(UsersGroupsRepoHelper.class);
	
	private Repository repository;
	
	private String BINDING_IRI = "groupIri";
	private String BINDING_SHORT_NAME = "shortName";
	private String BINDING_FULL_NAME = "fullName";
	private String BINDING_DESCRIPTION = "description";
	private String BINDING_WEB_PAGE = "webPage";
	private String BINDING_LOGO_URL = "logoUrl";
	
	public UsersGroupsRepoHelper() {
		MemoryStore memStore = new MemoryStore();
		memStore.setPersist(false);
		repository = new SailRepository(memStore);
		repository.init();
	}
	
	public void loadGroupDetails(File groupDetailsFile) throws RDFParseException, RepositoryException, IOException {
		RepositoryConnection conn = repository.getConnection();
		conn.add(groupDetailsFile, UserVocabulary.BASEURI, RDFFormat.TURTLE);
		conn.close();
	}
	
	/**
	 * Returns a list of all the users into the repository
	 * @return
	 * @throws ParseException
	 */
	public Collection<UsersGroup> listGroups() {
		String query = "SELECT * WHERE {"
				+ " ?" + BINDING_IRI + " a " + NTriplesUtil.toNTriplesString(UserVocabulary.GROUP) + " ."
				+ " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.GROUP_SHORT_NAME) + " ?" + BINDING_SHORT_NAME + " ."
				+ " OPTIONAL { ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.GROUP_FULL_NAME) + " ?" + BINDING_FULL_NAME + " . }"
				+ " OPTIONAL { ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.GROUP_DESCRIPTION) + " ?" + BINDING_DESCRIPTION + " . }"
				+ " OPTIONAL { ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.GROUP_LOGO_URL) + " ?" + BINDING_LOGO_URL + " . }"
				+ " OPTIONAL { ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.GROUP_WEB_PAGE) + " ?" + BINDING_WEB_PAGE+ " . }"
				+ "}";
		
		//execute query
		logger.debug(query);
		TupleQueryResult result = null;
		try (RepositoryConnection conn = repository.getConnection()) {
			TupleQuery tq = conn.prepareTupleQuery(query);
			result = tq.evaluate();
			//collect and return groups
			return getGroupsFromTupleResult(result);
		} finally {
			if (result != null) {
				result.close();
			}
		}
	}
	
	/**
	 * Insert the given group into the repository
	 * @param group
	 */
	public void insertGroup(UsersGroup group) {
		String query = "INSERT DATA {"
				+ " ?" + BINDING_IRI + " a " + NTriplesUtil.toNTriplesString(UserVocabulary.GROUP) + " ."
				+ " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.GROUP_SHORT_NAME) + " \"" + group.getShortName() + "\" .";
		if (group.getFullName() != null) {
			query += " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.GROUP_FULL_NAME) + " \"" + group.getFullName() + "\" .";
		}
		if (group.getDescription() != null) {
			query += " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.GROUP_DESCRIPTION) + " \"" + group.getDescription() + "\" .";
		}
		if (group.getWebPage() != null) {
			query += " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.GROUP_WEB_PAGE) + " \"" + group.getWebPage() + "\" .";
		}
		if (group.getLogoUrl() != null) {
			query += " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.GROUP_LOGO_URL) + " \"" + group.getLogoUrl() + "\" .";
		}
		query += " }";
		
		query = query.replace("?" + BINDING_IRI, NTriplesUtil.toNTriplesString(group.getIRI()));
		
		//execute query
		logger.debug(query);
		try (RepositoryConnection conn = repository.getConnection()) {
			Update update = conn.prepareUpdate(query);
			update.execute();
		}
	}
	
	/**
	 * Serialize the content of the repository in the given file
	 * @param file
	 * @throws IOException
	 */
	public void saveGroupDetailsFile(File file) throws IOException {
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
	
	private Collection<UsersGroup> getGroupsFromTupleResult(TupleQueryResult result) {
		Collection<UsersGroup> list = new ArrayList<UsersGroup>();
		while (result.hasNext()) {
			BindingSet tuple = result.next();

			IRI iri = (IRI) tuple.getValue(BINDING_IRI);
			String shortName = tuple.getValue(BINDING_SHORT_NAME).stringValue();
			UsersGroup group = new UsersGroup(iri, shortName);
			
			//Optional fields
			if (tuple.getBinding(BINDING_FULL_NAME) != null) {
				group.setFullName(tuple.getValue(BINDING_FULL_NAME).stringValue());
			}
			if (tuple.getBinding(BINDING_DESCRIPTION) != null) {
				group.setDescription(tuple.getValue(BINDING_DESCRIPTION).stringValue());
			}
			if (tuple.getBinding(BINDING_WEB_PAGE) != null) {
				group.setWebPage(tuple.getValue(BINDING_WEB_PAGE).stringValue());
			}
			if (tuple.getBinding(BINDING_LOGO_URL) != null) {
				group.setLogoUrl(tuple.getValue(BINDING_LOGO_URL).stringValue());
			}
			list.add(group);
		}
		return list;
	}
	
	public void shutDownRepository() {
		repository.shutDown();
	}

}
