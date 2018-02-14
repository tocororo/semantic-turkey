package it.uniroma2.art.semanticturkey.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public class UserVocabulary {
	
	public static final String BASEURI = "http://semanticturkey.uniroma2.it";
	public static final String NAMESPACE = BASEURI + "#";
	
	//NAMESPACE FOR MOST TERMS IN THE VOCABULARY
	public static final String PUVOC_NS = BASEURI + "/puvoc#";
    
    //e.g. project "Test" will be: "http://semanticturkey.uniroma2.it/projects/Test"
    public static final String PROJECTSBASEURI = BASEURI + "/projects/";
    
    //e.g. user "eehsdksdlksdk" will be: "http://semanticturkey.uniroma2.it/users/eehsdksdlksdk" 
    public static final String USERSBASEURI = BASEURI + "/users/";
    
    //e.g. role administrator will be: "http://semanticturkey.uniroma2.it/roles/administrator"
    //custom roles local to project will be: http://semanticturkey.uniroma2.it/roles/<projectname>/<roleName>
    public static final String ROLESBASEURI = BASEURI + "/roles/";

	public static final IRI USER;
	public static final IRI PASSWORD;
	public static final IRI URL;
	public static final IRI AVATAR_URL;
	public static final IRI COUNTRY;
	public static final IRI ADDRESS;
	public static final IRI REGISTRATION_DATE;
	public static final IRI STATUS;
	public static final IRI LANGUAGE_PROFICIENCIES;
	
	public static final IRI BINDING;
	public static final IRI ROLE_PROP;
	public static final IRI LANGUAGE_PROP;
	public static final IRI USER_PROP;
	public static final IRI PROJECT;
    
	static {
		ValueFactory fact = SimpleValueFactory.getInstance();
		USER = fact.createIRI(UserVocabulary.NAMESPACE, "User");
    	PASSWORD = fact.createIRI(UserVocabulary.PUVOC_NS, "password");
    	URL = fact.createIRI(UserVocabulary.PUVOC_NS, "url");
    	AVATAR_URL = fact.createIRI(UserVocabulary.PUVOC_NS, "avatar_url");
    	COUNTRY = fact.createIRI(UserVocabulary.PUVOC_NS, "country");
    	ADDRESS = fact.createIRI(UserVocabulary.PUVOC_NS, "address");
    	REGISTRATION_DATE = fact.createIRI(UserVocabulary.PUVOC_NS, "registration_date");
    	STATUS = fact.createIRI(UserVocabulary.PUVOC_NS, "status");
    	LANGUAGE_PROFICIENCIES = fact.createIRI(UserVocabulary.PUVOC_NS, "language_proficencies");
    	
    	BINDING = fact.createIRI(UserVocabulary.PUVOC_NS, "Binding");
    	ROLE_PROP = fact.createIRI(UserVocabulary.PUVOC_NS, "role");
    	LANGUAGE_PROP = fact.createIRI(UserVocabulary.PUVOC_NS, "language");
    	USER_PROP = fact.createIRI(UserVocabulary.PUVOC_NS, "user");
    	PROJECT = fact.createIRI(UserVocabulary.PUVOC_NS, "project");
	}

}
