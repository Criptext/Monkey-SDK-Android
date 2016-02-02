package com.criptext.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.crypto.BadPaddingException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.auth.*;
import com.criptext.comunication.AsyncConnSocket;
import com.criptext.comunication.Compressor;
import com.criptext.comunication.MessageTypes;
import com.criptext.comunication.MOKMessage;
import com.criptext.database.CriptextDBHandler;
import com.criptext.database.MessageModel;
import com.criptext.database.MonkeyKitRealmModule;
import com.criptext.database.RemoteMessage;
import com.criptext.database.TransitionMessage;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class CriptextLib extends Service {

    public static String URL="http://secure.criptext.com";
    private static String transitionMessages = "MonkeyKit.transitionMessages";
    //public static String URL="http://192.168.0.102";
    //VARIABLES PARA REQUERIMIENTOS
    private AQuery aq;
    private BasicHandle handle;
    private String urlUser;
    private String urlPass;
    //VARIABLES PARA EL SOCKET
    public Handler mainMessageHandler;
    private AsyncConnSocket asynConnSocket;
    public int portionsMessages=15;
    public String lastMessageId="0";
    public long lastTimeSynced=0;
    private AESUtil aesutil;
    //VARIABLES DE LA ACTIVITY
    public Context context;
    private String fullname;
    private String sessionid;
    private String expiring;
    private List<MOKMessage> messagesToSendAfterOpen;
    public Watchdog watchdog = null;

    //DELEGATE
    private List<CriptextLibDelegate> delegates;

    //SINGLETON
    static CriptextLib _sharedInstance=null;

    private RSAUtil rsaUtil;
    private boolean shouldAskForGroups;

    //PERSISTENCIA
    public static String null_ref = ";NULL;";
    private static String realmName = "MonkeyKit";
    private static Realm monkeyRealm;

    public RealmConfiguration getMonkeyConfig(){
        byte[] encryptKey= "132576QFS?(;oh{7Ds9vv|TsPP3=0izz5#6k):>h1&:Upz5[62X{ZPd|Aa522-8&".getBytes();
        RealmConfiguration libraryConfig = new RealmConfiguration.Builder(context)
                .name(realmName)
                .setModules(new MonkeyKitRealmModule())
                .encryptionKey(encryptKey)
                .build();
        return libraryConfig;
    }

    /**
     * obtiene una nueva referencia al realm encriptado de MonkeyKit, esto deberia de llamarse en
     * bsckground porque puede que sea muy lento. Hay que cerrar el realm cuando se termina de usarlo.
     * @return
     */
    public Realm getNewMonkeyRealm(){
        RealmConfiguration libraryConfig = getMonkeyConfig();
        return Realm.getInstance(libraryConfig);
    }

    /**
     * Obtiene el realm encriptado de MonkeyKit. Este realm siempre debe de estar abierto, No hay que
     * cerrarlo a menos que se vayan a eliminar todos los mensajes.
     * @return
     */
    public Realm getMonkeyKitRealm(){
        if(monkeyRealm == null)
            monkeyRealm = getNewMonkeyRealm();

        return monkeyRealm;
    }

    public void closeDatabase(){
        if(monkeyRealm != null)
            monkeyRealm.close();

        monkeyRealm = null;
    }

    public CriptextLib(){
        //System.out.println("CRIPTEXTLIB - contructor antes:"+delegates+" - "+context + " isInialized:" + isInialized());
        if(delegates==null)
            delegates=new ArrayList<CriptextLibDelegate>();
        if(context==null && isInialized())
            context=getApplicationContext();
        //System.out.println("CRIPTEXTLIB - contructor despues:" + delegates + " - " + context + " isInialized:" + isInialized());


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("####INICIANDO SERVICIO - " + delegates + " - " + context);
        if(!isInialized()) {
            CriptextLib.instance().setContext(this);
            CriptextLib.instance().startCriptext(intent.getStringExtra("fullname"),
                    intent.getStringExtra("sessionid"), "0", intent.getStringExtra("user"),
                    intent.getStringExtra("pass"), intent.getBooleanExtra("startsession", false));
        }
        else{
            System.out.println("#### SERVICIO - no hago startCriptext");
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static CriptextLib instance() {
        //System.out.println("CRIPTEXTLIB - _sharedInstance:"+_sharedInstance);
        if (_sharedInstance == null)
            _sharedInstance = new CriptextLib();
        return _sharedInstance;
    }

    public boolean isInialized(){
        return urlUser!=null;
    }

    //SETTERS
    public void setContext(Context context){
        //System.out.println("CRIPTEXTLIB - seteando contexto:"+context);
        this.context=context;
    }

    public void addDelegate(CriptextLibDelegate delegate){
        if(delegates==null)
            delegates=new ArrayList<CriptextLibDelegate>();
        delegates.add(delegate);
    }

    public void removeDelegate(CriptextLibDelegate delegate){
        if(delegates==null)
            delegates=new ArrayList<CriptextLibDelegate>();
        delegates.remove(delegate);
    }

    public void executeInDelegates(String method, Object[] info){
        if(method.compareTo("onSessionOK")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onSessionOK();
            }
        }else if(method.compareTo("onSessionError")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onSessionError((String)info[0]);
            }
        }else if(method.compareTo("onConnectOK")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onConnectOK((String)info[0],(String)info[1]);
            }
            if((String)info[1]!=null && ((String)info[1]).compareTo("null")!=0) {
                if(Long.parseLong((String)info[1]) >= Long.parseLong(CriptextDBHandler.get_LastMessage()))
                    CriptextDBHandler.set_LastMessage((String) info[1]);
            }
        }else if(method.compareTo("onMessageRecieved")==0){
            //MANDO EL MENSAJE A CRIPTEXT
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onMessageRecieved((MOKMessage)info[0]);
            }
            //GUARDO EL MENSAJE EN LA BASE DE MONKEY SOLO SI NO HAY DELEGATES
            MOKMessage message = (MOKMessage)info[0];
            int tipo = CriptextDBHandler.getMonkeyActionType(message);
            switch (tipo) {
                case MessageTypes.blMessageDefault: case MessageTypes.blMessageAudio: case MessageTypes.blMessageDocument:
                case MessageTypes.blMessagePhoto: case MessageTypes.blMessageShareAFriend:
                {
                    CriptextDBHandler.addMessage(CriptextDBHandler.createIncomingRemoteMessage(message, CriptextDBHandler.getMonkeyActionType(message), context));
                    break;
                }
            }
        }else if(method.compareTo("onMessageSaved")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onMessageSaved((RemoteMessage) info[0]);
            }
        }
        else if(method.compareTo("onAcknowledgeRecieved")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onAcknowledgeRecieved((MOKMessage)info[0]);
            }
        }else if(method.compareTo("onSocketConnected")==0){
            boolean hasDelegates = false;
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onSocketConnected();
                hasDelegates = true;
            }
            //MANDO EL GET

            //if(hasDelegates)//Comente esta linea porque
            //Si el service se levanta es bueno que haga un get y obtenga los mensajes
            //que importa si no se actualiza el lastmessage desde el service.
            //Con esto cuando abres el mensaje desde el push siempre muestra los unread messages
            CriptextLib.instance().sendSync(CriptextDBHandler.get_LastTimeSynced());
        }else if(method.compareTo("onSocketDisconnected")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onSocketDisconnected();
            }
        }else if(method.compareTo("onConnectError")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onConnectError((String) info[0]);
            }
        }else if(method.compareTo("onGetOK")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onGetOK();
            }
        }else if(method.compareTo("onOpenConversationOK")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onOpenConversationOK((String) info[0]);
            }
        }else if(method.compareTo("onOpenConversationError")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onOpenConversationError((String) info[0]);
            }
        }else if(method.compareTo("onDeleteRecieved")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onDeleteRecieved((MOKMessage) info[0]);
            }
        }else if(method.compareTo("onCreateGroupOK")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onCreateGroupOK((String) info[0]);
            }
        }else if(method.compareTo("onCreateGroupError")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onCreateGroupError((String) info[0]);
            }
        }else if(method.compareTo("onDeleteGroupOK")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onDeleteGroupOK((String) info[0]);
            }
        }else if(method.compareTo("onDeleteGroupError")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onDeleteGroupError((String) info[0]);
            }
        }else if(method.compareTo("onAddMemberToGroupOK")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onAddMemberToGroupOK();
            }
        }else if(method.compareTo("onAddMemberToGroupError")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onAddMemberToGroupError((String) info[0]);
            }
        }else if(method.compareTo("onContactOpenMyConversation")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onContactOpenMyConversation((String) info[0]);
            }
        }else if(method.compareTo("onGetGroupInfoOK")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onGetGroupInfoOK((JSONObject) info[0]);
            }
        }else if(method.compareTo("onGetGroupInfoError")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onGetGroupInfoError((String) info[0]);
            }
        }else if(method.compareTo("onNotificationReceived")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onNotificationReceived((MOKMessage)info[0]);
            }
        } else if(method.compareTo("onMessageBatchReady")==0){
            final ArrayList<MOKMessage> batch = (ArrayList<MOKMessage>)info[0];
            CriptextDBHandler.addMessageBatch(batch, context, new Realm.Transaction.Callback() {
                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                }

                @Override
                public void onSuccess() {
                    for (int i = 0; i < delegates.size(); i++) {
                        delegates.get(i).onMessageBatchReady(batch);
                    }
                }
            });

        }
    }

    /**
     * Start Session in Criptext
     *
     * @param fullname name of the user
     * @param sessionId session id of the user, empty for the fisrt time unless restore
     * @param expiring 0 means not expires and empty means expires
     * @param startSession true if usersync is needed if user has a session id.
     *
     *
     */
    public void startCriptext(String fullname, final String sessionId, String expiring, String user, String pass, final boolean startSession) {

        this.fullname=fullname;
        this.sessionid=sessionId;
        this.expiring=expiring;
        this.messagesToSendAfterOpen=new ArrayList<MOKMessage>();
        this.urlUser = user;
        this.urlPass = pass;
        if(aq==null) {
            aq = new AQuery(context);
            handle = new BasicHandle(urlUser, urlPass);
        }

        if(startSession && sessionId.length() > 0){
            userSync(sessionId);
        }
        else {
            //EJECUTO ESTO EN UN ASYNCTASK PORQUE AL GENERAR LAS CLAVES AES SE INHIBE
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        System.out.println("CRIPTEXTLIB - inicializando aesutil:"+context);
                        aesutil = new AESUtil(context, sessionId);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    if (startSession) {
                        didGenerateAESKeys();
                    } else {
                        System.out.println("CRIPTEXTLIB - no hago startsession");
                        executeInDelegates("onConnectOK", new Object[]{sessionId, null});
                        /****COMIENZA CONEXION CON EL SOCKET*****/
                        startSocketConnection(sessionId, null);
                    }
                }

            }.execute();
        }
    }

    public void userSync(final String sessionid){
		/*
		RSAUtil util = new RSAUtil();
		util.generateKeys();
		aq = new AQuery(context);
        handle = new BasicHandle(urlUser, urlPass);
		rsaUtil = util;
		String url = URL+"/user/sync";
        AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();
		JSONObject localJSONObject1 = new JSONObject();
		try{
			localJSONObject1.put("session_id", sessionid);
			localJSONObject1.put("public_key", "-----BEGIN PUBLIC KEY-----\n"+rsaUtil.pubKeyStr+"\n-----END PUBLIC KEY-----");
            System.out.println("-----BEGIN PUBLIC KEY-----\n" + rsaUtil.pubKeyStr + "\n-----END PUBLIC KEY-----");
			Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());
			cb.url(url).type(JSONObject.class).weakHandler(this, "onUserSync");
            cb.params(params);
            aq.auth(handle).ajax(cb);
			} catch(JSONException ex){
				ex.printStackTrace();
			}
*/
        //Generar RSA keys de forma asincrona
        new AsyncTask<Void, Void, RSAUtil>() {

            @Override
            protected RSAUtil doInBackground(Void... params) {
                RSAUtil util = new RSAUtil();
                util.generateKeys();
                return util;
            }

            @Override
            protected void onPostExecute(RSAUtil result) {
                aq = new AQuery(context);
                handle = new BasicHandle(urlUser, urlPass);
                rsaUtil = result;
                String url = URL+"/user/key/sync";
                AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();
                JSONObject localJSONObject1 = new JSONObject();

                try{
                    localJSONObject1.put("session_id", sessionid);
                    localJSONObject1.put("public_key", "-----BEGIN PUBLIC KEY-----\n"+rsaUtil.pubKeyStr+"\n-----END PUBLIC KEY-----");
                    System.out.println("-----BEGIN PUBLIC KEY-----\n" + rsaUtil.pubKeyStr + "\n-----END PUBLIC KEY-----");

                    Map<String, Object> params = new HashMap<String, Object>();
                    params.put("data", localJSONObject1.toString());

                    cb.url(url).type(JSONObject.class).weakHandler(this, "onUserSync");
                    cb.params(params);
                    aq.auth(handle).ajax(cb);
                } catch(JSONException ex){
                    ex.printStackTrace();
                }

            }

            public void onUserSync(String url, JSONObject jo, com.androidquery.callback.AjaxStatus status) {
                if(jo!=null){
                    Log.d("Sync", jo.toString());
                    try {
                        JSONObject json = jo.getJSONObject("data");

                        executeInDelegates("onConnectOK", new Object[]{sessionid, json.getString("last_time_synced")});
                        shouldAskForGroups=true;
                        //Get data from JSON
                        Log.d("RSADecrypt", json.toString());
                        final String keys=json.getString("keys");

                        new AsyncTask<Void, Void, Void>() {

                            @Override
                            protected Void doInBackground(Void... params) {
                                String decriptedKey=rsaUtil.desencrypt(keys);
                                KeyStoreCriptext.putString(context,sessionid,decriptedKey);
                                System.out.println("USERSYNC DESENCRIPTADO - " + decriptedKey + " " + decriptedKey.length());
                                try {
                                    aesutil = new AESUtil(context, sessionid);
                                }
                                catch (Exception ex){
                                    System.out.println("AES - BAD BASE-64 - borrando claves guardadas");
                                    KeyStoreCriptext.putString(context, sessionid, "");
                                    startCriptext(fullname, "", "0", urlUser, urlPass, true);
                                    CriptextLib.instance().sessionid=sessionid;
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void result) {
                                //executeInDelegates("onConnectOK", new Object[]{sessionid, null});//Porque se hace dos veces??
                                /****COMIENZA CONEXION CON EL SOCKET*****/
                                startSocketConnection(sessionid, null);
                            }

                        }.execute();
                    }
                    catch (Exception e) {
                        executeInDelegates("onConnectError", new Object[]{"Error at onUserSync"});
                        e.printStackTrace();
                    }
                }
                else{
                    executeInDelegates("onConnectError", new Object[]{status.getCode()+" - "+status.getMessage()});
                }
            }

        }.execute();
    }



    public void didGenerateAESKeys(){
        aq = new AQuery(context);
        handle = new BasicHandle(urlUser, urlPass);

        try{

            String url = URL+"/user/session";
            AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

            JSONObject localJSONObject1 = new JSONObject();
            JSONObject user_info = new JSONObject();
            user_info.put("name",fullname);

            localJSONObject1.put("username",urlUser);
            localJSONObject1.put("password",urlPass);
            localJSONObject1.put("session_id",sessionid);
            localJSONObject1.put("expiring",expiring);
            localJSONObject1.put("user_info",user_info);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            cb.url(url).type(JSONObject.class).weakHandler(this, "onSession");
            cb.params(params);
            System.out.println("Params start session: " + params);
            aq.auth(handle).ajax(cb);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void onStop() {

        if(asynConnSocket!=null){
            if(asynConnSocket.isConnected())
                asynConnSocket.disconectSocket();
        }
    }

    public void onResume() {

        startSocketConnection(this.sessionid, null);
    }

    public void sendDisconectOnPull(){
        if(asynConnSocket!=null){
            if(asynConnSocket.isConnected()) {
                asynConnSocket.sendDisconectFromPull();
                asynConnSocket=null;
            }
        }
    }

    /**
     * Evento cuando llega un nuevo session.
     * @param url
     * @param jo
     * @param status
     */
    public void onSession(String url, JSONObject jo, com.androidquery.callback.AjaxStatus status) {

        if(jo!=null){
            try {
                //Get data from JSON
                JSONObject json = jo.getJSONObject("data");
                System.out.println("response session: " + jo.toString());

                    sessionid = this.sessionid.isEmpty() ? json.getString("sessionId") : this.sessionid;
                    String pubKey=json.getString("publicKey");
                    pubKey=pubKey.replace("-----BEGIN PUBLIC KEY-----\n", "").replace("\n-----END PUBLIC KEY-----", "");

                    executeInDelegates("onSessionOK",new Object[]{""});

                    //Encrypt workers
                    RSAUtil rsa = new RSAUtil(Base64.decode(pubKey.getBytes(),0));
                    String usk=rsa.encrypt(aesutil.strKey+":"+aesutil.strIV);

                    //Guardo mis key & Iv
                    KeyStoreCriptext.putString(context, sessionid, aesutil.strKey+":"+aesutil.strIV);

                    //Make the new AJAX
                    String urlconnect = URL+"/user/connect";
                    AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

                    JSONObject localJSONObject1 = new JSONObject();
                    localJSONObject1.put("usk",usk);
                    localJSONObject1.put("session_id",sessionid);
                    System.out.println("CONNECT - " + sessionid + " - " + fullname);

                    Map<String, Object> params = new HashMap<String, Object>();
                    params.put("data", localJSONObject1.toString());

                    cb.url(urlconnect).type(JSONObject.class).weakHandler(this, "onConnect");
                    cb.params(params);

                    aq.auth(handle).ajax(cb);

            } catch (Exception e) {
                executeInDelegates("onSessionError", new Object[]{"Error at onSession"});
                e.printStackTrace();
            }
        }
        else{
            executeInDelegates("onSessionError", new Object[]{status.getCode()+" - "+status.getMessage()});
        }

    }

    public void onConnect(String url, JSONObject jo, com.androidquery.callback.AjaxStatus status) {

        if(jo!=null){
            try {
                JSONObject json = jo.getJSONObject("data");

                executeInDelegates("onConnectOK", new Object[]{json.getString("sessionId"),json.getString("last_message_id")});
                //Get data from JSON
                final String sessionId=json.getString("sessionId");

                /****COMIENZA CONEXION CON EL SOCKET*****/
                startSocketConnection(sessionId, null);

            } catch (Exception e) {
                executeInDelegates("onConnectError", new Object[]{"Error at onConnect"});
                e.printStackTrace();
            }
        }
        else{
            executeInDelegates("onConnectError", new Object[]{status.getCode()+" - "+status.getMessage()});
        }
    }

    /**
     * Se encarga de iniciar la conexion con el socket. Si el objeto asynConnSocket es NULL lo
     * inicializa.
     * @param sessionId Session Id del usuario
     * @param lastAction runnable con la ultima accion que se trato de ejecutar antes de reiniciar la conexion
     */
    private  void startSocketConnection(final String sessionId, Runnable lastAction){

        if(mainMessageHandler == null) {
            /****COMUNICACION ENTRE EL SOCKET Y LA INTERFAZ*****/
            mainMessageHandler = new MonkeyHandler(this);
        }

        if(asynConnSocket==null) {
            System.out.println("MONKEY - SOCKET - conectando con el socket - "+sessionId);
            asynConnSocket = new AsyncConnSocket(sessionId, urlUser + ":" + urlPass, mainMessageHandler, lastAction);
        }

        try{
            System.out.println("MONKEY - onResume SOCKET - isConnected:"+asynConnSocket.isConnected() + " " + asynConnSocket.getSocketStatus());
			/*
			if(asynConnSocket.getSocketStatus() == AsyncConnSocket.Status.sinIniciar){
				System.out.println("MONKEY - onResume SOCKET fireInTheHole");
				asynConnSocket.fireInTheHole();
			} */
            if(asynConnSocket.getSocketStatus() != AsyncConnSocket.Status.conectado && asynConnSocket.getSocketStatus() != AsyncConnSocket.Status.reconectando) {
                System.out.println("MONKEY - onResume SOCKET - connect");
                asynConnSocket.conectSocket();
            }else{
                executeInDelegates("onSocketConnected", new Object[]{""});
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    /************************************************************************/

    public void downloadFile(String filepath, final JsonObject props, final String sender_id,
                             final Runnable runnable){

        if(context==null)
            return;

        final String claves=KeyStoreCriptext.getString(context,sender_id);
        File target = new File(filepath);
        System.out.println("MONKEY - Descargando:"+ filepath + " " + URL+"/file/open/"+target.getName());
        aq.auth(handle).download(URL+"/file/open/"+target.getName(), target, new AjaxCallback<File>(){
            public void callback(String url, File file, com.androidquery.callback.AjaxStatus status) {
                if(file != null){
                    try {
                        String finalContent = "";
                        byte[] finalData = null;
                        //COMPRUEBO SI DESENCRIPTO EL CONTENIDO DEL ARCHIVO
                        if (props.get("encr").getAsString().compareTo("1") == 0) {
                            String[] claveArray = claves.split(":");
                            finalData = aesutil.decryptWithCustomKeyAndIV(IOUtils.toByteArray(new FileInputStream(file.getAbsolutePath())),
                                    claveArray[0], claveArray[1]);
                            //COMPRUEBO SI ES DESDE EL WEB
                            if (props.get("device").getAsString().compareTo("web") == 0) {
                                finalContent = new String(finalData, "UTF-8");
                                finalContent = finalContent.substring(finalContent.indexOf(",") + 1, finalContent.length());
                                finalData = Base64.decode(finalContent.getBytes(), 0);
                            }
                        }
                        //COMPRUEBO SI EL ARCHIVO ESTA COMPRIMIDO
                        if (props.has("cmpr")) {
                            if (props.get("cmpr").getAsString().compareTo("gzip") == 0) {
                                Compressor compressor = new Compressor();
                                finalData = compressor.gzipDeCompress(finalData);
                            }
                        }
                        //VUELVO A GUARDAR EL ARCHIVO
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(finalData);
                        fos.close();
                        //System.out.println("TAM FILE:" + finalData.length);

                        //LE PONGO LA EXTENSION SI LA TIENE
                        if (props.has("ext")) {
                            System.out.println("MONKEY - chmod 777 " + file.getAbsolutePath());
                            Runtime.getRuntime().exec("chmod 777 " + file.getAbsolutePath());
                            //file.renameTo(new File(file.getAbsolutePath()+"."+message.getProps().get("ext").getAsString()));
                        }

                        //message.setMsg(file.getAbsolutePath());
                        //message.setFile(file);

                        //EXCUTE CALLBACK
                        runnable.run();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }else{
                    System.out.println("MONKEY - File failed to donwload - "+status.getCode()+" - "+status.getMessage());
                }
            }

        });
    }
    private void procesarMokMessage(final MOKMessage message, final String claves) throws Exception{
        new AsyncTask<String , Void, Integer>() {

            @Override
            protected Integer doInBackground(String... params) {
                String clave = params[0];
                try {
                    if (message.getProps().get("encr").getAsString().compareTo("1") == 0){
                        //Log.d("CriptextLib", "Decrypt: "+  message.getMsg());
                        message.setMsg(AESUtil.decryptWithCustomKeyAndIV(message.getMsg(),
                                clave.split(":")[0], clave.split(":")[1]));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return 0;
                }
                return 1;
            }

            @Override
            protected void onPostExecute(Integer integer) {
                if (integer.intValue() == 1) {
                    Log.d("CriptextLib", "process MOKMessage successful");
                    executeInDelegates("onMessageRecieved", new Object[]{message});
                }
            }
        }.execute(claves);



    }

    /************************************************************************/

    /**
     * Esto tambien funciona con el sync ok
     */
    public void sendGetOK(){
        Log.d("MonkeyKit", "SyncOK");
        executeInDelegates("onGetOK", new Object[]{});
    }

    /************************************************************************/

    /**
     * Manda un requerimiento HTTP a Monkey para obtener las llaves mas recientes de un usuario que
     * tiene el server. Esta funcion debe de ser llamada en background de lo contrario lanza una excepcion.
     * @param sessionIdTo El session id del usuario cuyas llaves se desean obtener
     * @return Un String con las llaves del usuario. Antes de retornar el resultado, las llaves se
     * guardan en el KeyStoreCriptext.
     */
    public String requestKeyBySession(String sessionIdTo){
        // Create a new HttpClient and Post Header
        HttpClient httpclient = newMonkeyHttpClient();
        HttpPost httppost = new HttpPost(URL+"/user/key/exchange");

        try {

            String base64EncodedCredentials = "Basic " + Base64.encodeToString(
                    (urlUser + ":" + urlPass).getBytes(),
                    Base64.NO_WRAP);

            httppost.setHeader("Authorization", base64EncodedCredentials);

            JSONObject localJSONObject1 = new JSONObject();
            JSONObject params = new JSONObject();
            localJSONObject1.put("user_to",sessionIdTo);
            localJSONObject1.put("session_id",sessionid);
            params.put("data", localJSONObject1.toString());

            Log.d("OpenConversation", "Req: " + params.toString());
            StringEntity se = new StringEntity(params.toString());
            // Add your data
            httppost.setEntity(se);
            //sets a request header so the page receving the request
            //will know what to do with it
            httppost.setHeader("Accept", "application/json");
            httppost.setHeader("Content-type", "application/json");
            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            String json = reader.readLine();
            JSONTokener tokener = new JSONTokener(json);
            JSONObject finalResult = new JSONObject(tokener);

            Log.d("OpenConversation", finalResult.toString());
            String newKeys = aesutil.decrypt(finalResult.getJSONObject("data").getString("convKey"));
            KeyStoreCriptext.putString(context, sessionIdTo, newKeys);
            return newKeys;

        } catch (JSONException ex) {
            ex.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            // TODO Auto-generated catch block
        } catch (IOException e) {
            e.printStackTrace();
            // TODO Auto-generated catch block
        } catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Crea un HTTP Client con timeout
     * @return
     */
    public HttpClient newMonkeyHttpClient(){
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 3000);
        HttpConnectionParams.setSoTimeout(httpParams, 5000);
        return new DefaultHttpClient(httpParams);
    }

    /**
     * Manda un requerimiento HTTP a Monkey para obtener el texto de un mensaje encriptado con
     * las ultimas llaves del remitente que tiene el server. Esta funcion debe de ser llamada en
     * background de lo contrario lanza una excepcion
     * @param messageId Id del mensaje cuyo texto se quiere obtener
     * @return Un String con el texto encriptado con las llaves mas recientes.
     */
    public String requestTextWithLatestKeys(String messageId){
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 3000);
        HttpConnectionParams.setSoTimeout(httpParams, 5000);
        // Create a new HttpClient and Post Header
        HttpClient httpclient = newMonkeyHttpClient();
        HttpGet httppost = new HttpGet(URL+"/message/"+messageId+"/open/secure");

        try {

            String base64EncodedCredentials = "Basic " + Base64.encodeToString(
                    (urlUser + ":" + urlPass).getBytes(),
                    Base64.NO_WRAP);


            httppost.setHeader("Authorization", base64EncodedCredentials);

            Log.d("OpenSecure", "Req: " + messageId);
            //sets a request header so the page receving the request
            //will know what to do with it
            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            String json = reader.readLine();
            JSONTokener tokener = new JSONTokener(json);
            JSONObject finalResult = new JSONObject(tokener);

            Log.d("OpenSecure", finalResult.toString());
            String newEncryptedMessage = finalResult.getJSONObject("data").getString("message");
            Log.d("OpenSecure", newEncryptedMessage);
            return newEncryptedMessage;

        } catch (JSONException ex) {
            ex.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            // TODO Auto-generated catch block
        } catch (IOException e) {
            e.printStackTrace();
            // TODO Auto-generated catch block
        }

        return null;
    }

    public void sendOpenConversation(String sessionId, String sessionIdTo){

        try {
            String urlconnect = URL+"/user/key/exchange";
            AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("user_to",sessionIdTo);
            localJSONObject1.put("session_id",sessionId);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            System.out.println("MONKEY - sending:" + params.toString());
            cb.url(urlconnect).type(JSONObject.class).weakHandler(CriptextLib.this, "onOpenConversation");
            cb.params(params);

            aq.auth(handle).ajax(cb);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void onOpenConversation(String url, final JSONObject jo, com.androidquery.callback.AjaxStatus status) {

        if(jo!=null){
            MOKMessage actual_message=null;
            try {
                if(aesutil==null)
                    aesutil = new AESUtil(context, sessionid);

                System.out.println("MONKEY - onopenConv:"+jo.toString());
                JSONObject json = jo.getJSONObject("data");

                String convKey=json.getString("convKey");
                String desencriptConvKey=aesutil.decrypt(convKey);

                KeyStoreCriptext.putString(context,json.getString("session_to"), desencriptConvKey);
                executeInDelegates("onOpenConversationOK", new Object[]{json.getString("session_to")});

                //SI HAY MENSAJES QUE NO SE HAN PODIDO DESENCRIPTAR
                if(messagesToSendAfterOpen.size()>0){
                    List<MOKMessage> messagesToDelete=new ArrayList<MOKMessage>();
                    for(int i=0;i<messagesToSendAfterOpen.size();i++){
                        actual_message=messagesToSendAfterOpen.get(i);
                        if(actual_message.getSid().compareTo(json.getString("session_to"))==0){
                            int numTries=KeyStoreCriptext.getInt(context,"tries:"+actual_message.getMessage_id());
                            System.out.println("MONKEY - mensaje en espera de procesar, numTries:" + numTries);
                            if(numTries<=1){
                                procesarMokMessage(actual_message, desencriptConvKey);
                                messagesToDelete.add(actual_message);
                            }
                            else if(numTries==2){
                                sendOpenSecure(actual_message.getMessage_id());
                            }
                            else{
                                System.out.println("MONKEY - descarto el mensaje al intento #"+numTries);
                            }
                        }
                    }
                    //BORReeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeO DE LA LISTA
                    for(int i=0;i<messagesToDelete.size();i++){
                        System.out.println("MONKEY - Borrando de la lista");
                        messagesToSendAfterOpen.remove(messagesToDelete.get(i));
                    }
                }
            }
            catch (BadPaddingException e){
                e.printStackTrace();
                if(actual_message!=null) {
                    messagesToSendAfterOpen.add(actual_message);
                    int numTries = KeyStoreCriptext.getInt(context,"tries:" + actual_message.getMessage_id());
                    KeyStoreCriptext.putInt(context,"tries:" + actual_message.getMessage_id(), numTries + 1);
                    sendOpenConversation(actual_message.getRid(), actual_message.getSid());
                }
            }
            catch (Exception e) {
                executeInDelegates("onOpenConversationError", new Object[]{""});
                e.printStackTrace();
            }
        }
        else{
            executeInDelegates("onOpenConversationError", new Object[]{status.getCode()+" - "+status.getMessage()});
        }
    }

    /************************************************************************/

    public void sendOpenSecure(String messageId){

        try {
            String urlconnect = URL+"/message/"+messageId+"/open/secure";
            AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

            System.out.println("MONKEY - sending open secure");
            cb.url(urlconnect).type(JSONObject.class).weakHandler(CriptextLib.this, "onSendOpenSecure");

            aq.auth(handle).ajax(cb);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void onSendOpenSecure(String url, final JSONObject jo, com.androidquery.callback.AjaxStatus status) {

        System.out.println("MONKEY - onopenSecure:"+status.getCode());
        if(jo!=null){
            MOKMessage actual_message=null;
            try {
                System.out.println("MONKEY - onopenSecure:"+jo.toString());
                JSONObject json = jo.getJSONObject("data");

                String message_encrypted=json.getString("message");
                List<MOKMessage> messagesToDelete=new ArrayList<MOKMessage>();
                for(int i=0;i<messagesToSendAfterOpen.size();i++) {
                    actual_message = messagesToSendAfterOpen.get(i);
                    if(actual_message.getMessage_id().compareTo(json.getString("message_id"))==0) {
                        actual_message.setMsg(message_encrypted);
                        procesarMokMessage(actual_message, KeyStoreCriptext.getString(context,actual_message.getRid()));
                        messagesToDelete.add(actual_message);
                        break;
                    }
                }
                //BORRO DE LA LISTA
                for(int i=0;i<messagesToDelete.size();i++){
                    System.out.println("MONKEY - Borrando de la lista en opensecure");
                    messagesToSendAfterOpen.remove(messagesToDelete.get(i));
                }

            }
            catch (Exception e) {
                executeInDelegates("onSendOpenSecure", new Object[]{""});
                e.printStackTrace();
            }
        }
        else{
            executeInDelegates("onSendOpenSecure", new Object[]{status.getCode()+" - "+status.getMessage()});
        }
    }

    /************************************************************************/

    public void subscribePush(String token, String sessionId){

        try {

            String urlconnect = URL+"/push/subscribe";
            AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("token",token);
            localJSONObject1.put("device","android");
            localJSONObject1.put("mode","1");
            localJSONObject1.put("userid",sessionId);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            cb.url(urlconnect).type(JSONObject.class).weakHandler(CriptextLib.this, "onSubscribePush");
            cb.params(params);

            aq.auth(handle).ajax(cb);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onSubscribePush(String url, final JSONObject json, com.androidquery.callback.AjaxStatus status) {

        if(json!=null){
            System.out.println("MONKEY - onSubscribePushOK");
        }
        else
            System.out.println("MONKEY - onSubscribePushError - "+status.getCode()+" - "+status.getMessage());
    }

    /************************************************************************/

    public void createGroup(String members, String groupname, String sessionId){
        try {

            String urlconnect = URL+"/group/create";
            AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

            JSONObject localJSONObjectInfo = new JSONObject();
            localJSONObjectInfo.put("name", groupname);

            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("members",members);
            localJSONObject1.put("info",localJSONObjectInfo);
            localJSONObject1.put("session_id",sessionId);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            cb.url(urlconnect).type(JSONObject.class).weakHandler(CriptextLib.this, "onCreateGroup");
            cb.params(params);

            aq.auth(handle).ajax(cb);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onCreateGroup(String url, final JSONObject jo, com.androidquery.callback.AjaxStatus status) {

        if(jo!=null){
            try {
                JSONObject json = jo.getJSONObject("data");
                executeInDelegates("onCreateGroupOK", new Object[]{json.getString("group_id")});
            }
            catch(Exception e){
                executeInDelegates("onCreateGroupError", new Object[]{""});
                e.printStackTrace();
            }
        }
        else
            executeInDelegates("onCreateGroupError", new Object[]{status.getCode()+" - "+status.getMessage()});
    }

    /************************************************************************/

    public void deleteGroup(String sessionId, String groupID){
        try {

            String urlconnect = URL+"/group/delete";
            AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("group_id",groupID);
            localJSONObject1.put("session_id",sessionId);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            cb.url(urlconnect).type(JSONObject.class).weakHandler(CriptextLib.this, "onDeleteGroup");
            cb.params(params);

            aq.auth(handle).ajax(cb);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onDeleteGroup(String url, final JSONObject jo, com.androidquery.callback.AjaxStatus status) {

        if(jo!=null){
            try {
                System.out.println("MONKEY - onDeleteGroup: "+jo.toString());
                JSONObject json = jo.getJSONObject("data");
                executeInDelegates("onDeleteGroupOK", new Object[]{json.getString("group_id")});
            }
            catch(Exception e){
                executeInDelegates("onDeleteGroupError", new Object[]{""});
                e.printStackTrace();
            }
        }
        else
            executeInDelegates("onDeleteGroupError", new Object[]{status.getCode()+" - "+status.getMessage()});
    }

    /************************************************************************/

    public void addMemberToGroup(String new_member, String groupID, String sessionId ){
        try {

            String urlconnect = URL+"/group/addmember";
            AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("session_id",sessionId);
            localJSONObject1.put("group_id",groupID);
            localJSONObject1.put("new_member",new_member);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            System.out.println("MONKEY - Sending api - "+localJSONObject1);

            cb.url(urlconnect).type(JSONObject.class).weakHandler(CriptextLib.this, "onAddMemberToGroup");
            cb.params(params);

            aq.auth(handle).ajax(cb);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onAddMemberToGroup(String url, final JSONObject jo, com.androidquery.callback.AjaxStatus status) {

        if(jo!=null){
            try {
                System.out.println("MONKEY - onAddMemberToGroup - " + jo.toString());
                //JSONObject json = jo.getJSONObject("data");
                executeInDelegates("onAddMemberToGroupOK", new Object[]{});
            }
            catch(Exception e){
                executeInDelegates("onAddMemberToGroupError", new Object[]{""});
                e.printStackTrace();
            }
        }
        else
            executeInDelegates("onAddMemberToGroupError", new Object[]{status.getCode()+" - "+status.getMessage()});
    }

    /************************************************************************/

    public void getGroupInfo(String groupID){
        try {
            Log.d("MonkeyKit", "Info for " + groupID + " plz.");
            String urlconnect = URL+"/group/info";
            AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("group_id",groupID);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            System.out.println("MONKEY - Sending api - "+localJSONObject1);

            cb.url(urlconnect).type(JSONObject.class).weakHandler(CriptextLib.this, "onGetGroupInfo");
            cb.params(params);

            if(handle==null){
                aq = new AQuery(context);
                handle = new BasicHandle(urlUser, urlPass);
            }
            System.out.println("handle:"+handle);
            aq.auth(handle).ajax(cb);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onGetGroupInfo(String url, final JSONObject jo, com.androidquery.callback.AjaxStatus status) {

        if(jo!=null){
            try {
                JSONObject json = jo.getJSONObject("data");
                System.out.println("MONKEY - onGetGroupInfo - " + json);
                executeInDelegates("onGetGroupInfoOK", new Object[]{json});
            }
            catch(Exception e){
                executeInDelegates("onGetGroupInfoError", new Object[]{""});
                e.printStackTrace();
            }
        }
        else
            executeInDelegates("onGetGroupInfoError", new Object[]{status.getCode()+" - "+status.getMessage()});
    }

    /************************************************************************/

    public void sendOpen(String sessionIDTo){

        try {

            JSONObject args=new JSONObject();
            JSONObject json=new JSONObject();

            args.put("rid", sessionIDTo);

            json.put("args", args);
            json.put("cmd", MessageTypes.MOKProtocolOpen);

            if(asynConnSocket.isConnected()){
                System.out.println("MONKEY - Enviando open:"+json.toString());
                asynConnSocket.sendMessage(json);
            }
            else
                System.out.println("MONKEY - no pudo enviar open - socket desconectado");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void sendMessage(final String idnegative,final String elmensaje, final String sessionIDFrom,
                            final String sessionIDTo, final String pushMessage, final JSONObject params,
                            final JSONObject props){

        if(elmensaje.length()>0){

            try {

                JSONObject args=new JSONObject();
                JSONObject json=new JSONObject();

                args.put("id",(idnegative.contains("-")?"":"-")+idnegative);
                args.put("sid", sessionIDFrom);
                args.put("rid", sessionIDTo);
                args.put("msg", aesutil.encrypt(elmensaje));
                args.put("type", MessageTypes.MOKText);
                args.put("push", pushMessage.replace("\\\\","\\"));
                if(params != null)
                    args.put("params", params.toString());
                if(props != null)
                    args.put("props", props.toString());

                json.put("args", args);
                json.put("cmd", MessageTypes.MOKProtocolMessage);

                addMessageToWatchdog(json);
                if(asynConnSocket.isConnected()){
                    System.out.println("MONKEY - Enviando mensaje:"+json.toString());
                    asynConnSocket.sendMessage(json);
                }
                else
                    System.out.println("MONKEY - no pudo enviar mensaje - socket desconectado");

            }
            catch(NullPointerException ex){
                if(asynConnSocket == null)
                    startSocketConnection(sessionIDFrom, new Runnable() {
                        @Override
                        public void run() {
                            sendMessage(idnegative, elmensaje, sessionIDFrom, sessionIDTo, pushMessage, params, props);
                        }
                    });
                else
                    ex.printStackTrace();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendJSONviaSocket(JSONObject params){
        if(asynConnSocket != null)
            asynConnSocket.sendMessage(params);
        else
            Log.d("MonkeyKit", "NO PUEDO ENVIAR SOCKET POR JSON");
    }

    /**
     * Envia una notificación.
     * @param sessionIDFrom
     * @param sessionIDTo
     * @param paramsObject
     */
    public void sendNotification(final String sessionIDFrom, final String sessionIDTo, final JSONObject paramsObject, final String pushMessage){


        try {

            JSONObject args = new JSONObject();
            JSONObject json=new JSONObject();

            args.put("sid",sessionIDFrom);
            args.put("rid",sessionIDTo);
            args.put("params", paramsObject.toString());
            args.put("type", MessageTypes.MOKNotif);
            args.put("msg", "");
            args.put("push", pushMessage.replace("\\\\","\\"));

            json.put("args", args);

            json.put("cmd", MessageTypes.MOKProtocolMessage);


            if(asynConnSocket.isConnected()){
                System.out.println("MONKEY - Enviando mensaje:"+json.toString());
                asynConnSocket.sendMessage(json);
            }
            else
                System.out.println("MONKEY - no pudo enviar mensaje - socket desconectado");

        } catch(NullPointerException ex){
            if(asynConnSocket == null)
                startSocketConnection(sessionIDFrom, new Runnable() {
                    @Override
                    public void run() {
                        sendNotification(sessionIDFrom, sessionIDTo, paramsObject, pushMessage);
                    }
                });
            else
                ex.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Envia una notificación.
     * @param sessionIDFrom
     * @param sessionIDTo
     * @param paramsObject
     */
    public void sendTemporalNotification(final String sessionIDFrom, final String sessionIDTo, final JSONObject paramsObject){

        try {

            JSONObject args = new JSONObject();
            JSONObject json=new JSONObject();

            args.put("sid",sessionIDFrom);
            args.put("rid",sessionIDTo);
            args.put("params", paramsObject.toString());
            args.put("type", MessageTypes.MOKTempNote);
            args.put("msg", "");

            json.put("args", args);
            json.put("cmd", MessageTypes.MOKProtocolMessage);

            if(asynConnSocket.isConnected()){
                System.out.println("MONKEY - Enviando mensaje:"+json.toString());
                asynConnSocket.sendMessage(json);
            }
            else
                System.out.println("MONKEY - no pudo enviar mensaje - socket desconectado");

        } catch(NullPointerException ex){
            if(asynConnSocket == null)
                startSocketConnection(sessionIDFrom, new Runnable() {
                    @Override
                    public void run() {
                        sendTemporalNotification(sessionIDFrom, sessionIDTo, paramsObject);
                    }
                });
            else
                ex.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFileMessage(final String idnegative, final String elmensaje, final String sessionIDFrom,
                                final String sessionIDTo, final String file_type, final String eph,
                                final String pushMessage, final String paramsFile){
        if(elmensaje.length()>0){

            try {

                JSONObject args = new JSONObject();
                JSONObject propsMessage = new JSONObject();
                JSONObject paramsMessage = new JSONObject();
                propsMessage.put("cmpr", "gzip");
                propsMessage.put("device", "android");
                propsMessage.put("encr", "1");
                propsMessage.put("eph", eph);
                propsMessage.put("file_type", file_type);
                propsMessage.put("str", "0");
                propsMessage.put("ext", FilenameUtils.getExtension(elmensaje));

                if(paramsFile!=null && paramsFile.length()>0) {
                    JsonParser parser = new JsonParser();
                    JsonObject jsonObject = parser.parse(paramsFile).getAsJsonObject();
                    if (jsonObject.has("length")) {
                        paramsMessage.put("length", jsonObject.get("length").getAsInt());
                    }
                }

                args.put("sid",sessionIDFrom);
                args.put("rid",sessionIDTo);
                args.put("props",propsMessage);
                args.put("params",paramsMessage);
                args.put("id",idnegative);
                args.put("push", pushMessage.replace("\\\\","\\"));

                Map<String, Object> params = new HashMap<String, Object>();
                params.put("data", args.toString());
                byte[] finalData=IOUtils.toByteArray(new FileInputStream(elmensaje));

                //COMPRIMIMOS CON GZIP
                Compressor compressor = new Compressor();
                finalData = compressor.gzipCompress(finalData);

                //ENCRIPTAMOS
                finalData=aesutil.encrypt(finalData);

                params.put("file", finalData);

                System.out.println("send file: " + params);
                aq.auth(handle).ajax(URL+"/file/new", params, JSONObject.class, new AjaxCallback<JSONObject>() {
                    @Override
                    public void callback(String url, JSONObject json, AjaxStatus status) {
                        if(json != null){
                            System.out.println(json);
                            try {
                                JSONObject response = json.getJSONObject("data");
                                System.out.println("MONKEY - sendFileMessage ok - "+response.toString()+" - "+response.getString("messageId"));
                                executeInDelegates("onAcknowledgeRecieved", new Object[]{new MOKMessage(response.getString("messageId"), sessionIDTo, sessionIDFrom, idnegative, "", "50", new JsonObject(), new JsonObject())});
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        else{
                            System.out.println("MONKEY - sendFileMessage error - "+status.getCode()+" - "+status.getMessage());
                        }
                    }
                });

            } catch(NullPointerException ex){
                if(asynConnSocket == null)
                    startSocketConnection(sessionIDFrom, new Runnable() {
                        @Override
                        public void run() {
                            sendFileMessage(idnegative, elmensaje, sessionIDFrom, sessionIDTo,
                                    file_type, eph, pushMessage, paramsFile);
                        }
                    });
                else
                    ex.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendGet(final String since){

        try {

            JSONObject args=new JSONObject();
            JSONObject json=new JSONObject();

            args.put("messages_since",since);
            if(since == null || since.equals("0") || shouldAskForGroups) {
                args.put("groups", 1);
                shouldAskForGroups=false;
            }
            args.put("qty", ""+portionsMessages);
            //args.put("G", requestGroups ? 1 : 0);
            json.put("args", args);
            json.put("cmd", MessageTypes.MOKProtocolGet);

            if(asynConnSocket != null && asynConnSocket.isConnected()){
                Log.d("MonkeyKit", "Send Get " + since);
                System.out.println("MONKEY - Enviando Get:"+json.toString());
                asynConnSocket.sendMessage(json);
            }
            else
                System.out.println("MONKEY - no pudo enviar Get - socket desconectado");

            if(watchdog == null) {
                watchdog = new Watchdog(context);
            }
            watchdog.didResponseGet = false;
            Log.d("Watchdog", "Watchdog ready sending Get");
            watchdog.start();

        } catch(NullPointerException ex){
            if(asynConnSocket == null)
                startSocketConnection(this.sessionid, new Runnable() {
                    @Override
                    public void run() {
                        sendGet(since);
                    }
                });
            else
                ex.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }

        lastMessageId=since;
    }

    public void sendSync(final long last_time_synced){

        try {

           JSONObject args=new JSONObject();
            JSONObject json=new JSONObject();

            args.put("since",last_time_synced);
            if(last_time_synced==0 || shouldAskForGroups) {
                args.put("groups", 1);
                shouldAskForGroups=false;
            }
            args.put("qty", ""+portionsMessages);
            //args.put("G", requestGroups ? 1 : 0);
            json.put("args", args);
            json.put("cmd", MessageTypes.MOKProtocolSync);

            if(asynConnSocket != null && asynConnSocket.isConnected()){
                System.out.println("MONKEY - Enviando Sync:"+json.toString());
                asynConnSocket.sendMessage(json);
            }
            else
                System.out.println("MONKEY - no pudo enviar Sync - socket desconectado");

            if(watchdog == null) {
                watchdog = new Watchdog(context);
            }
            watchdog.didResponseGet = false;
            Log.d("Watchdog", "Watchdog ready sending Sync");
            watchdog.start();

        } catch(NullPointerException ex){
            if(asynConnSocket == null)
                startSocketConnection(this.sessionid, new Runnable() {
                    @Override
                    public void run() {
                        sendSync(last_time_synced);
                    }
                });
            else
                ex.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }

        lastTimeSynced=last_time_synced;
    }

    public void sendSet(final String online){

        try {
            JSONObject args=new JSONObject();
            JSONObject json=new JSONObject();

            args.put("online",online);
            json.put("args", args);
            json.put("cmd", MessageTypes.MOKProtocolSet);

            if(asynConnSocket.isConnected()){ //Aqui hay nullpointerexception
                System.out.println("MONKEY - Enviando Set:"+json.toString());
                asynConnSocket.sendMessage(json);
            }
            else
                System.out.println("MONKEY - no pudo enviar Set - socket desconectado");

        } catch(NullPointerException ex){
            if(asynConnSocket == null)
                startSocketConnection(this.sessionid, new Runnable() {
                    @Override
                    public void run() {
                        sendSet(online);
                    }
                });
            else
                ex.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendClose(final String sessionid){

        try {
            JSONObject args=new JSONObject();
            JSONObject json=new JSONObject();

            args.put("rid",sessionid);
            json.put("args", args);
            json.put("cmd", MessageTypes.MOKProtocolClose);

            if(asynConnSocket.isConnected()){
                System.out.println("MONKEY - Enviando Close:"+json.toString());
                asynConnSocket.sendMessage(json);
            }
            else
                System.out.println("MONKEY - no pudo enviar Close - socket desconectado");

        } catch(NullPointerException ex){
            if(asynConnSocket == null)
                startSocketConnection(this.sessionid, new Runnable() {
                    @Override
                    public void run() {
                        sendClose(sessionid);
                    }
                });
            else
                ex.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendDelete(final String sessionid, final String messageid){

        try {
            JSONObject args=new JSONObject();
            JSONObject json=new JSONObject();

            args.put("id",messageid);
            args.put("rid",sessionid);
            json.put("args", args);
            json.put("cmd", MessageTypes.MOKProtocolDelete);

            if(asynConnSocket.isConnected()){
                System.out.println("MONKEY - Enviando Delete:"+json.toString());
                asynConnSocket.sendMessage(json);
            }
            else
                System.out.println("MONKEY - no pudo enviar Delete - socket desconectado");

        } catch(NullPointerException ex){
            if(asynConnSocket == null)
                startSocketConnection(this.sessionid, new Runnable() {
                    @Override
                    public void run() {
                        sendDelete(sessionid, messageid);
                    }
                });
            else
                ex.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean monkeyIsConnected(){
        if(asynConnSocket != null && asynConnSocket.getSocketStatus() == AsyncConnSocket.Status.conectado)
            return asynConnSocket.isConnected();
        return false;
    }

    public void destroyCriptextLib(){
        if(asynConnSocket != null) {
            asynConnSocket.removeContext();
            asynConnSocket.socketMessageHandler = null;
        }
        closeDatabase();
        context = null;

    }

    /**
     * Agrega un mensaje a la base de datos del watchdog. Si el watchdog no esta corriendo, lo
     * inicia.
     * @param json mensaje a guardar
     * @throws JSONException
     */
    private void addMessageToWatchdog(JSONObject json) throws JSONException{
        TransitionMessage.addTransitionMessage(context, json);
        if(watchdog == null) {
            watchdog = new Watchdog(context);
            Log.d("Watchdog", "Watchdog ready sending Message");
            watchdog.start();
        }  else if (!watchdog.isWorking()) {
            Log.d("Watchdog", "Watchdog ready sending Message");
            watchdog.start();
        }
    }

    /**
     * Reconecta el socket con el session id ya existente y le manda un runnable con las acciones que
     * debe de ejecutar cuando se conecte
     * @param run
     */
    public void reconnectSocket(Runnable run){
        if(this.sessionid==null) {
            System.out.println("CRIPTEXTLIB - No puedo recontectar el socket sessionid es null");
            return;
        }
        startSocketConnection(this.sessionid, run);
    }

    public static class MonkeyHandler extends Handler{
        private WeakReference<CriptextLib> libWeakReference;

        public MonkeyHandler(CriptextLib lib){
            libWeakReference = new WeakReference<CriptextLib>(lib);
        }

        public void handleMessage(Message msg) {

            MOKMessage message = null;
            if(msg.obj instanceof MOKMessage){
                message=(MOKMessage)msg.obj;
            }

            //if(message != null && message.getMsg() != null)
            //Log.d("MonkeyHandler", "message: " + message.getMsg() + " tipo: " + msg.what);
            switch (msg.what) {
                case MessageTypes.MOKProtocolMessage:
                    int type = 0;
                    if(message.getProps() != null) {
                        if (message.getProps().has("file_type")) {
                            type = message.getProps().get("file_type").getAsInt();
                            if (type <= 4 && type >= 0)
                                libWeakReference.get().executeInDelegates("onMessageRecieved", new Object[]{message});
                            else
                                System.out.println("MONKEY - archivo no soportado");
                        } else if (message.getProps().has("type")) {
                            type = message.getProps().get("type").getAsInt();
                            if (type == 2 || type == 1)
                                libWeakReference.get().executeInDelegates("onMessageRecieved", new Object[]{message});
                        } else if (message.getProps().has("monkey_action")) {
                            type = message.getProps().get("monkey_action").getAsInt();
                            //if(type == MessageTypes.MOKGroupNewMember) {//PORQUE ESTABA ESTE IF?
                            message.setMonkeyAction(type);
                            //}
                            libWeakReference.get().executeInDelegates("onNotificationReceived", new Object[]{message});
                        } else
                            libWeakReference.get().executeInDelegates("onNotificationReceived", new Object[]{message});
                    }
                    break;
                case MessageTypes.MOKProtocolMessageBatch:
                    libWeakReference.get().executeInDelegates("onMessageBatchReady", new Object[]{(ArrayList<MOKMessage>)msg.obj});
                    break;
                case MessageTypes.MOKProtocolMessageHasKeys:
                    libWeakReference.get().executeInDelegates("onMessageRecieved", new Object[]{message});
                    break;
                case MessageTypes.MOKProtocolMessageNoKeys:
                    libWeakReference.get().messagesToSendAfterOpen.add(message);
                    libWeakReference.get().sendOpenConversation(libWeakReference.get().sessionid,message.getSid());
                    break;
                case MessageTypes.MOKProtocolMessageWrongKeys:
                    libWeakReference.get().messagesToSendAfterOpen.add(message);
                    int numTries=KeyStoreCriptext.getInt(libWeakReference.get().context,
                            "tries:"+message.getMessage_id());
                    KeyStoreCriptext.putInt(libWeakReference.get().context,
                            "tries:" + message.getMessage_id(), numTries + 1);
                    libWeakReference.get().sendOpenConversation(
                            libWeakReference.get().sessionid, message.getSid());
                    break;
                case MessageTypes.MOKProtocolAck:
                    try {
                        System.out.println("ack 205");
                        TransitionMessage.rmTransitionMessage(libWeakReference.get(), message.getMsg());
                        libWeakReference.get().executeInDelegates("onAcknowledgeRecieved", new Object[]{message});
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case MessageTypes.MOKProtocolOpen:{
                    if(KeyStoreCriptext.getString(libWeakReference.get().context,message.getRid()).compareTo("")==0)
                        libWeakReference.get().sendOpenConversation(libWeakReference.get().sessionid ,message.getRid());
                    else
                        System.out.println("MONKEY - llego open pero ya tengo las claves");
                    //MANDAR AL APP QUE PONGA LEIDO TODOS LOS MENSAJES
                    libWeakReference.get().executeInDelegates("onContactOpenMyConversation", new Object[]{message.getSid()});
                    break;
                }
                case MessageTypes.MOKProtocolDelete:{
                    libWeakReference.get().executeInDelegates("onDeleteRecieved", new Object[]{message});
                    break;
                }
                case MessageTypes.MessageSocketConnected:{
                    libWeakReference.get().executeInDelegates("onSocketConnected", new Object[]{""});
                    break;
                }
                case MessageTypes.MessageSocketDisconnected:{
                    libWeakReference.get().executeInDelegates("onSocketDisconnected", new Object[]{""});//new Object[]{""}
                    break;
                }
                case MessageTypes.MOKProtocolGet: {
                    libWeakReference.get().executeInDelegates("onMessageRecieved", new Object[]{message});
                    break;
                }
                case MessageTypes.MOKProtocolSync: {
                    libWeakReference.get().executeInDelegates("onMessageRecieved", new Object[]{message});
                    break;
                }
                default:
                    break;
            }

        }
    }
}