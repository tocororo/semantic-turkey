package it.uniroma2.art.semanticturkey.user;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.ORG;
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

import it.uniroma2.art.semanticturkey.vocabulary.UserVocabulary;

public class UsersRepoHelper {
	
	protected static Logger logger = LoggerFactory.getLogger(UsersRepoHelper.class);
	
	private Repository repository;
	
	private DateFormat dateFormat;
	
	private String BINDING_IRI = "userIri";
	private String BINDING_GIVEN_NAME = "givenName";
	private String BINDING_FAMILY_NAME = "familyName";
	private String BINDING_PASSWORD = "password";
	private String BINDING_EMAIL = "email";
	private String BINDING_URL = "url";
	private String BINDING_PHONE = "phone";
	private String BINDING_BIRTHDAY = "birthday";
	private String BINDING_GENDER = "gender";
	private String BINDING_AFFILIATION = "affiliation";
	private String BINDING_COUNTRY = "country";
	private String BINDING_ADDRESS = "address";
	private String BINDING_REGISTRATION_DATE = "registrationDate";
	private String BINDING_STATUS = "status";
	
	public UsersRepoHelper() {
		MemoryStore memStore = new MemoryStore();
		memStore.setPersist(false);
		repository = new SailRepository(memStore);
		repository.initialize();
		dateFormat = new SimpleDateFormat(STUser.USER_DATE_FORMAT);
	}
	
	public void loadUserDetails(File userDetailsFile) throws RDFParseException, RepositoryException, IOException {
		RepositoryConnection conn = repository.getConnection();
		conn.add(userDetailsFile, UserVocabulary.BASEURI, RDFFormat.TURTLE);
		conn.close();
	}
	
	/**
	 * Insert the given user into the repository
	 * @param user
	 */
	public void insertUser(STUser user) {
		String query = "INSERT DATA {"
				+ " ?" + BINDING_IRI + " a " + NTriplesUtil.toNTriplesString(UserVocabulary.USER) + " ."
				+ " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(FOAF.GIVEN_NAME) + " '" + user.getGivenName() + "' ."
				+ " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(FOAF.FAMILY_NAME) + " '" + user.getFamilyName() + "' ."
				+ " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(FOAF.MBOX) + " '" + user.getEmail() + "' ."
				+ " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.PASSWORD) + " '" + user.getPassword() + "' ."
				+ " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.REGISTRATION_DATE) + " '" + dateFormat.format(user.getRegistrationDate()) + "' ."
				+ " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.STATUS) + " '" + user.getStatus() + "' .";
		if (user.getUrl() != null) {
			query += " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.URL) + " '" + user.getUrl() + "' .";
		}
		if (user.getPhone() != null) {
			query += " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(FOAF.PHONE) + " '" + user.getPhone() + "' .";
		}
		if (user.getBirthday() != null) {
			query += " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(FOAF.BIRTHDAY) + " '" + dateFormat.format(user.getBirthday()) + "' .";
		}
		if (user.getGender() != null) {
			query += " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(FOAF.GENDER) + " '" + user.getGender() + "' .";
		}
		if (user.getAffiliation() != null) {
			query += " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(ORG.MEMBER_OF) + " '" + user.getAffiliation() + "' .";
		}
		if (user.getCountry() != null) {
			query += " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.COUNTRY) + " '" + user.getCountry() + "' .";
		}
		if (user.getAddress() != null) {
			query += " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.ADDRESS) + " '" + user.getAddress() + "' .";
		}
		query += " }";
		
		query = query.replace("?" + BINDING_IRI, NTriplesUtil.toNTriplesString(user.getIRI()));
		
		//execute query
		logger.debug(query);
		try (RepositoryConnection conn = repository.getConnection()) {
			Update update = conn.prepareUpdate(query);
			update.execute();
		}
	}
	
	/**
	 * Returns a list of all the users into the repository
	 * @return
	 * @throws ParseException
	 */
	public Collection<STUser> listUsers() {
		String query = "SELECT * WHERE {"
				+ " ?" + BINDING_IRI + " a " + NTriplesUtil.toNTriplesString(UserVocabulary.USER) + " ."
				+ " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(FOAF.GIVEN_NAME) + " ?" + BINDING_GIVEN_NAME + " ."
				+ " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(FOAF.FAMILY_NAME) + " ?" + BINDING_FAMILY_NAME + " ."
				+ " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.PASSWORD) + " ?" + BINDING_PASSWORD + " ."
				+ " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(FOAF.MBOX) + " ?" + BINDING_EMAIL + " ."
				+ " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.REGISTRATION_DATE) + " ?" + BINDING_REGISTRATION_DATE + " ."
				+ " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.STATUS) + " ?" + BINDING_STATUS + " ."
				+ " OPTIONAL { ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.URL) + " ?" + BINDING_URL + " . }"
				+ " OPTIONAL { ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(FOAF.PHONE) + " ?" + BINDING_PHONE + " . }"
				+ " OPTIONAL { ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(FOAF.BIRTHDAY) + " ?" + BINDING_BIRTHDAY + " . }"
				+ " OPTIONAL { ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(FOAF.GENDER) + " ?" + BINDING_GENDER + " . }"
				+ " OPTIONAL { ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(ORG.MEMBER_OF) + " ?" + BINDING_AFFILIATION + " . }"
				+ " OPTIONAL { ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.COUNTRY) + " ?" + BINDING_COUNTRY + " . }"
				+ " OPTIONAL { ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.ADDRESS) + " ?" + BINDING_ADDRESS + " . }"
				+ "}";
		
		//execute query
		logger.debug(query);
		TupleQueryResult result = null;
		try (RepositoryConnection conn = repository.getConnection()) {
			TupleQuery tq = conn.prepareTupleQuery(query);
			result = tq.evaluate();
			//collect and return users
			return getUsersFromTupleResult(result);
		} finally {
			if (result != null) {
				result.close();
			}
		}
	}
	
	/**
	 * Serialize the content of the repository in the given file
	 * @param file
	 * @throws IOException
	 */
	public void saveUserDetailsFile(File file) throws IOException {
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
	
	private Collection<STUser> getUsersFromTupleResult(TupleQueryResult result) {
		// collect users
		Collection<STUser> list = new ArrayList<STUser>();

		while (result.hasNext()) {
			BindingSet tuple = result.next();

			IRI iri = (IRI) tuple.getValue(BINDING_IRI);
			String email = tuple.getValue(BINDING_EMAIL).stringValue();
			STUser user = new STUser(iri, email, tuple.getValue(BINDING_PASSWORD).stringValue(),
					tuple.getValue(BINDING_GIVEN_NAME).stringValue(), tuple.getValue(BINDING_FAMILY_NAME).stringValue());
			
			user.setStatus(UserStatus.valueOf(tuple.getValue(BINDING_STATUS).stringValue()));
			
			String registrationDate = tuple.getValue(BINDING_REGISTRATION_DATE).stringValue();
			try {
				Date d = dateFormat.parse(registrationDate);
				user.setRegistrationDate(d);
			} catch (ParseException e) {
				e.printStackTrace();
				// in case of wrong registration date, set 1st January 1970
				Calendar cal = Calendar.getInstance();
				cal.set(Calendar.YEAR, 1970);
				cal.set(Calendar.MONTH, Calendar.JANUARY);
				cal.set(Calendar.DAY_OF_MONTH, 1);
				user.setRegistrationDate(cal.getTime());
			}

			//Optional fields
			if (tuple.getBinding(BINDING_URL) != null) {
				user.setUrl(tuple.getValue(BINDING_URL).stringValue());
			}
			if (tuple.getBinding(BINDING_PHONE) != null) {
				user.setPhone(tuple.getValue(BINDING_PHONE).stringValue());
			}
			if (tuple.getBinding(BINDING_BIRTHDAY) != null) {
				try {
					Date d = dateFormat.parse(tuple.getValue(BINDING_BIRTHDAY).stringValue());
					user.setBirthday(d);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			if (tuple.getBinding(BINDING_GENDER) != null) {
				user.setGender(tuple.getValue(BINDING_GENDER).stringValue());
			}
			if (tuple.getBinding(BINDING_AFFILIATION) != null) {
				user.setAffiliation(tuple.getValue(BINDING_AFFILIATION).stringValue());
			}
			if (tuple.getBinding(BINDING_COUNTRY) != null) {
				user.setCountry(tuple.getValue(BINDING_COUNTRY).stringValue());
			}
			if (tuple.getBinding(BINDING_ADDRESS) != null) {
				user.setAddress(tuple.getValue(BINDING_ADDRESS).stringValue());
			}
			list.add(user);
		}
		return list;
	}
	
	public void shutDownRepository() {
		repository.shutDown();
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
