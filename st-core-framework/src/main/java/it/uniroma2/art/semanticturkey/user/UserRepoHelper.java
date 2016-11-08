package it.uniroma2.art.semanticturkey.user;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
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
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import it.uniroma2.art.semanticturkey.vocabulary.UserVocabulary;

public class UserRepoHelper {
	
	private DateFormat dateFormat;
	private Repository repository;
	
	public static String BINDING_FIRST_NAME = "firstName";
	public static String BINDING_LAST_NAME = "lastName";
	public static String BINDING_PASSWORD = "password";
	public static String BINDING_EMAIL = "email";
	public static String BINDING_ROLE = "role";
	public static String BINDING_URL = "url";
	public static String BINDING_PHONE = "phone";
	public static String BINDING_BIRTHDAY = "birthday";
	public static String BINDING_GENDER = "gender";
	public static String BINDING_AFFILIATION = "affiliation";
	public static String BINDING_COUNTRY = "country";
	public static String BINDING_ADDRESS = "address";
	public static String BINDING_REGISTRATION_DATE = "registrationDate";
	
	public UserRepoHelper() {
		
		dateFormat = new SimpleDateFormat(STUser.USER_DATE_FORMAT);
		// initialize a non-persistent repository
		MemoryStore memStore = new MemoryStore();
		memStore.setPersist(false);
		repository = new SailRepository(memStore);
		repository.initialize();
//		RepositoryConnection conn = repository.getConnection();
//		conn.setNamespace("", UserVocabulary.NAMESPACE);
//		conn.close();
	}
	
	public void loadUserDetails(File userDetailsFile) throws RDFParseException, RepositoryException, IOException {
		RepositoryConnection conn = repository.getConnection();
		conn.add(userDetailsFile, UserVocabulary.URI, RDFFormat.TURTLE);
		conn.close();
	}
	
	public void insertUser(STUser user) {
		String query = "INSERT DATA {"
				+ " _:user a <" + UserVocabulary.USER + "> ."
				+ " _:user <" + UserVocabulary.FIRST_NAME + "> '" + user.getFirstName() + "' ."
				+ " _:user <" + UserVocabulary.LAST_NAME + "> '" + user.getLastName() + "' ."
				+ " _:user <" + UserVocabulary.EMAIL+ "> '" + user.getEmail() + "' ."
				+ " _:user <" + UserVocabulary.PASSWORD+ "> '" + user.getPassword() + "' ."
				+ " _:user <" + UserVocabulary.REGISTRATION_DATE+ "> '" + dateFormat.format(new Date()) + "' .";
		Iterator<? extends GrantedAuthority> itRoles = user.getAuthorities().iterator();
		while (itRoles.hasNext()) {
			query += " _:user <" + UserVocabulary.ROLE + "> '" + itRoles.next().getAuthority() + "' .";
		}
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
		RepositoryConnection conn = repository.getConnection();
		Update update = conn.prepareUpdate(query);
		update.execute();
		conn.close();
		
	}
	
	/**
	 * Returns a list of all the registered users
	 * @return
	 * @throws ParseException
	 */
	public List<STUser> listUsers() throws ParseException {
		String query = "SELECT * WHERE {"
				+ " ?userNode a <" + UserVocabulary.USER + "> ."
				+ " ?userNode <" + UserVocabulary.FIRST_NAME + "> ?" + BINDING_FIRST_NAME + " ."
				+ " ?userNode <" + UserVocabulary.LAST_NAME + "> ?" + BINDING_LAST_NAME + " ."
				+ " ?userNode <" + UserVocabulary.PASSWORD + "> ?" + BINDING_PASSWORD + " ."
				+ " ?userNode <" + UserVocabulary.EMAIL + "> ?" + BINDING_EMAIL + " ."
				+ " OPTIONAL { ?userNode <" + UserVocabulary.ROLE + "> ?" + BINDING_ROLE + " . }"
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
		RepositoryConnection conn = repository.getConnection();
		TupleQuery tq = conn.prepareTupleQuery(query);
		TupleQueryResult result = tq.evaluate();
		//collect users
		ArrayList<STUser> list = new ArrayList<STUser>();
		tupleLoop: while (result.hasNext()) {
			BindingSet tuple = result.next();
			
			String email = tuple.getValue(BINDING_EMAIL).stringValue();
			STUser user = new STUser(
					email,
					tuple.getValue(BINDING_PASSWORD).stringValue(),
					tuple.getValue(BINDING_FIRST_NAME).stringValue(),
					tuple.getValue(BINDING_LAST_NAME).stringValue());
			
			//Check if the current tuple is about an user already fetched (and differs just for the role)
			for (STUser u : list) {
				if (u.getEmail().equals(email)) {
					//user already in list
					//don't check if binding != null, cause it is so for sure, since it is the only value that differs
					String role = tuple.getValue(BINDING_ROLE).stringValue();
					u.addAuthority(new SimpleGrantedAuthority(role));
					continue tupleLoop; //ignore other bindings and go to the following tuple 
				}
			}
			
			if (tuple.getBinding(BINDING_ROLE) != null) {
				user.addAuthority(new SimpleGrantedAuthority(tuple.getValue(BINDING_ROLE).stringValue()));
			}
			if (tuple.getBinding(BINDING_URL) != null) {
				user.setUrl(tuple.getValue(BINDING_URL).stringValue());
			}
			if (tuple.getBinding(BINDING_PHONE) != null) {
				user.setPhone(tuple.getValue(BINDING_PHONE).stringValue());
			}
			if (tuple.getBinding(BINDING_BIRTHDAY) != null) {
			    Date d =  dateFormat.parse(tuple.getValue(BINDING_BIRTHDAY).stringValue());
				user.setBirthday(d);
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
			    Date d =  dateFormat.parse(tuple.getValue(BINDING_REGISTRATION_DATE).stringValue());
				user.setRegistrationDate(d);
			}
			list.add(user);
		}
		
		conn.close();
		return list;
	}
	
	/**
	 * Searches and returns a list of users that respect the filters
	 * @param filters Map of key value where the key is the field that the user should have and the 
	 * value is the value of that field
	 * @return
	 * @throws ParseException
	 */
	public List<STUser> searchUsers(Map<String, String> filters) throws ParseException {
		String query = "SELECT * WHERE { ?userNode a <" + UserVocabulary.USER + "> .";
		for (String key : filters.keySet()) {
			query += " BIND('" + filters.get(key) + "' AS ?" + key + ")";
		}
		query += " ?userNode <" + UserVocabulary.FIRST_NAME + "> ?" + BINDING_FIRST_NAME + " ."
				+ " ?userNode <" + UserVocabulary.LAST_NAME + "> ?" + BINDING_LAST_NAME + " ."
				+ " ?userNode <" + UserVocabulary.PASSWORD + "> ?" + BINDING_PASSWORD + " ."
				+ " ?userNode <" + UserVocabulary.EMAIL + "> ?" + BINDING_EMAIL + " ."
				+ " OPTIONAL { ?userNode <" + UserVocabulary.ROLE + "> ?" + BINDING_ROLE + " . }"
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
		RepositoryConnection conn = repository.getConnection();
		TupleQuery tq = conn.prepareTupleQuery(query);
		TupleQueryResult result = tq.evaluate();
		// collect users
		ArrayList<STUser> list = new ArrayList<STUser>();
		while (result.hasNext()) {
			BindingSet tuple = result.next();
			STUser user = new STUser(
					tuple.getValue(BINDING_EMAIL).stringValue(),
					tuple.getValue(BINDING_PASSWORD).stringValue(),
					tuple.getValue(BINDING_FIRST_NAME).stringValue(),
					tuple.getValue(BINDING_LAST_NAME).stringValue());
			
			if (tuple.getBinding(BINDING_URL) != null) {
				user.setUrl(tuple.getValue(BINDING_URL).stringValue());
			}
			if (tuple.getBinding(BINDING_PHONE) != null) {
				user.setPhone(tuple.getValue(BINDING_PHONE).stringValue());
			}
			if (tuple.getBinding(BINDING_BIRTHDAY) != null) {
			    Date d =  dateFormat.parse(tuple.getValue(BINDING_BIRTHDAY).stringValue());
				user.setBirthday(d);
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
			    Date d =  dateFormat.parse(tuple.getValue(BINDING_REGISTRATION_DATE).stringValue());
				user.setRegistrationDate(d);
			}
			list.add(user);
		}
		
		conn.close();
		return list;
	}
	
	/**
	 * Delete the given user. 
	 * Note, since e-mail should be unique, delete the user with the same e-mail of the given user.
	 * @param user
	 */
	public void deleteUser(STUser user) {
		String query = "DELETE WHERE {"
				+ " ?user a <" + UserVocabulary.USER + "> ."
				+ " ?user <" + UserVocabulary.EMAIL + "> '" + user.getEmail() + "' ."
				+ " ?user ?p ?o . }";
		RepositoryConnection conn = repository.getConnection();
		Update update = conn.prepareUpdate(query);
		update.execute();
		conn.close();
	}
	
	public void saveUserDetailsFile(File file) {
		RepositoryConnection conn = repository.getConnection();
		
		try {
			RepositoryResult<Statement> stats = conn.getStatements(null, null, null, false);
			Model model = Iterations.addAll(stats, new LinkedHashModel());
			FileOutputStream out = new FileOutputStream(file);
			
			RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, out);
			writer.startRDF();
			for (Statement st : model) {
				writer.handleStatement(st);
			}
			writer.endRDF();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			conn.close();
		}
	}
	
}
