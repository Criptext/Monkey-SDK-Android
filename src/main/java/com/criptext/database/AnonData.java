package com.criptext.database;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Esta clase es un RealmObject que guarda datos del usuario que necesitan accederse frecuentemente
 * y no necesitan ser encriptados ya que no revelan datos importantes.
 * Created by gesuwall on 12/23/15.
 */
public class AnonData extends RealmObject {

    @PrimaryKey
    private String _id;
    private String lastMessage;
    private long lastTimeSynced;

    public String get_id() {
        return _id;
    }

    public void set_id(String id){
        _id=id;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastTimeSynced() {
        return lastTimeSynced;
    }

    public void setLastTimeSynced(long lastTimeSynced) {
        this.lastTimeSynced = lastTimeSynced;
    }
}
