package org.popper.ddp.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.bson.types.ObjectId;

/**
 * User: kaa
 * Date: 16.06.13
 */
public class IdentifiableJSONEntity {
    private String id;
    private String jsonContent;

    public static IdentifiableJSONEntity valueOf(String fullJsonContent, Gson gson) {
        JsonParser parser = new JsonParser();
        JsonObject jsObj = parser.parse(fullJsonContent).getAsJsonObject();
        String objId = null;
        if (jsObj.has("_id"))
            objId = jsObj.remove("_id").getAsString();
        else
            objId = new ObjectId().toString();
        return new IdentifiableJSONEntity(objId, gson.toJson(jsObj));
    }

    public IdentifiableJSONEntity(String id, String jsonContent) {
        this.id = id;
        this.jsonContent = jsonContent;
    }

    public IdentifiableJSONEntity(String jsonContent) {
        this.id = new ObjectId().toString();
        this.jsonContent = jsonContent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJsonContent() {
        return jsonContent;
    }

    public void setJsonContent(String jsonContent) {
        this.jsonContent = jsonContent;
    }

    public String getFullJsonContent(Gson gson) {
        return gson.toJson(getAsJsonObject());
    }

    public JsonObject getAsJsonObject() {
        JsonObject jsObj = getContentJsonObject();
        jsObj.add("_id", new JsonPrimitive(id));
        return jsObj;
    }

    public JsonObject getContentJsonObject() {
        JsonParser parser = new JsonParser();
        return parser.parse(jsonContent).getAsJsonObject();
    }
}
