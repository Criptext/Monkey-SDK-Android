package com.criptext.database;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by daniel on 12/23/15.
 */

public class KeyChainModel extends RealmObject {

    @PrimaryKey
    private String key;
    private String value;

    public KeyChainModel(String key, String value) {
        this.key = key;
        this.value = value;
    }
    public KeyChainModel() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
