package it.uniroma2.art.semanticturkey.email;

public class EmailServiceFactory {

    public static EmailService getService(EmailApplicationContext ctx) {
        if (ctx == EmailApplicationContext.PMKI) {
            return new PmkiEmailService();
        } else {
            return new VbEmailService();
        }
    }

}
