package com.criptext.database;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by gesuwall on 10/5/15.
 */
public class TransitionMessageModel extends RealmObject {
    @PrimaryKey
    private String id;
    private String message;

    public TransitionMessageModel(String id, String message) {
       this.message = message;
        this.id = id;
    }
    public TransitionMessageModel() {

    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


}
