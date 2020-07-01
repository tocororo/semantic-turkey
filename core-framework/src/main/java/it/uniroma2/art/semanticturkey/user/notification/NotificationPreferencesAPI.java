package it.uniroma2.art.semanticturkey.user.notification;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.resources.Resources;
import it.uniroma2.art.semanticturkey.user.STUser;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class NotificationPreferencesAPI {

	private static final NotificationPreferencesAPI sharedInsance = new NotificationPreferencesAPI();

	private final String indexMainDirName = "index";
	private final String luceneDirName = "notificationPrefIndex";

	private final String SEPARATOR = "|_|";

	private final String USER_FIELD = "user";
	private final String PROJ_ROLE_ACT_FIELD = "project-role-action";
	private final String PROJ_RES_FIELD = "project-resource";

	private int MAX_RESULTS = 200;

	//list of roles allowed in notifications
	public static final List<RDFResourceRole> availableRoles = Arrays.asList(RDFResourceRole.cls, RDFResourceRole.concept,
			RDFResourceRole.conceptScheme, RDFResourceRole.dataRange, RDFResourceRole.individual,
			RDFResourceRole.limeLexicon, RDFResourceRole.ontolexForm, RDFResourceRole.ontolexLexicalEntry,
			RDFResourceRole.ontolexLexicalSense, RDFResourceRole.ontology, RDFResourceRole.property,
			RDFResourceRole.skosCollection, RDFResourceRole.skosOrderedCollection, RDFResourceRole.xLabel
	);

	public enum Action {any, creation, deletion, update}

	public static NotificationPreferencesAPI getInstance() {
		return sharedInsance;
	}

    /*private IndexWriter createIndexWriter() throws IOException {
        Directory directory = FSDirectory.open(new File(luceneDirName).toPath());
        SimpleAnalyzer simpleAnalyzer = new SimpleAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(simpleAnalyzer);
        IndexWriter writer = new IndexWriter(directory, config);
        return writer;
    }*/

	/**
	 * Returns the resources that a user is watching in a project.
	 * @param project
	 * @param user
	 * @return
	 * @throws IOException
	 */
	public List<Resource> listResourcesFromUserInProject(Project project, STUser user) throws IOException {
		List<Resource> resources = new ArrayList<>();
		ValueFactory vf = SimpleValueFactory.getInstance();
		List<String> projResRecords = this.searchResourceFromUser(user);
		for (String record: projResRecords) {
			String[] projRes = record.split(Pattern.quote(SEPARATOR));
			if (projRes[0].equals(project.getName())) {
				resources.add(NTriplesUtil.parseResource(projRes[1], vf));
			}
		}
		return resources;
	}

	public Map<RDFResourceRole, List<Action>> getRoleActionsNotificationPreferences(Project project, STUser user) throws IOException {
		Map<RDFResourceRole, List<Action>> roleActionsMap = new HashMap<>();
		for (RDFResourceRole role: availableRoles) { //setup the list for all the admitted roles
			roleActionsMap.put(role, new ArrayList<>());
		}
		List<String> projRuleActionRecords = this.searchProjRoleActionFromUser(user);
		for (String record: projRuleActionRecords) {
			String[] projRuleAction = record.split(Pattern.quote(SEPARATOR));
			if (projRuleAction[0].equals(project.getName())) {
				RDFResourceRole role = RDFResourceRole.valueOf(projRuleAction[1]);
				Action action = Action.valueOf(projRuleAction[2]);
				if (roleActionsMap.containsKey(role)) {
					roleActionsMap.get(role).add(action);
				}
			}
		}
		return roleActionsMap;
	}

	private List<String> searchResourceFromUser(STUser user) throws IOException {
		List<String> projResList = new ArrayList<>();
		BooleanQuery.Builder builderBoolean = new BooleanQuery.Builder();
		builderBoolean.add(new TermQuery(new Term(USER_FIELD, user.getIRI().stringValue())), BooleanClause.Occur.MUST);
		Query query = builderBoolean.build();
		IndexSearcher searcher = createSearcher();
		TopDocs hits = searcher.search(query, MAX_RESULTS);
		for (ScoreDoc sd : hits.scoreDocs) {
			Document doc = searcher.doc(sd.doc);
			IndexableField[] indexableFieldArray = doc.getFields(PROJ_RES_FIELD);
			for (IndexableField indexableField : indexableFieldArray) {
				String proj_res = indexableField.stringValue();
				projResList.add(proj_res);
			}
		}
		return projResList;
	}

	private List<String> searchProjRoleActionFromUser(STUser user) throws IOException {
		List<String> projRoleActionList = new ArrayList<>();
		BooleanQuery.Builder builderBoolean = new BooleanQuery.Builder();
		builderBoolean.add(new TermQuery(new Term(USER_FIELD, user.getIRI().stringValue())), BooleanClause.Occur.MUST);
		Query query = builderBoolean.build();
		IndexSearcher searcher = createSearcher();
		TopDocs hits = searcher.search(query, MAX_RESULTS);
		//each user is a single doc, so there should be 0 or 1 document in the result
		for (ScoreDoc sd : hits.scoreDocs) {
			Document doc = searcher.doc(sd.doc);
			IndexableField[] indexableFieldArray = doc.getFields(PROJ_ROLE_ACT_FIELD);
			for (IndexableField indexableField : indexableFieldArray) {
				String proj_role_action = indexableField.stringValue();
				projRoleActionList.add(proj_role_action);
			}
		}
		return projRoleActionList;
	}

	public List<String> searchUserFromProjRes(Project project, Resource resource) throws IOException {
		List<String> userList = new ArrayList<>();
		TopDocs hits;
		String resNT = NTriplesUtil.toNTriplesString(resource);
		IndexSearcher searcher = createSearcher();
		BooleanQuery.Builder builderBoolean = new BooleanQuery.Builder();
		if (project != null) {
			String projRes = project.getName() + SEPARATOR + resNT;
			builderBoolean.add(new TermQuery(new Term(PROJ_RES_FIELD, projRes)), BooleanClause.Occur.MUST);
		} else {
			WildcardQuery wildcardQuery = new WildcardQuery(new Term(PROJ_RES_FIELD, "*" + SEPARATOR + resNT));
			builderBoolean.add(wildcardQuery, BooleanClause.Occur.MUST);
		}
		Query query = builderBoolean.build();
		hits = searcher.search(query, MAX_RESULTS);

		for (ScoreDoc sd : hits.scoreDocs) {
			Document doc = searcher.doc(sd.doc);
			IndexableField[] indexableFieldArray = doc.getFields(USER_FIELD);
			for (IndexableField indexableField : indexableFieldArray) {
				String user = indexableField.stringValue();
				userList.add(user);
			}
		}
		return userList;
	}

	public List<String> searchUserFromProjRoleAction(Project project, RDFResourceRole role, Action action) throws IOException {
		//role could be null in this case, no restriction is applied for the role

		List<String> userList = new ArrayList<>();
		TopDocs hits;

		//construct the String that will be used during the search
		boolean useWildCard = false;
		String searchString = project.getName() + SEPARATOR;
		if (role == RDFResourceRole.undetermined) {
			searchString += "*" + SEPARATOR;
			useWildCard = true;
		} else {
			searchString += role.name() + SEPARATOR;
		}
		if (action == Action.any) {
			searchString += "*";
			useWildCard = true;
		} else {
			searchString += action.name();
		}

		IndexSearcher searcher = createSearcher();
		BooleanQuery.Builder builderBoolean = new BooleanQuery.Builder();
		if (!useWildCard) {
			builderBoolean.add(new TermQuery(new Term(PROJ_ROLE_ACT_FIELD, searchString)), BooleanClause.Occur.MUST);
		} else {
			WildcardQuery wildcardQuery = new WildcardQuery(new Term(PROJ_ROLE_ACT_FIELD, searchString));
			builderBoolean.add(wildcardQuery, BooleanClause.Occur.MUST);
		}
		Query query = builderBoolean.build();
		hits = searcher.search(query, MAX_RESULTS);

		for (ScoreDoc sd : hits.scoreDocs) {
			Document doc = searcher.doc(sd.doc);
			IndexableField[] indexableFieldArray = doc.getFields(USER_FIELD);
			for (IndexableField indexableField : indexableFieldArray) {
				String user = indexableField.stringValue();
				userList.add(user);
			}
		}
		return userList;
	}

	public boolean addToUser(STUser user, Project project, Resource resource) throws IOException {
		String resNT = NTriplesUtil.toNTriplesString(resource);
		String valueToAdd = project.getName() + SEPARATOR + resNT;
		//Document doc = getDocumentFromUser(user);
		List<Document> documentList = getDocumentsFromUser(user);
		if(documentList.isEmpty()){
			Document doc = new Document();
			doc.add(new StringField(USER_FIELD, user.getIRI().stringValue(), Field.Store.YES));
			documentList.add(doc);
		}
		if(documentList.size()==1){
			//check if the project-resource is already present
			Document doc = documentList.get(0);
			IndexableField[] indexableFields = doc.getFields(PROJ_RES_FIELD);
			for (IndexableField indexableField : indexableFields) {
				if (valueToAdd.equals(indexableField.stringValue())) {
					return false;
				}
			}
		}
		try (IndexWriter writer = createIndexWriter()) {
			Document newDoc = cloneAndCompactDocuments(documentList);
			newDoc.add(new StringField(PROJ_RES_FIELD, valueToAdd, Field.Store.YES));
			writer.deleteDocuments(new Term(USER_FIELD, user.getIRI().stringValue()));
			writer.addDocument(newDoc);
		}
		return true;
	}

	public boolean addToUser(STUser user, Project project, RDFResourceRole role, Action action) throws IOException {
		String valueToAdd = project.getName() + SEPARATOR + role.name() + SEPARATOR + action.name();
		List <Document> documentList = getDocumentsFromUser(user);
		if(documentList.isEmpty()){
			Document doc = new Document();
			doc.add(new StringField(USER_FIELD, user.getIRI().stringValue(), Field.Store.YES));
			documentList.add(doc);
		}
		if(documentList.size()==1){
			//check if the project-role-action is already present
			Document doc = documentList.get(0);
			IndexableField[] indexableFields = doc.getFields(PROJ_ROLE_ACT_FIELD);
			for (IndexableField indexableField : indexableFields) {
				if (valueToAdd.equals(indexableField.stringValue())) {
					return false;
				}
			}
		}
		try (IndexWriter writer = createIndexWriter()) {
			Document newDoc = cloneAndCompactDocuments(documentList);
			newDoc.add(new StringField(PROJ_ROLE_ACT_FIELD, valueToAdd, Field.Store.YES));
			writer.deleteDocuments(new Term(USER_FIELD, user.getIRI().stringValue()));
			writer.addDocument(newDoc);
		}

		return true;
	}

	public void addToUser(STUser user, Project project, Map<RDFResourceRole, List<Action>> preferences) throws IOException {
		List<Document> documentList = getDocumentsFromUser(user);
		if(documentList.isEmpty()){
			Document doc = new Document();
			doc.add(new StringField(USER_FIELD, user.getIRI().stringValue(), Field.Store.YES));
			documentList.add(doc);
		}
		try (IndexWriter writer = createIndexWriter()) {
			Document newDoc = cloneAndCompactDocumentMinusField(documentList, PROJ_ROLE_ACT_FIELD);
			//add the value from the map preferences
			for (RDFResourceRole role : preferences.keySet()) {
				for(Action action : preferences.get(role)){
					String valueToAdd = project.getName() + SEPARATOR + role.name() + SEPARATOR + action.name();
					newDoc.add(new StringField(PROJ_ROLE_ACT_FIELD, valueToAdd, Field.Store.YES));
				}
			}
			writer.deleteDocuments(new Term(USER_FIELD, user.getIRI().stringValue()));
			writer.addDocument(newDoc);
		}
	}

	public boolean removeUser(STUser user) throws IOException {
		List<Document> documentList = getDocumentsFromUser(user);
		if (documentList.isEmpty()) {
			return false;
		}
		try (IndexWriter writer = createIndexWriter()) {
			writer.deleteDocuments(new Term(USER_FIELD, user.getIRI().stringValue()));
		}
		return true;
	}

	public boolean removeProjResFromUser(STUser user, Project project, Resource resource) throws IOException {
        String escapedSeparator = Pattern.quote(SEPARATOR);
		String resNT = NTriplesUtil.toNTriplesString(resource);
		String fieldValueRegex = project.getName() + escapedSeparator + resNT;
		List<Document> documentList = getDocumentsFromUser(user);
		if (documentList.isEmpty()) {
			return false;
		}
		try (IndexWriter writer = createIndexWriter()) {
			Document newDoc = cloneAndCompactDocumentMinusField(documentList, PROJ_RES_FIELD, fieldValueRegex);
			writer.deleteDocuments(new Term(USER_FIELD, user.getIRI().stringValue()));
			writer.addDocument(newDoc);
		}
		return true;
	}

	public boolean removeProjRoleActionFromUser(STUser user, Project project, RDFResourceRole role, Action action) throws IOException {
        //String escapedSeparator = SEPARATOR.replace("|", "\\|");
        String escapedSeparator = Pattern.quote(SEPARATOR);
		String fieldValueRegex = project.getName() + escapedSeparator;
		if (role.equals(RDFResourceRole.undetermined)) {
			fieldValueRegex += ".+" + escapedSeparator;
		} else {
			fieldValueRegex += role.name() + escapedSeparator;
		}
		if (action.equals(Action.any)) {
			fieldValueRegex += ".+";
		} else {
			fieldValueRegex += action.name();
		}

		List<Document> documentList = getDocumentsFromUser(user);
		if (documentList.isEmpty()) {
			return false;
		}
		try (IndexWriter writer = createIndexWriter()) {
			Document newDoc = cloneAndCompactDocumentMinusField(documentList, PROJ_ROLE_ACT_FIELD, fieldValueRegex);
			writer.deleteDocuments(new Term(USER_FIELD, user.getIRI().stringValue()));
			writer.addDocument(newDoc);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	private List<Document> getDocumentsFromUser(STUser user) throws IOException {
		//it should be 0 or 1 document, but better to be sure
		List<Document> documentList = new ArrayList<>();
		BooleanQuery.Builder builderBoolean = new BooleanQuery.Builder();
		builderBoolean.add(new TermQuery(new Term(USER_FIELD, user.getIRI().stringValue())), BooleanClause.Occur.MUST);
		Query query = builderBoolean.build();
		IndexSearcher searcher = createSearcher();
		TopDocs hits = searcher.search(query, MAX_RESULTS);
		for (ScoreDoc sd : hits.scoreDocs) {
			Document doc = searcher.doc(sd.doc);
			documentList.add(doc);
		}
		return documentList;
	}

	private IndexWriter createIndexWriter() throws IOException {
		Directory directory = FSDirectory.open(getLuceneDir().toPath());
		SimpleAnalyzer simpleAnalyzer = new SimpleAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(simpleAnalyzer);
		IndexWriter writer = new IndexWriter(directory, config);
		return writer;
	}

	private IndexSearcher createSearcher() throws IOException {
		Directory directory = FSDirectory.open(getLuceneDir().toPath());
		IndexReader reader;
		try {
			reader = DirectoryReader.open(directory);
		} catch (IndexNotFoundException e) {
			//there was no index, so create it
			try (IndexWriter indexWriter = createIndexWriter()) {}
			reader = DirectoryReader.open(directory);
		}
		return new IndexSearcher(reader);
	}

	public void deleteIndex() throws IOException {
		Directory directory = FSDirectory.open(getLuceneDir().toPath());
		try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(new SimpleAnalyzer()))) {
			writer.deleteAll();
		}
	}

	private Document cloneAndCompactDocuments(List<Document> inputDocList) {
		HashSet<String> projRoleActionSet = new HashSet<>();
		HashSet<String> projResSet = new HashSet<>();
		Document doc = new Document();
		for(Document inputDoc : inputDocList){
			for(IndexableField indexableField : inputDoc.getFields(PROJ_ROLE_ACT_FIELD)){
				String value = indexableField.stringValue();
				if(!projRoleActionSet.contains(value)){
					projRoleActionSet.add(value);
					doc.add(new StringField(PROJ_ROLE_ACT_FIELD, indexableField.stringValue(), Field.Store.YES));
				}
			}
			for(IndexableField indexableField : inputDoc.getFields(PROJ_RES_FIELD)){
				String value = indexableField.stringValue();
				if(!projResSet.contains(value)){
					projResSet.add(value);
					doc.add(new StringField(PROJ_RES_FIELD, indexableField.stringValue(), Field.Store.YES));
				}
			}
		}
		return doc;
	}

	private Document cloneAndCompactDocumentMinusField(List<Document> inputDocList, String fieldName, String fieldValueRegex) {
		HashSet<String> projRoleActionSet = new HashSet<>();
		HashSet<String> projResSet = new HashSet<>();
		Document doc = new Document();
		for(Document inputDoc : inputDocList){
			for(IndexableField indexableField : inputDoc.getFields(PROJ_ROLE_ACT_FIELD)){
				String value = indexableField.stringValue();
				if(PROJ_ROLE_ACT_FIELD.equals(fieldName) && value.matches(fieldValueRegex)){
					continue;
				}
				if(!projRoleActionSet.contains(value)){
					projRoleActionSet.add(value);
					doc.add(new StringField(PROJ_ROLE_ACT_FIELD, indexableField.stringValue(), Field.Store.YES));
				}
			}
			for(IndexableField indexableField : inputDoc.getFields(PROJ_RES_FIELD)){
				String value = indexableField.stringValue();
				if(PROJ_RES_FIELD.equals(fieldName) && value.matches(fieldValueRegex)){
					continue;
				}
				if(!projResSet.contains(value)){
					projResSet.add(value);
					doc.add(new StringField(PROJ_RES_FIELD, indexableField.stringValue(), Field.Store.YES));
				}
			}
		}
		return doc;
	}

	private Document cloneAndCompactDocumentMinusField(List<Document> inputDocList, String fieldName) {
		HashSet<String> projRoleActionSet = new HashSet<>();
		HashSet<String> projResSet = new HashSet<>();
		Document doc = new Document();
		for(Document inputDoc : inputDocList){
			if(!PROJ_ROLE_ACT_FIELD.equals(fieldName)) {
				for (IndexableField indexableField : inputDoc.getFields(PROJ_ROLE_ACT_FIELD)) {
					String value = indexableField.stringValue();
					if (!projRoleActionSet.contains(value)) {
						projRoleActionSet.add(value);
						doc.add(new StringField(PROJ_ROLE_ACT_FIELD, indexableField.stringValue(), Field.Store.YES));
					}
				}
			}
			if(!PROJ_RES_FIELD.equals(fieldName)) {
				for (IndexableField indexableField : inputDoc.getFields(PROJ_RES_FIELD)) {
					String value = indexableField.stringValue();
					if (!projResSet.contains(value)) {
						projResSet.add(value);
						doc.add(new StringField(PROJ_RES_FIELD, indexableField.stringValue(), Field.Store.YES));
					}
				}
			}
		}
		return doc;
	}

	private File getLuceneDir() {

		String mainIndexDirPath = Resources.getSemTurkeyDataDir() + File.separator + indexMainDirName;
		File mainIndexDir = new File(mainIndexDirPath);
		if (!mainIndexDir.exists()) {
			mainIndexDir.mkdir();
		}
		//String luceneIndexDirPath = Resources.getSemTurkeyDataDir()+File.separator+lucDirName;
		//File luceneIndexDir = new File(luceneIndexDirPath);
		File luceneIndexDir = new File(mainIndexDir, luceneDirName);
		if (!luceneIndexDir.exists()) {
			luceneIndexDir.mkdir();
		}
		return luceneIndexDir;
	}
}
