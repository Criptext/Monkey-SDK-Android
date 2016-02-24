package com.criptext.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Base64;
import android.util.Log;

public abstract class MonkeyKit extends Service {

    public enum CBTypes { onSessionOK, onSessionError, onConnectOK, onMessageReceived,
        onMessageSaved, onAcknowledgeReceived, onSocketConnected, onSocketDisconnected, onConnectError,
        onGetOK, onOpenConversationOK, onOpenConversationError, onDeleteReceived, onCreateGroupOK,
        onCreateGroupError, onDeleteGroupOK, onDeleteGroupError, onAddMemberToGroupOK, onAddMemberToGroupError,
        onContactOpenMyConversation, onGetGroupInfoOK, onGetGroupInfoError, onNotificationReceived,
        onMessageBatchReady}

    public static String URL="http://secure.criptext.com";
    private static String transitionMessagesPrefs = "MonkeyKit.transitionMessages";
    private static String lastSyncPrefs = "MonkeyKit.lastSyncTime";
    private static String lastSyncKey = "MonkeyKit.lastSyncKey";

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
    private String fullname;
    public String sessionid;
    private String expiring;
    private List<MOKMessage> messagesToSendAfterOpen;
    public Watchdog watchdog = null;

    //DELEGATE
    private List<MonkeyKitDelegate> delegates;

    //SINGLETON
    static MonkeyKit _sharedInstance=null;

    private RSAUtil rsaUtil;
    private boolean shouldAskForGroups;

    //PERSISTENCIA
    public static String null_ref = ";NULL;";

    public MonkeyKit(){
        //System.out.println("CRIPTEXTLIB - contructor antes:"+delegates+" - "+getContext() + " isInialized:" + isInialized());
        if(delegates==null)
            delegates=new ArrayList<MonkeyKitDelegate>();
        //System.out.println("CRIPTEXTLIB - contructor despues:" + delegates + " - " + getContext() + " isInialized:" + isInialized());


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("####INICIANDO SERVICIO - " + delegates + " - ");
        _sharedInstance = this;
        if(!isInialized()) {
            MonkeyKit.instance().startCriptext(intent.getStringExtra("fullname"),
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

    public static MonkeyKit instance() {
        //System.out.println("CRIPTEXTLIB - _sharedInstance:"+_sharedInstance);
        return _sharedInstance;
    }

    public boolean isInialized(){
        return urlUser!=null;
    }

    public void addDelegate(MonkeyKitDelegate delegate){
        if(delegates==null)
            delegates=new ArrayList<MonkeyKitDelegate>();
        delegates.add(delegate);
    }

    public void removeDelegate(MonkeyKitDelegate delegate){
        if(delegates==null)
            delegates=new ArrayList<MonkeyKitDelegate>();
        delegates.remove(delegate);
    }

    public void executeInDelegates(CBTypes method, Object[] info){
        switch(method){
            case onSessionOK:{
                for(int i=0;i<delegates.size();i++){
                    delegates.get(i).onSessionOK();
                }
            }
            break;
            case onConnectOK: {
                for(int i=0;i<delegates.size();i++){
                    delegates.get(i).onConnectOK((String)info[0],(String)info[1]);
                }
                if(info[1]!=null && ((String)info[1]).compareTo("null")!=0) {
                    if(Long.parseLong((String)info[1]) >= getLastTimeSynced())
                        setLastTimeSynced(Long.parseLong((String) info[1]));
                }
            }
            break;
            case onMessageReceived:
            {
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
                        storeMessage(message);
                        //CriptextDBHandler.addMessage(CriptextDBHandler.createIncomingRemoteMessage(message,
                         //       CriptextDBHandler.getMonkeyActionType(message), getContext()));
                        break;
                    }
                }
            }
            break;
            case onMessageSaved:
            {
                for(int i=0;i<delegates.size();i++){
                    delegates.get(i).onMessageSaved((MOKMessage) info[0]);
                }
            }
            break;
            case onAcknowledgeReceived:
            {
                for(int i=0;i<delegates.size();i++){
                    delegates.get(i).onAcknowledgeRecieved((MOKMessage)info[0]);
                }
            }
            break;
            case onSocketConnected:
            {
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
                MonkeyKit.instance().sendSync(getLastTimeSynced());
            }
            break;
            case onSocketDisconnected:
            {
                for(int i=0;i<delegates.size();i++){
                    delegates.get(i).onSocketDisconnected();
                }
            }
            break;
            case onConnectError:{
                for(int i=0;i<delegates.size();i++){
                    delegates.get(i).onConnectError((String) info[0]);
                }
            }
            break;
            case onGetOK: {
                for(int i=0;i<delegates.size();i++){
                    delegates.get(i).onGetOK();
                }
            }
            break;
            case onOpenConversationOK: {
                for(int i=0;i<delegates.size();i++){
                    delegates.get(i).onOpenConversationOK((String) info[0]);
                }
            }
            break;
            case onOpenConversationError: {
                for(int i=0;i<delegates.size();i++){
                    delegates.get(i).onOpenConversationError((String) info[0]);
                }
            }
            break;
            case onDeleteReceived: {
                for(int i=0;i<delegates.size();i++){
                    delegates.get(i).onDeleteRecieved((MOKMessage) info[0]);
                }
            }
            break;
            case onCreateGroupOK: {
                for(int i=0;i<delegates.size();i++){
                    delegates.get(i).onCreateGroupOK((String) info[0]);
                }
            }
            break;
            case onCreateGroupError: {
                for(int i=0;i<delegates.size();i++){
                    delegates.get(i).onCreateGroupError((String) info[0]);
                }
            }
            break;
            case onDeleteGroupOK: {
                for(int i=0;i<delegates.size();i++){
                    delegates.get(i).onDeleteGroupOK((String) info[0]);
                }
            }
            break;
            case onDeleteGroupError: {
                for(int i=0;i<delegates.size();i++){
                    delegates.get(i).onDeleteGroupError((String) info[0]);
                }
            }
            break;
            case onAddMemberToGroupOK: {
                for(int i=0;i<delegates.size();i++){
                    delegates.get(i).onAddMemberToGroupOK();
                }
            }
            break;
            case onAddMemberToGroupError: {
                for(int i=0;i<delegates.size();i++){
                    delegates.get(i).onAddMemberToGroupError((String) info[0]);
                }
            }
            break;
            case onContactOpenMyConversation: {
                for(int i=0;i<delegates.size();i++){
                    delegates.get(i).onContactOpenMyConversation((String) info[0]);
                }
            }
            break;
            case onGetGroupInfoOK: {
                for(int i=0;i<delegates.size();i++){
                    delegates.get(i).onGetGroupInfoOK((JsonObject) info[0]);
                }
            }
            break;
            case onGetGroupInfoError: {
                for(int i=0;i<delegates.size();i++){
                    delegates.get(i).onGetGroupInfoError((String) info[0]);
                }
            }
            break;
            case onNotificationReceived: {
                for(int i=0;i<delegates.size();i++){
                    delegates.get(i).onNotificationReceived((MOKMessage)info[0]);
                }
            }
            break;
            case onMessageBatchReady: {
                final ArrayList<MOKMessage> batch = (ArrayList<MOKMessage>)info[0];
                storeMessageBatch(batch);
            }
        }

    }

    public void batchStored(ArrayList<MOKMessage> batch){
        for (int i = 0; i < delegates.size(); i++) {
            delegates.get(i).onMessageBatchReady(batch);
         }
    }

    public void messageStored(MOKMessage message){

        executeInDelegates(MonkeyKit.CBTypes.onMessageSaved, new Object[]{message});

    }

    private void initAESUtilAsync(final String sessionId, final Runnable runnable){
        new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        System.out.println("CRIPTEXTLIB - inicializando aesutil:");
                        aesutil = new AESUtil(getContext(), sessionId);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    runnable.run();
                }

            }.execute();
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
            aq = new AQuery(getContext());
            handle = new BasicHandle(urlUser, urlPass);
        }

        if(startSession && sessionId.length() > 0){
            userSync(sessionId);
        }
        else {
            //EJECUTO ESTO EN UN ASYNCTASK PORQUE AL GENERAR LAS CLAVES AES SE INHIBE
            initAESUtilAsync(sessionId, new Runnable() {
                @Override
                public void run() {
                    if (startSession) {
                        didGenerateAESKeys();
                    } else {
                        System.out.println("CRIPTEXTLIB - no hago startsession");
                        executeInDelegates(CBTypes.onConnectOK, new Object[]{sessionId, null});
                        /****COMIENZA CONEXION CON EL SOCKET*****/
                        startSocketConnection(sessionId, null);
                    }
                }
            });
        }
    }

    public void userSync(final String sessionid){
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
                aq = new AQuery(getContext());
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

                        executeInDelegates(CBTypes.onConnectOK, new Object[]{sessionid, json.getString("last_time_synced")});
                        shouldAskForGroups=true;
                        //Get data from JSON
                        Log.d("RSADecrypt", json.toString());
                        final String keys=json.getString("keys");

                        new AsyncTask<Void, Void, Void>() {

                            @Override
                            protected Void doInBackground(Void... params) {
                                String decriptedKey=rsaUtil.desencrypt(keys);
                                KeyStoreCriptext.putString(MonkeyKit.this.getContext() ,sessionid,decriptedKey);
                                System.out.println("USERSYNC DESENCRIPTADO - " + decriptedKey + " " + decriptedKey.length());
                                try {
                                    aesutil = new AESUtil(MonkeyKit.this.getContext(), sessionid);
                                }
                                catch (Exception ex){
                                    System.out.println("AES - BAD BASE-64 - borrando claves guardadas");
                                    KeyStoreCriptext.putString(getContext(), sessionid, "");
                                    startCriptext(fullname, "", "0", urlUser, urlPass, true);
                                    MonkeyKit.instance().sessionid=sessionid;
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
                        executeInDelegates(CBTypes.onConnectError, new Object[]{"Error at onUserSync"});
                        e.printStackTrace();
                    }
                }
                else{
                    executeInDelegates(CBTypes.onConnectError, new Object[]{status.getCode()+" - "+status.getMessage()});
                }
            }

        }.execute();
    }



    public void didGenerateAESKeys(){
        aq = new AQuery(getContext());
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

                    executeInDelegates(CBTypes.onSessionOK,new Object[]{""});

                    //Encrypt workers
                    RSAUtil rsa = new RSAUtil(Base64.decode(pubKey.getBytes(),0));
                    String usk=rsa.encrypt(aesutil.strKey+":"+aesutil.strIV);

                    //Guardo mis key & Iv
                    KeyStoreCriptext.putString(getContext(), sessionid, aesutil.strKey+":"+aesutil.strIV);

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
                executeInDelegates(CBTypes.onSessionError, new Object[]{"Error at onSession"});
                e.printStackTrace();
            }
        }
        else{
            executeInDelegates(CBTypes.onSessionError, new Object[]{status.getCode()+" - "+status.getMessage()});
        }

    }

    public void onConnect(String url, JSONObject jo, com.androidquery.callback.AjaxStatus status) {

        if(jo!=null){
            try {
                JSONObject json = jo.getJSONObject("data");

                executeInDelegates(CBTypes.onConnectOK, new Object[]{json.getString("sessionId"),json.getString("last_message_id")});
                //Get data from JSON
                final String sessionId=json.getString("sessionId");

                /****COMIENZA CONEXION CON EL SOCKET*****/
                startSocketConnection(sessionId, null);

            } catch (Exception e) {
                executeInDelegates(CBTypes.onConnectError, new Object[]{"Error at onConnect"});
                e.printStackTrace();
            }
        }
        else{
            executeInDelegates(CBTypes.onConnectError, new Object[]{status.getCode()+" - "+status.getMessage()});
        }
    }

    /**
     * Se encarga de iniciar la conexion con el socket. Si el objeto asynConnSocket es NULL lo
     * inicializa.
     * @param sessionId Session Id del usuario
     * @param lastAction runnable con la ultima accion que se trato de ejecutar antes de reiniciar la conexion
     */
    private void startSocketConnection(final String sessionId, Runnable lastAction){

        if(mainMessageHandler == null) {
            /****COMUNICACION ENTRE EL SOCKET Y LA INTERFAZ*****/
            mainMessageHandler = new MonkeyHandler(this);
        }

        if(urlUser == null || urlPass == null) {
            System.out.println("MONKEY - SOCKET - urlUser o urlUser son null");
            return;
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
                executeInDelegates(CBTypes.onSocketConnected, new Object[]{""});
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    /************************************************************************/

    /**
     * Descarga un archivo del servidor de MonkeyKit por HTTP.
     * @param filepath ruta absoluta del lugar donde se guardara el archivo
     * @param props JsonObject con los props del MOKMessage que tenia el mensaje al ser transmitido
     * @param sender_id session ID del usuario que envio el archivo
     * @param runnable Runnable con el codigo que se desee ejecutar una vez que la descarga termine.
     */
    public void downloadFile(String filepath, final JsonObject props, final String sender_id,
                             final Runnable runnable){

        if(getContext()==null)
            return;

        if(aq==null) {
            aq = new AQuery(getContext());
            handle = new BasicHandle(urlUser, urlPass);
        }

        final String claves=KeyStoreCriptext.getString(getContext(),sender_id);
        File target = new File(filepath);
        System.out.println("MONKEY - Descargando:"+ filepath + " " + URL+"/file/open/"+target.getName());
        aq.auth(handle).download(URL + "/file/open/" + target.getName(), target, new AjaxCallback<File>() {
            public void callback(String url, File file, com.androidquery.callback.AjaxStatus status) {
                if (file != null) {
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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("MONKEY - File failed to donwload - " + status.getCode() + " - " + status.getMessage());
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
                        //Log.d("MonkeyKit", "Decrypt: "+  message.getMsg());
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
                    Log.d("MonkeyKit", "process MOKMessage successful");
                    executeInDelegates(CBTypes.onMessageReceived, new Object[]{message});
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
        executeInDelegates(CBTypes.onGetOK, new Object[]{});
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

        try {

            HttpPost httppost = newMonkeyHttpPost(URL + "/user/key/exchange", urlUser, urlPass);

            JSONObject localJSONObject1 = new JSONObject();
            JSONObject params = new JSONObject();
            localJSONObject1.put("user_to",sessionIdTo);
            localJSONObject1.put("session_id",sessionid);
            params.put("data", localJSONObject1.toString());
            Log.d("OpenConversation", "Req: " + params.toString());

            JSONObject finalResult = getHttpResponseJson(httpclient, httppost, params.toString());
            Log.d("OpenConversation", finalResult.toString());
            String newKeys = aesutil.decrypt(finalResult.getJSONObject("data").getString("convKey"));
            KeyStoreCriptext.putString(getContext(), sessionIdTo, newKeys);
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
    public static HttpClient newMonkeyHttpClient(){
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 3000);
        HttpConnectionParams.setSoTimeout(httpParams, 5000);
        return new DefaultHttpClient(httpParams);
    }

    public static HttpPost newMonkeyHttpPost(String url, String user, String pass){
        HttpPost httppost = new HttpPost(url);
        String base64EncodedCredentials = "Basic " + Base64.encodeToString(
             (user + ":" + pass).getBytes(), Base64.NO_WRAP);
        httppost.setHeader("Authorization", base64EncodedCredentials);
        //sets a request header so the page receving the request
        //will know what to do with it
        httppost.setHeader("Accept", "application/json");
        httppost.setHeader("Content-type", "application/json");
        return httppost;

    }

    public static JSONObject getHttpResponseJson(HttpClient httpclient, HttpPost httppost, String params) throws IOException,
    JSONException{
        // Execute HTTP Post Request
        StringEntity se = new StringEntity(params);
            // Add your data
            httppost.setEntity(se);
            HttpResponse response = httpclient.execute(httppost);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            String json = reader.readLine();
            JSONTokener tokener = new JSONTokener(json);
            return new JSONObject(tokener);
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
            cb.url(urlconnect).type(JSONObject.class).weakHandler(MonkeyKit.this, "onOpenConversation");
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
                    aesutil = new AESUtil(getContext(), sessionid);

                System.out.println("MONKEY - onopenConv:"+jo.toString());
                JSONObject json = jo.getJSONObject("data");

                String convKey=json.getString("convKey");
                String desencriptConvKey=aesutil.decrypt(convKey);

                KeyStoreCriptext.putString(getContext(), json.getString("session_to"), desencriptConvKey);
                executeInDelegates(CBTypes.onOpenConversationOK, new Object[]{json.getString("session_to")});

                //SI HAY MENSAJES QUE NO SE HAN PODIDO DESENCRIPTAR
                if(messagesToSendAfterOpen.size()>0){
                    List<MOKMessage> messagesToDelete=new ArrayList<MOKMessage>();
                    for(int i=0;i<messagesToSendAfterOpen.size();i++){
                        actual_message=messagesToSendAfterOpen.get(i);
                        if(actual_message.getSid().compareTo(json.getString("session_to"))==0){
                            int numTries=KeyStoreCriptext.getInt(getContext(),"tries:"+actual_message.getMessage_id());
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
                    int numTries = KeyStoreCriptext.getInt(getContext(),"tries:" + actual_message.getMessage_id());
                    KeyStoreCriptext.putInt(getContext(),"tries:" + actual_message.getMessage_id(), numTries + 1);
                    sendOpenConversation(actual_message.getRid(), actual_message.getSid());
                }
            }
            catch (Exception e) {
                executeInDelegates(CBTypes.onOpenConversationError, new Object[]{""});
                e.printStackTrace();
            }
        }
        else{
            executeInDelegates(CBTypes.onOpenConversationError, new Object[]{status.getCode()+" - "+status.getMessage()});
        }
    }

    /************************************************************************/

    public void sendOpenSecure(String messageId){

        try {
            String urlconnect = URL+"/message/"+messageId+"/open/secure";
            AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

            System.out.println("MONKEY - sending open secure");
            cb.url(urlconnect).type(JSONObject.class).weakHandler(MonkeyKit.this, "onSendOpenSecure");

            aq.auth(handle).ajax(cb);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void onSendOpenSecure(String url, final JSONObject jo, com.androidquery.callback.AjaxStatus status) {

        System.out.println("MONKEY - onopenSecure:" + status.getCode());
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
                        procesarMokMessage(actual_message, KeyStoreCriptext.getString(getContext(),actual_message.getRid()));
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
                e.printStackTrace();
            }
        }
    }

    /************************************************************************/
    public static void subscribePushHttp(String token, String sessionId, String urlUser, String urlPass){
        // Create a new HttpClient and Post Header
        HttpClient httpclient = newMonkeyHttpClient();

        try {

            HttpPost httppost = newMonkeyHttpPost(URL + "/push/subscribe", urlUser, urlPass);

            JSONObject params = new JSONObject();
            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("token",token);
            localJSONObject1.put("device","android");
            localJSONObject1.put("mode","1");
            localJSONObject1.put("userid", sessionId);

            params.put("data", localJSONObject1.toString());
            Log.d("subscribePushHttp", "Req: " + params.toString());

            JSONObject finalResult = getHttpResponseJson(httpclient, httppost, params.toString());
            Log.d("subscribePushHttp", finalResult.toString());

        } catch (JSONException ex) {
            ex.printStackTrace();
        } catch (IOException ex){
            ex.printStackTrace();
        }
    }

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

            cb.url(urlconnect).type(JSONObject.class).weakHandler(MonkeyKit.this, "onSubscribePush");
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

            cb.url(urlconnect).type(JSONObject.class).weakHandler(MonkeyKit.this, "onCreateGroup");
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
                executeInDelegates(CBTypes.onCreateGroupOK, new Object[]{json.getString("group_id")});
            }
            catch(Exception e){
                executeInDelegates(CBTypes.onCreateGroupError, new Object[]{""});
                e.printStackTrace();
            }
        }
        else
            executeInDelegates(CBTypes.onCreateGroupError, new Object[]{status.getCode()+" - "+status.getMessage()});
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

            cb.url(urlconnect).type(JSONObject.class).weakHandler(MonkeyKit.this, "onDeleteGroup");
            cb.params(params);

            aq.auth(handle).ajax(cb);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onDeleteGroup(String url, final JSONObject jo, com.androidquery.callback.AjaxStatus status) {

        if(jo!=null){
            try {
                System.out.println("MONKEY - onDeleteGroup: " + jo.toString());
                JSONObject json = jo.getJSONObject("data");
                executeInDelegates(CBTypes.onDeleteGroupOK, new Object[]{json.getString("group_id")});
            }
            catch(Exception e){
                executeInDelegates(CBTypes.onDeleteGroupError, new Object[]{""});
                e.printStackTrace();
            }
        }
        else
            executeInDelegates(CBTypes.onDeleteGroupError, new Object[]{status.getCode()+" - "+status.getMessage()});
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

            cb.url(urlconnect).type(JSONObject.class).weakHandler(MonkeyKit.this, "onAddMemberToGroup");
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
                executeInDelegates(CBTypes.onAddMemberToGroupOK, new Object[]{});
            }
            catch(Exception e){
                executeInDelegates(CBTypes.onAddMemberToGroupError, new Object[]{""});
                e.printStackTrace();
            }
        }
        else
            executeInDelegates(CBTypes.onAddMemberToGroupError, new Object[]{status.getCode()+" - "+status.getMessage()});
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

            System.out.println("MONKEY - Sending api - " + localJSONObject1);

            cb.url(urlconnect).type(JSONObject.class).weakHandler(MonkeyKit.this, "onGetGroupInfo");
            cb.params(params);

            if(handle==null){
                aq = new AQuery(getContext());
                handle = new BasicHandle(urlUser, urlPass);
            }
            System.out.println("handle:" + handle);
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

                JsonParser jsonParser = new JsonParser();
                JsonObject gsonObject = (JsonObject)jsonParser.parse(json.toString());
                executeInDelegates(CBTypes.onGetGroupInfoOK, new Object[]{gsonObject});
            }
            catch(Exception e){
                executeInDelegates(CBTypes.onGetGroupInfoError, new Object[]{""});
                e.printStackTrace();
            }
        }
        else
            executeInDelegates(CBTypes.onGetGroupInfoError, new Object[]{status.getCode() + " - " + status.getMessage()});
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

    public MOKMessage sendMessage(final String elmensaje, final String sessionIDTo,
                            final String pushMessage, final JsonObject params){

        MOKMessage newMessage = null;
        if(elmensaje.length()>0){
            try {

                long datetimeorder = System.currentTimeMillis();
                long datetime = datetimeorder/1000;
                final String idnegative = "-" + datetime;
                JsonObject props = new JsonObject();
                props.addProperty("str", "0");
                props.addProperty("encr", "1");
                props.addProperty("device", "android");

                newMessage = new MOKMessage(idnegative, this.sessionid, sessionIDTo, elmensaje,
                        "" + datetime, "" + MessageTypes.blMessageDefault, params, props);
                newMessage.setDatetimeorder(datetimeorder);

                if(aesutil == null) {
                    initAESUtilAsync(this.sessionid, new Runnable() {
                        @Override
                        public void run() {
                            sendMessage(elmensaje, sessionIDTo, pushMessage, params);
                        }
                    });
                    return newMessage;
                }
                JSONObject args=new JSONObject();
                JSONObject json=new JSONObject();

                args.put("id",idnegative);
                args.put("sid", this.sessionid);
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

                if(asynConnSocket == null) {
                    System.out.println("MONKEY - no pudo enviar mensaje - socket desconectado");
                    startSocketConnection(this.sessionid, new Runnable() {
                        @Override
                        public void run() {
                            sendMessage(elmensaje, sessionIDTo, pushMessage, params);
                        }
                    });
                    return newMessage;
                } else if(asynConnSocket.isConnected()){
                    System.out.println("MONKEY - Enviando mensaje:"+json.toString());
                    asynConnSocket.sendMessage(json);
                }

                storeMessage(newMessage);
                return newMessage;
            }
            catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        return null;
    }

    public void sendJSONviaSocket(JSONObject params){
        if(asynConnSocket != null)
            asynConnSocket.sendMessage(params);
        else
            Log.d("MonkeyKit", "NO PUEDO ENVIAR SOCKET POR JSON");
    }

    /**
     * Envia una notificación a traves de MonkeyKit. Las notificaciones no se persisten. Si el
     * destinatario no la pudo recibir a tiempo, no la recibira nunca
     * @param sessionIDTo session ID del usuario que recibira la notificacion
     * @param paramsObject JsonObject con parametros adicionales que necesita la aplicacion
     * @param pushMessage Mensaje a mostrar en el push notification
     */
    public void sendNotification(final String sessionIDTo, final JSONObject paramsObject, final String pushMessage){


        try {

            JSONObject args = new JSONObject();
            JSONObject json=new JSONObject();

            args.put("sid",this.sessionid);
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
                startSocketConnection(this.sessionid, new Runnable() {
                    @Override
                    public void run() {
                        sendNotification(sessionIDTo, paramsObject, pushMessage);
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
    public void sendTemporalNotification(final String sessionIDTo, final JSONObject paramsObject){

        try {

            JSONObject args = new JSONObject();
            JSONObject json=new JSONObject();

            args.put("sid",this.sessionid);
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
                startSocketConnection(MonkeyKit.this.sessionid, new Runnable() {
                    @Override
                    public void run() {
                        sendTemporalNotification(sessionIDTo, paramsObject);
                    }
                });
            else
                ex.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Envia un archivo a traves de MonkeyKit. Se envia un mensaje por el socket con metadata del archivo
     * y posteriormente el archivo es subido por HTTP al servidor
     * @param pathToFile Ruta del archivo
     * @param sessionIDTo session ID del destinatario del archivo
     * @param file_type tipo de archivo. Debe de ser igual a una de las constantes de MessageTypes.FileTypes
     * @param paramsMessage JsonObject con parametros adicionales que necesita la aplicacion
     * @param pushMessage Mensaje a mostrar en el push notification
     * @return
     */
    public MOKMessage sendFileMessage(final String pathToFile, final String sessionIDTo, final int file_type, final JsonObject paramsMessage, final String pushMessage){
        MOKMessage newMessage = null;
        if(pathToFile.length()>0){

            try {

                long datetimeorder = System.currentTimeMillis();
                long datetime = datetimeorder/1000;
                final String idnegative = "-" + datetime;
                JsonObject props = new JsonObject();
                props.addProperty("str", "0");
                props.addProperty("encr", "1");
                props.addProperty("device", "android");

                newMessage = new MOKMessage(idnegative, this.sessionid, sessionIDTo, pathToFile,
                       "" + datetime, "" + file_type, paramsMessage, props);
                newMessage.setDatetimeorder(datetimeorder);

                if(aesutil == null) {
                    initAESUtilAsync(this.sessionid, new Runnable() {
                        @Override
                        public void run() {
                            sendFileMessage(pathToFile, sessionIDTo, file_type, paramsMessage, pushMessage);
                        }
                    });
                    return newMessage;
                }

                JSONObject args = new JSONObject();
                JSONObject propsMessage = new JSONObject();
                propsMessage.put("cmpr", "gzip");
                propsMessage.put("device", "android");
                propsMessage.put("encr", "1");
                propsMessage.put("file_type", file_type);
                propsMessage.put("str", "0");
                propsMessage.put("ext", FilenameUtils.getExtension(pathToFile));

                args.put("sid",this.sessionid);
                args.put("rid",sessionIDTo);
                args.put("props",propsMessage);
                if(paramsMessage != null)
                    args.put("params",paramsMessage);
                args.put("id",idnegative);
                args.put("push", pushMessage.replace("\\\\","\\"));

                Map<String, Object> params = new HashMap<String, Object>();
                params.put("data", args.toString());
                byte[] finalData=IOUtils.toByteArray(new FileInputStream(pathToFile));

                //COMPRIMIMOS CON GZIP
                Compressor compressor = new Compressor();
                finalData = compressor.gzipCompress(finalData);

                //ENCRIPTAMOS
                finalData=aesutil.encrypt(finalData);

                params.put("file", finalData);

                System.out.println("send file: " + params);
                aq.auth(handle).ajax(URL + "/file/new", params, JSONObject.class, new AjaxCallback<JSONObject>() {
                    @Override
                    public void callback(String url, JSONObject json, AjaxStatus status) {
                        if (json != null) {
                            System.out.println(json);
                            try {
                                JSONObject response = json.getJSONObject("data");
                                System.out.println("MONKEY - sendFileMessage ok - " + response.toString() + " - " + response.getString("messageId"));
                                executeInDelegates(CBTypes.onAcknowledgeReceived,
                                        new Object[]{new MOKMessage(response.getString("messageId"), sessionIDTo, MonkeyKit.this.sessionid,
                                                idnegative, "", "50", new JsonObject(), new JsonObject())});
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            System.out.println("MONKEY - sendFileMessage error - " + status.getCode() + " - " + status.getMessage());
                        }
                    }
                });
                storeMessage(newMessage);

            }  catch (Exception e) {
                e.printStackTrace();
            }
        }

        return newMessage;
    }

    public void sendGet(final String since){

        try {

            JSONObject args=new JSONObject();
            JSONObject json=new JSONObject();

            args.put("messages_since", since);
            if(since == null || since.equals("0") || shouldAskForGroups) {
                args.put("groups", 1);
                shouldAskForGroups=false;
            }
            args.put("qty", "" + portionsMessages);
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
                watchdog = new Watchdog();
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

            Log.d("MissingGroups", "Send Sync");
            if(last_time_synced==0 || shouldAskForGroups) {
                Log.d("MissingGroups", "groups = 1");
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
                watchdog = new Watchdog();
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
            else {
                System.out.println("MONKEY - no pudo enviar Close - socket desconectado");
                onResume();//Reconecto el socket
            }

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

    public void destroyMonkeyKit(){
        if(asynConnSocket != null) {
            asynConnSocket.removeContext();
            asynConnSocket.socketMessageHandler = null;
        }

    }

    /**
     * Agrega un mensaje a la base de datos del watchdog. Si el watchdog no esta corriendo, lo
     * inicia.
     * @param json mensaje a guardar
     * @throws JSONException
     */
    private void addMessageToWatchdog(JSONObject json) throws JSONException{
        JSONObject args = json.getJSONObject("args");
        try {
            addPendingMessage(args.get("id").toString(), json.toString());
            if (watchdog == null) {
                watchdog = new Watchdog();
                Log.d("Watchdog", "Watchdog ready sending Message");
                watchdog.start();
            } else if (!watchdog.isWorking()) {
                Log.d("Watchdog", "Watchdog ready sending Message");
                watchdog.start();
            }
        }catch (Exception ex){
            ex.printStackTrace();
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
        System.out.println("CRIPTEXTLIB - Reconectando socket");
        startSocketConnection(this.sessionid, run);
    }

    public static class MonkeyHandler extends Handler{
        private WeakReference<MonkeyKit> libWeakReference;

        public MonkeyHandler(MonkeyKit lib){
            libWeakReference = new WeakReference<MonkeyKit>(lib);
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
                                libWeakReference.get().executeInDelegates(CBTypes.onMessageReceived, new Object[]{message});
                            else
                                System.out.println("MONKEY - archivo no soportado");
                        } else if (message.getProps().has("type")) {
                            type = message.getProps().get("type").getAsInt();
                            if (type == 2 || type == 1)
                                libWeakReference.get().executeInDelegates(CBTypes.onMessageReceived, new Object[]{message});
                        } else if (message.getProps().has("monkey_action")) {
                            type = message.getProps().get("monkey_action").getAsInt();
                            //if(type == MessageTypes.MOKGroupNewMember) {//PORQUE ESTABA ESTE IF?
                            message.setMonkeyAction(type);
                            //}
                            libWeakReference.get().executeInDelegates(CBTypes.onNotificationReceived, new Object[]{message});
                        } else
                            libWeakReference.get().executeInDelegates(CBTypes.onNotificationReceived, new Object[]{message});
                    }
                    break;
                case MessageTypes.MOKProtocolMessageBatch:
                    libWeakReference.get().executeInDelegates(CBTypes.onMessageBatchReady, new Object[]{(ArrayList<MOKMessage>)msg.obj});
                    break;
                case MessageTypes.MOKProtocolMessageHasKeys:
                    libWeakReference.get().executeInDelegates(CBTypes.onMessageReceived, new Object[]{message});
                    break;
                case MessageTypes.MOKProtocolMessageNoKeys:
                    libWeakReference.get().messagesToSendAfterOpen.add(message);
                    libWeakReference.get().sendOpenConversation(libWeakReference.get().sessionid,message.getSid());
                    break;
                case MessageTypes.MOKProtocolMessageWrongKeys:
                    libWeakReference.get().messagesToSendAfterOpen.add(message);
                    int numTries=KeyStoreCriptext.getInt(libWeakReference.get().getContext(),
                            "tries:"+message.getMessage_id());
                    KeyStoreCriptext.putInt(libWeakReference.get().getContext(),
                            "tries:" + message.getMessage_id(), numTries + 1);
                    libWeakReference.get().sendOpenConversation(
                            libWeakReference.get().sessionid, message.getSid());
                    break;
                case MessageTypes.MOKProtocolAck:
                    try {
                        System.out.println("ack 205");
                        libWeakReference.get().removePendingMessage(message.getMsg());
                        libWeakReference.get().executeInDelegates(CBTypes.onAcknowledgeReceived, new Object[]{message});
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case MessageTypes.MOKProtocolOpen:{
                    if(KeyStoreCriptext.getString(libWeakReference.get().getContext(),message.getRid()).compareTo("")==0)
                        libWeakReference.get().sendOpenConversation(libWeakReference.get().sessionid ,message.getRid());
                    else
                        System.out.println("MONKEY - llego open pero ya tengo las claves");
                    //MANDAR AL APP QUE PONGA LEIDO TODOS LOS MENSAJES
                    libWeakReference.get().executeInDelegates(CBTypes.onContactOpenMyConversation, new Object[]{message.getSid()});
                    break;
                }
                case MessageTypes.MOKProtocolDelete:{
                    libWeakReference.get().executeInDelegates(CBTypes.onDeleteReceived, new Object[]{message});
                    break;
                }
                case MessageTypes.MessageSocketConnected:{
                    libWeakReference.get().executeInDelegates(CBTypes.onSocketConnected, new Object[]{""});
                    break;
                }
                case MessageTypes.MessageSocketDisconnected:{
                    libWeakReference.get().executeInDelegates(CBTypes.onSocketDisconnected, new Object[]{""});//new Object[]{""}
                    break;
                }
                case MessageTypes.MOKProtocolGet: {
                    libWeakReference.get().executeInDelegates(CBTypes.onMessageReceived, new Object[]{message});
                    break;
                }
                case MessageTypes.MOKProtocolSync: {
                    libWeakReference.get().executeInDelegates(CBTypes.onMessageReceived, new Object[]{message});
                    break;
                }
                default:
                    break;
            }

        }
    }

    public static void startMonkeyService(Context context, Class<?> service, String fullname, String session_id, String app_id,
                                         String app_key){
        Intent intent = new Intent(context, service);
        intent.putExtra("fullname", fullname);
        intent.putExtra("sessionid", session_id);
        intent.putExtra("user", app_id);
        intent.putExtra("pass", app_key);
        intent.putExtra("startsession", false);
        context.startService(intent);
    }
    /**
     * Devuelve el context de la applicacion. Este metodo deberia implementarse usando el context
     * que se obtiene al usar la clase Application para asegurar que nunca sea null.
     * @return El context de la aplicacion
     */
    public Context getContext(){
       return getApplicationContext();
    };

    /**
     * Guarda un mensaje de MonkeyKit en la base de datos. La implementacion de este metodo deberia de
     * ser asincrona para mejorar el rendimiento del servicio. MonkeyKit llamara a este metodo cada
     * vez que reciba un mensaje para guardarlo.
     * @param message
     */
    public abstract void storeMessage(MOKMessage message);

    /**
     * Guarda un grupo de mensajes de MonkeyKit que se recibieron despues de un sync en la base de datos.
     * Es sumamente importante implementar esto de forma asincrona porque potencialmente, podrian
     * llegar cientos de mensajes, haciendo la operacion sumamente costosa.
     * @param messages
     */
    public abstract void storeMessageBatch(ArrayList<MOKMessage> messages);

    /**
     * Guarda el timestamp de la ultima vez que se llamo a sync. MonkeyKit llamara a este metodo
     * cada vez que haga sync. La implementacion de este metodo deberia de ser asincrona para mejorar
     * el rendimiento del servicio.
     * @param lastTime
     */
    public void setLastTimeSynced(long lastTime){
        new AsyncTask<Long, Long, Long>(){
            @Override
            protected Long doInBackground(Long... params) {
                SharedPreferences prefs = getContext().getSharedPreferences(lastSyncPrefs, 0);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong(lastSyncKey, params[0]);
                return params[0];
            }
        }.execute(lastTime);
    }

    /**
     * Obtiene el timestamp de la ultima vez que se llamo a sync. MonkeyKit llamara a este metodo cada
     * vez que haga sync. La implementacion de este metodo debe de ser sincrona y retornar lo mas
     * rapido posible.
     * @return El timestamp del ultimo sync
     */
    public long getLastTimeSynced(){
        SharedPreferences prefs = getContext().getSharedPreferences(lastSyncPrefs, 0);
        return prefs.getLong(lastSyncKey, 0);
    };

    /**
     * Guarda un mensaje que aun no se envia exitosamente en la base de datos. Es necesario persistir esto
     * para que se pueda retransimitir en caso de que falle.  La implementacion de este metodo deberia de
     * ser asincrona para mejorar el rendimiento del servicio.
     * @param id id del mensaje a guardar.
     * @param message String serializado del mensaje a guardar
     */
    public void addPendingMessage(String id, String message){
        new AsyncTask<String, Integer, Integer>(){
            @Override
            protected Integer doInBackground(String... params) {
                SharedPreferences prefs = getContext().getSharedPreferences(transitionMessagesPrefs, 0);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(params[1], params[1]);
                return 0;
            }
        }.execute(id, message);
    }

    /**
     * Elimina un mensaje de la base de datos de mensajes pendientes. MonkeyKit llamara a esta funcion
     * despues de una transmision exitosa.  La implementacion de este metodo deberia de
     * ser asincrona para mejorar el rendimiento del servicio.
     * @param id Id del mensaje a borrar
     */
    public void removePendingMessage(String id){
        new AsyncTask<String, Integer, Integer>(){
            @Override
            protected Integer doInBackground(String... params) {
                SharedPreferences prefs = getContext().getSharedPreferences(transitionMessagesPrefs, 0);
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove(params[0]);
                return 0;
            }
        }.execute(id);
    }

    /**
     * Obtiene todos los mensajes pendientes de la base de datos. MonkeyKit llamara este metodo cuando
     * este dispuesto a retransimitir datos. La implementacion de este metodo debe de ser sincrona y
     * retornar lo mas rapido posible.
     * @return
     */
    public String[] getPendingMessages(){
        SharedPreferences prefs = getContext().getSharedPreferences(transitionMessagesPrefs, 0);
        Map<String, ?> prefsMap = prefs.getAll();

        if(prefsMap != null) {
            int index = 0;
            final int size = prefsMap.size();
            String[] result = new String[size];
            for (Map.Entry<String, ?> entry : prefsMap.entrySet()) {
                result[index++] = entry.getValue().toString();
            }
            return result;
        }

        return new String[0];

    }
}
