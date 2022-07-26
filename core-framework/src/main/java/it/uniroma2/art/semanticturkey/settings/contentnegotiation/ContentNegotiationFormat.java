package it.uniroma2.art.semanticturkey.settings.contentnegotiation;

public enum ContentNegotiationFormat {
    all,
    alldata,
    html,
    jsonld,
    n3,
    nt,
    rdf,
    ttl;

    public boolean covers(ContentNegotiationFormat format) {
        if (this.equals(all)) {
            return true;
        } else if (this.equals(alldata)) {
            return !format.equals(all) && !format.equals(html);
        } else {
            return this.equals(format);
        }
    }

}
