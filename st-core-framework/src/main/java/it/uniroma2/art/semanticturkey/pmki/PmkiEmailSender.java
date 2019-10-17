package it.uniroma2.art.semanticturkey.pmki;

import it.uniroma2.art.semanticturkey.config.contribution.StoredContributionConfiguration;
import it.uniroma2.art.semanticturkey.config.contribution.StoredDevResourceContributionConfiguration;
import it.uniroma2.art.semanticturkey.config.contribution.StoredMetadataContributionConfiguration;
import it.uniroma2.art.semanticturkey.config.contribution.StoredStableResourceContributionConfiguration;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.user.UsersManager;
import it.uniroma2.art.semanticturkey.utilities.EmailSender;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class PmkiEmailSender {

	public static void sendContributionSubmittedMail() throws STPropertyAccessException, UnsupportedEncodingException, MessagingException {
		for (String adminEmail: UsersManager.getAdminEmailList()) {
			String mailContent = "Dear PMKI administrator,\n" +
					"a new contribution request has been submitted to the PMKI portal.";
			EmailSender.sendMail(adminEmail, "PMKI Contribution request submitted", mailContent);
		}
	}

	/**
	 * Note: the project name is not retrieved from the contribution since it could be change from the admin during the approval
	 * @param reference
	 * @param contribution
	 * @param projectName
	 * @param pmkiHostUrl
	 * @param  token
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 * @throws STPropertyAccessException
	 */
	public static void sendAcceptedStableResourceContributionMail(Reference reference,
			StoredStableResourceContributionConfiguration contribution, String projectName,
			String pmkiHostUrl, String token)
			throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		String formattedDate = getDateFromTimestamp(Long.parseLong(reference.getIdentifier()));
		String loadPageUrl = pmkiHostUrl + "#/load/rdf/" + token;
		String mailContent = "Dear " + contribution.contributorName + " " + contribution.contributorLastName + ",\n" +
				"Your contribution request submitted on " + formattedDate + " about the '" + contribution.resourceName +
				"' resource has been accepted. The project '" + projectName + "' has been created. " +
				"Now you can now upload the RDF resource at the following link " + loadPageUrl;
		EmailSender.sendMail(contribution.contributorEmail, "PMKI Contribution approved", mailContent);
	}

	/**
	 * Note: the project name is not retrieved from the contribution since it could be change from the admin during the approval
	 * @param reference
	 * @param contribution
	 * @param projectName
	 * @param pmkiHostUrl
	 * @param token
	 * @param vbLink
	 * @param tempPwd provided if a user is created contextually
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 * @throws STPropertyAccessException
	 */
	public static void sendAcceptedDevResourceContributionMail(Reference reference,
			StoredDevResourceContributionConfiguration contribution, String projectName,
			String pmkiHostUrl, String token, String vbLink, String tempPwd)
			throws IOException, MessagingException, STPropertyAccessException {
		String formattedDate = getDateFromTimestamp(Long.parseLong(reference.getIdentifier()));

		String mailContent = "Dear " + contribution.contributorName + " " + contribution.contributorLastName + ",\n" +
				"Your contribution request submitted on " + formattedDate + " for the development of the resource '" +
				contribution.resourceName + "' has been accepted. The project '" + projectName + "' has been created " +
				"on the VocBench instance available at " + vbLink + " where you can login with ";
		if (tempPwd != null) { //user created
			mailContent += "the email address " + contribution.contributorEmail + " and the temporary password " +
					tempPwd + " (which is recommended to change at the first login).\n";
		} else { //user already registered
			mailContent += "your pre-existing account (email address: " + contribution.contributorEmail + ").\n";
		}
		if (contribution.format == null) { //No conversion needed
			String loadPageUrl = pmkiHostUrl + "#/load/dev/RDF/" + token;
		} else if (contribution.format == PmkiConversionFormat.EXCEL) {
			mailContent += "Now you can load and convert the data directly from VocBench exploiting the Sheet2RDF tool";
		} else if (contribution.format == PmkiConversionFormat.TBX || contribution.format == PmkiConversionFormat.ZTHES) {
			String loadPageUrl = pmkiHostUrl + "#/load/dev/" + contribution.format + "/" + token;
			mailContent += "Now you can load and convert the data at the following link " + loadPageUrl;
		}
		EmailSender.sendMail(contribution.contributorEmail, "PMKI Contribution approved", mailContent);
	}

	public static void sendRejectedContributionMail(Reference reference, StoredContributionConfiguration contribution)
			throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		String formattedDate = getDateFromTimestamp(Long.parseLong(reference.getIdentifier()));
		String mailContent = "Dear " + contribution.contributorName + " " + contribution.contributorLastName + ",\n" +
				"Your contribution submitted on " + formattedDate;
		if (contribution instanceof  StoredStableResourceContributionConfiguration) {
			StoredStableResourceContributionConfiguration contribImpl = (StoredStableResourceContributionConfiguration) contribution;
			mailContent += " about the '" + contribImpl.resourceName + "' resource has been rejected.";
		} else if (contribution instanceof  StoredDevResourceContributionConfiguration) {
			StoredDevResourceContributionConfiguration contribImpl = (StoredDevResourceContributionConfiguration) contribution;
			mailContent += " about the '" + contribImpl.resourceName + "' resource has been rejected.";
		} else if (contribution instanceof StoredMetadataContributionConfiguration) {
			StoredMetadataContributionConfiguration contribImpl = (StoredMetadataContributionConfiguration) contribution;
			mailContent += " about metadata of the '" + contribImpl.resourceName + "' resource has been rejected.";
		}
		EmailSender.sendMail(contribution.contributorEmail, "PMKI Contribution rejected", mailContent);
	}

	public static void sendTestMailConfiguration(String mailTo) throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		String mailContent = "This message has been sent in order to check the PMKI e-mail configuration.\n"
				+ "If you did not request to send this e-mail, please ignore it.";
		EmailSender.sendMail(mailTo, "PMKI e-mail configuration check", mailContent);
	}

	private static String getDateFromTimestamp(long timestamp) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(new Date(timestamp));
	}

}
