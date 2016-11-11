package it.uniroma2.art.semanticturkey.security;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserPermissionsEnum;
import it.uniroma2.art.semanticturkey.user.UserRolesEnum;

/**
 * @author Tiziano
 * This class handles the user authentication (retrieves authorities for a given username-password pair)
 * It's an alternative solution for STAuthenticationProvider and it allows to use remeber-me feature
 * (that AuthenticationProvider doesn't support)
 * (Referenced in WEB-INF/spring-security.xml)
 */
public class STUserDetailsService implements UserDetailsService {
	
	@Autowired
	UserManager userMgr;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		STUser user;
		try {
			user = userMgr.getUserByEmail(username);
			if (user != null) {
				addAuthoritiesToUser(user);
				return user;
			} else {
				throw new UsernameNotFoundException("username " + username+ " not found");
			}
		} catch (ParseException e) {
			throw new UsernameNotFoundException("Error retrieving user " + username);
		}
	}
	
	private void addAuthoritiesToUser(STUser user) {
		Collection<UserRolesEnum> roles = user.getRoles();
		System.out.println("Role for " + user.getEmail());
		for (UserRolesEnum r : roles) {
			System.out.println("\t" + r.name());
			System.out.println("\tPermissions:");
			List<UserPermissionsEnum> permissions = PermissionManager.getPermissionsForRole(r);
			for (UserPermissionsEnum p : permissions) {
				System.out.println("\t\t" + p.name());
				user.addAuthority(new SimpleGrantedAuthority(p.name()));
			}
		}
	}

}

