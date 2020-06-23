package it.uniroma2.art.semanticturkey.user.notification;

import it.uniroma2.art.semanticturkey.data.role.RDFResourceRole;
import it.uniroma2.art.semanticturkey.resources.Resources;
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
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserNotificationAPI {

    private static final UserNotificationAPI sharedInsance = new UserNotificationAPI();

    private final String indexMainDirName = "index";
    private String luceneDirName = "userNotificationIndex";

    private final String SEPARATOR = "|_|";

    private final String USER_FIELD = "user";
    private final String PROJ_ROLE_ACT_FIELD = "project-role-action";
    private final String PROJ_RES_FIELD= "project-resource";

    private int MAX_RESULTS = 200;

    public enum Action {any, create, delete, update}

    public static UserNotificationAPI getInstance() {
        return sharedInsance;
    }

    /*private IndexWriter createIndexWriter() throws IOException {
        Directory directory = FSDirectory.open(new File(luceneDirName).toPath());
        SimpleAnalyzer simpleAnalyzer = new SimpleAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(simpleAnalyzer);
        IndexWriter writer = new IndexWriter(directory, config);
        return writer;
    }*/

    public List<String> searchResourceFromUser(String user) throws IOException {
        List<String> projResList = new ArrayList<>();
        BooleanQuery.Builder builderBoolean = new BooleanQuery.Builder();
        builderBoolean.add(new TermQuery(new Term(USER_FIELD, user)), BooleanClause.Occur.MUST);
        Query query = builderBoolean.build();
        IndexSearcher searcher = createSearcher();
        TopDocs hits = searcher.search(query, MAX_RESULTS);
        for(ScoreDoc sd : hits.scoreDocs){
            Document doc = searcher.doc(sd.doc);
            IndexableField[] indexableFieldArray = doc.getFields(PROJ_RES_FIELD);
            for(IndexableField indexableField : indexableFieldArray){
                String proj_res = indexableField.stringValue();
                projResList.add(proj_res);
            }
        }
        return projResList;
    }

    public List<String> searchProjRoleActionFromUser(String user) throws IOException {
        List<String> projRoleActionList = new ArrayList<>();
        BooleanQuery.Builder builderBoolean = new BooleanQuery.Builder();
        builderBoolean.add(new TermQuery(new Term(USER_FIELD, user)), BooleanClause.Occur.MUST);
        Query query = builderBoolean.build();
        IndexSearcher searcher = createSearcher();
        TopDocs hits = searcher.search(query, MAX_RESULTS);
        //each user is a single doc, so there shoule be 0 or 1 document in the result
        for(ScoreDoc sd : hits.scoreDocs){
            Document doc = searcher.doc(sd.doc);
            IndexableField[] indexableFieldArray = doc.getFields(PROJ_ROLE_ACT_FIELD);
            for(IndexableField indexableField : indexableFieldArray){
                String proj_role_action = indexableField.stringValue();
                projRoleActionList.add(proj_role_action);
            }
        }
        return projRoleActionList;
    }

    public List<String> searchUserFromProjRes(String proj, IRI res) throws IOException {
        List<String> userList = new ArrayList<>();
        TopDocs hits;
        String resNT = NTriplesUtil.toNTriplesString(res);
        IndexSearcher searcher = createSearcher();
        BooleanQuery.Builder builderBoolean = new BooleanQuery.Builder();
        if(proj != null && !proj.isEmpty()) {
            String projRes = proj + SEPARATOR + resNT;
            builderBoolean.add(new TermQuery(new Term(PROJ_RES_FIELD, projRes)), BooleanClause.Occur.MUST);
        } else {
            WildcardQuery wildcardQuery = new WildcardQuery(new Term(PROJ_RES_FIELD, "*"+SEPARATOR+ resNT));
            builderBoolean.add(wildcardQuery, BooleanClause.Occur.MUST);
        }
        Query query = builderBoolean.build();
        hits = searcher.search(query, MAX_RESULTS);

        for(ScoreDoc sd : hits.scoreDocs){
            Document doc = searcher.doc(sd.doc);
            IndexableField[] indexableFieldArray = doc.getFields(USER_FIELD);
            for(IndexableField indexableField : indexableFieldArray){
                String user = indexableField.stringValue();
                userList.add(user);
            }
        }
        return userList;
    }

    public List<String> searchUserFromProjRoleAction(String proj, RDFResourceRole role, Action action) throws IOException {
        //role could be null in this case, no restriction is applied for the role

        List<String> userList = new ArrayList<>();
        TopDocs hits;

        //construct the String that will be used during the search
        boolean useWildCard = false;
        String searchString = proj+SEPARATOR;
        if(role==RDFResourceRole.undetermined){
            searchString += "*"+SEPARATOR;
            useWildCard = true;
        } else {
            searchString += role.name()+SEPARATOR;
        }
        if(action == Action.any ){
            searchString += "*";
            useWildCard = true;
        } else {
            searchString += action.name();
        }

        IndexSearcher searcher = createSearcher();
        BooleanQuery.Builder builderBoolean = new BooleanQuery.Builder();
        if(!useWildCard) {
            builderBoolean.add(new TermQuery(new Term(PROJ_ROLE_ACT_FIELD, searchString)), BooleanClause.Occur.MUST);
        } else {
            WildcardQuery wildcardQuery = new WildcardQuery(new Term(PROJ_ROLE_ACT_FIELD, searchString));
            builderBoolean.add(wildcardQuery, BooleanClause.Occur.MUST);
        }
        Query query = builderBoolean.build();
        hits = searcher.search(query, MAX_RESULTS);

        for(ScoreDoc sd : hits.scoreDocs){
            Document doc = searcher.doc(sd.doc);
            IndexableField[] indexableFieldArray = doc.getFields(USER_FIELD);
            for(IndexableField indexableField : indexableFieldArray){
                String user = indexableField.stringValue();
                userList.add(user);
            }
        }
        return userList;
    }

    public boolean addToUser(String user, String proj, IRI res) throws IOException {
        String resNT = NTriplesUtil.toNTriplesString(res);
        String valueToAdd = proj+SEPARATOR+resNT;
        Document doc = getDocumentFromUser(user);

        if(doc == null){
            doc = new Document();
            doc.add(new StringField(USER_FIELD, user, Field.Store.YES));
        }
        else {
            //check if the value "proj res" is not already present, if not add it, otherweise do not add it
            IndexableField[] indexableFields = doc.getFields(PROJ_RES_FIELD);
            for(IndexableField indexableField : indexableFields){
                if(valueToAdd.equals(indexableField.stringValue())){
                    return false;
                }
            }
        }
        try(IndexWriter writer = createIndexWriter()) {
            Document newDoc = cloneDocument(doc);
            newDoc.add(new StringField(PROJ_RES_FIELD, valueToAdd, Field.Store.YES));
            writer.deleteDocuments(new Term(USER_FIELD, user));
            writer.addDocument(newDoc);
        }
        return true;
    }

    public boolean addToUser(String user, String proj, RDFResourceRole role, Action action) throws IOException {
        String valueToAdd = proj+SEPARATOR+role.name()+SEPARATOR+action.name();
        Document doc = getDocumentFromUser(user);

        if(doc == null){
            doc = new Document();
            doc.add(new StringField(USER_FIELD, user, Field.Store.YES));
        } else {
            //check if the value "proj role action" is not already present, if not add it, otherwise do not add it
            IndexableField[] indexableFields = doc.getFields(PROJ_ROLE_ACT_FIELD);
            for(IndexableField indexableField : indexableFields){
                if(valueToAdd.equals(indexableField.stringValue())){
                    return false;
                }
            }
        }
        try(IndexWriter writer = createIndexWriter()) {
            Document newDoc = cloneDocument(doc);
            newDoc.add(new StringField(PROJ_ROLE_ACT_FIELD, valueToAdd, Field.Store.YES));
            writer.deleteDocuments(new Term(USER_FIELD, user));
            writer.addDocument(newDoc);
        }

        return true;
    }

    public boolean removeUser(String user) throws IOException {
        Document doc = getDocumentFromUser(user);
        if(doc == null){
            return false;
        }
        try(IndexWriter writer = createIndexWriter()) {
            writer.deleteDocuments(new Term(USER_FIELD, user));
        }
        return true;
    }

    public boolean removeProjResFromUser(String user, String proj, IRI res) throws IOException {
        String escapedSeparator = SEPARATOR.replace("|", "\\|");
        String resNT = NTriplesUtil.toNTriplesString(res);
        String fieldValueRegex = proj+escapedSeparator+resNT;
        Document doc = getDocumentFromUser(user);
        if(doc == null){
            return false;
        }
        try(IndexWriter writer = createIndexWriter()) {
            Document newDoc = cloneDocumentMinusField(doc, PROJ_RES_FIELD, fieldValueRegex);
            writer.deleteDocuments(new Term(USER_FIELD, user));
            writer.addDocument(newDoc);
        }
        return true;
    }

    public boolean removeProjRoleActionFromUser(String user, String proj, RDFResourceRole role, Action action) throws IOException {
        String escapedSeparator = SEPARATOR.replace("|", "\\|");
        String fieldValueRegex = proj+escapedSeparator;
        if(role.equals(RDFResourceRole.undetermined)){
            fieldValueRegex += ".+"+escapedSeparator;
        } else {
            fieldValueRegex += role.name()+escapedSeparator;
        }
        if(action.equals(Action.any)){
            fieldValueRegex += ".+";
        } else {
            fieldValueRegex += action.name();
        }

        Document doc = getDocumentFromUser(user);
        if(doc == null){
            return false;
        }
        try(IndexWriter writer = createIndexWriter()) {
            Document newDoc = cloneDocumentMinusField(doc, PROJ_ROLE_ACT_FIELD, fieldValueRegex);
            writer.deleteDocuments(new Term(USER_FIELD, user));
            writer.addDocument(newDoc);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private Document getDocumentFromUser(String user) throws IOException {
        BooleanQuery.Builder builderBoolean = new BooleanQuery.Builder();
        builderBoolean.add(new TermQuery(new Term(USER_FIELD, user)), BooleanClause.Occur.MUST);
        Query query = builderBoolean.build();
        IndexSearcher searcher = createSearcher();
        TopDocs hits = searcher.search(query, MAX_RESULTS);
        //each user is a single doc, so there shoule be 0 or 1 document in the result
        if(hits.scoreDocs.length>0) {
            return searcher.doc(hits.scoreDocs[0].doc);
        }
        return null;
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
        } catch (IndexNotFoundException e){
            //there was no index, so create it
            try(IndexWriter indexWriter = createIndexWriter()){}
            reader = DirectoryReader.open(directory);
        }
        return new IndexSearcher(reader);
    }

    public void deleteIndex() throws IOException {
        Directory directory = FSDirectory.open(getLuceneDir().toPath());
        try(IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(new SimpleAnalyzer()))) {
            writer.deleteAll();
        }
    }

    private Document cloneDocument(Document inputDoc){
        Document doc = new Document();
        for(IndexableField indexableField : inputDoc.getFields()){
            doc.add(new StringField(indexableField.name(), indexableField.stringValue(), Field.Store.YES));
        }
        return doc;
    }

    private Document cloneDocumentMinusField(Document inputDoc, String fieldName, String fieldValueRegex){
        Document doc = new Document();
        for(IndexableField indexableField : inputDoc.getFields()){
            if(!indexableField.name().equals(fieldName) || !indexableField.stringValue().matches(fieldValueRegex)) {
                doc.add(new StringField(indexableField.name(), indexableField.stringValue(), Field.Store.YES));
            }
        }
        return doc;
    }

    private File getLuceneDir() {

        String mainIndexDirPath = Resources.getSemTurkeyDataDir()+File.separator+ indexMainDirName;
        File mainIndexDir = new File(mainIndexDirPath);
        if(!mainIndexDir.exists()){
            mainIndexDir.mkdir();
        }
        //String luceneIndexDirPath = Resources.getSemTurkeyDataDir()+File.separator+lucDirName;
        //File luceneIndexDir = new File(luceneIndexDirPath);
        File luceneIndexDir = new File(mainIndexDir, luceneDirName);
        if(!luceneIndexDir.exists()) {
            luceneIndexDir.mkdir();
        }
        return luceneIndexDir;
    }
}
