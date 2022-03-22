package it.uniroma2.art.semanticturkey.config.customview;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomViewSparqlUtils {

    private static final String PIVOT_PATTERN = "\\$(pivot(_(\\d)+))?s*";
    private static final String VAR_PATTERN = "[$|?]([a-zA-Z0-9_]+)"; //group captures only the var name (without ? or $)
    private static final String OBJ_PATTERN = "\\$resource\\s*\\$trigprop\\s*" + VAR_PATTERN + "\\s*\\.";
    private static final String VALUE_PATTERN = "[$|?](([a-zA-Z0-9_]+)_value)"; //admit also $field_value, even if this is not foreseen by CV

    /**
     * Returns a set of pivot variables (pivot, pivot_1, pivot_123, ...) returned by the query
     * @param retrieveQuery
     * @return
     */
    public static Set<String> getReturnedPivotVariables(String retrieveQuery) {
        String queryFragment; //fragment of query where the pivots are detected

        String retrieveSelect = getSelectStatement(retrieveQuery);
        if (retrieveSelect.contains("*")) { //returns all the variables => get pivots from where block
            queryFragment = getWhereBlock(retrieveQuery);
        } else { // get pivots from returned variables
            queryFragment = retrieveSelect;
        }

        Pattern pattern = Pattern.compile(PIVOT_PATTERN);
        Matcher matcher = pattern.matcher(queryFragment);

        Set<String> pivots = new HashSet<>();
        while (matcher.find()) {
            pivots.add(matcher.group(1));
        }
        return pivots;
    }

    /**
     * Returns the object variable name for the triple $resource $predicate ?object
     * @return
     */
    public static String getRetrieveObjectVariable(String retrieveQuery) {
        Pattern pattern = Pattern.compile(OBJ_PATTERN);
        Matcher matcher = pattern.matcher(retrieveQuery);
        if (matcher.find()) {
            return matcher.group(1); //group 0 is the whole captured pattern; 1 is the one in VAR_PATTERN
        } else {
            return null; //should never happen: retrieve query should be checked when submitted
        }
    }

    /**
     * Returns mappings for field variables (for each variable like ?foo_value ?bar_value in the select)
     * foo_value => foo
     * bar_value => bar
     * @return
     */
    public static Map<String, String> getValueVariables(String retrieveQuery) {
        String queryFragment; //fragment of query where the _value variables are detected

        String retrieveSelect = getSelectStatement(retrieveQuery);
        if (retrieveSelect.contains("*")) { //returns all the variables => get pivots from where block
            queryFragment = getWhereBlock(retrieveQuery);
        } else { // get pivots from returned variables
            queryFragment = retrieveSelect;
        }

        Pattern pattern = Pattern.compile(VALUE_PATTERN);
        Matcher matcher = pattern.matcher(queryFragment);

        Map<String, String> map = new HashMap<>();
        while (matcher.find()) {
            map.put(matcher.group(1), matcher.group(2)); //group 1 "foo_value", group 2 "foo"
        }
        return map;
    }

    private static String getSelectStatement(String query) {
        return query.substring(query.toLowerCase().indexOf("select"), query.toLowerCase().indexOf("{"));
    }

    private static String getWhereBlock(String query) {
        return query.substring(query.toLowerCase().indexOf("where"));
    }


}
