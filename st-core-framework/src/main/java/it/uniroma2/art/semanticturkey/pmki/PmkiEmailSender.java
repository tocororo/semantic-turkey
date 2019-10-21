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

	public static void sendContributionSubmittedMail(StoredContributionConfiguration contribution)
			throws STPropertyAccessException, UnsupportedEncodingException, MessagingException {
		String contributionType = null;
		if (contribution instanceof StoredDevResourceContributionConfiguration) {
			contributionType = "Development";
		} else if (contribution instanceof StoredMetadataContributionConfiguration) {
			contributionType = "Metadata";
		} else if (contribution instanceof StoredStableResourceContributionConfiguration) {
			contributionType = "Stable";
		}
		for (String adminEmail: UsersManager.getAdminEmailList()) {
			String mailContent = "Dear PMKI administrator,\n" +
					"a new contribution request has been submitted to the PMKI portal:\n" +
					"Contribution type: " + contributionType + "\n" +
					"Contributor: " + contribution.contributorName + " " + contribution.contributorLastName +
					" (email: " + contribution.contributorEmail + ")";
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
		String loadPageUrl = pmkiHostUrl + "#/load/stable/" + token;
		String mailContent = "Dear " + contribution.contributorName + " " + contribution.contributorLastName + ",\n" +
				"Your contribution request submitted on " + formattedDate + " about the '" + contribution.resourceName +
				"' resource has been accepted.\n" +
				"Now you can now upload the RDF resource at the following link " + loadPageUrl + "\n" +
				"In the loading page, please insert the following project name in the proper input field: " + projectName;
		EmailSender.sendMail(contribution.contributorEmail, "PMKI Contribution approved", mailContent);
	}

	/**
	 * Send email notification to the contributor about the approval of a contribution request of a dev resource that
	 * requires no conversion or a conversion from TBX or Zthes.
	 * Note: the project name is not retrieved from the contribution since it could be change from the admin during the approval
	 * @param reference
	 * @param contribution
	 * @param projectName
	 * @param pmkiHostUrl
	 * @param token
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 * @throws STPropertyAccessException
	 */
	public static void sendAcceptedDevGenericResourceContributionMail(Reference reference,
			StoredDevResourceContributionConfiguration contribution, String projectName,
			String pmkiHostUrl, String token) throws IOException, MessagingException, STPropertyAccessException {

		String formattedDate = getDateFromTimestamp(Long.parseLong(reference.getIdentifier()));
		String mailContent = "Dear " + contribution.contributorName + " " + contribution.contributorLastName + ",\n" +
				"Your contribution request submitted on " + formattedDate + " for the development of the resource '" +
				contribution.resourceName + "' has been accepted.";
		if (contribution.format == null) { //No conversion needed => load RDF
			String loadPageUrl = pmkiHostUrl + "#/load/dev/" + PmkiConversionFormat.RDF + "/" + token;
			mailContent += "Now you can load the data at the following link " + loadPageUrl + ".\n";
		} else if (contribution.format == PmkiConversionFormat.TBX || contribution.format == PmkiConversionFormat.ZTHES) {
			String loadPageUrl = pmkiHostUrl + "#/load/dev/" + contribution.format + "/" + token;
			mailContent += "Now you can load the data at the following link " + loadPageUrl + " using the proper lifter for the conversion.\n";
		}
		mailContent += "In the loading page, please insert the following project name in the proper input field: " + projectName;
		EmailSender.sendMail(contribution.contributorEmail, "PMKI Contribution approved", mailContent);
	}

	/**
	 * Send email notification to the contributor about the approval of a contribution request of a dev resource that
	 * requires conversion from excel
	 * Note: the project name is not retrieved from the contribution since it could be change from the admin during the approval
	 * @param reference
	 * @param contribution
	 * @param projectName
	 * @param vbLink
	 * @param tempPwd provided if a user is created contextually
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 * @throws STPropertyAccessException
	 */
	public static void sendAcceptedDevExcelResourceContributionMail(Reference reference,
			StoredDevResourceContributionConfiguration contribution, String projectName,
			String vbLink, String tempPwd) throws IOException, MessagingException, STPropertyAccessException {
		String formattedDate = getDateFromTimestamp(Long.parseLong(reference.getIdentifier()));
		String mailContent = "Dear " + contribution.contributorName + " " + contribution.contributorLastName + ",\n" +
				"Your contribution request submitted on " + formattedDate + " for the development of the resource '" +
				contribution.resourceName + "' has been accepted.\n" +
				"Now you can load the data directly from VocBench exploiting the Sheet2RDF tool.\n" +
				"The VocBench instance is available at " + vbLink + " where you can login with ";
		if (tempPwd != null) { //user created
			mailContent += "the following credentials:\n" +
					"E-mail address: " + contribution.contributorEmail + "\n" +
					"Password: " + tempPwd + "\nIt is recommended to change the password at the first login.";
		} else { //user already registered
			mailContent += "your pre-existing account (email address: " + contribution.contributorEmail + ").\n";
		}
		EmailSender.sendMail(contribution.contributorEmail, "PMKI Contribution approved", mailContent);
	}

	/**
	 * Send email notification to a contributor that has just loaded data. The email contains the reference to the
	 * VB instance where the data has been loaded.
	 * @param projectName
	 * @param vbLink
	 * @param contributorEmail
	 * @param tempPwd
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 * @throws STPropertyAccessException
	 */
	public static void sendLoadedDevGenericResourceContributionMail(String projectName, String vbLink,
			String contributorEmail, String tempPwd) throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		String mailContent = "Dear contributor,\n" +
				"the data you provided has been successfully loaded into the '" + projectName + "' project." +
				"Now you can access the project from the VocBench instance available at " + vbLink + " where you can login with ";
		if (tempPwd != null) { //user created
			mailContent += "the following credentials:\n" +
					"E-mail address: " + contributorEmail + "\n" +
					"Password: " + tempPwd + "\nIt is recommended to change the password at the first login.";
		} else { //user already registered
			mailContent += "your pre-existing account (email address: " + contributorEmail + ").\n";
		}
		EmailSender.sendMail(contributorEmail, "PMKI Contribution approved", mailContent);
	}

	/**
	 *
	 * @param reference
	 * @param contribution
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 * @throws STPropertyAccessException
	 */
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

	/**
	 *
	 * @param mailTo
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 * @throws STPropertyAccessException
	 */
	public static void sendTestMailConfiguration(String mailTo) throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		String mailContent = "This message has been sent in order to check the PMKI e-mail configuration.\n"
				+ "If you did not request to send this e-mail, please ignore it.";
		EmailSender.sendMail(mailTo, "PMKI e-mail configuration check", mailContent);
	}

	/**
	 *
	 * @param timestamp
	 * @return
	 */
	private static String getDateFromTimestamp(long timestamp) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(new Date(timestamp));
	}

}
