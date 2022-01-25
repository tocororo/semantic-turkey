package it.uniroma2.art.semanticturkey.settings.facets;

import it.uniroma2.art.semanticturkey.exceptions.InvalidProjectNameException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectAccessException;
import it.uniroma2.art.semanticturkey.exceptions.ProjectInexistentException;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.project.ProjectInfo;
import it.uniroma2.art.semanticturkey.project.ProjectManager;
import it.uniroma2.art.semanticturkey.properties.PropertyNotFoundException;
import it.uniroma2.art.semanticturkey.properties.STProperties;
import it.uniroma2.art.semanticturkey.properties.dynamic.DynamicSTProperties;
import it.uniroma2.art.semanticturkey.resources.Resources;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ProjectFacetsIndexUtils {

	public static final int MAX_RESULT_QUERY_FACETS = 500;

	public static final String indexMainDir = "index";

	public static final String lucDirName = "facetsIndex";

	public static final String PROJECT_NAME = "prjName";
	public static final String PROJECT_MODEL = "prjModel";
	public static final String PROJECT_LEX_MODEL = "prjLexModel";
	public static final String PROJECT_HISTORY = "prjHistoryEnabled";
	public static final String PROJECT_VALIDATION = "prjValidationEnabled";
	public static final String PROJECT_DESCRIPTION = "prjDescription";

	public static void createFacetIndexAPI(List<ProjectInfo> projectInfoList)
			throws PropertyNotFoundException, InvalidProjectNameException,
			ProjectAccessException, IOException {
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		try {
			Directory directory = FSDirectory.open(getLuceneDir().toPath());
			SimpleAnalyzer simpleAnalyzer = new SimpleAnalyzer();
			IndexWriterConfig config = new IndexWriterConfig(simpleAnalyzer);
			try (IndexWriter writer = new IndexWriter(directory, config)) {
				// clear all indexes
				writer.deleteAll();
				// add the projects facets and properties to the index (one project at a time)
				for (ProjectInfo projectInfo : projectInfoList) {
					addProjectToIndex(projectInfo, false, writer);
				}
			}
		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}
	}

	public static void recreateFacetIndexForProjectAPI(String projectName, ProjectInfo projectInfo)
			throws PropertyNotFoundException, InvalidProjectNameException, ProjectInexistentException,
			ProjectAccessException, IOException {
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		try {
			Project project = ProjectManager.getProject(projectName);

			Directory directory = FSDirectory.open(getLuceneDir().toPath());
			SimpleAnalyzer simpleAnalyzer = new SimpleAnalyzer();
			IndexWriterConfig config = new IndexWriterConfig(simpleAnalyzer);
			try (IndexWriter writer = new IndexWriter(directory, config)) {
				addProjectToIndex(projectInfo, true, writer);
			}
		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}
	}


	private static void addProjectToIndex(ProjectInfo projectInfo, boolean removePrevIndex,
			IndexWriter writer) throws InvalidProjectNameException,
			ProjectAccessException, IOException {
		ProjectForIndex projectForIndex = new ProjectForIndex();

		String projectName = projectInfo.getName();

		projectForIndex.addNameValue(PROJECT_NAME, projectInfo.getName());
		projectForIndex.addNameValue(PROJECT_MODEL, projectInfo.getModel());
		projectForIndex.addNameValue(PROJECT_LEX_MODEL, projectInfo.getLexicalizationModel());
		projectForIndex.addNameValue(PROJECT_HISTORY, Boolean.toString(projectInfo.isHistoryEnabled()));
		projectForIndex.addNameValue(PROJECT_VALIDATION, Boolean.toString(projectInfo.isValidationEnabled()));
		projectForIndex.addNameValue(PROJECT_DESCRIPTION, projectInfo.getDescription());

		// for each project, get all the facets
		ProjectFacets projectFacets = projectInfo.getFacets();
		for (String propName : projectFacets.getProperties()) {
			try {
				String propValue = null;
				Object value = projectFacets.getPropertyValue(propName);
				if (value instanceof DynamicSTProperties) {
					getValuesFromSTProperties((STProperties) value, projectForIndex);
				} else if (value != null) {
					propValue = normalizeFacetValue(value);
					if (propValue != null && !propValue.isEmpty()) {
						projectForIndex.addNameValue(propName, propValue.toString());
					}
				}
			} catch (PropertyNotFoundException e) {
				// the facet was not found, so skip it and pass to the next one
			}
		}

		// remove the previous entry, if needed
		if (removePrevIndex) {
			BooleanQuery.Builder builderBoolean = new BooleanQuery.Builder();
			builderBoolean.add(new TermQuery(new Term(PROJECT_NAME, projectName)), BooleanClause.Occur.MUST);
			writer.deleteDocuments(builderBoolean.build());
		}
		// add ProjectForIndex in the index
		addProjectForIndexToIndex(projectForIndex, writer);
	}

	public static Optional<Document> getDocumentForProject(String name) throws IOException {
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		try {

			BooleanQuery.Builder builderBoolean = new BooleanQuery.Builder();
			builderBoolean.add(new TermQuery(new Term(PROJECT_NAME, name)), BooleanClause.Occur.MUST);

			IndexSearcher searcher = ProjectFacetsIndexUtils.createSearcher();
			TopDocs topDocs = searcher.search(builderBoolean.build(), 1);
			ScoreDoc[] scoreDocs = topDocs.scoreDocs;

			if (scoreDocs == null || scoreDocs.length == 0) {
				return Optional.empty();
			} else {
				return Optional.of(searcher.doc(scoreDocs[0].doc));
			}
		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}
	}

	public static String normalizeFacetValue(Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof IRI) {
			return NTriplesUtil.toNTriplesString((IRI) value);
		} else if (value instanceof Literal) {
			return NTriplesUtil.toNTriplesString((Literal) value);
		} else {
			return value.toString();
		}
	}

	private static void getValuesFromSTProperties(STProperties stProperties, ProjectForIndex projectForIndex)
			throws PropertyNotFoundException {
		Collection<String> propertiesList = stProperties.getProperties();
		for (String propName : propertiesList) {
			if (stProperties.getPropertyValue(propName) == null) {
				// no value, so skip it
				continue;
			}
			String valueString = normalizeFacetValue(stProperties.getPropertyValue(propName));
			projectForIndex.addNameValue(propName, valueString);
		}
	}

	public static IndexSearcher createSearcher() throws IOException {
		Directory directory = FSDirectory.open(getLuceneDir().toPath());
		IndexReader reader = DirectoryReader.open(directory);
		return new IndexSearcher(reader);
	}

	public static boolean isLuceneDirPresent() {
		String mainIndexPath = Resources.getSemTurkeyDataDir() + File.separator + indexMainDir;
		File mainIndexDir = new File(mainIndexPath);
		if (!mainIndexDir.exists()) {
			mainIndexDir.mkdir();
		}
		File luceneIndexDir = new File(mainIndexDir, lucDirName);
		if (!luceneIndexDir.exists()) {
			return false;
		}
		return true;
	}

	public static File getLuceneDir() {

		String mainIndexPath = Resources.getSemTurkeyDataDir() + File.separator + indexMainDir;
		File mainIndexDir = new File(mainIndexPath);
		if (!mainIndexDir.exists()) {
			mainIndexDir.mkdir();
		}
		// String luceneIndexDirPath = Resources.getSemTurkeyDataDir()+File.separator+lucDirName;
		// File luceneIndexDir = new File(luceneIndexDirPath);
		File luceneIndexDir = new File(mainIndexDir, lucDirName);
		if (!luceneIndexDir.exists()) {
			luceneIndexDir.mkdir();
		}

		return luceneIndexDir;
	}

	private static void addProjectForIndexToIndex(ProjectForIndex projectForIndex, IndexWriter writer)
			throws IOException {
		Document doc = new Document();
		for (String propName : projectForIndex.getNameList()) {
			String propValue = projectForIndex.getValue(propName);
			if (propValue != null && !propValue.isEmpty()) {
				doc.add(new StringField(propName, propValue, Field.Store.YES));
			}
		}
		writer.addDocument(doc);
	}

	public static void deleteProjectFromFacetIndex(String projectName) throws IOException {
		ClassLoader oldCtxClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(IndexWriter.class.getClassLoader());
		Directory directory = FSDirectory.open(getLuceneDir().toPath());
		SimpleAnalyzer simpleAnalyzer = new SimpleAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(simpleAnalyzer);
		try (IndexWriter writer = new IndexWriter(directory, config)) {
			deleteProjectFromFacetIndex(projectName, writer);
		} finally {
			Thread.currentThread().setContextClassLoader(oldCtxClassLoader);
		}
	}

	public static void deleteProjectFromFacetIndex(String projectName, IndexWriter writer)
			throws IOException {
		BooleanQuery.Builder builderBoolean = new BooleanQuery.Builder();
		builderBoolean.add(new TermQuery(new Term(PROJECT_NAME, projectName)), BooleanClause.Occur.MUST);
		writer.deleteDocuments(builderBoolean.build());
	}

	private static class ProjectForIndex {
		Map<String, String> nameValueForIndexMap = new HashMap<>();

		public ProjectForIndex() {
		}

		public Set<String> getNameList() {
			return nameValueForIndexMap.keySet();
		}

		public String getValue(String name) {
			return nameValueForIndexMap.get(name);
		}

		public boolean addNameValue(String name, String value) {
			if (nameValueForIndexMap.containsKey(name)) {
				return false;
			}
			nameValueForIndexMap.put(name, value);
			return true;
		}
	}
}
