package it.uniroma2.art.semanticturkey.user;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.resources.Config;
import it.uniroma2.art.semanticturkey.vocabulary.UserVocabulary;

public class STUser implements UserDetails {

	private static final long serialVersionUID = -5621952496841740959L;
	
	private IRI iri;
	private String givenName;
	private String familyName;
	private String password; //encoded
	private String email;
	private Collection<GrantedAuthority> authorities;
	private String url;
	private String phone;
	private Date birthday;
	private String gender;
	private String affiliation;
	private String country;
	private String address;
	private Date registrationDate;
	private UserStatus status;
	
	public static String USER_DATE_FORMAT = "yyyy-MM-dd";
	
	public STUser(String email, String password, String givenName, String familyName) {
		IRI iri = SimpleValueFactory.getInstance().createIRI(UserVocabulary.USERSBASEURI, email);
		this.iri = iri;
		this.email = email;
		this.password = password;
		this.givenName = givenName;
		this.familyName = familyName;
		this.authorities = new ArrayList<GrantedAuthority>();
		this.status = UserStatus.NEW;
	}
	
	public STUser(IRI iri, String email, String password, String givenName, String familyName) {
		this.iri = iri;
		this.email = email;
		this.password = password;
		this.givenName = givenName;
		this.familyName = familyName;
		this.authorities = new ArrayList<GrantedAuthority>();
		this.status = UserStatus.NEW;
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
	
	public String getPhone() {
		return phone;
	}
	
	public void setPhone(String phone) {
		this.phone = phone;
	}
	
	public Date getBirthday() {
		return birthday;
	}
	
	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}
	
	public void setBirthday(String birthday) throws ParseException {
		this.birthday = new SimpleDateFormat(USER_DATE_FORMAT).parse(birthday);
	}
	
	public String getGender() {
		return gender;
	}
	
	public void setGender(String gender) {
		this.gender = gender;
	}
	
	public String getAffiliation() {
		return affiliation;
	}
	
	public void setAffiliation(String affiliation) {
		this.affiliation = affiliation;
	}
	
	public String getCountry() {
		return country;
	}
	
	public void setCountry(String country) {
		this.country = country;
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
	
	public boolean isAdmin() {
		//check every time in order to returns the correct boolean even if the admin email address changes
		return Config.getEmailAdminAddress().equals(email);
	}
	
	public ObjectNode getAsJsonObject() {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode userJson = jsonFactory.objectNode();
		userJson.set("email", jsonFactory.textNode(email));
		userJson.set("iri", jsonFactory.textNode(iri.stringValue()));
		userJson.set("givenName", jsonFactory.textNode(givenName));
		userJson.set("familyName", jsonFactory.textNode(familyName));
		if (birthday != null) {
			userJson.set("birthday", jsonFactory.textNode(new SimpleDateFormat(STUser.USER_DATE_FORMAT).format(birthday)));
		} else {
			userJson.set("birthday", null); //empty field
		}
		userJson.set("gender", jsonFactory.textNode(gender));
		userJson.set("country", jsonFactory.textNode(country));
		userJson.set("address", jsonFactory.textNode(address));
		//skip test registrationDate != null because registrationDate is automatically set during registration
		userJson.set("registrationDate", jsonFactory.textNode(new SimpleDateFormat(STUser.USER_DATE_FORMAT).format(registrationDate)));
		userJson.set("affiliation", jsonFactory.textNode(affiliation));
		userJson.set("url", jsonFactory.textNode(url));
		userJson.set("phone", jsonFactory.textNode(phone));
		userJson.set("status", jsonFactory.textNode(status.name()));
		userJson.set("admin", jsonFactory.booleanNode(isAdmin()));
		
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
		s += "\nPhone: " + this.phone;
		if (this.birthday != null) {
			s += "\nBirthday: " + dateFormat.format(this.birthday);
		} else {
			s += "\nBirthday: " + this.birthday;
		}
		s += "\nGender: " + this.gender;
		s += "\nAffiliation: " + this.affiliation;
		s += "\nCountry: " + this.country;
		s += "\nAddress: " + this.address;
		s += "\nRegistraion date: " + dateFormat.format(this.registrationDate);
		s += "\nStatus: " + this.status;
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

}
