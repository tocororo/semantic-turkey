package it.uniroma2.art.semanticturkey.security.impl;

import it.uniroma2.art.semanticturkey.security.STUserDetailsService;
import it.uniroma2.art.semanticturkey.user.STUser;
import it.uniroma2.art.semanticturkey.user.UserException;
import it.uniroma2.art.semanticturkey.user.UserNotFoundException;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;

public class SAMLUserDetails implements SAMLUserDetailsService {

    @Autowired
    STUserDetailsService stUserDetailsService;

   /* @Override
    public SAMLCredential loadUserBySAML(SAMLCredential cred) throws UsernameNotFoundException {
        // this is set as details of the Authentication object ( where the principal is saml user id )
        return cred;
    }
    */

    @Override
    public STUser loadUserBySAML(SAMLCredential cred) throws UsernameNotFoundException {
        String userEmail = cred.getAttributeAsString("emailAddress");
        STUser loggedUser = null;
        try {
            loggedUser = UsersManager.getUser(userEmail);
            UserDetails userDetails = stUserDetailsService.loadUserByUsername(loggedUser.getEmail());
            Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(), userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (UserException u) {
            String firstName = cred.getAttributeAsString("firstName");
            String lastName = cred.getAttributeAsString("lastName");
            loggedUser = new STUser(userEmail, null, firstName, lastName);
            STUser.SamlLevel samlLevel = UsersManager.listUsers().isEmpty() ? STUser.SamlLevel.LEV_1 : STUser.SamlLevel.LEV_2;
            loggedUser.setSamlLevel(samlLevel);
            Authentication auth = new UsernamePasswordAuthenticationToken(loggedUser, loggedUser.getPassword(), null);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        // this logged user is set as details of the Authentication object ( where the principal is saml user id )
        return loggedUser;
    }
}
