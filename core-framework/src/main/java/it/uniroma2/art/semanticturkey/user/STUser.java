package it.uniroma2.art.semanticturkey.user;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.vocabulary.UserVocabulary;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class STUser implements UserDetails {

	private static final long serialVersionUID = -5621952496841740959L;
	
	private IRI iri;
	private String givenName;
	private String familyName;
	private String password; //encoded
	private String email;
	private Collection<GrantedAuthority> authorities;
	private String url;
	private String avatarUrl;
	private String phone;
	private String affiliation;
	private String address;
	private Date registrationDate;
	private UserStatus status;
	private Collection<String> languageProficiencies;
	private Map<IRI, String> customProps;
	private String verificationToken;
	private String activationToken;

	public static String USER_DATE_FORMAT = "yyyy-MM-dd";
	
	public STUser(String email, String password, String givenName, String familyName) {
		this(generateUserIri(email), email, password, givenName, familyName);
	}
	
	public STUser(IRI iri, String email, String password, String givenName, String familyName) {
		this.iri = iri;
		this.email = email;
		this.password = password;
		this.givenName = givenName;
		this.familyName = familyName;
		this.authorities = new ArrayList<>();
		this.status = UserStatus.NEW;
		this.languageProficiencies = new ArrayList<>();
		this.customProps = new HashMap<>();
	}
	
	public IRI getIRI() {
		return iri;
	}
	
	public void setIRI(IRI iri) {
		this.iri = iri;
	}
	
	public String getGivenName() {
		return givenName;
	}
	
	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}
	
	public String getFamilyName() {
		return familyName;
	}
	
	public void setFamilyName(String familyName) {
		this.familyName = familyName;
	}
	
	@Override
	public String getUsername() {
		return email;
	}
	
	public String getEmail() {
		return this.getUsername();
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	@Override
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}
	
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return this.status.equals(UserStatus.ACTIVE);
	}
	
	public String getUrl() {
		return url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getAvatarUrl() {
		return avatarUrl;
	}
	
	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}
	
	public String getPhone() {
		return phone;
	}
	
	public void setPhone(String phone) {
		this.phone = phone;
	}
	
	public String getAffiliation() {
		return affiliation;
	}
	
	public void setAffiliation(String affiliation) {
		this.affiliation = affiliation;
	}
	
	public String getAddress() {
		return address;
	}
	
	public void setAddress(String address) {
		this.address = address;
	}
	
	public Date getRegistrationDate() {
		return registrationDate;
	}
	
	public void setRegistrationDate(Date registrationDate) {
		this.registrationDate = registrationDate;
	}
	
	public UserStatus getStatus() {
		return this.status;
	}
	
	public void setStatus(UserStatus status) {
		this.status = status;
	}
	
	public Collection<String> getLanguageProficiencies() {
		return languageProficiencies;
	}
	
	public void addLanguageProficiency(String lang) {
		this.languageProficiencies.add(lang);
	}

	public void setLanguageProficiencies(Collection<String> languageProficiencies) {
		this.languageProficiencies = languageProficiencies;
	}
	
	public Map<IRI, String> getCustomProperties() {
		return customProps;
	}
	
	public void setCustomProperty(IRI prop, String value) {
		if (value == null) {
			customProps.remove(prop);
		} else {
			customProps.put(prop, value);
		}
	}
	
	public void removeCustomProperty(IRI prop) {
		customProps.remove(prop);
	}

	public String getVerificationToken() {
		return verificationToken;
	}

	public void setVerificationToken(String verificationToken) {
		this.verificationToken = verificationToken;
	}

	public String getActivationToken() {
		return activationToken;
	}

	public void setActivationToken(String activationToken) {
		this.activationToken = activationToken;
	}

	public boolean isAdmin() {
		boolean isAdmin = false;
		try {
			isAdmin = UsersManager.getAdminEmailList().contains(this.email);
		} catch (STPropertyAccessException e) {}
		return isAdmin;
	}
	
	public ObjectNode getAsJsonObject() {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode userJson = jsonFactory.objectNode();
		userJson.set("email", jsonFactory.textNode(email));
		userJson.set("iri", jsonFactory.textNode(iri.stringValue()));
		userJson.set("givenName", jsonFactory.textNode(givenName));
		userJson.set("familyName", jsonFactory.textNode(familyName));
		userJson.set("address", jsonFactory.textNode(address));
		//skip test registrationDate != null because registrationDate is automatically set during registration
		userJson.set("registrationDate", jsonFactory.textNode(new SimpleDateFormat(STUser.USER_DATE_FORMAT).format(registrationDate)));
		userJson.set("affiliation", jsonFactory.textNode(affiliation));
		userJson.set("url", jsonFactory.textNode(url));
		userJson.set("avatarUrl", jsonFactory.textNode(avatarUrl));
		userJson.set("phone", jsonFactory.textNode(phone));
		userJson.set("status", jsonFactory.textNode(status.name()));
		userJson.set("admin", jsonFactory.booleanNode(isAdmin()));
		
		ArrayNode langsArrayNode = jsonFactory.arrayNode();
		for (String l : languageProficiencies) {
			langsArrayNode.add(l);
		}
		userJson.set("languageProficiencies", langsArrayNode);
		
		ArrayNode customPropsArrayNode = jsonFactory.arrayNode();
		for (IRI prop: UserForm.customFieldsProperties) {
			UserFormCustomField field = UsersManager.getUserForm().getCustomField(prop);
			if (field != null) {
				ObjectNode customPropNode = jsonFactory.objectNode();
				customPropNode.set("iri", jsonFactory.textNode(field.getIri().stringValue()));
				customPropNode.set("value", jsonFactory.textNode(customProps.get(prop)));
				customPropsArrayNode.add(customPropNode);
			}
		}
		userJson.set("customProperties", customPropsArrayNode);
		
		return userJson;
	}
	
	public String toString() {
		DateFormat dateFormat = new SimpleDateFormat(STUser.USER_DATE_FORMAT);
		String s = "";
		s += "Given Name: " + this.givenName;
		s += "\nFamily Name: " + this.familyName;
		s += "\nPassword: " + this.password;
		s += "\ne-mail: " + this.email;
		s += "\nUrl: " + this.url;
		s += "\navatarUrl: " + this.avatarUrl;
		s += "\nPhone: " + this.phone;
		s += "\nAffiliation: " + this.affiliation;
		s += "\nAddress: " + this.address;
		s += "\nRegistraion date: " + dateFormat.format(this.registrationDate);
		s += "\nStatus: " + this.status;
		s += "\nLanguage proficiencies: " + this.languageProficiencies;
		s += "\nisAdmin: " + isAdmin();
		s += "\n";
		return s;
	}
	
	public static String encodeUserIri(IRI iri) {
		String fileNameCompliantCharacters = "([^ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._-])+";
		String replaced = iri.stringValue();
		replaced = replaced.substring(replaced.indexOf("://")+3);
		replaced = replaced.replaceAll(fileNameCompliantCharacters, ".");
		try {
			replaced = URLEncoder.encode(replaced, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return replaced;
	}

	public static IRI generateUserIri(String email) {
		return SimpleValueFactory.getInstance().createIRI(UserVocabulary.USERSBASEURI, email);
	}

}
