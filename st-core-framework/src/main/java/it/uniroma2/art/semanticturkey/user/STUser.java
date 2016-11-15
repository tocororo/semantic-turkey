package it.uniroma2.art.semanticturkey.user;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class STUser implements UserDetails {

	private static final long serialVersionUID = -5621952496841740959L;
	
	private String firstName;
	private String lastName;
	private String password; //encoded
	private String email;
	private Collection<UserRolesEnum> roles;
	private Collection<GrantedAuthority> authorities;
	private String url;
	private String phone;
	private Date birthday;
	private String gender;
	private String affiliation;
	private String country;
	private String address;
	private Date registrationDate;
	
	public static String USER_DATE_FORMAT = "yyyy-MM-dd";
	
	public STUser(String email, String password, String firstName, String lastName) {
		this.email = email;
		this.password = password;
		this.firstName = firstName;
		this.lastName = lastName;
		roles = new ArrayList<UserRolesEnum>();
		authorities = new ArrayList<GrantedAuthority>();
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
	
	public void addAuthorities(Collection<GrantedAuthority> authorities) {
		this.authorities.addAll(authorities);
	}
	
	public void addAuthority(GrantedAuthority authority) {
		this.authorities.add(authority);
	}
	
	public Collection<UserRolesEnum> getRoles() {
		return this.roles;
	}
	
	public void addRole(UserRolesEnum role) {
		if (!this.roles.contains(role)) {
			this.roles.add(role);
		}
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
		return true;
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
	
	public JSONObject getAsJSONObject() throws JSONException {
		JSONObject userJson = new JSONObject();
		userJson.put("email", email);
		userJson.put("firstName", firstName);
		userJson.put("lastName", lastName);
		
		List<String> roleList = new ArrayList<String>();
		for (UserRolesEnum r : roles) {
			roleList.add(r.name());
		}
		userJson.put("roles", roleList);
		
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
		
		return userJson;
	}
	
	public String toString() {
		DateFormat dateFormat = new SimpleDateFormat(STUser.USER_DATE_FORMAT);
		String s = "";
		s += "First Name: " + this.firstName;
		s += "\nLast Name: " + this.lastName;
		s += "\nPassword: " + this.password;
		s += "\ne-mail: " + this.email;
		s += "\nroles:";
		for (UserRolesEnum r : roles) {
			s += " " + r.name();
		}
		s += "\nauthorities:";
		Iterator<GrantedAuthority> itAuth = authorities.iterator();
		while (itAuth.hasNext()) {
			s += " " + itAuth.next().getAuthority();
		}
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
		if (this.registrationDate != null) {
			s += "\nRegistraion date: " + dateFormat.format(this.registrationDate);
		} else {
			s += "\nRegistraion date: " + this.registrationDate;
		}
		s += "\n";
		return s;
	}

}
