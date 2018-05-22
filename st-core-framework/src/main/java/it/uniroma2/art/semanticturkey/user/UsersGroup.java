package it.uniroma2.art.semanticturkey.user;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.vocabulary.UserVocabulary;

public class UsersGroup {
	
	private IRI iri;
	private String shortName;
	private String fullName;
	private String description;
	private String webPage;
	private String logoUrl;
	
	public UsersGroup(String shortName) {
		this(null, shortName);
	}
	
	public UsersGroup(IRI iri, String shortName) {
		if (iri == null) {
			iri = createIriFromShortName(shortName);
		}
		this.iri = iri;
		this.shortName = shortName;
	}

	public IRI getIri() {
		return iri;
	}

	public void setIri(IRI iri) {
		this.iri = iri;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getWebPage() {
		return webPage;
	}

	public void setWebPage(String webPage) {
		this.webPage = webPage;
	}

	public String getLogoUrl() {
		return logoUrl;
	}

	public void setLogoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
	}
	
	public ObjectNode getAsJsonObject() {
		JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
		ObjectNode groupJson = jsonFactory.objectNode();
		groupJson.set("shortName", jsonFactory.textNode(shortName));
		groupJson.set("fullName", jsonFactory.textNode(fullName));
		groupJson.set("description", jsonFactory.textNode(description));
		groupJson.set("iri", jsonFactory.textNode(iri.stringValue()));
		groupJson.set("logoUrl", jsonFactory.textNode(logoUrl));
		groupJson.set("webPage", jsonFactory.textNode(webPage));
		return groupJson;
	}
	
	public static String encodeGroupIri(IRI iri) {
		String fileNameCompliantCharacters = "([^ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._-])+";
		String replaced = iri.stringValue();
		replaced = replaced.substring(replaced.indexOf("://")+3);
		replaced = replaced.replaceAll(fileNameCompliantCharacters, ".");
		try {
			replaced = URLEncoder.encode(replaced, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return replaced;
	}
	
	private IRI createIriFromShortName(String shortName) {
		String localNameCompliantCharacters = "([^ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._-])+";
		String localName = shortName.replaceAll(localNameCompliantCharacters, ".");
		return SimpleValueFactory.getInstance().createIRI(UserVocabulary.GROUPSBASEURI, localName);
	}
}
