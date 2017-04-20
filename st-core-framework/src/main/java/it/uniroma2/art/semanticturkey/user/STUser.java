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
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import it.uniroma2.art.semanticturkey.vocabulary.UserVocabulary;

public class STUser implements UserDetails {

	private static final long serialVersionUID = -5621952496841740959L;
	
	private IRI iri;
	private String firstName;
	private String lastName;
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
	
	public STUser(String email, String password, String firstName, String lastName) {
		IRI iri = SimpleValueFactory.getInstance().createIRI(UserVocabulary.USERSBASEURI, encodeUserEmail(email));
		this.iri = iri;
		this.email = email;
		this.password = password;
		this.firstName = firstName;
		this.lastName = lastName;
		this.authorities = new ArrayList<GrantedAuthority>();
		this.status = UserStatus.REGISTERED;
	}
	
	public STUser(IRI iri, String email, String password, String firstName, String lastName) {
		this.iri = iri;
		this.email = email;
		this.password = password;
		this.firstName = firstName;
		this.lastName = lastName;
		this.authorities = new ArrayList<GrantedAuthority>();
		this.status = UserStatus.REGISTERED;
	}
	
	public IRI getIRI() {
		return iri;
	}
	
	public void setIRI(IRI iri) {
		this.iri = iri;
	}
	
	public String getFirstName() {
		return firstName;
	}
	
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	
	public String getLastName() {
		return lastName;
	}
	
	public void setLastName(String lastName) {
		this.lastName = lastName;
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
		return this.status.equals(UserStatus.ENABLED);
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
	
	public JSONObject getAsJSONObject() throws JSONException {
		JSONObject userJson = new JSONObject();
		userJson.put("email", email);
		userJson.put("firstName", firstName);
		userJson.put("lastName", lastName);
		if (birthday != null) {
			userJson.put("birthday", new SimpleDateFormat(STUser.USER_DATE_FORMAT).format(birthday));
		} else {
			userJson.put("birthday", birthday); //empty field
		}
		userJson.put("gender", gender);
		userJson.put("country", country);
		userJson.put("address", address);
		//skip test registrationDate != null because registrationDate is automatically set during registration
		userJson.put("registrationDate", new SimpleDateFormat(STUser.USER_DATE_FORMAT).format(registrationDate));
		userJson.put("affiliation", affiliation);
		userJson.put("url", url);
		userJson.put("phone", phone);
		userJson.put("status", status);
		
		return userJson;
	}
	
	public String toString() {
		DateFormat dateFormat = new SimpleDateFormat(STUser.USER_DATE_FORMAT);
		String s = "";
		s += "First Name: " + this.firstName;
		s += "\nLast Name: " + this.lastName;
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
		s += "\n";
		return s;
	}
	
	public static String encodeUserEmail(String email) {
		String encodedEMail = "";
		try {
			encodedEMail = URLEncoder.encode(email, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return encodedEMail;
	}

}
