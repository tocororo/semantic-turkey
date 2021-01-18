package it.uniroma2.art.semanticturkey.services.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.TupleQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import it.uniroma2.art.lime.model.vocabulary.ONTOLEX;
import it.uniroma2.art.semanticturkey.services.AnnotatedValue;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Read;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;

/**
 * This class provides services related to the lexicographer view, which provides a simplified, streamlined
 * visualization of OntoLex-Lemon lexicons.
 * 
 * @author <a href="mailto:fiorelli@info.uniroma2.it">Manuel Fiorelli</a>
 */
@STService
public class LexicographerView extends STServiceAdapter {

	private static Logger logger = LoggerFactory.getLogger(LexicographerView.class);

	public static class BindingSets2Model extends AbstractTupleQueryResultHandler {

		private Model model;

		public BindingSets2Model(Model model) {
			this.model = model;
		}

		public void startQueryResult(java.util.List<String> bindingNames)
				throws org.eclipse.rdf4j.query.TupleQueryResultHandlerException {
			model.clear();
		};

		public void handleSolution(org.eclipse.rdf4j.query.BindingSet bindingSet)
				throws org.eclipse.rdf4j.query.TupleQueryResultHandlerException {
			Resource subj = (Resource) bindingSet.getValue("s");
			IRI pred = (IRI) bindingSet.getValue("p");
			Value obj = (Value) bindingSet.getValue("o");
			Resource ctx = (Resource) bindingSet.getValue("c");

			if (ctx != null) {
				model.add(subj, pred, obj, ctx);
			} else {
				model.add(subj, pred, obj);
			}
		};
	}

	public static class LexicalEntry {

		private Resource id;
		private List<Form> lemma;
		private List<Form> otherForms;
		private List<Sense> senses;

		@JsonSerialize(using = ToStringSerializer.class)
		public Resource getID() {
			return id;
		}

		public List<Form> getLemma() {
			return lemma;
		}

		public List<Form> getOtherForms() {
			return otherForms;
		}

		public List<Sense> getSenses() {
			return senses;
		}

		public static LexicalEntry parse(Model input, Resource lexicalEntry) {
			LexicalEntry lexicalEntryObj = new LexicalEntry();
			lexicalEntryObj.id = lexicalEntry;

			List<Form> lemma = parseForms(input, lexicalEntry, ONTOLEX.CANONICAL_FORM);
			List<Form> otherForms = parseForms(input, lexicalEntry, ONTOLEX.OTHER_FORM);

			Map<Resource, Sense> senses = new HashMap<>();

			for (Statement stmt : input.filter(lexicalEntry, ONTOLEX.SENSE, null)) {
				Resource sense = (Resource) stmt.getObject();

				Sense senseObj = senses.computeIfAbsent(sense, s -> {
					Sense so = new Sense();
					so.id = s;
					return so;
				});

			}

			for (Statement stmt : input.filter(null, ONTOLEX.IS_SENSE_OF, lexicalEntry)) {
				Resource sense = (Resource) stmt.getSubject();

				Sense senseObj = senses.computeIfAbsent(sense, s -> {
					Sense so = new Sense();
					so.id = s;
					return so;
				});
			}

			for (Sense senseObj : senses.values()) {
				Map<Value, List<Statement>> ref2statements = input
						.filter(senseObj.id, ONTOLEX.REFERENCE, null).stream()
						.collect(Collectors.groupingBy(Statement::getObject));
				Map<Value, List<Statement>> ref2invStatements = input
						.filter(null, ONTOLEX.IS_REFERENCE_OF, senseObj.id).stream()
						.collect(Collectors.groupingBy(Statement::getSubject));

				ref2invStatements.forEach((k, v) -> ref2statements.merge(k, v, (v1, v2) -> {
					List<Statement> v3 = new ArrayList<>(v1);
					v3.addAll(v2);
					return v3;
				}));
				senseObj.reference = ref2statements.keySet().stream()
						.map(r -> new AnnotatedValue<>((Resource) r)).collect(Collectors.toList());
			}

			for (Sense senseObj : senses.values()) {
				Map<Value, List<Statement>> concept2statements = input
						.filter(senseObj.id, ONTOLEX.IS_LEXICALIZED_SENSE_OF, null).stream()
						.collect(Collectors.groupingBy(Statement::getObject));
				Map<Value, List<Statement>> concept2invStatements = input
						.filter(null, ONTOLEX.LEXICALIZED_SENSE, senseObj.id).stream()
						.collect(Collectors.groupingBy(Statement::getSubject));

				concept2invStatements.forEach((k, v) -> concept2statements.merge(k, v, (v1, v2) -> {
					List<Statement> v3 = new ArrayList<>(v1);
					v3.addAll(v2);
					return v3;
				}));
				senseObj.concept = concept2statements.keySet().stream()
						.map(c -> new AnnotatedValue<>((Resource) c)).collect(Collectors.toList());

				for (AnnotatedValue<Resource> c : senseObj.concept) {
					Map<Value, List<Statement>> def2statements = input
							.filter(c.getValue(), SKOS.DEFINITION, null).stream()
							.collect(Collectors.groupingBy(Statement::getObject));
					senseObj.definition = def2statements.keySet().stream()
							.map(def -> new AnnotatedValue<>((Literal) def)).collect(Collectors.toList());
				}
			}

			lexicalEntryObj.lemma = lemma;
			lexicalEntryObj.otherForms = otherForms;
			lexicalEntryObj.senses = new ArrayList<>(senses.values());

			return lexicalEntryObj;
		}

		protected static List<Form> parseForms(Model input, Resource lexicalEntry, IRI pred) {
			return Models.getPropertyResources(input, lexicalEntry, pred).stream()
					.map(form -> Form.parse(input, (Resource) form)).collect(Collectors.toList());
		}
	}

	public static class Form {

		private Resource id;
		private List<AnnotatedValue<Literal>> writtenRep;
		private List<AnnotatedValue<Literal>> phoneticRep;

		@JsonSerialize(using = ToStringSerializer.class)
		public Resource getID() {
			return id;
		}

		public List<AnnotatedValue<Literal>> getWrittenRep() {
			return writtenRep;
		}

		public List<AnnotatedValue<Literal>> getPhoneticRep() {
			return phoneticRep;
		}

		public static Form parse(Model input, Resource form) {
			List<AnnotatedValue<Literal>> writtenRep = parseRepresentations(input, form, ONTOLEX.WRITTEN_REP);
			List<AnnotatedValue<Literal>> phoneticRep = parseRepresentations(input, form,
					ONTOLEX.PHONETIC_REP);

			Form formObj = new Form();
			formObj.id = form;
			formObj.writtenRep = writtenRep;
			formObj.phoneticRep = phoneticRep;

			return formObj;
		}

		protected static List<AnnotatedValue<Literal>> parseRepresentations(Model input, Resource form,
				IRI pred) {
			return Models.getPropertyLiterals(input, form, pred).stream().map(r -> new AnnotatedValue<>(r))
					.collect(Collectors.toList());
		}
	}

	public static class Sense {
		@JsonSerialize(using = ToStringSerializer.class)
		private @Nullable Resource id;
		private List<AnnotatedValue<Literal>> definition;
		private List<AnnotatedValue<Resource>> reference;
		private List<AnnotatedValue<Resource>> concept;

		public @Nullable Resource getId() {
			return id;
		}

		public List<AnnotatedValue<Resource>> getConcept() {
			return concept;
		}

		public List<AnnotatedValue<Literal>> getDefinition() {
			return definition;
		}

		public List<AnnotatedValue<Resource>> getReference() {
			return reference;
		}

	}

	@STServiceOperation
	@Read
	public LexicalEntry getLexicalEntryView(Resource lexicalEntry) {
		TupleQuery inputQuery = getManagedConnection().prepareTupleQuery(
		// @formatter:off
			"SELECT * WHERE {                                                    \n" +
			"    ?resource (<http://www.w3.org/ns/lemon/ontolex#canonicalForm>|<http://www.w3.org/ns/lemon/ontolex#otherForm>|(<http://www.w3.org/ns/lemon/ontolex#sense>|^<http://www.w3.org/ns/lemon/ontolex#isSenseOf>)/(<http://www.w3.org/ns/lemon/ontolex#isReferenceOf>|<http://www.w3.org/ns/lemon/ontolex#isLexicalizedSenseOf>|^<http://www.w3.org/ns/lemon/ontolex#lexicalizedSense>)?)? ?s . \n" +
			"    GRAPH ?g {                                                      \n" +
			"    	?s ?p ?o .                                                   \n" +
			"    }                                                               \n" +
			"}                                                                   \n"
			// @formatter:on
		);
		inputQuery.setBinding("resource", lexicalEntry);

		Model input = new LinkedHashModel();
		inputQuery.evaluate(new BindingSets2Model(input));

		return LexicalEntry.parse(input, lexicalEntry);
	}

}
