package com.criptext.database;

import android.content.Context;

import com.criptext.lib.CriptextLib;
import com.criptext.lib.KeyStoreCriptext;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by jigl on 12/23/15.
 */
public class KeyChain {


    public static void putString(Context context, String key, String value){
        Realm realm = CriptextLib.instance().getNewMonkeyRealm();
        KeyChainModel keyChainModel = new KeyChainModel(key,value);
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(keyChainModel);
        realm.commitTransaction();
    }

    public static String getString(Context context, String key){
        Realm realm = CriptextLib.instance().getNewMonkeyRealm();
        KeyChainModel result = realm.where(KeyChainModel.class).equalTo("key", key).findFirst();
        if(result != null){
            return result.getValue();
        }
        realm.close();
        return "";
    }

}
