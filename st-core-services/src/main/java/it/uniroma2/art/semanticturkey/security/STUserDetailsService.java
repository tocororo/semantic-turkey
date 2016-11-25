package it.uniroma2.art.semanticturkey.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * @author Tiziano
 * This class handles the user authentication.
 * It's an alternative solution for STAuthenticationProvider and it allows to use remember-me feature
 * (that AuthenticationProvider doesn't support)
 * (Referenced in WEB-INF/spring-security.xml)
 */
public class STUserDetailsService implements UserDetailsService {

	@Autowired
	UsersManager userMgr;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		STUser user;
		user = userMgr.getUserByEmail(username);
		if (user != null) {
			return user;
		} else {
			throw new UsernameNotFoundException("User with e-mail address '" + username + "' not found");
		}
	}
	
}

