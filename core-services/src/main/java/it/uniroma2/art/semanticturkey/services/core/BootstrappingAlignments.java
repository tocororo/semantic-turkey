package it.uniroma2.art.semanticturkey.services.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniroma2.art.semanticturkey.services.STServiceAdapter;
import it.uniroma2.art.semanticturkey.services.annotations.Optional;
import it.uniroma2.art.semanticturkey.services.annotations.RequestMethod;
import it.uniroma2.art.semanticturkey.services.annotations.STService;
import it.uniroma2.art.semanticturkey.services.annotations.STServiceOperation;
import it.uniroma2.art.semanticturkey.services.core.bootstrapAlign.MatchTaskInfo;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides services for interacting with remote alignment services.
 *
 * @author <a href="mailto:turbati@info.uniroma2.it">Andrea Turbati</a>
 */
@STService
public class BootstrappingAlignments extends STServiceAdapter {

    private static final Logger logger = LoggerFactory.getLogger(BootstrappingAlignments.class);

    private final String SERVER_HOST = "http://localhost";
    private final String SERVER_PORT = "7576";

    private final String SERVER_NAME_WITH_SLASH = "st-rsc/";
    private final String BASE_PATH_WITH_SLASH = "";

    private final String SOURCE_PROJECT = "sourceProject";
    private final String SPARQL_ENDPOINT_L1 = "sparqlEndpointL1";
    private final String IS_L1_REVERSE= "isL1Reverse";
    private final String SPARQL_ENDPOINT_L2 = "sparqlEndpointL2";
    private final String IS_L2_REVERSE = "isL2Reverse";
    private final String NAMESPACEA = "namespaceA";
    private final String NAMESPACEB = "namespaceB";
    private final String NAMESPACEC = "namespaceC";

    public static class MessageKeys {
        public static final String keyBase = "it.uniroma2.art.semanticturkey.services.core.BootstrappingAlignments";
        public static final String exceptionConfigurationAlreadyExists$message = keyBase
                + ".exceptionConfigurationAlreadyExists.message";
    }


    @STServiceOperation
    public JsonNode getServiceMetadata() throws IOException {
        JsonNode jsonNode;
        String url;
        url = SERVER_HOST+":"+SERVER_PORT+"/"+SERVER_NAME_WITH_SLASH+BASE_PATH_WITH_SLASH+"match/getServiceMetadata";

        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(httpGet)) {

            // Get HttpResponse Status
            //String status = response.getStatusLine().toString();

            HttpEntity entity = response.getEntity();

            String jsonString = EntityUtils.toString(entity);

            System.out.println("\njsonString:\n"+jsonString);

            ObjectMapper mapper = new ObjectMapper();
            jsonNode = mapper.readTree(jsonString);
        }
        return  jsonNode;
    }


    @STServiceOperation(method = RequestMethod.POST)
    public String runMatch() throws IOException {
        // TODO decide what the input is

        // extract from the input the data that is needed to call the service

        // example of the needed data
        String sourceProject, endpoint1, endpoint2, namespaceA, namespaceB, namespaceC;
        boolean isL1Reverse, isL2Reverse;
        sourceProject = "Project-A";
        endpoint1 = "http://localhost:7200/repositories/Project-A_core";
        isL1Reverse = false;
        endpoint2 = "http://localhost:7200/repositories/Project-B_core";
        isL2Reverse = false;
        namespaceA = "http://test.it/A/";
        namespaceB = "http://test.it/B/";
        namespaceC = "http://test.it/C/";


        String url;
        url = SERVER_HOST+":"+SERVER_PORT+"/"+SERVER_NAME_WITH_SLASH+BASE_PATH_WITH_SLASH+"match/executeMatchTask";

        //prepare the HTTP POST
        HttpPost httpPost = new HttpPost(url);
        JsonNodeFactory jsonFactory = JsonNodeFactory.instance;
        ObjectNode objectNode = jsonFactory.objectNode();
        objectNode.set(SOURCE_PROJECT, jsonFactory.textNode(sourceProject));
        objectNode.set(SPARQL_ENDPOINT_L1, jsonFactory.textNode(endpoint1));
        objectNode.set(IS_L1_REVERSE, jsonFactory.booleanNode(isL1Reverse));
        objectNode.set(SPARQL_ENDPOINT_L2, jsonFactory.textNode(endpoint2));
        objectNode.set(IS_L2_REVERSE, jsonFactory.booleanNode(isL2Reverse));
        objectNode.set(NAMESPACEA, jsonFactory.textNode(namespaceA));
        objectNode.set(NAMESPACEB, jsonFactory.textNode(namespaceB));
        objectNode.set(NAMESPACEC, jsonFactory.textNode(namespaceC));

        String jsonString = objectNode.toString();

        httpPost.setHeader("Accept", "application/json");
        org.apache.http.HttpEntity entity = new StringEntity(jsonString,
                ContentType.APPLICATION_JSON);
        httpPost.setEntity(entity);


        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(httpPost)) {
            return EntityUtils.toString(response.getEntity());
        }
    }


    @STServiceOperation(method = RequestMethod.GET)
    public List<MatchTaskInfo> getAllTasksInfo(@Optional String projectName) throws IOException {

        List<MatchTaskInfo> tasks = new ArrayList<>();

        String url;
        url = SERVER_HOST +":"+ SERVER_PORT +"/"+ SERVER_NAME_WITH_SLASH + BASE_PATH_WITH_SLASH +"match/tasksInfo";
        if(projectName!=null && !projectName.isEmpty()){
            url += "?projectName="+projectName;
        }

        //prepare and execute the HTTP GET
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(httpGet)) {

            // Get HttpResponse Status
            HttpEntity entity = response.getEntity();
            Header headers = entity.getContentType();
            if (entity != null) {
                // return it as a String
                String result = EntityUtils.toString(entity);
                tasks = new ObjectMapper().readValue(result, new TypeReference<List<MatchTaskInfo>>(){});
            }
        }
        return tasks;
    }

    @STServiceOperation(method = RequestMethod.POST)
    public void deleteTask(String taskId) throws IOException {
        String url = SERVER_HOST +":"+ SERVER_PORT +"/"+ SERVER_NAME_WITH_SLASH + BASE_PATH_WITH_SLASH +"match/"+taskId;
        HttpDelete httpDelete = new HttpDelete(url);
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(httpDelete)) {
        }
    }

    @STServiceOperation(method = RequestMethod.GET)
    public void getTaskResult(HttpServletResponse response, String taskId, SkosDiffing.ResultType resultType)
            throws IOException, ParserConfigurationException, TransformerException {
        String url = SERVER_HOST + ":" + SERVER_PORT + "/" + SERVER_NAME_WITH_SLASH + BASE_PATH_WITH_SLASH + "match/" + taskId;

        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/rdf+xml");
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse responseFromServer = httpClient.execute(httpGet)) {
            HttpEntity entity = responseFromServer.getEntity();

            String result = EntityUtils.toString(entity);

            // TODO the result contains the full rdf text with all the alignemnts, decide what to do with it

        }
    }
}
