package com.criptext.database;

import android.content.Context;

import com.criptext.lib.CriptextLib;
import com.criptext.lib.KeyStoreCriptext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * Created by gesuwall on 10/5/15.
 */
public class TransitionMessage {
    private static String realmName = "MonkeyKit";
    /**
     * Agrega un mensaje a la base de datos del watchdog. Al estar en la base, quiere decir que el
     * mensaje esta en transicion, es decir
     * @param context
     * @param id
     * @param message
     */
    public static void addTransitionMessage(Context context, String id, JSONObject message){
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        TransitionMessageModel newmessage  = new TransitionMessageModel(id, message.toString());
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(newmessage);
        realm.commitTransaction();
    }

    public static void addTransitionMessage(Context context, JSONObject message) throws JSONException{
        JSONObject args = message.getJSONObject("args");
        addTransitionMessage(context, args.get("id").toString(), message);
    }
    public static JSONArray getMessagesInTransition(Context context){
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        RealmResults<TransitionMessageModel> results = realm.where(TransitionMessageModel.class).findAll();
        JSONArray array = new JSONArray();
        for(TransitionMessageModel model : results){
            try {
                JSONObject json = new JSONObject(model.getMessage());
                array.put(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return array;
    }

    /**
     * Remueve un mensaje de la base de datos del watchdog. A estar eliminado, indica que ese
     * mensaje ya no esta en transicion.
     * @param context
     * @param id id del mensaje a remover
     */
    public static void rmTransitionMessage(Context context, String id){
        if(!id.startsWith("-"))
            id = "-" + id;
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        realm.beginTransaction();
        TransitionMessageModel sentmessage = realm.where(TransitionMessageModel.class)
                    .equalTo("id", id).findFirst();
        if(sentmessage != null)
            sentmessage.removeFromRealm();
        realm.commitTransaction();
    }

    /**
     * Calcula el numero total de mensajes en transicion
     * @param context
     * @return el numero de mensajes en transicion, es decir, que se enviaron pero que aun no se
     * recibe el ack de que llegaron al servidor
     */
    public static int getMessagesInTransitionSize(Context context){
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        int result = realm.where(TransitionMessageModel.class).findAll().size();
        return result;
    }

}
