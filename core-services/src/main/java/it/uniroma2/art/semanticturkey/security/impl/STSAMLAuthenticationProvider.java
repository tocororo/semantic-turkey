package it.uniroma2.art.semanticturkey.security.impl;

import org.opensaml.common.SAMLException;
import org.opensaml.common.SAMLRuntimeException;
import org.opensaml.xml.encryption.DecryptionException;
import org.opensaml.xml.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.saml.SAMLAuthenticationProvider;
import org.springframework.security.saml.SAMLAuthenticationToken;
import org.springframework.security.saml.SAMLConstants;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.context.SAMLMessageContext;

import java.util.Collection;
import java.util.Date;

/**
 * This is a custom SAMLAuthenticationProvider that returns a UsernamePasswordAuthenticationToken
 * without expiration (instead of a ExpiringUsernameAuthenticationToken as in the extended class).
 * This is needed in order to prevent the user to be kicked out from ST once the assertion
 * received from the IDP has an expiration time (NotOnOrAfter attribute) even if the user
 * is active during the session.
 */
public class STSAMLAuthenticationProvider extends SAMLAuthenticationProvider {

    private final Logger log = LoggerFactory.getLogger(STSAMLAuthenticationProvider.class);

    private boolean excludeCredential = false;

    /**
     * This is the only method that differs from the extended class:
     * The returned result is a UsernamePasswordAuthenticationToken and not an ExpiringUsernameAuthenticationToken.
     * In this way, the expiration included in the assertion is ignored.
     */
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        if (!supports(authentication.getClass())) {
            throw new IllegalArgumentException("Only SAMLAuthenticationToken is supported, " + authentication.getClass() + " was attempted");
        }

        SAMLAuthenticationToken token = (SAMLAuthenticationToken) authentication;
        SAMLMessageContext context = token.getCredentials();

        if (context == null) {
            throw new AuthenticationServiceException("SAML message context is not available in the authentication token");
        }

        SAMLCredential credential;

        try {
            if (SAMLConstants.SAML2_WEBSSO_PROFILE_URI.equals(context.getCommunicationProfileId())) {
                credential = consumer.processAuthenticationResponse(context);
            } else if (SAMLConstants.SAML2_HOK_WEBSSO_PROFILE_URI.equals(context.getCommunicationProfileId())) {
                credential = hokConsumer.processAuthenticationResponse(context);
            } else {
                throw new SAMLException("Unsupported profile encountered in the context " + context.getCommunicationProfileId());
            }
        } catch (SAMLRuntimeException e) {
            log.debug("Error validating SAML message", e);
            samlLogger.log(SAMLConstants.AUTH_N_RESPONSE, SAMLConstants.FAILURE, context, e);
            throw new AuthenticationServiceException("Error validating SAML message", e);
        } catch (SAMLException e) {
            log.debug("Error validating SAML message", e);
            samlLogger.log(SAMLConstants.AUTH_N_RESPONSE, SAMLConstants.FAILURE, context, e);
            throw new AuthenticationServiceException("Error validating SAML message", e);
        } catch (ValidationException e) {
            log.debug("Error validating signature", e);
            samlLogger.log(SAMLConstants.AUTH_N_RESPONSE, SAMLConstants.FAILURE, context, e);
            throw new AuthenticationServiceException("Error validating SAML message signature", e);
        } catch (org.opensaml.xml.security.SecurityException e) {
            log.debug("Error validating signature", e);
            samlLogger.log(SAMLConstants.AUTH_N_RESPONSE, SAMLConstants.FAILURE, context, e);
            throw new AuthenticationServiceException("Error validating SAML message signature", e);
        } catch (DecryptionException e) {
            log.debug("Error decrypting SAML message", e);
            samlLogger.log(SAMLConstants.AUTH_N_RESPONSE, SAMLConstants.FAILURE, context, e);
            throw new AuthenticationServiceException("Error decrypting SAML message", e);
        }

        Object userDetails = getUserDetails(credential);
        Object principal = getPrincipal(credential, userDetails);
        Collection<? extends GrantedAuthority> entitlements = getEntitlements(credential, userDetails);

        SAMLCredential authenticationCredential = excludeCredential ? null : credential;

        //Commented these two line of code in order to ignore the expiration contained in the assertion
//        Date expiration = getExpirationDate(credential);
//        ExpiringUsernameAuthenticationToken result = new ExpiringUsernameAuthenticationToken(expiration, principal, authenticationCredential, entitlements);
        UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(principal, authenticationCredential, entitlements);

        result.setDetails(userDetails);

        samlLogger.log(SAMLConstants.AUTH_N_RESPONSE, SAMLConstants.SUCCESS, context, result, null);

        return result;

    }

}

