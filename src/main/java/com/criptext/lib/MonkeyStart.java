package com.criptext.lib;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;

/**
 * Objeto usado para registrar con MonkeyKit. Se debe inicializar con el constructor pasandole el
 * APP_ID, PASS y nombre completo del usuario. Despues al llamar a start() se generan llaves AES y
 * se pide un MonkeyId con /user/session/ y luego se entregan las llaves con /user/connect. Al
 * finalizar se llama onSessionOK(), ese callback te permite obtener tu session listo para usar
 * Monkey.
 * Created by gesuwall on 2/10/16.
 */
public class MonkeyStart {
    private AsyncTask<String, String, String> async;
    private WeakReference<Context> ctxRef;
    private AESUtil aesUtil;
    String urlUser, urlPass, fullname;

    public MonkeyStart(Context context, String user, String pass, String fullname){
        this.urlUser = user;
        this.urlPass = pass;
        this.fullname = fullname;
        ctxRef = new WeakReference<>(context);
        async = new AsyncTask<String, String, String>(){

            @Override
            protected void onPostExecute(String res){
                if(res != null)
                    onSessionOK(res);

            }

            @Override
            protected String doInBackground(String... params) {
                try {
                    //Generate keys
                    aesUtil = new AESUtil(ctxRef.get(), "");
                    return getSessionHTTP(params[0], params[1], params[2]);

                }catch (Exception ex){
                    ex.printStackTrace();
                    return null;
                }
            }
        };
    }

    public void onSessionOK(String session){
    //grab your new session id
    }

    public void execute(){
        async.execute(urlUser, urlPass, fullname);
    }

    public void cancel(){
        if(async != null)
            async.cancel(true);
    }

    private String storeKeysIV(String sessionId, String pubKey){
        //Encrypt workers
        RSAUtil rsa = new RSAUtil(Base64.decode(pubKey.getBytes(),0));
        String usk=rsa.encrypt(aesUtil.strKey+":"+aesUtil.strIV);
        //Guardo mis key & Iv
        KeyStoreCriptext.putString(ctxRef.get(), sessionId, aesUtil.strKey+":"+aesUtil.strIV);
        return usk;

    }

    private String getSessionHTTP(String urlUser, String urlPass, String fullname) throws JSONException,
            UnsupportedEncodingException, ClientProtocolException, IOException{
        // Create a new HttpClient and Post Header
        HttpClient httpclient = CriptextLib.newMonkeyHttpClient();
        HttpPost httppost = CriptextLib.newMonkeyHttpPost(CriptextLib.URL + "/user/session", urlUser, urlPass);

        JSONObject localJSONObject1 = new JSONObject();

        JSONObject user_info = new JSONObject();
        user_info.put("name",fullname);

        localJSONObject1.put("username",urlUser);
        localJSONObject1.put("password",urlPass);
        localJSONObject1.put("session_id", "");
        localJSONObject1.put("expiring","0");
        localJSONObject1.put("user_info",user_info);

        JSONObject params = new JSONObject();
        params.put("data", localJSONObject1.toString());
        Log.d("getSessionHTTP", "Req: " + params.toString());

        JSONObject finalResult = CriptextLib.getHttpResponseJson(httpclient, httppost, params.toString());

        Log.d("getSesssionHTTP", finalResult.toString());
        finalResult = finalResult.getJSONObject("data");

        String sessionId = finalResult.getString("sessionId");
        String pubKey = finalResult.getString("publicKey");
        pubKey = pubKey.replace("-----BEGIN PUBLIC KEY-----\n", "").replace("\n-----END PUBLIC KEY-----", "");

        String encriptedKeys = storeKeysIV(sessionId, pubKey);

        //retornar el session solo despues del connect exitoso
        return  connectHTTP(finalResult.getString("sessionId"), encriptedKeys);

    }

    private String connectHTTP(String sessionId, String encriptedKeys) throws JSONException,
            UnsupportedEncodingException, ClientProtocolException, IOException{
 // Create a new HttpClient and Post Header
        HttpClient httpclient = CriptextLib.newMonkeyHttpClient();
        HttpPost httppost = CriptextLib.newMonkeyHttpPost(CriptextLib.URL+"/user/connect", urlUser, urlPass);

        String base64EncodedCredentials = "Basic " + Base64.encodeToString(
                (urlUser + ":" + urlPass).getBytes(),
                Base64.NO_WRAP);

        httppost.setHeader("Authorization", base64EncodedCredentials);
        JSONObject localJSONObject1 = new JSONObject();

        localJSONObject1.put("usk", encriptedKeys);
        localJSONObject1.put("session_id", sessionId);

        JSONObject params = new JSONObject();
        params.put("data", localJSONObject1.toString());
        Log.d("connectHTTP", "Req: " + params.toString());

        JSONObject finalResult = CriptextLib.getHttpResponseJson(httpclient, httppost, params.toString());
         Log.d("connectHTTP", finalResult.toString());
        finalResult = finalResult.getJSONObject("data");

        return finalResult.getString("sessionId");


    }
}
