package it.uniroma2.art.semanticturkey.user;

import it.uniroma2.art.semanticturkey.vocabulary.UserVocabulary;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.ORG;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.query.Binding;
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
import java.util.Map.Entry;

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
	private String BINDING_AVATAR_URL = "avatarUrl";
	private String BINDING_PHONE = "phone";
	private String BINDING_AFFILIATION = "affiliation";
	private String BINDING_ADDRESS = "address";
	private String BINDING_VERIFICATION_TOKEN = "verificationToken";
	private String BINDING_ACTIVATION_TOKEN = "verificationToken";
	private String BINDING_REGISTRATION_DATE = "registrationDate";
	private String BINDING_STATUS = "status";
	private String BINDING_LANGUAGE_PROFICIENCIES = "languageProficiencies";
	private String BINDING_CUSTOM_PROP_PREFIX = "customProp";

	public UsersRepoHelper() {
		MemoryStore memStore = new MemoryStore();
		memStore.setPersist(false);
		repository = new SailRepository(memStore);
		repository.init();
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
		if (user.getAvatarUrl() != null) {
			query += " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.AVATAR_URL) + " '" + user.getAvatarUrl() + "' .";
		}
		if (user.getPhone() != null) {
			query += " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(FOAF.PHONE) + " '" + user.getPhone() + "' .";
		}
		if (user.getAffiliation() != null) {
			query += " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(ORG.MEMBER_OF) + " '" + user.getAffiliation() + "' .";
		}
		if (user.getAddress() != null) {
			query += " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.ADDRESS) + " '" + user.getAddress() + "' .";
		}
		if (user.getVerificationToken() != null) {
			query += " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.VERIFICATION_TOKEN) + " '" + user.getVerificationToken() + "' .";
		}
		if (user.getActivationToken() != null) {
			query += " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.ACTIVATION_TOKEN) + " '" + user.getActivationToken() + "' .";
		}
		for (String lang : user.getLanguageProficiencies()) {
			query += " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.LANGUAGE_PROFICIENCIES) + 
					" '" + lang + "' .";
		}
		for (Entry<IRI, String> entry : user.getCustomProperties().entrySet()) {
			query += " ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(entry.getKey()) + " '" + entry.getValue() + "' .";
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
				+ " OPTIONAL { ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.AVATAR_URL) + " ?" + BINDING_AVATAR_URL + " . }"
				+ " OPTIONAL { ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(FOAF.PHONE) + " ?" + BINDING_PHONE + " . }"
				+ " OPTIONAL { ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(ORG.MEMBER_OF) + " ?" + BINDING_AFFILIATION + " . }"
				+ " OPTIONAL { ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.ADDRESS) + " ?" + BINDING_ADDRESS + " . }"
				+ " OPTIONAL { ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.VERIFICATION_TOKEN) + " ?" + BINDING_VERIFICATION_TOKEN + " . }"
				+ " OPTIONAL { ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.ACTIVATION_TOKEN) + " ?" + BINDING_ACTIVATION_TOKEN + " . }"
				+ " OPTIONAL { ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(UserVocabulary.LANGUAGE_PROFICIENCIES) 
					+ " ?" + BINDING_LANGUAGE_PROFICIENCIES + " . }";
		for (int i = 0; i < UserForm.customFieldsProperties.size(); i++) {
			IRI prop = UserForm.customFieldsProperties.get(i);
			query += " OPTIONAL { ?" + BINDING_IRI + " " + NTriplesUtil.toNTriplesString(prop) + " ?" + BINDING_CUSTOM_PROP_PREFIX + i + " . }";
		}
		query += " }";
		
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
	
	private Collection<STUser> getUsersFromTupleResult(TupleQueryResult result) {
		// collect users
		Collection<STUser> list = new ArrayList<STUser>();

		tupleLoop: while (result.hasNext()) {
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
			if (tuple.getBinding(BINDING_AVATAR_URL) != null) {
				user.setAvatarUrl(tuple.getValue(BINDING_AVATAR_URL).stringValue());
			}
			if (tuple.getBinding(BINDING_PHONE) != null) {
				user.setPhone(tuple.getValue(BINDING_PHONE).stringValue());
			}
			if (tuple.getBinding(BINDING_AFFILIATION) != null) {
				user.setAffiliation(tuple.getValue(BINDING_AFFILIATION).stringValue());
			}
			if (tuple.getBinding(BINDING_ADDRESS) != null) {
				user.setAddress(tuple.getValue(BINDING_ADDRESS).stringValue());
			}
			if (tuple.getBinding(BINDING_VERIFICATION_TOKEN) != null) {
				user.setVerificationToken(tuple.getValue(BINDING_VERIFICATION_TOKEN).stringValue());
			}
			if (tuple.getBinding(BINDING_ACTIVATION_TOKEN) != null) {
				user.setActivationToken(tuple.getValue(BINDING_ACTIVATION_TOKEN).stringValue());
			}
			
			String lang = null;
			if (tuple.getBinding(BINDING_LANGUAGE_PROFICIENCIES) != null) {
				lang = tuple.getValue(BINDING_LANGUAGE_PROFICIENCIES).stringValue();
			}
			if (lang != null) {
				//Check if the current tuple is about a user already fetched (and differs just for the language proficiency)
				for (STUser u: list) {
					if (u.getEmail().equals(email)) {
						u.addLanguageProficiency(lang);
						continue tupleLoop;
					}
				}
				//if this line of code is reached, current user was not already fetched
				//so add the language to its proficiency and then the user to the list
				user.addLanguageProficiency(lang);
			}
			
			for (int i = 0; i < UserForm.customFieldsProperties.size(); i++) {
				String bindingName = BINDING_CUSTOM_PROP_PREFIX + i;
				if (tuple.getBinding(bindingName) != null) {
					String customPropValue = tuple.getValue(bindingName).stringValue();
					user.setCustomProperty(UserForm.customFieldsProperties.get(i), customPropValue);
				}
			}
			
			list.add(user);
		}
		return list;
	}
	
	
	/*
	 * User form fields handlers
	 */
	
	public void loadUserFormFields(File userFormFieldsFile) throws RDFParseException, RepositoryException, IOException {
		RepositoryConnection conn = repository.getConnection();
		conn.add(userFormFieldsFile, UserVocabulary.BASEURI, RDFFormat.TURTLE);
		conn.close();
	}

	public UserForm initUserForm() {
		UserForm form = new UserForm();

		/**
		 * Optional fields
		 */
		String query = "SELECT * WHERE { " +
				" VALUES ?prop { ";
		for (IRI p : UserForm.optionalFieldsProperties) {
			query += NTriplesUtil.toNTriplesString(p) + " ";
		}
		query += " } " + //close VALUES
				"?prop " + NTriplesUtil.toNTriplesString(UserVocabulary.VISIBLE_PROP) + " ?visible . " +
				" } ";
		logger.debug(query);
		try (
				RepositoryConnection conn = repository.getConnection();
				TupleQueryResult result = conn.prepareTupleQuery(query).evaluate();
		) {
			while (result.hasNext()) {
				BindingSet bs = result.next();
				Binding bsProp = bs.getBinding("prop");
				if (bsProp != null) {
					boolean visible = ((Literal) bs.getValue("visible")).booleanValue();
					form.setOptionalFieldVisibility((IRI)bsProp.getValue(), visible);
				}
			}
		}


		/**
		 * Custom fields
		 */
		query = "SELECT * WHERE { " +
			" VALUES ?prop { ";
		for (IRI p : UserForm.customFieldsProperties) {
			query += NTriplesUtil.toNTriplesString(p) + " ";
		}
		query += " } " + //close VALUES
				"?prop " + NTriplesUtil.toNTriplesString(RDFS.LABEL) + " ?label . " +
				"?prop " + NTriplesUtil.toNTriplesString(RDF.VALUE) + " ?position . " +
				"OPTIONAL { ?prop " + NTriplesUtil.toNTriplesString(SKOS.DEFINITION) + " ?descr .  }" +
				" } ";
		logger.debug(query);
		try (
				RepositoryConnection conn = repository.getConnection();
			 	TupleQueryResult result = conn.prepareTupleQuery(query).evaluate();
		) {
			while (result.hasNext()) {
				BindingSet bs = result.next();
				Binding bsProp = bs.getBinding("prop");
				if (bsProp != null) {
					String fieldLabel = bs.getValue("label").stringValue();
					int fieldPosition = ((Literal)bs.getValue("position")).intValue();
					Value descrValue = bs.getValue("descr");
					String description = descrValue != null ? descrValue.stringValue() : null;
					UserFormCustomField field = new UserFormCustomField((IRI)bsProp.getValue(), fieldPosition, fieldLabel, description);
					form.addField(field);
				}
			}
		}
		return form;
	}

	public void insertUserFormOptionalField(IRI field, boolean visibility) {
		String query = "INSERT DATA { " +
				NTriplesUtil.toNTriplesString(field) + " " +
				NTriplesUtil.toNTriplesString(UserVocabulary.VISIBLE_PROP) + " " +
				NTriplesUtil.toNTriplesString(SimpleValueFactory.getInstance().createLiteral(visibility)) + " . " +
				"}";
		//execute query
		logger.debug(query);
		try (RepositoryConnection conn = repository.getConnection()) {
			Update update = conn.prepareUpdate(query);
			update.execute();
		}
	}

	public void insertUserFormCustomField(UserFormCustomField field) {
		String query = "INSERT DATA { " +
			NTriplesUtil.toNTriplesString(field.getIri()) + " " + NTriplesUtil.toNTriplesString(RDFS.LABEL) + " '" + field.getLabel() + "' . " +
			NTriplesUtil.toNTriplesString(field.getIri()) + " " + NTriplesUtil.toNTriplesString(RDF.VALUE) + " " + field.getPosition() + " . ";
		if (field.getDescription() != null) {
			query += NTriplesUtil.toNTriplesString(field.getIri()) + " " + NTriplesUtil.toNTriplesString(SKOS.DEFINITION) + " '" + field.getDescription() + "' . ";
		}
		query += "}";
		//execute query
		logger.debug(query);
		try (RepositoryConnection conn = repository.getConnection()) {
			Update update = conn.prepareUpdate(query);
			update.execute();
		}
	}
	
	
	/*
	 * Repository utils
	 */
	
	/**
	 * Serialize the content of the repository in the given file
	 * @param file destination file for the serialization
	 * @throws IOException
	 */
	public void serializeRepoContent(File file) throws IOException {
		try (RepositoryConnection conn = repository.getConnection()) {
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
		}
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
