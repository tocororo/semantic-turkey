package it.uniroma2.art.semanticturkey.security.impl;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler

import org.opensaml.common.SAMLException;
import org.opensaml.saml2.core.*;
import org.springframework.security.saml.context.SAMLMessageContext;
import org.springframework.security.saml.websso.WebSSOProfileConsumerImpl;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class WebSSOProfileConsumerImplCustom extends WebSSOProfileConsumerImpl {

    @Override
    protected void verifyAssertionConditions(Conditions conditions, SAMLMessageContext context, boolean audienceRequired) throws SAMLException {
        if (!audienceRequired || conditions != null && conditions.getAudienceRestrictions().size() != 0) {
            if (conditions != null) {
                if (conditions.getNotBefore() != null && conditions.getNotBefore().minusSeconds(this.getResponseSkew()).isAfterNow()) {
                    throw new SAMLException("Assertion is not yet valid, invalidated by condition notBefore " + conditions.getNotBefore());
                } else if (conditions.getNotOnOrAfter() != null && conditions.getNotOnOrAfter().plusSeconds(this.getResponseSkew()).isBeforeNow()) {
                    throw new SAMLException("Assertion is no longer valid, invalidated by condition notOnOrAfter " + conditions.getNotOnOrAfter());
                } else {
                    List<Condition> notUnderstoodConditions = new LinkedList();
                    Iterator var5 = conditions.getConditions().iterator();

                    while (var5.hasNext()) {
                        Condition condition = (Condition) var5.next();
                        QName conditionQName = condition.getElementQName();
                        if (conditionQName.equals(AudienceRestriction.DEFAULT_ELEMENT_NAME)) {
                            this.verifyAudience(context, conditions.getAudienceRestrictions());
                        } else {
                            if (conditionQName.equals(OneTimeUse.DEFAULT_ELEMENT_NAME)) {
                                // throw new SAMLException("System cannot honor OneTimeUse condition of the Assertion for WebSSO");
                            } else if (conditionQName.equals(ProxyRestriction.DEFAULT_ELEMENT_NAME)) {
                                ProxyRestriction restriction = (ProxyRestriction) condition;
                                this.log.debug("Honoring ProxyRestriction with count {}, system does not issue assertions to 3rd parties", restriction.getProxyCount());
                            } else {
                                this.log.debug("Condition {} is not understood", condition);
                                notUnderstoodConditions.add(condition);
                            }
                        }
                    }

                    this.verifyConditions(context, notUnderstoodConditions);
                }
            }
        } else {
            throw new SAMLException("Assertion invalidated by missing Audience Restriction");
        }
    }


}

