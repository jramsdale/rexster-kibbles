package com.tinkerpop.rexster.kibbles.batch;


import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.util.json.JSONWriter;
import com.tinkerpop.rexster.RexsterApplicationGraph;
import com.tinkerpop.rexster.RexsterResourceContext;
import com.tinkerpop.rexster.Tokens;
import com.tinkerpop.rexster.extension.AbstractRexsterExtension;
import com.tinkerpop.rexster.extension.ExtensionApi;
import com.tinkerpop.rexster.extension.ExtensionDefinition;
import com.tinkerpop.rexster.extension.ExtensionDescriptor;
import com.tinkerpop.rexster.extension.ExtensionMethod;
import com.tinkerpop.rexster.extension.ExtensionNaming;
import com.tinkerpop.rexster.extension.ExtensionPoint;
import com.tinkerpop.rexster.extension.ExtensionResponse;
import com.tinkerpop.rexster.extension.HttpMethod;
import com.tinkerpop.rexster.extension.RexsterContext;
import com.tinkerpop.rexster.util.RequestObjectHelper;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This extension allows batch/transactional operations on a graph.
 */
@ExtensionNaming(namespace = BatchExtension.EXTENSION_NAMESPACE, name = BatchExtension.EXTENSION_NAME)
public class BatchExtension extends AbstractRexsterExtension {

    private static Logger logger = Logger.getLogger(BatchExtension.class);

    public static final String EXTENSION_NAMESPACE = "tp";
    public static final String EXTENSION_NAME = "batch";
    private static final String WILDCARD = "*";

    private static final String API_SHOW_TYPES = "displays the properties of the elements with their native data type (default is false)";
    private static final String API_IDENTIFIERS = "an array of element identifiers to retrieve from the graph";
    private static final String API_RETURN_KEYS = "an array of element property keys to return (default is to return all element properties)";

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.GET, path = "vertices")
    @ExtensionDescriptor(description = "get a set of vertices from the graph.",
            api = {
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.SHOW_TYPES, description = API_SHOW_TYPES),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.RETURN_KEYS, description = API_RETURN_KEYS),
                    @ExtensionApi(parameterName = "idList", description = API_IDENTIFIERS)
            })
    public ExtensionResponse getVertices(@RexsterContext RexsterResourceContext context,
                                         @RexsterContext Graph graph) {

        JSONObject requestObject = context.getRequestObject();
        JSONArray idList = requestObject.optJSONArray("idList");
        if (idList == null || idList.length() == 0) {
            ExtensionMethod extMethod = context.getExtensionMethod();
            return ExtensionResponse.error(
                    "the idList parameter cannot be empty",
                    null,
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    null,
                    generateErrorJson(extMethod.getExtensionApiAsJson()));
        }

        boolean showDataTypes = RequestObjectHelper.getShowTypes(requestObject);
        List<String> returnKeys = RequestObjectHelper.getReturnKeys(requestObject, WILDCARD);

        try {

            JSONArray jsonArray = new JSONArray();

            for (int ix = 0; ix < idList.length(); ix++) {
                Vertex vertexFound = graph.getVertex(idList.optString(ix));
                if (vertexFound != null) {
                    jsonArray.put(JSONWriter.createJSONElement(vertexFound, returnKeys, showDataTypes));
                }
            }

            HashMap<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(Tokens.SUCCESS, true);
            resultMap.put(Tokens.RESULTS, jsonArray);

            JSONObject resultObject = new JSONObject(resultMap);
            return ExtensionResponse.ok(resultObject);

        } catch (Exception mqe) {
            logger.error(mqe);
            return ExtensionResponse.error(
                    "Error retrieving batch of vertices [" + idList + "]", generateErrorJson());
        }

    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.GET, path = "edges")
    @ExtensionDescriptor(description = "get a set of edges from the graph.",
            api = {
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.SHOW_TYPES, description = API_SHOW_TYPES),
                    @ExtensionApi(parameterName = Tokens.REXSTER + "." + Tokens.RETURN_KEYS, description = API_RETURN_KEYS),
                    @ExtensionApi(parameterName = "idList", description = API_IDENTIFIERS)
            })
    public ExtensionResponse getEdges(@RexsterContext RexsterResourceContext context,
                                      @RexsterContext Graph graph) {

        JSONObject requestObject = context.getRequestObject();
        JSONArray idList = requestObject.optJSONArray("idList");
        if (idList == null || idList.length() == 0) {
            ExtensionMethod extMethod = context.getExtensionMethod();
            return ExtensionResponse.error(
                    "the idList parameter cannot be empty",
                    null,
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    null,
                    generateErrorJson(extMethod.getExtensionApiAsJson()));
        }

        boolean showDataTypes = RequestObjectHelper.getShowTypes(requestObject);
        List<String> returnKeys = RequestObjectHelper.getReturnKeys(requestObject, WILDCARD);

        try {

            JSONArray jsonArray = new JSONArray();

            for (int ix = 0; ix < idList.length(); ix++) {
                Edge edgeFound = graph.getEdge(idList.optString(ix));
                if (edgeFound != null) {
                    jsonArray.put(JSONWriter.createJSONElement(edgeFound, returnKeys, showDataTypes));
                }
            }

            HashMap<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(Tokens.SUCCESS, true);
            resultMap.put(Tokens.RESULTS, jsonArray);

            JSONObject resultObject = new JSONObject(resultMap);
            return ExtensionResponse.ok(resultObject);

        } catch (Exception mqe) {
            logger.error(mqe);
            return ExtensionResponse.error(
                    "Error retrieving batch of edges [" + idList + "]", generateErrorJson());
        }

    }

    @ExtensionDefinition(extensionPoint = ExtensionPoint.GRAPH, method = HttpMethod.POST, path = "tx")
    @ExtensionDescriptor(description = "post a transaction to the graph.")
    public ExtensionResponse postTx(@RexsterContext RexsterResourceContext context,
                                    @RexsterContext Graph graph,
                                    @RexsterContext RexsterApplicationGraph rag) {

        JSONObject transactionJson = context.getRequestObject();
        if (transactionJson == null) {
            ExtensionMethod extMethod = context.getExtensionMethod();
            return ExtensionResponse.error(
                    "no transaction JSON posted",
                    null,
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    null,
                    generateErrorJson(extMethod.getExtensionApiAsJson()));
        }

        try {
            rag.tryStartTransaction();

            JSONArray txArray = transactionJson.optJSONArray("tx");
            String currentAction;
            for (int ix = 0; ix < txArray.length(); ix++) {
                JSONObject txElement = txArray.optJSONObject(ix);
                currentAction = txElement.optString("_action");
                if (currentAction.equals("create")) {
                    create(txElement, graph);
                } else if (currentAction.equals("update")) {
                    update(txElement, graph);
                } else if (currentAction.equals("delete")) {
                    delete(txElement, graph);
                }
            }

            rag.tryStopTransactionSuccess();

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(Tokens.SUCCESS, true);
            resultMap.put("txProcessed", txArray.length());

            return ExtensionResponse.ok(new JSONObject(resultMap));

        } catch (IllegalArgumentException iae) {
            rag.tryStopTransactionFailure();

            logger.error(iae);

            ExtensionMethod extMethod = context.getExtensionMethod();
            return ExtensionResponse.error(
                    iae.getMessage(),
                    null,
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    null,
                    generateErrorJson(extMethod.getExtensionApiAsJson()));
        } catch (Exception ex) {
            rag.tryStopTransactionFailure();

            logger.error(ex);
            return ExtensionResponse.error(
                    "Error executing transaction: " + ex.getMessage(), generateErrorJson());
        }
    }

    private void create(JSONObject elementAsJson, Graph graph) throws Exception {
        String id = elementAsJson.optString(Tokens._ID);
        String elementType = elementAsJson.optString(Tokens._TYPE);

        if (elementType == null) {
            throw new IllegalArgumentException("each element in the transaction must have an " + Tokens._TYPE + " key");
        }

        if (!elementType.equals(Tokens.VERTEX) && !elementType.equals(Tokens.EDGE)) {
            throw new IllegalArgumentException("the " + Tokens._TYPE + " element in the transaction must be either " + Tokens.VERTEX + " or " + Tokens.EDGE);
        }

        Element graphElementCreated = null;
        if (elementType.equals(Tokens.VERTEX)) {
            graphElementCreated = graph.getVertex(id);

            if (graphElementCreated != null) {
                throw new Exception("Vertex with id " + id + " already exists.");
            }

            graphElementCreated = graph.addVertex(id);

        } else if (elementType.equals(Tokens.EDGE)) {

            String inV = null;
            Object temp = elementAsJson.opt(Tokens._IN_V);
            if (null != temp)
                inV = temp.toString();
            String outV = null;
            temp = elementAsJson.opt(Tokens._OUT_V);
            if (null != temp)
                outV = temp.toString();
            String label = null;
            temp = elementAsJson.opt(Tokens._LABEL);
            if (null != temp)
                label = temp.toString();

            if (outV == null || inV == null || outV.isEmpty() || inV.isEmpty()) {
                throw new IllegalArgumentException("an edge must specify a " + Tokens._IN_V + " and " + Tokens._OUT_V);
            }

            graphElementCreated = graph.getEdge(id);

            if (graphElementCreated != null) {
                throw new Exception("Edge with id " + id + " already exists.");
            }

            // there is no edge but the in/out vertex params and label are present so
            // validate that the vertexes are present before creating the edge
            final Vertex out = graph.getVertex(outV);
            final Vertex in = graph.getVertex(inV);
            if (out != null && in != null) {
                // in/out vertexes are found so edge can be created
                graphElementCreated = graph.addEdge(id, out, in, label);
            } else {
                throw new Exception("the " + Tokens._IN_V + " or " + Tokens._OUT_V + " vertices could not be found.");
            }

        }

        if (graphElementCreated != null) {
            Iterator keys = elementAsJson.keys();
            while (keys.hasNext()) {
                String key = keys.next().toString();
                if (!key.startsWith(Tokens.UNDERSCORE)) {
                    graphElementCreated.setProperty(key, this.getTypedPropertyValue(elementAsJson.getString(key)));
                }
            }
        }
    }

    private void update(JSONObject elementAsJson, Graph graph) throws Exception {
        String id = elementAsJson.optString(Tokens._ID);
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("each element in the transaction must have an " + Tokens._ID + " key");
        }

        String elementType = elementAsJson.optString(Tokens._TYPE);

        if (elementType == null) {
            throw new IllegalArgumentException("each element in the transaction must have an " + Tokens._TYPE + " key");
        }

        if (!elementType.equals(Tokens.VERTEX) && !elementType.equals(Tokens.EDGE)) {
            throw new IllegalArgumentException("the " + Tokens._TYPE + " element in the transaction must be either " + Tokens.VERTEX + " or " + Tokens.EDGE);
        }

        Element graphElementUpdated = null;
        if (elementType.equals(Tokens.VERTEX)) {
            graphElementUpdated = graph.getVertex(id);
        } else if (elementType.equals(Tokens.EDGE)) {
            graphElementUpdated = graph.getEdge(id);
        }

        if (graphElementUpdated != null) {
            Iterator keys = elementAsJson.keys();
            while (keys.hasNext()) {
                String key = keys.next().toString();
                if (!key.startsWith(Tokens.UNDERSCORE)) {
                    graphElementUpdated.setProperty(key, this.getTypedPropertyValue(elementAsJson.getString(key)));
                }
            }
        }
    }

    private void delete(JSONObject elementAsJson, Graph graph) throws Exception {
        String id = elementAsJson.optString(Tokens._ID);
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("each element in the transaction must have an " + Tokens._ID + " key");
        }

        String elementType = elementAsJson.optString(Tokens._TYPE);

        if (elementType == null) {
            throw new IllegalArgumentException("each element in the transaction must have an " + Tokens._TYPE + " key");
        }

        if (!elementType.equals(Tokens.VERTEX) && !elementType.equals(Tokens.EDGE)) {
            throw new IllegalArgumentException("the " + Tokens._TYPE + " element in the transaction must be either " + Tokens.VERTEX + " or " + Tokens.EDGE);
        }

        JSONArray keysToDelete = elementAsJson.optJSONArray("_keys");

        Element graphElementDeleted = null;
        if (elementType.equals(Tokens.VERTEX)) {
            graphElementDeleted = graph.getVertex(id);
        } else if (elementType.equals(Tokens.EDGE)) {
            graphElementDeleted = graph.getEdge(id);
        }

        if (graphElementDeleted != null) {
            if (keysToDelete != null && keysToDelete.length() > 0) {
                // just delete keys from the element
                for (int ix = 0; ix < keysToDelete.length(); ix++) {
                    graphElementDeleted.removeProperty(keysToDelete.optString(ix));
                }
            } else {
                // delete the whole element
                if (elementType.equals(Tokens.VERTEX)) {
                    graph.removeVertex((Vertex) graphElementDeleted);
                } else if (elementType.equals(Tokens.EDGE)) {
                    graph.removeEdge((Edge) graphElementDeleted);
                }
            }
        }
    }

    private Object getTypedPropertyValue(String propertyValue) {
        Object typedPropertyValue = propertyValue;
        if (typedPropertyValue == null) {
            typedPropertyValue = "";
        }

        // determine if the property is typed, otherwise assume it is a string
        if (propertyValue != null && propertyValue.startsWith("(") && propertyValue.endsWith(")")) {
            String dataType = this.getDataTypeSegment(propertyValue);
            String theValue = this.getValueSegment(propertyValue);

            if (dataType.equals("string")) {
                typedPropertyValue = theValue;
            } else if (dataType.equals("integer")) {
                typedPropertyValue = tryParseInteger(theValue);
            } else if (dataType.equals("long")) {
                typedPropertyValue = tryParseLong(theValue);
            } else if (dataType.equals("double")) {
                typedPropertyValue = tryParseDouble(theValue);
            } else if (dataType.equals("float")) {
                typedPropertyValue = tryParseFloat(theValue);
            } else if (dataType.equals("list")) {
                ArrayList<String> items = this.tryParseList(theValue);
                ArrayList typedItems = new ArrayList();
                for (String item : items) {
                    typedItems.add(this.getTypedPropertyValue(item));
                }

                typedPropertyValue = typedItems;
            } else if (dataType.equals("map")) {
                HashMap<String, String> stringProperties = this.tryParseMap(theValue);
                HashMap<String, Object> properties = new HashMap<String, Object>();

                for (Map.Entry<String, String> entry : stringProperties.entrySet()) {
                    properties.put(entry.getKey(), this.getTypedPropertyValue(entry.getValue()));
                }

                typedPropertyValue = properties;
            }
        }

        return typedPropertyValue;
    }

    private HashMap<String, String> tryParseMap(String mapValue) {
        // parens on the ends have been validated already...they must be
        // here to have gotten this far.
        String stripped = mapValue.substring(1, mapValue.length() - 1);

        HashMap<String, String> pairs = new HashMap<String, String>();

        ArrayList<Integer> delimiterPlaces = new ArrayList<Integer>();
        int parensOpened = 0;

        for (int ix = 0; ix < stripped.length(); ix++) {
            char c = stripped.charAt(ix);
            if (c == ',') {
                if (parensOpened == 0) {
                    delimiterPlaces.add(ix);
                }
            } else if (c == '(') {
                parensOpened++;
            } else if (c == ')') {
                parensOpened--;
            }
        }

        int lastPlace = 0;
        int equalPlace = 0;
        for (Integer place : delimiterPlaces) {
            String property = stripped.substring(lastPlace, place);
            equalPlace = property.indexOf("=");
            pairs.put(property.substring(0, equalPlace), property.substring(equalPlace + 1));
            lastPlace = place + 1;
        }

        String property = stripped.substring(lastPlace);
        equalPlace = property.indexOf("=");
        pairs.put(property.substring(0, equalPlace), property.substring(equalPlace + 1));

        return pairs;
    }

    private ArrayList<String> tryParseList(String listValue) {

        // square brackets on the ends have been validated already...they must be
        // here to have gotten this far.
        String stripped = listValue.substring(1, listValue.length() - 1);

        ArrayList<String> items = new ArrayList<String>();

        int place = stripped.indexOf(',');

        if (place > -1) {

            boolean isEndItem = false;
            int parensOpened = 0;
            StringBuffer sb = new StringBuffer();
            for (int ix = 0; ix < stripped.length(); ix++) {
                char c = stripped.charAt(ix);
                if (c == ',') {
                    // a delimiter was found, determine if it is a true
                    // delimiter or if it is part of a value or a separator
                    // with a paren set
                    if (parensOpened == 0) {
                        isEndItem = true;
                    }
                } else if (c == '(') {
                    parensOpened++;
                } else if (c == ')') {
                    parensOpened--;
                }

                if (ix == stripped.length() - 1) {
                    // this is the last character in the value...by default
                    // this is an end item event
                    isEndItem = true;

                    // append the character because it is valid in the value and
                    // not a delimiter that would normally be added.
                    sb.append(c);
                }

                if (isEndItem) {
                    items.add(sb.toString());
                    sb = new StringBuffer();
                    isEndItem = false;
                } else {
                    sb.append(c);
                }
            }
        } else {
            // there is one item in the array or the array is empty
            items.add(stripped);
        }

        return items;
    }

    private String getValueSegment(String propertyValue) {
        // assumes that the propertyValue has open and closed parens
        String value = "";
        String stripped = propertyValue.substring(1, propertyValue.length() - 1);
        int place = stripped.indexOf(',');

        // assume that the first char could be the comma, which
        // would default to a string data type.
        if (place > -1) {
            if (place + 1 < stripped.length()) {
                // stripped doesn't have the trailing paren so take
                // the value from the first character after the comma to the end
                value = stripped.substring(place + 1);
            } else {
                // there is nothing after the comma
                value = "";
            }
        } else {
            // there was no comma, so the whole thing is the value and it
            // is assumed to be a string
            value = stripped;
        }

        return value;
    }

    private String getDataTypeSegment(String propertyValue) {
        // assumes that the propertyValue has open and closed parens
        String dataType = "string";

        // strip the initial parens and read up to the comman.
        // no need to check for string as that is the default
        int place = propertyValue.indexOf(',');
        String inner = propertyValue.substring(1, place).trim();
        if (inner.equals("s") || inner.equals("string")) {
            dataType = "string";
        } else if (inner.equals("i") || inner.equals("integer")) {
            dataType = "integer";
        } else if (inner.equals("d") || inner.equals("double")) {
            dataType = "double";
        } else if (inner.equals("f") || inner.equals("float")) {
            dataType = "float";
        } else if (inner.equals("list")) {
            // TODO: need to ensure square brackets enclose the array
            dataType = "list";
        } else if (inner.equals("l") || inner.equals("long")) {
            dataType = "long";
        } else if (inner.equals("map")) {
            // TODO: need to validate format...outer parens plus name value pairs
            dataType = "map";
        }

        return dataType;
    }

    private Object tryParseInteger(String intValue) {
        Object parsedValue;
        try {
            parsedValue = Integer.parseInt(intValue);
        } catch (NumberFormatException nfe) {
            parsedValue = intValue;
        }

        return parsedValue;
    }

    private Object tryParseFloat(String floatValue) {
        Object parsedValue;
        try {
            parsedValue = Float.parseFloat(floatValue);
        } catch (NumberFormatException nfe) {
            parsedValue = floatValue;
        }

        return parsedValue;
    }

    private Object tryParseLong(String longValue) {
        Object parsedValue;
        try {
            parsedValue = Long.parseLong(longValue);
        } catch (NumberFormatException nfe) {
            parsedValue = longValue;
        }

        return parsedValue;
    }

    private Object tryParseDouble(String doubleValue) {
        Object parsedValue;
        try {
            parsedValue = Double.parseDouble(doubleValue);
        } catch (NumberFormatException nfe) {
            parsedValue = doubleValue;
        }

        return parsedValue;
    }
}
