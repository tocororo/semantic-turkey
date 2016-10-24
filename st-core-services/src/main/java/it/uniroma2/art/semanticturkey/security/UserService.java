package it.uniroma2.art.semanticturkey.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import it.uniroma2.art.semanticturkey.exceptions.InvalidAccountDataException;
import it.uniroma2.art.semanticturkey.user.STUser;

@Component
public class UserService {
	
	private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	
//	private Map<String, List<String>> users;
	
	private List<STUser> userList;
	
	
	public UserService() {
//		List<String> userRoles;
//		users = new HashMap<String, List<String>>();
//		//user
//		userRoles = new ArrayList<String>();
//		userRoles.add("ROLE_USER");
//		users.put("user", userRoles);
//		//admin
//		userRoles = new ArrayList<String>();
//		userRoles.add("ROLE_USER");
//		userRoles.add("ROLE_ADMIN");
//		users.put("admin", userRoles);
		
		userList = new ArrayList<>();
		Collection<GrantedAuthority> grantedAuths;
		//tiziano
		STUser tiziano = new STUser(
				"tiziano.lorenzetti@gmail.com",
				passwordEncoder.encode("tiziano"),
				"Tiziano", "Lorenzetti");
		grantedAuths = new ArrayList<>();
		grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));
		tiziano.addAuthorities(grantedAuths);
		userList.add(tiziano);
		//admin
		STUser admin = new STUser(
				"admin@admin.com",
				passwordEncoder.encode("admin"),
				"Admin", "Admin");
		grantedAuths = new ArrayList<>();
		grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));
		grantedAuths.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
		admin.addAuthorities(grantedAuths);
		userList.add(admin);
	}
	
	/**
	 * Returns the user with the given email. Null if no user with that email exists.
	 * TODO at the moment simulate the search in a static map, in the future this should look for 
	 * user in a dynamic and external data source
	 * @param username
	 * @return
	 */
	public STUser findUser(String email) {
		STUser user = null;
		for (int i = 0; i < userList.size(); i++) {
			if (userList.get(i).getUsername().equals(email)) {
				user = userList.get(i);
				break;
			}
		}
		return user;
	}
	
	public void registerUser(String email, String password, String firstName, String lastName) throws InvalidAccountDataException {
		//check if there is an existing user with the same email
		for (int i = 0; i < userList.size(); i++) {
			if (userList.get(i).getUsername().equals(email)) {
				//user with same email already exists, throw new Exception...
				throw new InvalidAccountDataException("E-mail address " + email + " already used");
			}
		}
		STUser newUser = new STUser(
				email,
				passwordEncoder.encode(password),
				firstName, lastName);
		Collection<GrantedAuthority> grantedAuths = new ArrayList<>();
		grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));
		newUser.addAuthorities(grantedAuths);
		userList.add(newUser);
		//TODO send mail message to the given email address
	}

}
