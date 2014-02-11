package org.popper.ddp.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.kutrumbos.DdpClient;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Observer;

/**
 * User: kaa
 * Date: 24.06.13
 */
public class KaaSampleClient {
    public static void main(String[] args) {
        Gson gson = new Gson();
        // specify location of Meteor server (assumes it is running locally)
        String meteorIp = "localhost";
        Integer meteorPort = 3000;

        try {

            // create DDP client instance
            DdpClient ddp = new DdpClient(meteorIp, meteorPort);

            // create DDP client observer
            ObservableDdpClientObserver obs = new ObservableDdpClientObserver(ddp);
            obs.addMessageHandler(new MessageHandler() {
                @Override
                public void update(Object message) {
                    System.out.println(message);
                }
            });

            // make connection to Meteor server
            obs.connect();


            obs.subscribe("lists", new Object[]{});

            try {
                Thread.sleep(1000);
                Object[] lists = obs.getJSONObjectsList("lists");
                ArrayList<IdentifiableJSONEntity> tmpResult = new ArrayList<IdentifiableJSONEntity>();
                for (Object listItem : lists) {
                    tmpResult.add(IdentifiableJSONEntity.valueOf((String) listItem, gson));
                }
                JsonObject fullJsonObject = tmpResult.get(0).getAsJsonObject();
                fullJsonObject.remove("name");
                fullJsonObject.add("name", new JsonPrimitive(String.valueOf(System.currentTimeMillis())));
                tmpResult.set(0, IdentifiableJSONEntity.valueOf(gson.toJson(fullJsonObject), gson));
                String[] arrayElems = new String[tmpResult.size()];
                for (int i=0; i< tmpResult.size(); i++) {
                    arrayElems[i] = tmpResult.get(i).getFullJsonContent(gson);
                }
                obs.syncCollection("lists", arrayElems);
                Thread.sleep(1000);
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
