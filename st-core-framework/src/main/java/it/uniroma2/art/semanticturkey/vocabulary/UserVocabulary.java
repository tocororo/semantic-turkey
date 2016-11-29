package it.uniroma2.art.semanticturkey.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class UserVocabulary {
	
	public static final String URI = "http://baseuri"; //TODO choose a baseuri
	public static final String NAMESPACE = URI + "#";
	
	// User
	public static final String USER = NAMESPACE + "User";
	public static final String FIRST_NAME = NAMESPACE + "firstName";
	public static final String LAST_NAME = NAMESPACE + "lastName";
	public static final String PASSWORD = NAMESPACE + "password";
	public static final String EMAIL = NAMESPACE + "email";
	public static final String URL = NAMESPACE + "namespace";
	public static final String PHONE = NAMESPACE + "phone";
	public static final String BIRTHDAY = NAMESPACE + "birthday";
	public static final String GENDER = NAMESPACE + "gender";
	public static final String AFFILIATION = NAMESPACE + "affiliation";
	public static final String COUNTRY = NAMESPACE + "country";
	public static final String ADDRESS = NAMESPACE + "address";
	public static final String REGISTRATION_DATE = NAMESPACE + "registration_date";
	public static final String STATUS = NAMESPACE + "status";
	
	//Role
	public static final String ROLE = NAMESPACE + "Role";
	public static final String ROLE_NAME = NAMESPACE + "roleName";
	public static final String CAPABILITY = NAMESPACE + "capability";
	
	//Binding
	public static final String BINDING = NAMESPACE + "Binding";
	public static final String ROLE_PROP = NAMESPACE + "role";
	public static final String USER_PROP = NAMESPACE + "user";
	public static final String PROJECT = NAMESPACE + "project";
	
	
	public static class Res {
    	public static IRI URI;
    	public static IRI USER;
    	public static IRI FIRST_NAME;
    	public static IRI LAST_NAME;
    	public static IRI PASSWORD;
    	public static IRI EMAIL;
    	public static IRI URL;
    	public static IRI PHONE;
    	public static IRI BIRTHDAY;
    	public static IRI GENDER;
    	public static IRI AFFILIATION;
    	public static IRI COUNTRY;
    	public static IRI ADDRESS;
    	public static IRI REGISTRATION_DATE;
    	public static IRI STATUS;
    	
    	public static IRI ROLE;
    	public static IRI ROLE_NAME;
    	public static IRI CAPABILITY;
    	
    	public static IRI BINDING;
    	public static IRI ROLE_PROP;
    	public static IRI USER_PROP;
    	public static IRI PROJECT;
        
		static {
			ValueFactory fact = SimpleValueFactory.getInstance();
			URI = fact.createIRI(UserVocabulary.URI);
			USER = fact.createIRI(UserVocabulary.USER);
			FIRST_NAME = fact.createIRI(UserVocabulary.FIRST_NAME);
	    	LAST_NAME = fact.createIRI(UserVocabulary.LAST_NAME);
	    	PASSWORD = fact.createIRI(UserVocabulary.PASSWORD);
	    	EMAIL = fact.createIRI(UserVocabulary.EMAIL);
	    	URL = fact.createIRI(UserVocabulary.URL);
	    	PHONE = fact.createIRI(UserVocabulary.PHONE);
	    	BIRTHDAY = fact.createIRI(UserVocabulary.BIRTHDAY);
	    	GENDER = fact.createIRI(UserVocabulary.GENDER);
	    	AFFILIATION = fact.createIRI(UserVocabulary.AFFILIATION);
	    	COUNTRY = fact.createIRI(UserVocabulary.COUNTRY);
	    	ADDRESS = fact.createIRI(UserVocabulary.ADDRESS);
	    	REGISTRATION_DATE = fact.createIRI(UserVocabulary.REGISTRATION_DATE);
	    	STATUS = fact.createIRI(UserVocabulary.STATUS);
	    	
	    	ROLE = fact.createIRI(UserVocabulary.ROLE);
	    	ROLE_NAME = fact.createIRI(UserVocabulary.ROLE_NAME);
	    	CAPABILITY = fact.createIRI(UserVocabulary.CAPABILITY);
	    	
	    	BINDING = fact.createIRI(UserVocabulary.BINDING);
	    	ROLE_PROP = fact.createIRI(UserVocabulary.ROLE_PROP);
	    	USER_PROP = fact.createIRI(UserVocabulary.USER_PROP);
	    	PROJECT = fact.createIRI(UserVocabulary.PROJECT);
		}
		
	}

}
