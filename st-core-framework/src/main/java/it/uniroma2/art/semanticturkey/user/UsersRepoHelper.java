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
import java.util.Map;

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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import it.uniroma2.art.semanticturkey.vocabulary.UserVocabulary;

public class UsersRepoHelper {
	
	protected static Logger logger = LoggerFactory.getLogger(UsersRepoHelper.class);
	
	private Repository repository;
	
	private DateFormat dateFormat;
	private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	
	public static String BINDING_FIRST_NAME = "firstName";
	public static String BINDING_LAST_NAME = "lastName";
	public static String BINDING_PASSWORD = "password";
	public static String BINDING_EMAIL = "email";
	public static String BINDING_URL = "url";
	public static String BINDING_PHONE = "phone";
	public static String BINDING_BIRTHDAY = "birthday";
	public static String BINDING_GENDER = "gender";
	public static String BINDING_AFFILIATION = "affiliation";
	public static String BINDING_COUNTRY = "country";
	public static String BINDING_ADDRESS = "address";
	public static String BINDING_REGISTRATION_DATE = "registrationDate";
	
	public UsersRepoHelper(Repository repo) {
		this.repository = repo;
		dateFormat = new SimpleDateFormat(STUser.USER_DATE_FORMAT);
	}
	
	public void loadUserDetails(File userDetailsFile) throws RDFParseException, RepositoryException, IOException {
		RepositoryConnection conn = repository.getConnection();
		conn.add(userDetailsFile, UserVocabulary.URI, RDFFormat.TURTLE);
		conn.close();
	}
	
	/**
	 * 
	 * @param user User to register
	 * (Note: the password is encoded here, so the user should have a not encoded pwd)
	 */
	public void insertUser(STUser user) {
		String query = "INSERT DATA {"
				+ " _:user a <" + UserVocabulary.USER + "> ."
				+ " _:user <" + UserVocabulary.FIRST_NAME + "> '" + user.getFirstName() + "' ."
				+ " _:user <" + UserVocabulary.LAST_NAME + "> '" + user.getLastName() + "' ."
				+ " _:user <" + UserVocabulary.EMAIL+ "> '" + user.getEmail() + "' ."
				+ " _:user <" + UserVocabulary.PASSWORD+ "> '" + passwordEncoder.encode(user.getPassword()) + "' ."
				+ " _:user <" + UserVocabulary.REGISTRATION_DATE+ "> '" + dateFormat.format(new Date()) + "' .";
		if (user.getUrl() != null) {
			query += " _:user <" + UserVocabulary.URL+ "> '" + user.getUrl() + "' .";
		}
		if (user.getPhone() != null) {
			query += " _:user <" + UserVocabulary.PHONE+ "> '" + user.getPhone() + "' .";
		}
		if (user.getBirthday() != null) {
			query += " _:user <" + UserVocabulary.BIRTHDAY+ "> '" + dateFormat.format(user.getBirthday()) + "' .";
		}
		if (user.getGender() != null) {
			query += " _:user <" + UserVocabulary.GENDER+ "> '" + user.getGender() + "' .";
		}
		if (user.getAffiliation() != null) {
			query += " _:user <" + UserVocabulary.AFFILIATION+ "> '" + user.getAffiliation() + "' .";
		}
		if (user.getCountry() != null) {
			query += " _:user <" + UserVocabulary.COUNTRY+ "> '" + user.getCountry() + "' .";
		}
		if (user.getAddress() != null) {
			query += " _:user <" + UserVocabulary.ADDRESS+ "> '" + user.getAddress() + "' .";
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
	 * @throws ParseException
	 */
	public Collection<STUser> listUsers() {
		String query = "SELECT * WHERE {"
				+ " ?userNode a <" + UserVocabulary.USER + "> ."
				+ " ?userNode <" + UserVocabulary.FIRST_NAME + "> ?" + BINDING_FIRST_NAME + " ."
				+ " ?userNode <" + UserVocabulary.LAST_NAME + "> ?" + BINDING_LAST_NAME + " ."
				+ " ?userNode <" + UserVocabulary.PASSWORD + "> ?" + BINDING_PASSWORD + " ."
				+ " ?userNode <" + UserVocabulary.EMAIL + "> ?" + BINDING_EMAIL + " ."
				+ " OPTIONAL { ?userNode <" + UserVocabulary.URL + "> ?" + BINDING_URL + " . }"
				+ " OPTIONAL { ?userNode <" + UserVocabulary.PHONE + "> ?" + BINDING_PHONE + " . }"
				+ " OPTIONAL { ?userNode <" + UserVocabulary.BIRTHDAY + "> ?" + BINDING_BIRTHDAY + " . }"
				+ " OPTIONAL { ?userNode <" + UserVocabulary.GENDER + "> ?" + BINDING_GENDER + " . }"
				+ " OPTIONAL { ?userNode <" + UserVocabulary.AFFILIATION + "> ?" + BINDING_AFFILIATION + " . }"
				+ " OPTIONAL { ?userNode <" + UserVocabulary.COUNTRY + "> ?" + BINDING_COUNTRY + " . }"
				+ " OPTIONAL { ?userNode <" + UserVocabulary.ADDRESS + "> ?" + BINDING_ADDRESS + " . }"
				+ " OPTIONAL { ?userNode <" + UserVocabulary.REGISTRATION_DATE + "> ?" + BINDING_REGISTRATION_DATE + " . }"
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
	 * Searches and returns a list of users that respect the filters
	 * @param filters Map of key value where the key is the field that the user should have and the 
	 * value is the value of that field
	 * @return
	 * @throws ParseException
	 */
	public Collection<STUser> searchUsers(Map<String, String> filters) {
		String query = "SELECT * WHERE { ?userNode a <" + UserVocabulary.USER + "> .";
		for (String key : filters.keySet()) {
			query += " BIND('" + filters.get(key) + "' AS ?" + key + ")";
		}
		query += " ?userNode <" + UserVocabulary.FIRST_NAME + "> ?" + BINDING_FIRST_NAME + " ."
				+ " ?userNode <" + UserVocabulary.LAST_NAME + "> ?" + BINDING_LAST_NAME + " ."
				+ " ?userNode <" + UserVocabulary.PASSWORD + "> ?" + BINDING_PASSWORD + " ."
				+ " ?userNode <" + UserVocabulary.EMAIL + "> ?" + BINDING_EMAIL + " ."
				+ " OPTIONAL { ?userNode <" + UserVocabulary.URL + "> ?" + BINDING_URL + " . }"
				+ " OPTIONAL { ?userNode <" + UserVocabulary.PHONE + "> ?" + BINDING_PHONE + " . }"
				+ " OPTIONAL { ?userNode <" + UserVocabulary.BIRTHDAY + "> ?" + BINDING_BIRTHDAY + " . }"
				+ " OPTIONAL { ?userNode <" + UserVocabulary.GENDER + "> ?" + BINDING_GENDER + " . }"
				+ " OPTIONAL { ?userNode <" + UserVocabulary.AFFILIATION + "> ?" + BINDING_AFFILIATION + " . }"
				+ " OPTIONAL { ?userNode <" + UserVocabulary.COUNTRY + "> ?" + BINDING_COUNTRY + " . }"
				+ " OPTIONAL { ?userNode <" + UserVocabulary.ADDRESS + "> ?" + BINDING_ADDRESS + " . }"
				+ " OPTIONAL { ?userNode <" + UserVocabulary.REGISTRATION_DATE + "> ?" + BINDING_REGISTRATION_DATE + " . }"
				+ "}";
		
		// execute query
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
	
	public void updateUserInfo(String userEmail, String bindingToUpdate, String newValue) {
		String propertyToUpdate = "";
		if (bindingToUpdate.equals(BINDING_FIRST_NAME)) {
			propertyToUpdate = UserVocabulary.FIRST_NAME;
		} else if (bindingToUpdate.equals(BINDING_LAST_NAME)) {
			propertyToUpdate = UserVocabulary.LAST_NAME;
		} else if (bindingToUpdate.equals(BINDING_GENDER)) {
			propertyToUpdate = UserVocabulary.GENDER;
		} else if (bindingToUpdate.equals(BINDING_BIRTHDAY)) {
			propertyToUpdate = UserVocabulary.BIRTHDAY;
		} else if (bindingToUpdate.equals(BINDING_PHONE)) {
			propertyToUpdate = UserVocabulary.PHONE;
		} else if (bindingToUpdate.equals(BINDING_COUNTRY)) {
			propertyToUpdate = UserVocabulary.COUNTRY;
		} else if (bindingToUpdate.equals(BINDING_ADDRESS)) {
			propertyToUpdate = UserVocabulary.ADDRESS;
		} else if (bindingToUpdate.equals(BINDING_AFFILIATION)) {
			propertyToUpdate = UserVocabulary.AFFILIATION;
		} else if (bindingToUpdate.equals(BINDING_URL)) {
			propertyToUpdate = UserVocabulary.URL;
		}
		
		String query = "DELETE { ?user <" + propertyToUpdate + "> ?oldValue } \n"
				+ "INSERT { ?user  <" + propertyToUpdate + "> '" + newValue + "' } \n"
				+ "WHERE {\n"
				+ "?user <" + UserVocabulary.EMAIL + "> '" + userEmail + "' . \n"
				+ "?user <" + propertyToUpdate + "> ?oldValue . \n"
				+ "}";
		//execute query
		logger.debug(query);
		try (RepositoryConnection conn = repository.getConnection()) {
			Update update = conn.prepareUpdate(query);
			update.execute();
		}
	}
	
	/**
	 * Delete the given user. 
	 * Note, since e-mail should be unique, delete the user with the same e-mail of the given user.
	 * @param userEmail
	 */
	public void deleteUser(String userEmail) {
		String query = "DELETE { ?user ?p ?o }"
				+ " WHERE {"
				+ " ?user a <" + UserVocabulary.USER + "> ."
				+ " ?user <" + UserVocabulary.EMAIL + "> '" + userEmail + "' ."
				+ " }";
		logger.debug(query);
		try (RepositoryConnection conn = repository.getConnection()) {
			Update update = conn.prepareUpdate(query);
			update.execute();
		}
	}
	
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
//		tupleLoop: 
		while (result.hasNext()) {
			BindingSet tuple = result.next();

			String email = tuple.getValue(BINDING_EMAIL).stringValue();
			STUser user = new STUser(email, tuple.getValue(BINDING_PASSWORD).stringValue(),
					tuple.getValue(BINDING_FIRST_NAME).stringValue(),
					tuple.getValue(BINDING_LAST_NAME).stringValue());

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
			if (tuple.getBinding(BINDING_REGISTRATION_DATE) != null) {
				try {
					Date d = dateFormat.parse(tuple.getValue(BINDING_REGISTRATION_DATE).stringValue());
					user.setRegistrationDate(d);
				} catch (ParseException e) {
					e.printStackTrace();
					//in case of wrong registration date, set 1st January 1970
					Calendar cal = Calendar.getInstance();
					cal.set(Calendar.YEAR, 1970);
					cal.set(Calendar.MONTH, Calendar.JANUARY);
					cal.set(Calendar.DAY_OF_MONTH, 1);
					user.setRegistrationDate(cal.getTime());
				}
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
