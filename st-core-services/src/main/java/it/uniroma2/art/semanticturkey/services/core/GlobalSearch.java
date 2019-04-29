package it.uniroma2.art.semanticturkey.services.core;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.search.SearchMode;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.annotations.Write;

@STService
public class GlobalSearch extends STServiceAdapter {

	protected static Logger logger = LoggerFactory.getLogger(GlobalSearch.class);

	private final String lucDirName = "luceneIndex";

	private final int MAX_RESULTS = 100;

	// private static String CLASS_ROLE = "class";
	// private static String CONCEPT_ROLE = "concept";
	// private static String INSTANCE_ROLE = "instance";

	//@formatter:off
	/*protected SearchStrategy instantiateSearchStrategy() {
		SearchStrategies searchStrategy = STRepositoryInfoUtils
				.getSearchStrategy(getProject().getRepositoryManager()
						.getSTRepositoryInfo(STServiceContextUtils.getRepostoryId(stServiceContext)));

		return SearchStrategyUtils.instantiateSearchStrategy(searchStrategy);
	}*/
	//@formatter:on

	/**
	 * Create the Lucene index for the current project
	 */
	@STServiceOperation
	@Write
	// TODO decide the @PreAuthorize
	public void createIndex() throws Exception {
		// classloader magic
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		try {
			Directory directory = FSDirectory.open(getLuceneDir().toPath());

			SimpleAnalyzer simpleAnalyzer = new SimpleAnalyzer();
			IndexWriterConfig config = new IndexWriterConfig(simpleAnalyzer);
			try (IndexWriter writer = new IndexWriter(directory, config)) {

				// TODO decide what data should be taken to be indexes (maybe depending on the Lexicalization
				// Model)

				// TODO now, consider only skosxl:prefLabel and skosxl:altLabel
				String query = "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>"
						+ "\nPREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>"
						+ "\nSELECT ?resource ?lexProp ?label" + "\nWHERE{" + "\n?resource a skos:Concept ."
						+ "\n?resource ?lexProp ?xlabel ." + "\n?xlabel skosxl:literalForm ?label ." + "\n}";

				RepositoryConnection conn = getManagedConnection();
				TupleQuery tupleQuery = conn.prepareTupleQuery(query);
				try (TupleQueryResult tupleQueryResult = tupleQuery.evaluate()) {
					while (tupleQueryResult.hasNext()) {
						BindingSet bindingSet = tupleQueryResult.next();
						String resourceIRI = bindingSet.getValue("resource").stringValue();
						String lexProp = bindingSet.getValue("lexProp").stringValue();
						Literal labelLiteral = ((Literal) bindingSet.getValue("label"));
						String label = labelLiteral.getLabel();
						String lang = labelLiteral.getLanguage().get();

						String repId = getProject().getName();

						writer.addDocument(addResourceWithLabel(
								new ResourceWithLabel(resourceIRI, lang, label, lexProp, repId)));
					}
				}

			}
		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}
	}

	/**
	 * Remove from the Lucene index all the information of the current project
	 * 
	 * @throws Exception
	 */
	@STServiceOperation
	@Write
	// TODO decide the @PreAuthorize
	public void clearIndex() throws Exception {
		// classloader magic
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		try {
			Directory directory = FSDirectory.open(getLuceneDir().toPath());
			try (IndexWriter writer = new IndexWriter(directory,
					new IndexWriterConfig(new SimpleAnalyzer()))) {

				Map<String, String> nameValueSearchMap = new HashMap<String, String>();
				nameValueSearchMap.put("repId", getProject().getName());
				Builder builder = new BooleanQuery.Builder();
				for (String name : nameValueSearchMap.keySet()) {
					String value = nameValueSearchMap.get(name);

					QueryParser qp = new QueryParser(name, new SimpleAnalyzer());
					Query query = qp.parse("\"" + value + "\"");

					// builder.add(new BooleanClause(query, Occur.MUST));
					builder.add(query, Occur.MUST);
				}

				writer.deleteDocuments(builder.build());
			}
		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}
	}

	/**
	 * Search in the Lucene index all resources/project matching the input string
	 * 
	 * @param searchString
	 * @param lang
	 * @param searchMode
	 *            can only be startsWith or
	 * @throws Exception
	 */
	@STServiceOperation
	@Write
	// TODO decide the @PreAuthorize
	public JsonNode search(String searchString, @Optional List<String> langs,
			@Optional(defaultValue = "exact") SearchMode searchMode,
			@Optional(defaultValue = "0") int maxResults) throws Exception {
		// classloader magic
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		try {

			JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
			ObjectNode responseJson = jsonFactory.objectNode();

			Builder builderBooleanGlobal = new BooleanQuery.Builder();
			Map<String, String> nameValueSearchMap = new HashMap<String, String>();
			// TODO decide what to do for the mode
			if (searchMode.equals(SearchMode.exact)) {
//				 nameValueSearchMap.put("label", "\"" + searchString + "\"");
				nameValueSearchMap.put("label", searchString);
			} else if (searchMode.equals(SearchMode.startsWith)) {
//				 nameValueSearchMap.put("label", "\"" + searchString + "*\"");
				nameValueSearchMap.put("label", searchString + "*");
			} else {
				// this should not happen
				throw new IllegalArgumentException("searchMode should be " + SearchMode.exact + " or "
						+ SearchMode.startsWith + ", no other values are accepted");
			}

			if (langs != null && langs.size() > 0) {
				String langString = "";
				for (String lang : langs) {
					langString += lang + " ";
				}
				nameValueSearchMap.put("lang", langString.trim());
			}

			for (String name : nameValueSearchMap.keySet()) {
				Query query = null;
				String value = nameValueSearchMap.get(name);
				// behave differently according to the field used for the search
				if (name.equals("lang")) {
					Builder builderBoolean = new BooleanQuery.Builder(); // (0, BooleanClause.Occur.SHOULD);
					String[] valueArray = value.split(" ");
					for (String lang : valueArray) {
						builderBoolean.add(new TermQuery(new Term(name, lang)), Occur.SHOULD);
					}
					query = builderBoolean.build();
				} else if (name.equals("label")) {
					// split the value into single words
					String[] valueArray = value.split(" ");
					PhraseQuery.Builder builderTemp = new PhraseQuery.Builder();
					int count = 0;
					for (String singleValue : valueArray) {
						builderTemp.add(new Term(name, singleValue), count++);
					}
					query = builderTemp.build();
				}

				builderBooleanGlobal.add(query, Occur.MUST);
			}

			BooleanQuery booleanQuery = builderBooleanGlobal.build();
			System.out.println("query = " + booleanQuery.toString());
			if (maxResults == 0) {
				maxResults = MAX_RESULTS;
			}

			IndexSearcher searcher = createSearcher();
			TopDocs hits = searcher.search(booleanQuery, maxResults);

			// construct the response
			ArrayNode jsonArray = jsonFactory.arrayNode();
			for (ScoreDoc sd : hits.scoreDocs) {
				Document doc = searcher.doc(sd.doc);
				ObjectNode json = jsonFactory.objectNode();
				json.set("resource", jsonFactory.textNode(doc.get("resource")));
				json.set("lang", jsonFactory.textNode(doc.get("lang")));
				json.set("repId", jsonFactory.textNode(doc.get("repId")));
				json.set("labelType", jsonFactory.textNode(doc.get("labelType")));
				json.set("label", jsonFactory.textNode(doc.get("label")));

				jsonArray.add(json);
			}
			responseJson.set("results", jsonArray);

			return responseJson;
		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}
	}

	private IndexSearcher createSearcher() throws IOException {
		Directory directory = FSDirectory.open(getLuceneDir().toPath());
		IndexReader reader = DirectoryReader.open(directory);
		IndexSearcher searcher = new IndexSearcher(reader);
		return searcher;
	}

	private File getLuceneDir() {
		String luceneIndexDirPath = Resources.getSemTurkeyDataDir() + File.separator + lucDirName;
		File luceneIndexDir = new File(luceneIndexDirPath);
		if (!luceneIndexDir.exists()) {
			luceneIndexDir.mkdir();
		}
		return luceneIndexDir;
	}

	private Document addResourceWithLabel(ResourceWithLabel resourceWithLabel) throws IOException {
		Document doc = new Document();

		/*
		 * doc.add(new TextField("concept", conceptWithLabel.getConcept(), Field.Store.YES)); doc.add(new
		 * TextField("lang", conceptWithLabel.getLang(), Field.Store.YES)); doc.add(new TextField("label",
		 * conceptWithLabel.getLabel(), Field.Store.YES)); doc.add(new TextField("repId",
		 * conceptWithLabel.getRepId(), Field.Store.YES)); doc.add(new TextField("labelType",
		 * conceptWithLabel.getLabelType(), Field.Store.YES));
		 */

		doc.add(new StringField("resource", resourceWithLabel.getResource(), Field.Store.YES));
		doc.add(new StringField("lang", resourceWithLabel.getLang(), Field.Store.YES));
		doc.add(new StringField("repId", resourceWithLabel.getRepId(), Field.Store.YES));
		doc.add(new StringField("labelType", resourceWithLabel.getLabelType(), Field.Store.YES));
		doc.add(new TextField("label", resourceWithLabel.getLabel(), Field.Store.YES));

		return doc;
	}

	private class ResourceWithLabel {
		private String resource;
		private String lang;
		private String label;
		private String labelType;
		private String repId;

		public ResourceWithLabel(String resource, String lang, String label, String labelType, String repId) {
			this.resource = resource;
			this.lang = lang;
			this.label = label;
			this.labelType = labelType;
			this.repId = repId;
		}

		public String getResource() {
			return resource;
		}

		public String getLang() {
			return lang;
		}

		public String getLabel() {
			return label;
		}

		public String getLabelType() {
			return labelType;
		}

		public String getRepId() {
			return repId;
		}

	}

}
