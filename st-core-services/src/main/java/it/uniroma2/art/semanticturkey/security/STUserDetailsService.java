package it.uniroma2.art.semanticturkey.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import it.uniroma2.art.semanticturkey.user.STUser;

/**
 * @author Tiziano
 * This class handles the user authentication (retrieves authorities for a given username-password pair)
 * It's an alternative solution for STAuthenticationProvider and it allows to use remeber-me feature
 * (that AuthenticationProvider doesn't support)
 * (Referenced in WEB-INF/spring-security.xml)
 */
public class STUserDetailsService implements UserDetailsService {
	
	@Autowired
	UserService userService;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		STUser user = userService.findUser(username);
		if (user != null) {
			return user;
		} else {
			throw new UsernameNotFoundException("username " + username+ " not found");
		}
	}

}
