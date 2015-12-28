package com.criptext.database;

import android.content.Context;

import com.criptext.lib.KeyStoreCriptext;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by jigl on 12/23/15.
 */
public class KeyChain {

    private static String realmName = "MonkeyKit";

    public static void putString(Context context, String key, String value){
        Realm realm = getMonkeyKitRealm(context);
        KeyChainModel keyChainModel = new KeyChainModel(key,value);
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(keyChainModel);
        realm.commitTransaction();
        realm.close();
    }

    public static String getString(Context context, String key){
        Realm realm = getMonkeyKitRealm(context);
        KeyChainModel result = realm.where(KeyChainModel.class).equalTo("key", key).findFirst();
        if(result != null){
            return result.getValue();
        }
        realm.close();
        return "";
    }

    private static Realm getMonkeyKitRealm(Context context){
        //System.out.println("KEYCHAIN - "+context);
        byte[] encryptKey= "132576QFS?(;oh{7Ds9vv|TsPP3=0izz5#6k):>h1&:Upz5[62X{ZPd|Aa522-8&".getBytes();
        RealmConfiguration libraryConfig = new RealmConfiguration.Builder(context)
                .name(realmName)
                .setModules(new MonkeyKitRealmModule())
                .encryptionKey(encryptKey)
                .build();
        return Realm.getInstance(libraryConfig);
    }
}
