package it.uniroma2.art.semanticturkey.zthes;

public enum TermType {
	PT, //Preferred term (also known as a descriptor)
	ND,	//Non-descriptor: that is, a non-preferred term.
	NL; /* (Not used)
		* Node label: that is, a dummy term not assigned to documents when indexing,
		* but inserted into the hierarchy to indicate the logical basis on which a category has been divided - 
		* for example, by function. Also known as a guide term or a facet indicator.
		*/
}
