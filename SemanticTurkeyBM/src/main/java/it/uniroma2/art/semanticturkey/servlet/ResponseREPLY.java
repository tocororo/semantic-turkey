/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License");  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * http//www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is Semantic Turkey.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2009.
 * All Rights Reserved.
 *
 * Semantic Turkey was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata
 * Current information about Semantic Turkey can be obtained at 
 * http//ai-nlp.info.uniroma2.it/software/...
 *
 */
package it.uniroma2.art.semanticturkey.servlet;

import it.uniroma2.art.semanticturkey.servlet.ServiceVocabulary.RepliesStatus;
/**
 * Defines an interface for Semantic Turkey query response with data content
 * 
 * @author: Ramon Orrù
 * @author: Armando Stellato stellato@info.uniroma2.it
 */
public interface ResponseREPLY extends Response {
	
	public void setReplyStatus (RepliesStatus rep_status);
	public void setReplyStatusOK() ;
	
	public RepliesStatus getReplyStatus();

	public void setReplyStatusWARNING() ;
	public void setReplyStatusWARNING(String msg) ;

	public void setReplyStatusFAIL() ;
	public void setReplyStatusFAIL(String msg) ;

	public void setReplyMessage(String msg);

	public String getReplyMessage();
	
}
