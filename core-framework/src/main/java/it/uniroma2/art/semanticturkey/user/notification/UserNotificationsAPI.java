package it.uniroma2.art.semanticturkey.user.notification;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.project.Project;
import it.uniroma2.art.semanticturkey.user.notification.NotificationPreferencesAPI.Action;
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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;
import org.eclipse.rdf4j.sail.SailException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

public class UserNotificationsAPI {

	private static final UserNotificationsAPI sharedInsance = new UserNotificationsAPI();

	private final String indexMainDirName = "index";
	private final String luceneDirName = "userNotificationIndex";

	private final String SEPARATOR = "|_|";

	private final String USER_FIELD = "user";
	private final String PROJ_RES_ROLE_ACT_TIMESTAMP_FIELD = "project-res-role-action-timestamp";

	private int MAX_RESULTS = 200;

	public static UserNotificationsAPI getInstance() {
		return sharedInsance;
	}


	public List<ProjResRoleActionTimestamp> retrieveNotifications(STUser user) throws IOException {
		return searchNotificationsFromUser(user, null);
	}

	public List<ProjResRoleActionTimestamp> retrieveNotifications(STUser user, Project project) throws IOException {
		return searchNotificationsFromUser(user, project);
	}

	private List<ProjResRoleActionTimestamp> searchNotificationsFromUser(STUser user, Project project) throws IOException {
		List<Document> documentList = getDocumentsFromUser(user);
		if(documentList.isEmpty()){
			return new ArrayList<>();
		}
		List<ProjResRoleActionTimestamp> projResRoleActionTimestampList = new ArrayList<>();
		//it should be 0 or 1 document, but better to be sure
		for(Document doc : documentList){
			IndexableField[] indexableFieldArray = doc.getFields(PROJ_RES_ROLE_ACT_TIMESTAMP_FIELD);
			for (IndexableField indexableField : indexableFieldArray) {
				String[] proj_res_role_act_timestampArray = indexableField.stringValue().split(Pattern.quote(SEPARATOR));
				String proj = proj_res_role_act_timestampArray[0];
				if(project != null){
					//a project has been passed, check if the retrieve notification is about such project
					if(!proj.equals(project.getName())){
						//it is a different project, so skip such notification
						continue;
					}
				}
				String res = proj_res_role_act_timestampArray[1];
				RDFResourceRole role = RDFResourceRole.valueOf(proj_res_role_act_timestampArray[2]);
				Action action = Action.valueOf(proj_res_role_act_timestampArray[3]);
				String timestamp = proj_res_role_act_timestampArray[4];
				ProjResRoleActionTimestamp projResRoleActionTimestamp = new ProjResRoleActionTimestamp(proj, res,
						role, action, timestamp);
				projResRoleActionTimestampList.add(projResRoleActionTimestamp);
			}
		}
		return projResRoleActionTimestampList;
	}


	public boolean clearNotifications(STUser user) throws IOException {
		List<Document> docList = getDocumentsFromUser(user);
		if(docList.isEmpty()){
			return false;
		}
		try (IndexWriter writer = createIndexWriter()) {
			writer.deleteDocuments(new Term(USER_FIELD, user.getIRI().stringValue()));
		}
		return true;

	}

	public boolean clearNotifications(STUser user, Project project) throws IOException {
		List<Document> docList = getDocumentsFromUser(user);
		if(docList.isEmpty()){
			return false;
		}
		//create a new Document having all data from the retrieved documents (in theory there should be only 1),
		//remove such document and add the retrive one
		boolean emptyDoc = true;
		Document newDoc = new Document();
		newDoc.add(new StringField(USER_FIELD, user.getIRI().stringValue(), Field.Store.YES));
		for(Document doc : docList){
			IndexableField[] indexableFieldArray =doc.getFields(PROJ_RES_ROLE_ACT_TIMESTAMP_FIELD);
			for (IndexableField indexableField : indexableFieldArray) {
				String proj_res_role_act_timestamp = indexableField.stringValue();
				String[] proj_res_role_act_timestampArray = proj_res_role_act_timestamp.split(Pattern.quote(SEPARATOR));
				String proj = proj_res_role_act_timestampArray[0];
				//a project has been passed, check if the retrieve notification is about such project
				if (!proj.equals(project.getName())) {
					//it is the passed project, so do not copy in the new document
					newDoc.add(new StringField(PROJ_RES_ROLE_ACT_TIMESTAMP_FIELD, proj_res_role_act_timestamp, Field.Store.YES));
					emptyDoc = false;
				}
			}
		}
		try (IndexWriter writer = createIndexWriter()) {
			writer.deleteDocuments(new Term(USER_FIELD, user.getIRI().stringValue()));
			if (!emptyDoc) {
				writer.addDocument(newDoc);
			}
		}
		return true;
	}

	public boolean storeNotification(STUser user, Project project, Resource resource,
			RDFResourceRole role, Action action) throws IOException {
		boolean updateIndex = false;
		HashSet<String> presentValueSet = new HashSet<>();
		List <Document> documentList = getDocumentsFromUser(user);
		if(documentList.size()>1){
			//there are multiple documents for the same users, this should not happen, so update the index
			//with a single document containing all data from the various retrieved documents
			updateIndex = true;
		}
		Document newDoc = new Document();
		newDoc.add(new StringField(USER_FIELD, user.getIRI().stringValue(), Field.Store.YES));
		for(Document doc : documentList){
			IndexableField[] indexableFieldArray =doc.getFields(PROJ_RES_ROLE_ACT_TIMESTAMP_FIELD);
			for (IndexableField indexableField : indexableFieldArray) {
				String proj_res_role_act_timestamp = indexableField.stringValue();
				if(!presentValueSet.contains(proj_res_role_act_timestamp)){
					newDoc.add(new StringField(PROJ_RES_ROLE_ACT_TIMESTAMP_FIELD, proj_res_role_act_timestamp, Field.Store.YES));
					presentValueSet.add(proj_res_role_act_timestamp);
				}
			}
		}
		//now add the new value (if not alredy present)
		String proj_res_role_act_timestamp = project.getName()+SEPARATOR+NTriplesUtil.toNTriplesString(resource)+
				SEPARATOR+role.name()+SEPARATOR+action.name()+SEPARATOR+currentTime();
		if(!presentValueSet.contains(proj_res_role_act_timestamp)){
			newDoc.add(new StringField(PROJ_RES_ROLE_ACT_TIMESTAMP_FIELD, proj_res_role_act_timestamp, Field.Store.YES));
			updateIndex = true;
		}
		//if the index should be update, remove the old document(s) and insert the new one
		if(updateIndex){
			try (IndexWriter writer = createIndexWriter()) {
				if(!documentList.isEmpty()) {
					writer.deleteDocuments(new Term(USER_FIELD, user.getIRI().stringValue()));
				}
				writer.addDocument(newDoc);
			}
		}
		return true;
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
		return  documentList;
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

	protected String currentTime() throws SailException {
		GregorianCalendar calendar = new GregorianCalendar();
		XMLGregorianCalendar currentDateTimeXML;
		try {
			currentDateTimeXML = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
		} catch (DatatypeConfigurationException e) {
			throw new SailException(e);
		}
		return currentDateTimeXML.toString();
	}

	public class ProjResRoleActionTimestamp {
		private String proj;
		private String resource;
		private RDFResourceRole role;
		private NotificationPreferencesAPI.Action action;
		private String timestamp;

		public ProjResRoleActionTimestamp(String proj, String resource, RDFResourceRole role, NotificationPreferencesAPI.Action action, String timestamp) {
			this.proj = proj;
			this.resource = resource;
			this.role = role;
			this.action = action;
			this.timestamp = timestamp;
		}

		public String getProj() {
			return proj;
		}

		public String getResource() {
			return resource;
		}

		public RDFResourceRole getRole() {
			return role;
		}

		public NotificationPreferencesAPI.Action getAction() {
			return action;
		}

		public String getTimestamp() {
			return timestamp;
		}
	}
}
