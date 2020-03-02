package it.uniroma2.art.semanticturkey.email;

import it.uniroma2.art.semanticturkey.config.contribution.StoredContributionConfiguration;
import it.uniroma2.art.semanticturkey.config.contribution.StoredDevResourceContributionConfiguration;
import it.uniroma2.art.semanticturkey.config.contribution.StoredMetadataContributionConfiguration;
import it.uniroma2.art.semanticturkey.config.contribution.StoredStableResourceContributionConfiguration;
import it.uniroma2.art.semanticturkey.pmki.PmkiConversionFormat;
import it.uniroma2.art.semanticturkey.properties.STPropertyAccessException;
import it.uniroma2.art.semanticturkey.resources.Reference;
import it.uniroma2.art.semanticturkey.user.UsersManager;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class PmkiEmailService extends EmailService {

	PmkiEmailService() {
		super(EmailApplicationContext.PMKI);
	}

	/**
	 * Mail to:
	 *  - the administrator that informs that a new contribution has been submitted
	 *  - the contributor that informs that the contribution request has been received
	 * @param contribution
	 * @throws STPropertyAccessException
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 */
	public void sendContributionSubmittedMail(StoredContributionConfiguration contribution)
			throws STPropertyAccessException, UnsupportedEncodingException, MessagingException {
		//to admin
		for (String adminEmail: UsersManager.getAdminEmailList()) {
			String mailContent = "Dear PMKI administrator,<br>" +
					"a new contribution request has been submitted to the PMKI portal:<br><br>" +
					formatBold("Contribution type: ") + contribution.getContributionTypeLabel() + "<br>" +
					formatBold("Contributor: ") + contribution.contributorName + " " + contribution.contributorLastName +
					" (email: " + contribution.contributorEmail + ")";
			EmailSender.sendMail(adminEmail, "PMKI Contribution request submitted", mailContent);
		}
		//to contributor
		String mailContent = "Dear " + contribution.contributorName + " " + contribution.contributorLastName + ",<br>" +
				"your contribution request ";
		if (contribution instanceof StoredStableResourceContributionConfiguration) {
			mailContent += "about the resource " + formatItalic(contribution.resourceName);
		} else if (contribution instanceof StoredMetadataContributionConfiguration) {
			mailContent += "about the metadata of the resource " + formatItalic(contribution.resourceName);
		} else if (contribution instanceof StoredDevResourceContributionConfiguration) {
			mailContent += "for the development of the resource " + formatItalic(contribution.resourceName);
		}
		mailContent += " has been successfully submitted. It will be evaluated by the administrator and " +
				"you will receive the response to this email address.";
		EmailSender.sendMail(contribution.contributorEmail, "PMKI Contribution request submitted", mailContent);
	}

	/**
	 * Mail to the contributor that inform that his contribution has been accepted
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
	public void sendAcceptedStableResourceContributionMail(Reference reference,
			StoredStableResourceContributionConfiguration contribution, String projectName,
			String pmkiHostUrl, String token)
			throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		String formattedDate = getDateFromTimestamp(Long.parseLong(reference.getIdentifier()));
		String loadPageUrl = pmkiHostUrl + "#/load/stable/" + token;
		String mailContent = "Dear " + contribution.contributorName + " " + contribution.contributorLastName + ",<br>" +
				"Your contribution request submitted on " + formattedDate + " about the " + formatItalic(contribution.resourceName) +
				" resource has been accepted.<br>" +
				"Now you can now upload the RDF resource at the following link " + loadPageUrl + "<br>" +
				"In the loading page, please insert the following project name in the proper input field: " +
				formatItalic(projectName);
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
	public void sendAcceptedDevGenericResourceContributionMail(Reference reference,
			StoredDevResourceContributionConfiguration contribution, String projectName,
			String pmkiHostUrl, String token) throws IOException, MessagingException, STPropertyAccessException {

		String formattedDate = getDateFromTimestamp(Long.parseLong(reference.getIdentifier()));
		String mailContent = "Dear " + contribution.contributorName + " " + contribution.contributorLastName + ",<br>" +
				"Your contribution request submitted on " + formattedDate + " for the development of the resource " +
				formatItalic(contribution.resourceName) + " has been accepted.";
		if (contribution.format == null) { //No conversion needed => load RDF
			String loadPageUrl = pmkiHostUrl + "#/load/dev/" + PmkiConversionFormat.RDF + "/" + token;
			mailContent += "Now you can load the data at the following link " + loadPageUrl + ".<br>";
		} else if (contribution.format == PmkiConversionFormat.TBX || contribution.format == PmkiConversionFormat.ZTHES) {
			String loadPageUrl = pmkiHostUrl + "#/load/dev/" + contribution.format + "/" + token;
			mailContent += "Now you can load the data at the following link " + loadPageUrl +
					" using the proper lifter for the conversion.<br>";
		}
		mailContent += "In the loading page, please insert the following project name in the proper input field: " +
				formatItalic(projectName);
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
	public void sendAcceptedDevExcelResourceContributionMail(Reference reference,
			StoredDevResourceContributionConfiguration contribution, String projectName,
			String vbLink, String tempPwd) throws IOException, MessagingException, STPropertyAccessException {
		String formattedDate = getDateFromTimestamp(Long.parseLong(reference.getIdentifier()));
		String mailContent = "Dear " + contribution.contributorName + " " + contribution.contributorLastName + ",<br>" +
				"Your contribution request submitted on " + formattedDate + " for the development of the resource " +
				formatItalic(contribution.resourceName) + " has been accepted.<br>" +
				"Now you can load the data directly from VocBench exploiting the Sheet2RDF tool.<br>" +
				"The VocBench instance is available at " + vbLink + " where you can login with ";
		if (tempPwd != null) { //user created
			mailContent += "the following credentials:<br>" +
					formatBold("E-mail address: ") + contribution.contributorEmail + "<br>" +
					formatBold("Password: ") + tempPwd + "<br>It is recommended to change the password at the first login.";
		} else { //user already registered
			mailContent += "your pre-existing account (email address: " + contribution.contributorEmail + ").<br>";
		}
		EmailSender.sendMail(contribution.contributorEmail, "PMKI Contribution approved", mailContent);
	}

	/**
	 * Mail to the contributor that informs that his metadata contribution has been accepted
	 * @param reference
	 * @param contribution
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 * @throws STPropertyAccessException
	 */
	public void sendAcceptedMetadataContributionMail(Reference reference, StoredMetadataContributionConfiguration contribution)
			throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		String formattedDate = getDateFromTimestamp(Long.parseLong(reference.getIdentifier()));
		String mailContent = "Dear " + contribution.contributorName + " " + contribution.contributorLastName + ",<br>" +
				"Your contribution request submitted on " + formattedDate + " about the metadata of the resource " +
				formatItalic(contribution.resourceName) + " has been accepted.<br>" +
				"Thanks for your contribution.";
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
	public void sendLoadedDevGenericResourceContributionMail(String projectName, String vbLink,
			String contributorEmail, String tempPwd) throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		String mailContent = "Dear contributor,<br>" +
				"the data you provided has been successfully loaded into the " + formatItalic(projectName) + " project." +
				"Now you can access the project from the VocBench instance available at " + vbLink + " where you can login with ";
		if (tempPwd != null) { //user created
			mailContent += "the following credentials:<br>" +
					formatBold("E-mail address: ") + contributorEmail + "<br>" +
					formatBold("Password: ") + tempPwd + "<br>It is recommended to change the password at the first login.";
		} else { //user already registered
			mailContent += "your pre-existing account (email address: " + contributorEmail + ").<br>";
		}
		EmailSender.sendMail(contributorEmail, "PMKI Contribution approved", mailContent);
	}

	/**
	 * Mail to the administrator that informs that the contributor has loaded the data into a stable resource
	 * contribution project
	 * @param projectName
	 * @param contributorName
	 * @param contributorLastName
	 * @param contributorEmail
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 * @throws STPropertyAccessException
	 */
	public void sendLoadedStableResourceContributionMail(String projectName, String contributorName,
			String contributorLastName, String contributorEmail)
			throws UnsupportedEncodingException,MessagingException, STPropertyAccessException {
		for (String adminEmail: UsersManager.getAdminEmailList()) {
			String mailContent = "Dear PMKI administrator,<br>" +
					"the contributor: " + contributorName + " " + contributorLastName +
					" (email: " + contributorEmail + ") has just uploaded the data into the project " +
					formatItalic(projectName) + ". The project is now in the STAGING status until you approve it and make it PUBLIC";
			EmailSender.sendMail(adminEmail, "PMKI Contribution data loaded", mailContent);
		}
	}

	/**
	 *
	 * @param reference
	 * @param contribution
	 * @throws UnsupportedEncodingException
	 * @throws MessagingException
	 * @throws STPropertyAccessException
	 */
	public void sendRejectedContributionMail(Reference reference, StoredContributionConfiguration contribution)
			throws UnsupportedEncodingException, MessagingException, STPropertyAccessException {
		String formattedDate = getDateFromTimestamp(Long.parseLong(reference.getIdentifier()));
		String mailContent = "Dear " + contribution.contributorName + " " + contribution.contributorLastName + ",<br>" +
				"Your contribution submitted on " + formattedDate;
		if (contribution instanceof  StoredStableResourceContributionConfiguration) {
			StoredStableResourceContributionConfiguration contribImpl = (StoredStableResourceContributionConfiguration) contribution;
			mailContent += " about the " + formatItalic(contribImpl.resourceName) + " resource has been rejected.";
		} else if (contribution instanceof  StoredDevResourceContributionConfiguration) {
			StoredDevResourceContributionConfiguration contribImpl = (StoredDevResourceContributionConfiguration) contribution;
			mailContent += " about the " + formatItalic(contribImpl.resourceName) + " resource has been rejected.";
		} else if (contribution instanceof StoredMetadataContributionConfiguration) {
			StoredMetadataContributionConfiguration contribImpl = (StoredMetadataContributionConfiguration) contribution;
			mailContent += " about metadata of the " + formatItalic(contribImpl.resourceName) + " resource has been rejected.";
		}
		EmailSender.sendMail(contribution.contributorEmail, "PMKI Contribution rejected", mailContent);
	}

	private static String getDateFromTimestamp(long timestamp) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(new Date(timestamp));
	}

}
