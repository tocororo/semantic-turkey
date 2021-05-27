package it.uniroma2.art.semanticturkey.email;

public class EmailServiceFactory {

    public static EmailService getService(EmailApplicationContext ctx) {
        if (ctx == EmailApplicationContext.SHOWVOC) {
            return new ShowVocEmailService();
        } else {
            return new VbEmailService();
        }
    }

}
