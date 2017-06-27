/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License");  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is SemanticTurkey.
 *
 * The Initial Developer of the Original Code is University of Roma Tor Vergata.
 * Portions created by University of Roma Tor Vergata are Copyright (C) 2007.
 * All Rights Reserved.
 *
 * SemanticTurkey was developed by the Artificial Intelligence Research Group
 * (art.uniroma2.it) at the University of Roma Tor Vergata (ART)
 * Current information about SemanticTurkey can be obtained at 
 * http://semanticturkey.uniroma2.it
 *
 */

/*
 * Contributor(s): Armando Stellato stellato@info.uniroma2.it
 */
package it.uniroma2.art.semanticturkey.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * @author Armando Stellato
 * 
 */
public class SemAnnotVocab {

	/** http://art.uniroma2.it/ontologies/annotation# **/
	public static final String NAMESPACE = "http://art.uniroma2.it/ontologies/annotation#";

	/** http://art.uniroma2.it/ontologies/annotation#SemanticAnnotation **/
	public static final String SemanticAnnotation = NAMESPACE + "SemanticAnnotation";

	/** http://art.uniroma2.it/ontologies/annotation#RangeAnnotation **/
	public static final String RangeAnnotation = NAMESPACE + "RangeAnnotation";

	/** http://art.uniroma2.it/ontologies/annotation#annotation **/
	public static final String annotation = NAMESPACE + "annotation";

	/** http://art.uniroma2.it/ontologies/annotation#WebPage **/
	public static final String WebPage = NAMESPACE + "WebPage";

	/** http://art.uniroma2.it/ontologies/annotation#text **/
	public static final String text = NAMESPACE + "text";

	/** http://art.uniroma2.it/ontologies/annotation#url **/
	public static final String url = NAMESPACE + "url";

	/** http://art.uniroma2.it/ontologies/annotation#title **/
	public static final String title = NAMESPACE + "title";

	/** http://art.uniroma2.it/ontologies/annotation#range **/
	public static final String range = NAMESPACE + "range";

	/** http://art.uniroma2.it/ontologies/annotation#TextualAnnotation **/
	public static final String TextualAnnotation = NAMESPACE + "TextualAnnotation";

	/** http://art.uniroma2.it/ontologies/annotation#ImageAnnotation **/
	public static final String ImageAnnotation = NAMESPACE + "ImageAnnotation";

	/** http://art.uniroma2.it/ontologies/annotation#location **/
	public static final String location = NAMESPACE + "location";

	/** "http://purl.org/dc/terms/subject" **/
	public static final String topic = "http://purl.org/dc/terms/subject";

	public static class Res {
		public static IRI SemanticAnnotation;
		public static IRI RangeAnnotation;
		public static IRI annotation;
		public static IRI WebPage;
		public static IRI text;
		public static IRI url;
		public static IRI location;
		public static IRI title;
		public static IRI range;
		public static IRI topic;
		public static IRI TextualAnnotation;
		public static IRI ImageAnnotation;

		public static void initialize() {
			SemanticAnnotation = SimpleValueFactory.getInstance().createIRI(SemAnnotVocab.SemanticAnnotation);
			RangeAnnotation = SimpleValueFactory.getInstance().createIRI(SemAnnotVocab.RangeAnnotation);
			annotation = SimpleValueFactory.getInstance().createIRI(SemAnnotVocab.annotation);
			WebPage = SimpleValueFactory.getInstance().createIRI(SemAnnotVocab.WebPage);
			url = SimpleValueFactory.getInstance().createIRI(SemAnnotVocab.url);
			location = SimpleValueFactory.getInstance().createIRI(SemAnnotVocab.location);
			text = SimpleValueFactory.getInstance().createIRI(SemAnnotVocab.text);
			title = SimpleValueFactory.getInstance().createIRI(SemAnnotVocab.title);
			range = SimpleValueFactory.getInstance().createIRI(SemAnnotVocab.range);
			TextualAnnotation = SimpleValueFactory.getInstance().createIRI(SemAnnotVocab.TextualAnnotation);
			ImageAnnotation = SimpleValueFactory.getInstance().createIRI(SemAnnotVocab.ImageAnnotation);
			topic = SimpleValueFactory.getInstance().createIRI(SemAnnotVocab.topic);
		}

	}

}
