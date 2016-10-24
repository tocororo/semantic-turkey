package it.uniroma2.art.semanticturkey.user;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class STUser implements UserDetails {

	private static final long serialVersionUID = -5621952496841740959L;
	
	private String firstName;
	private String lastName;
	private String password; //encoded
	private String email;
	private Collection<GrantedAuthority> authorities;
//	private String affiliation;
//	private String country;
//	private Date registrationDate;
	
	public STUser(String email, String password, String firstName, String lastName) {
		this.email = email;
		this.password = password;
		this.firstName = firstName;
		this.lastName = lastName;
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
	
	public void setUsername(String email) {
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
	
//	public String getAffiliation() {
//		return affiliation;
//	}
//	
//	public void setAffiliation(String affiliation) {
//		this.affiliation = affiliation;
//	}
//	
//	public String getCountry() {
//		return country;
//	}
//	
//	public void setCountry(String country) {
//		this.country = country;
//	}
//	
//	public Date getRegistrationDate() {
//		return registrationDate;
//	}
//	
//	public void setRegistrationDate(Date registrationDate) {
//		this.registrationDate = registrationDate;
//	}

}
