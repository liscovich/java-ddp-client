package org.popper.ddp.client;

import com.google.gson.*;
import me.kutrumbos.DdpClient;
import org.bson.types.ObjectId;

import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: kaa
 * Date: 24.06.13
 */
public class ObservableDdpClientObserver extends Observable implements Observer {
    private final String SERVER_ID_PROPERTY = "server_id";
    private final String SESSION_ID_PROPERTY = "session";
    private final String MSG_PROPERTY = "msg";
    private final String MESSAGE_TYPE_CONNECTED = "connected";
    private final String MESSAGE_TYPE_ADDED = "added";
    private final String MESSAGE_TYPE_CHANGED = "changed";
    private final String MESSAGE_TYPE_REMOVED = "removed";

    private String serverId = null;
    private String sessionId = null;
    private Map<String, List<IdentifiableJSONEntity>> collectionsMap = new ConcurrentHashMap<String, List<IdentifiableJSONEntity>>();
    private Gson gson = new Gson();
    private DdpClient ddpConnector;
    private boolean connected = false;

    public ObservableDdpClientObserver() {
        try {
            String meteorIp = "localhost";
            Integer meteorPort = 3000;
            ddpConnector = new DdpClient(meteorIp, meteorPort);
            ddpConnector.addObserver(this);
        } catch (URISyntaxException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public ObservableDdpClientObserver(DdpClient connector) {
        ddpConnector = connector;
        connector.addObserver(this);
    }

    private int idIndex(List<IdentifiableJSONEntity> collection, String elemId) {
        for (IdentifiableJSONEntity ci : collection) {
            if (ci.getId().equals(elemId))
                return collection.indexOf(ci);
        }
        return -1;
    }

    public void addMessageHandler(final MessageHandler msgH) {
        addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                msgH.update(arg);
            }
        });
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof String) {
            synchronized (this) {
                try {
                    System.out.println(arg);
                    JsonParser parser = new JsonParser();
                    JsonObject messageObject = parser.parse((String)arg).getAsJsonObject();
                    String notifyArgument = null;
                    if (messageObject.has(SERVER_ID_PROPERTY)) {
                        serverId = messageObject.get(SERVER_ID_PROPERTY).getAsString();
                        connected = true;
                    } else if (messageObject.has(MSG_PROPERTY)) {
                        String messageType = messageObject.get(MSG_PROPERTY).getAsString();
                        if (messageType.equalsIgnoreCase(MESSAGE_TYPE_CONNECTED)) {
                            sessionId = messageObject.get(SESSION_ID_PROPERTY).getAsString();
                        } else if (messageType.equalsIgnoreCase(MESSAGE_TYPE_ADDED)) {
                            if (!collectionsMap.containsKey(messageObject.get("collection").getAsString())) {
                                collectionsMap.put(messageObject.get("collection").getAsString(), Collections.synchronizedList(new ArrayList<IdentifiableJSONEntity>()));
                            }
                            collectionsMap.get(messageObject.get("collection").getAsString()).add(new IdentifiableJSONEntity(messageObject.get("id").getAsString(), gson.toJson(messageObject.get("fields").getAsJsonObject())));
                            notifyArgument = messageObject.get("collection").getAsString();
                        } else if (messageType.equalsIgnoreCase(MESSAGE_TYPE_ADDED)) {
                            if (!collectionsMap.containsKey(messageObject.get("collection").getAsString())) {
                                collectionsMap.put(messageObject.get("collection").getAsString(),  Collections.synchronizedList(new ArrayList<IdentifiableJSONEntity>()));
                            }
                            collectionsMap.get(messageObject.get("collection").getAsString()).add(new IdentifiableJSONEntity(messageObject.get("id").getAsString(), gson.toJson(messageObject.get("fields").getAsJsonObject())));
                            notifyArgument = messageObject.get("collection").getAsString();
                        } else if (messageType.equalsIgnoreCase(MESSAGE_TYPE_CHANGED)) {
                            collectionsMap.get(messageObject.get("collection").getAsString()).set(idIndex(collectionsMap.get(messageObject.get("collection").getAsString()), messageObject.get("id").getAsString()), new IdentifiableJSONEntity(messageObject.get("id").getAsString(), gson.toJson(messageObject.get("fields").getAsJsonObject())));
                            notifyArgument = messageObject.get("collection").getAsString();
                        }  else if (messageType.equalsIgnoreCase(MESSAGE_TYPE_REMOVED)) {
                            collectionsMap.get(messageObject.get("collection").getAsString()).remove(idIndex(collectionsMap.get(messageObject.get("collection").getAsString()), messageObject.get("id").getAsString()));
                            notifyArgument = messageObject.get("collection").getAsString();
                        } else {
                            System.out.println("Unknown message type: " + messageType);
                        }
                    }
                    setChanged();
                    notifyObservers(notifyArgument);
                } catch (Exception e) {
                    System.out.println("Some Error for: " + (String)arg);
                    e.printStackTrace();
                }
            }
        }
    }

    public void insertEntity(final String collectionName, final String json) {
        JsonParser parser = new JsonParser();
        JsonObject objectToInsert = parser.parse(json).getAsJsonObject();
        if (!objectToInsert.has("_id"))
            objectToInsert.add("_id", new JsonPrimitive(new ObjectId().toString()));
        Object[] params = {objectToInsert};
        ddpConnector.call("/" + collectionName + "/insert", params);
    }

    private void insertIdentifiedEntity(final String collectionName, final IdentifiableJSONEntity entity) {
        Object[] params = {entity.getAsJsonObject()};
        ddpConnector.call("/" + collectionName + "/insert", params);
    }

    public void deleteEntity(final String collectionName, final String entityId) {
        JsonObject param = new JsonObject();
        param.add("_id", new JsonPrimitive(entityId));
        Object[] params = {param};
        ddpConnector.call("/" + collectionName + "/remove", params);
    }

    public void updateEntity(final String collectionName, final String entityId, final JsonObject objectToSet) {
        JsonObject queryParam = new JsonObject();
        queryParam.add("_id", new JsonPrimitive(entityId));
        JsonObject setParam = new JsonObject();
        setParam.add("$set", objectToSet);
        Object[] params = {queryParam, setParam};
        ddpConnector.call("/" + collectionName + "/update", params);
    }

    public void syncCollection(final String collectionName, final String[] collectionContents) {
        synchronized (this) {
            List<IdentifiableJSONEntity> currentContent = collectionsMap.get(collectionName);
            if (currentContent != null) {
                HashMap<String, IdentifiableJSONEntity> currentContentsMap = new HashMap<String, IdentifiableJSONEntity>(currentContent.size());
                ArrayList<String> newValuesIdentifiers = new ArrayList<String>(collectionContents.length);

                // Create hash map for quick access
                for (IdentifiableJSONEntity currentContentItem : currentContent) {
                    currentContentsMap.put(currentContentItem.getId(), currentContentItem);
                }

                for (String newContentItem : collectionContents) {
                    IdentifiableJSONEntity newEntity = IdentifiableJSONEntity.valueOf(newContentItem, gson);
                    newValuesIdentifiers.add(newEntity.getId());
                    if (!currentContentsMap.containsKey(newEntity.getId())) {
                        insertIdentifiedEntity(collectionName, newEntity);
                    } else if (!currentContentsMap.get(newEntity.getId()).getJsonContent().equals(newEntity.getJsonContent())){
                        updateEntity(collectionName, newEntity.getId(), newEntity.getContentJsonObject());
                    }
                }

                for (IdentifiableJSONEntity currentContentItem : currentContent) {
                    if (!newValuesIdentifiers.contains(currentContentItem.getId())) {
                      deleteEntity(collectionName, currentContentItem.getId());
                    }
                }
            }
        }
    }

    public Object[] getJSONObjectsList(String collectionIdentifier) {
        if (collectionsMap.containsKey(collectionIdentifier)) {
            ArrayList<String> result;
            synchronized (this) {
                result = new ArrayList<String>(collectionsMap.get(collectionIdentifier).size());
                for (IdentifiableJSONEntity entity : collectionsMap.get(collectionIdentifier)) {
                    result.add(entity.getFullJsonContent(gson));
                }
            }
            return result.toArray();
        }
        else return new Object[0];
    }

    public void connect() {
        ddpConnector.connect();
    }

    public void subscribe(String collectionName, Object[] params) {
        try{
            while (!connected) Thread.sleep(5);
            ddpConnector.subscribe(collectionName, params);
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
    }


    public String getServerId() {
        return serverId;
    }

    public String getSessionId() {
        return sessionId;
    }
}