package com.criptext.lib;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.criptext.database.TransitionMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by gesuwall on 10/5/15.
 */
public class Watchdog {
    private static int TIMEOUT = 5000;
    private final Context context;
    private final Handler handler;
    private boolean working;
    public boolean didResponseGet = true;

    public Watchdog(Context context) {
        this.context = context;
        this.handler = new Handler();
        working = false;

    }

    public void start(){
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                final JSONArray array = TransitionMessage.getMessagesInTransition(context);
                Log.d("Watchdog", "Watchdog there are "+array.length()+" messages to send and didResponseGet "+didResponseGet);
                if(array.length()>0 && didResponseGet) {
                    CriptextLib.instance().sendDisconectOnPull();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            CriptextLib.instance().reconnectSocket(new Runnable() {
                                @Override
                                public void run() {
                                    final int len = array.length();
                                    for (int i = 0; i < len; i++) {
                                        try {
                                            CriptextLib.instance().sendJSONviaSocket((JSONObject) array.get(i));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });
                        }
                    }, 2000);
                }
                working = false;
            }
        }, TIMEOUT);
        working = true;
    }

    public boolean isWorking(){
       return working;
    }


}
