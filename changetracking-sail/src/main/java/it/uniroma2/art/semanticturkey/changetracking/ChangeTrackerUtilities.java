package it.uniroma2.art.semanticturkey.changetracking;

import it.uniroma2.art.semanticturkey.changetracking.sail.ChangeTracker;
import it.uniroma2.art.semanticturkey.changetracking.sail.config.ChangeTrackerSchema;
import it.uniroma2.art.semanticturkey.changetracking.vocabulary.CHANGETRACKER;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import java.util.Set;

/**
 * Utility class for the {@link ChangeTracker} sail.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 *
 */
public abstract class ChangeTrackerUtilities {

	/**
	 * Checks the presence of the change tracking sail on the provided connection.
	 * 
	 * @param conn
	 * @param expectedSupportRepositoryId
	 * @param expectedServerURL
	 * @throws {@link
	 *             ChangeTrackerNotDetectedException}
	 * @throws {@link
	 *             ChangeTrackerParameterMismatchException}
	 * @throws {@link
	 *             ChangeTrackerDetectionException}
	 * 
	 */
	public static void checkChangeTrackingOnConnection(RepositoryConnection conn,
			String expectedSupportRepositoryId, /* @Nullable */ String expectedServerURL)
			throws ChangeTrackerNotDetectedException, ChangeTrackerParameterMismatchException,
			ChangeTrackerDetectionException {
		String nonce = Long.toString(System.currentTimeMillis());
		IRI sysInfoWithNonce = SimpleValueFactory.getInstance()
				.createIRI(CHANGETRACKER.SYSINFO.toString() + "?nonce=" + nonce);

		GraphQuery query = conn.prepareGraphQuery("describe" + NTriplesUtil.toNTriplesString(sysInfoWithNonce)
				+ " from " + NTriplesUtil.toNTriplesString(CHANGETRACKER.SYSINFO));

		Model systemInfo = QueryResults.asModel(query.evaluate());

		if (systemInfo.isEmpty()) {
			throw new ChangeTrackerNotDetectedException();
		} else {
			// Check version
			IRI versionProp = SimpleValueFactory.getInstance().createIRI("http://schema.org/version");
			Set<Literal> versions = Models.getPropertyLiterals(systemInfo, sysInfoWithNonce, versionProp);

			if (versions.isEmpty()) {
				throw new ChangeTrackerDetectionException("The change tracker didn't report any version");
			}

			if (versions.size() > 1) {
				throw new ChangeTrackerDetectionException(
						"The change tracker reported more than one version: " + versions.toString());
			}

			Literal versionLit = versions.iterator().next();
			if (!XSD.STRING.equals(versionLit.getDatatype())) {
				throw new ChangeTrackerDetectionException(
						"The change tracker reported its version with a datatype different from xsd:string:"
								+ versionLit.getDatatype());
			}

			String version = versionLit.getLabel();
			String expectedVersion = ChangeTracker.getVersion();
			if (!expectedVersion.equals(version)) {
				throw new ChangeTrackerParameterMismatchException(versionProp, expectedVersion, version);
			}

			// Check support repository identifier
			Set<Literal> supportRepositoryIds = Models.getPropertyLiterals(systemInfo, sysInfoWithNonce,
					ChangeTrackerSchema.SUPPORT_REPOSITORY_ID);
			if (supportRepositoryIds.isEmpty()) {
				throw new ChangeTrackerDetectionException(
						"The change tracker didn't report any support repository identifier");
			}
			if (supportRepositoryIds.size() > 1) {
				throw new ChangeTrackerDetectionException(
						"The change tracker reported more than one support repository identifier: "
								+ supportRepositoryIds.toString());
			}
			Literal supportRepositoryIdLit = supportRepositoryIds.iterator().next();
			if (!XSD.STRING.equals(supportRepositoryIdLit.getDatatype())) {
				throw new ChangeTrackerDetectionException(
						"The change tracker reported its support repository identifier with a datatype different from xsd:string: "
								+ supportRepositoryIdLit.getDatatype());
			}

			if (!expectedSupportRepositoryId.equals(supportRepositoryIdLit.stringValue())) {
				throw new ChangeTrackerParameterMismatchException(ChangeTrackerSchema.SUPPORT_REPOSITORY_ID,
						expectedSupportRepositoryId, supportRepositoryIdLit.stringValue());
			}

			// Check server URL
			Set<Literal> serverURLs = Models.getPropertyLiterals(systemInfo, sysInfoWithNonce,
					ChangeTrackerSchema.SERVER_URL);
			if (serverURLs.size() > 1) {
				throw new ChangeTrackerDetectionException(
						"The change tracker reported more than one server URLs: " + serverURLs.toString());
			}

			if (expectedServerURL != null) {
				if (serverURLs.isEmpty()) {
					throw new ChangeTrackerDetectionException(
							"The change tracker didn't report any support repository URL");
				}

				Literal serverURLLit = serverURLs.iterator().next();
				if (!XSD.STRING.equals(serverURLLit.getDatatype())) {
					throw new ChangeTrackerDetectionException(
							"The change tracker reported its server URL with a datatype different from xsd:string: "
									+ serverURLLit.getDatatype());
				}

				if (!expectedServerURL.equals(serverURLLit.stringValue())) {
					throw new ChangeTrackerParameterMismatchException(ChangeTrackerSchema.SERVER_URL,
							expectedServerURL, serverURLLit.stringValue());
				}
			} else {
				if (!serverURLs.isEmpty()) {
					throw new ChangeTrackerParameterMismatchException(ChangeTrackerSchema.SERVER_URL, null,
							serverURLs.iterator().next().toString());
				}
			}
		}
	}

}
