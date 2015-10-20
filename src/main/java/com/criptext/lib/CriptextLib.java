package com.criptext.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.crypto.BadPaddingException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.auth.*;
import com.criptext.comunication.AsyncConnSocket;
import com.criptext.comunication.Compressor;
import com.criptext.comunication.MessageTypes;
import com.criptext.comunication.MOKMessage;
import com.criptext.database.TransitionMessage;
import com.google.gson.JsonObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

public class CriptextLib{

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
    private AESUtil aesutil;
    //VARIABLES DE LA ACTIVITY
    private Context context;
    private String fullname;
    private String sessionid;
    private String expiring;
    private List<MOKMessage> messagesToSendAfterOpen;
    private Watchdog watchdog = null;
    //VARIALBES DE PERSISTENCIA
    public SharedPreferences prefs;

    //DELEGATE
    private List<CriptextLibDelegate> delegates;

    //SINGLETON
    static CriptextLib _sharedInstance=null;

    private RSAUtil rsaUtil;

    public static CriptextLib instance(){
        if (_sharedInstance == null)
            _sharedInstance = new CriptextLib();
        return _sharedInstance;
    }

    //SETTERS
    public void setContext(Context context){
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

    private void executeInDelegates(String method, Object[] info){
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
        }else if(method.compareTo("onMessageRecieved")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onMessageRecieved((MOKMessage)info[0]);
            }
        }else if(method.compareTo("onAcknowledgeRecieved")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onAcknowledgeRecieved((MOKMessage)info[0]);
            }
        }else if(method.compareTo("onSocketConnected")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onSocketConnected();
            }
        }else if(method.compareTo("onSocketDisconnected")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onSocketDisconnected();
            }
        }else if(method.compareTo("onConnectError")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onConnectError((String)info[0]);
            }
        }else if(method.compareTo("onCallOK")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onCallOK();
            }
        }else if(method.compareTo("onCallError")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onCallError();
            }
        }else if(method.compareTo("onOpenConversationOK")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onOpenConversationOK((String)info[0]);
            }
        }else if(method.compareTo("onOpenConversationError")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onOpenConversationError((String)info[0]);
            }
        }else if(method.compareTo("onDeleteRecieved")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onDeleteRecieved((MOKMessage)info[0]);
            }
        }else if(method.compareTo("onCreateGroupOK")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onCreateGroupOK((String)info[0]);
            }
        }else if(method.compareTo("onCreateGroupError")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onCreateGroupError((String)info[0]);
            }
        }else if(method.compareTo("onDeleteGroupOK")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onDeleteGroupOK();
            }
        }else if(method.compareTo("onDeleteGroupError")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onDeleteGroupError((String)info[0]);
            }
        }else if(method.compareTo("onAddMemberToGroupOK")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onAddMemberToGroupOK();
            }
        }else if(method.compareTo("onAddMemberToGroupError")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onAddMemberToGroupError((String)info[0]);
            }
        }else if(method.compareTo("onContactOpenMyConversation")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onContactOpenMyConversation((String)info[0]);
            }
        }else if(method.compareTo("onGetGroupInfoOK")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onGetGroupInfoOK((JSONObject)info[0]);
            }
        }else if(method.compareTo("onGetGroupInfoError")==0){
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onGetGroupInfoError((String)info[0]);
            }
        }else if(method.compareTo("onNotificationReceived")==0){
            System.out.println("notif received");
            for(int i=0;i<delegates.size();i++){
                delegates.get(i).onNotificationReceived((MOKMessage)info[0]);
            }
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
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
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
                    aesutil = new AESUtil(prefs, sessionId);
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

            }

            public void onUserSync(String url, JSONObject jo, com.androidquery.callback.AjaxStatus status) {
                Log.d("Sync", jo.toString());
                if(jo!=null){
                    try {
                        JSONObject json = jo.getJSONObject("data");
                        if(jo.getInt("status")==0){
                            executeInDelegates("onConnectOK", new Object[]{sessionid, json.getString("last_message_received")});
                            //Get data from JSON
                            Log.d("RSADecrypt", json.toString());
                            final String keys=json.getString("keys");

                            new AsyncTask<Void, Void, Void>() {

                                @Override
                                protected Void doInBackground(Void... params) {
                                    String decriptedKey=rsaUtil.desencrypt(keys);
                                    prefs.edit().putString(sessionid,decriptedKey).apply();
                                    System.out.println("USERSYNC DESENCRIPTADO - " + decriptedKey + " " + decriptedKey.length());
                                    aesutil = new AESUtil(prefs, sessionid);
                                    return null;
                                }

                                @Override
                                protected void onPostExecute(Void result) {
                                    executeInDelegates("onConnectOK", new Object[]{sessionid, null});
                                    /****COMIENZA CONEXION CON EL SOCKET*****/
                                    startSocketConnection(sessionid, null);
                                }

                            }.execute();
                        } else
                            executeInDelegates("onConnectError", new Object[]{"Error number "+jo.getInt("status")});

                    } catch (Exception e) {
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
            localJSONObject1.put("username",urlUser);
            localJSONObject1.put("password",urlPass);
            localJSONObject1.put("session_id",sessionid);
            localJSONObject1.put("expiring",expiring);

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
            if(asynConnSocket.isConnected())
                asynConnSocket.sendDisconectFromPull();
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
                if(jo.getInt("status")==0){
                    String sessionId= this.sessionid.isEmpty() ? json.getString("sessionId") : this.sessionid;
                    String pubKey=json.getString("publicKey");
                    pubKey=pubKey.replace("-----BEGIN PUBLIC KEY-----\n", "").replace("\n-----END PUBLIC KEY-----", "");

                    executeInDelegates("onSessionOK",new Object[]{""});

                    //Encrypt workers
                    RSAUtil rsa = new RSAUtil(Base64.decode(pubKey.getBytes(),0));
                    String usk=rsa.encrypt(aesutil.strKey+":"+aesutil.strIV);

                    //Guardo mis key & Iv
                    prefs.edit().putString(sessionId, aesutil.strKey+":"+aesutil.strIV).apply();

                    //Make the new AJAX
                    String urlconnect = URL+"/user/connect";
                    AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

                    JSONObject localJSONObject1 = new JSONObject();
                    localJSONObject1.put("usk",usk);
                    localJSONObject1.put("session_id",sessionId);
                    localJSONObject1.put("session_name", fullname);
                    System.out.println("CONNECT - "+sessionId+" - "+fullname);

                    Map<String, Object> params = new HashMap<String, Object>();
                    params.put("data", localJSONObject1.toString());

                    cb.url(urlconnect).type(JSONObject.class).weakHandler(this, "onConnect");
                    cb.params(params);

                    aq.auth(handle).ajax(cb);
                }
                else{
                    executeInDelegates("onSessionError", new Object[]{"Error number "+json.getInt("error")});
                }


            } catch (Exception e) {
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
                if(jo.getInt("status")==0){
                    executeInDelegates("onConnectOK", new Object[]{json.getString("sessionId"),json.getString("last_message_id")});
                    //Get data from JSON
                    final String sessionId=json.getString("sessionId");

                    /****COMIENZA CONEXION CON EL SOCKET*****/
                    startSocketConnection(sessionId, null);
                }
                else
                    executeInDelegates("onConnectError", new Object[]{"Error number "+jo.getInt("status")});

            } catch (Exception e) {
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
            mainMessageHandler = new Handler() {
                public void handleMessage(Message msg) {
                    MOKMessage message=(MOKMessage)msg.obj;
                    switch (msg.what) {
                        case MessageTypes.MOKProtocolMessage:
                            try {
                                if(message.getMsg().length()>0){
                                    //PUEDE SER DE TIPO TEXTO O FILE
                                    String claves=prefs.getString(message.getSid(), ":");
                                    if(claves.compareTo(":")==0 && !message.getSid().startsWith("legacy:")){
                                        System.out.println("MONKEY - NO TENGO CLAVES DE AMIGO LAS MANDO A PEDIR");
                                        messagesToSendAfterOpen.add(message);
                                        sendOpenConversation(sessionId,message.getSid());
                                    }
                                    else{
                                        procesarMokMessage(message, claves);
                                    }
                                }
                                else {
                                    int type = 0;
                                    if(message.getProps() != null){
                                        if(message.getProps().has("file_type")){
                                            type = message.getProps().get("file_type").getAsInt();
                                            if(type <= 4 && type >= 0)
                                                executeInDelegates("onMessageRecieved", new Object[]{message});
                                        } else if (message.getProps().has("type")){
                                            type = message.getProps().get("type").getAsInt();
                                            if(type == 2 || type == 1)
                                                executeInDelegates("onMessageRecieved", new Object[]{message});
                                        } else if(message.getProps().has("monkey_action")){
                                            type = message.getProps().get("monkey_action").getAsInt();
                                            if(type == MessageTypes.MOKGroupNewMember) {
                                                message.setMonkeyAction(type);
                                            }
                                        }

                                    }

                                    type = message.getMonkeyAction();
                                    if (type <= 5 && type >= 1)
                                        executeInDelegates("onMessageRecieved", new Object[]{message});
                                    else
                                        executeInDelegates("onNotificationReceived", new Object[]{message});
                                }
                            }
                            catch (BadPaddingException e){
                                e.printStackTrace();
                                messagesToSendAfterOpen.add(message);
                                int numTries=prefs.getInt("tries:"+message.getMessage_id(),0);
                                prefs.edit().putInt("tries:"+message.getMessage_id(),numTries+1).apply();
                                sendOpenConversation(sessionId, message.getSid());
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        case MessageTypes.MOKProtocolAck:
                            try {
                                System.out.println("ack 205");
                                TransitionMessage.rmTransitionMessage(context, message.getMessage_id());
                                executeInDelegates("onAcknowledgeRecieved", new Object[]{message});
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        case MessageTypes.MOKProtocolOpen:{
                            if(prefs.getString(message.getRid(),"").compareTo("")==0)
                                sendOpenConversation(sessionId,message.getRid());
                            else
                                System.out.println("MONKEY - llego open pero ya tengo las claves");
                            //MANDAR AL APP QUE PONGA LEIDO TODOS LOS MENSAJES
                            executeInDelegates("onContactOpenMyConversation", new Object[]{message.getSid()});
                            break;
                        }
                        case MessageTypes.MOKProtocolDelete:{
                            executeInDelegates("onDeleteRecieved", new Object[]{message});
                            break;
                        }
                        case MessageTypes.MessageSocketConnected:{
                            executeInDelegates("onSocketConnected", new Object[]{""});
                            break;
                        }
                        case MessageTypes.MessageSocketDisconnected:{
                            executeInDelegates("onSocketDisconnected", new Object[]{""});//new Object[]{""}
                            break;
                        }
                        case MessageTypes.MOKProtocolGet: {
                            executeInDelegates("onMessageRecieved", new Object[]{message});
                            break;
                        }
                        default:
                            break;
                    }

                }
            };
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
                asynConnSocket.conectSocket(new Runnable(){
                    @Override
                    public void run() {
                        //if(CriptextLib.instance() != null)
                        //	CriptextLib.instance().executeInDelegates("onSocketConnected", new Object[]{""});

                    }

                });
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

        final String claves=prefs.getString(sender_id, ":");
        File target = new File(filepath);
        System.out.println("MONKEY - Descargando:"+ filepath + " " + URL+"/file/open/"+target.getName());
        aq.auth(handle).download(URL+"/file/open/"+target.getName(), target, new AjaxCallback<File>(){
            public void callback(String url, File file, com.androidquery.callback.AjaxStatus status) {
                if(file != null){
                    //CONVIERTO EL ARCHIVO A STRING
                    Scanner scanner=null;
                    try {
                        scanner = new Scanner(file);
                        StringBuilder fileContents = new StringBuilder((int) file.length());
                        String lineSeparator = System.getProperty("line.separator");
                        while (scanner.hasNextLine()) {
                            fileContents.append(scanner.nextLine() + lineSeparator);
                        }
                        String finalContent = fileContents.toString();
                        byte[] finalData = null;
                        //COMPRUEBO SI DESENCRIPTO EL CONTENIDO DEL ARCHIVO
                        if (props.get("encr").getAsString().compareTo("1") == 0) {
                            //COMPRUEBO SI ES DESDE EL WEB O MOBILE
                            String[] claveArray = claves.split(":");
                            if (props.get("device").getAsString().compareTo("web") == 0) {
                                finalContent = aesutil.decryptWithCustomKeyAndIV(finalContent,
                                        claveArray[0], claveArray[1]);
                                finalContent = finalContent.substring(finalContent.indexOf(",") + 1, finalContent.length());
                                finalData = Base64.decode(finalContent.getBytes(), 0);
                            } else {
                                finalData = aesutil.decryptWithCustomKeyAndIV(IOUtils.toByteArray(new FileInputStream(file.getAbsolutePath())),
                                        claveArray[0], claveArray[1]);
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
                    } finally {
                        if(scanner!=null) scanner.close();
                    }

                }else{
                    System.out.println("MONKEY - File failed to donwload - "+status.getCode()+" - "+status.getMessage());
                }
            }

        });
    }
    private void procesarMokMessage(final MOKMessage message, final String claves) throws Exception{

        if(message.getProps().has("file_type")){
            //ME DESCARGO EL ARCHIVO
            //downloadFile(message.getMsg(), message.getProps(), message.getSid(), claves);
            //FINALMENTE ENVIO EL MENSAJE AL APP
            executeInDelegates("onMessageRecieved", new Object[]{message});
        }
        else{
            //ES DE TIPO TEXTO SIGA NO MAS
            if(message.getProps().get("encr").getAsString().compareTo("1")==0)
                message.setMsg(aesutil.decryptWithCustomKeyAndIV(message.getMsg(),
                        claves.split(":")[0], claves.split(":")[1]));
            executeInDelegates("onMessageRecieved", new Object[]{message});
        }

    }

    /************************************************************************/

    public void callCompany(String sessionId){
        try {
            String urlconnect = URL+"/user/call";
            AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>();

            JSONObject localJSONObject1 = new JSONObject();
            localJSONObject1.put("company_id","1");
            localJSONObject1.put("session_id",sessionId);
            localJSONObject1.put("name",fullname);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("data", localJSONObject1.toString());

            cb.url(urlconnect).type(JSONObject.class).weakHandler(CriptextLib.this, "onCall");
            cb.params(params);

            aq.auth(handle).ajax(cb);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onCall(String url, final JSONObject json, com.androidquery.callback.AjaxStatus status) {
        if(json!=null){
            executeInDelegates("onCallOK", new Object[]{""});
        }
        else
            executeInDelegates("onCallError", new Object[]{""});
    }

    /************************************************************************/

    public void sendOpenConversation(String sessionId, String sessionIdTo){

        try {
            String urlconnect = URL+"/user/open/secure";
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
                System.out.println("MONKEY - onopenConv:"+jo.toString());
                JSONObject json = jo.getJSONObject("data");
                if(jo.getInt("status")==0){
                    String convKey=json.getString("convKey");
                    String desencriptConvKey=aesutil.decrypt(convKey);

                    prefs.edit().putString(json.getString("session_to"), desencriptConvKey).apply();
                    executeInDelegates("onOpenConversationOK", new Object[]{json.getString("session_to")});

                    //SI HAY MENSAJES QUE NO SE HAN PODIDO DESENCRIPTAR
                    if(messagesToSendAfterOpen.size()>0){
                        List<MOKMessage> messagesToDelete=new ArrayList<MOKMessage>();
                        for(int i=0;i<messagesToSendAfterOpen.size();i++){
                            actual_message=messagesToSendAfterOpen.get(i);
                            if(actual_message.getSid().compareTo(json.getString("session_to"))==0){
                                int numTries=prefs.getInt("tries:"+actual_message.getMessage_id(),0);
                                System.out.println("MONKEY - mensaje en espera de procesar, numTries:" + numTries);
                                if(numTries<=1){
                                    procesarMokMessage(actual_message, desencriptConvKey);
                                    messagesToDelete.add(actual_message);
                                }
                                else{
                                    sendOpenSecure(actual_message.getMessage_id());
                                }
                            }
                        }
                        //BORRO DE LA LISTA
                        for(int i=0;i<messagesToDelete.size();i++){
                            System.out.println("MONKEY - Borrando de la lista");
                            messagesToSendAfterOpen.remove(messagesToDelete.get(i));
                        }
                    }
                }
                else{
                    executeInDelegates("onOpenConversationError", new Object[]{jo.getInt("status")+" - "+jo.getString("message")});
                }
            }
            catch (BadPaddingException e){
                e.printStackTrace();
                if(actual_message!=null) {
                    messagesToSendAfterOpen.add(actual_message);
                    int numTries = prefs.getInt("tries:" + actual_message.getMessage_id(), 0);
                    prefs.edit().putInt("tries:" + actual_message.getMessage_id(), numTries + 1).apply();
                    sendOpenConversation(actual_message.getRid(), actual_message.getSid());
                }
            }
            catch (Exception e) {
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
                if(jo.getInt("status") == 0) {
                    String message_encrypted=json.getString("message");
                    List<MOKMessage> messagesToDelete=new ArrayList<MOKMessage>();
                    for(int i=0;i<messagesToSendAfterOpen.size();i++) {
                        actual_message = messagesToSendAfterOpen.get(i);
                        if(actual_message.getMessage_id().compareTo(json.getString("message_id"))==0) {
                            actual_message.setMsg(message_encrypted);
                            procesarMokMessage(actual_message, prefs.getString(actual_message.getRid(), ""));
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
                else{
                    executeInDelegates("onOpenConversationError", new Object[]{jo.getInt("status")+" - "+jo.getString("message")});
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            executeInDelegates("onOpenConversationError", new Object[]{status.getCode()+" - "+status.getMessage()});
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
            try {
                if(json.getInt("status")==0){
                    System.out.println("MONKEY - onSubscribePushOK");
                }
                else{
                    System.out.println("MONKEY - onSubscribePushError - "+json.getInt("status")+" - "+json.getString("message"));
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
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
                if(jo.getInt("status")==0){
                    executeInDelegates("onCreateGroupOK", new Object[]{json.getString("group_id")});
                }
                else{
                    executeInDelegates("onCreateGroupError", new Object[]{jo.getInt("status")+" - "+jo.getString("message")});
                }
            }
            catch(Exception e){
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
                JSONObject json = jo.getJSONObject("data");
                if(jo.getInt("status")==0){
                    executeInDelegates("onDeleteGroupOK", new Object[]{});
                }
                else{
                    executeInDelegates("onDeleteGroupError", new Object[]{jo.getInt("status")+" - "+jo.getString("message")});
                }
            }
            catch(Exception e){
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
                JSONObject json = jo.getJSONObject("data");
                System.out.println("MONKEY - onAddMemberToGroup - "+json);

                if(jo.getInt("status")==0){
                    executeInDelegates("onAddMemberToGroupOK", new Object[]{});
                }
                else{
                    executeInDelegates("onAddMemberToGroupError", new Object[]{jo.getInt("status")+" - "+jo.getString("message")});
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        else
            executeInDelegates("onAddMemberToGroupError", new Object[]{status.getCode()+" - "+status.getMessage()});
    }

    /************************************************************************/

    public void getGroupInfo(String groupID){
        try {

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
                System.out.println("MONKEY - onGetGroupInfo - "+json);

                if(jo.getInt("status")==0){
                    executeInDelegates("onGetGroupInfoOK", new Object[]{json});
                }
                else{
                    executeInDelegates("onGetGroupInfoError", new Object[]{jo.getInt("status")+" - "+json.getString("message")});
                }
            }
            catch(Exception e){
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
                args.put("push", pushMessage);
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
            args.put("push", pushMessage);

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


    public void sendFileMessage(final String idnegative, final String elmensaje, final String sessionIDFrom,
                                final String sessionIDTo, final String file_type, final String eph,
                                final String pushMessage){
        if(elmensaje.length()>0){

            try {

                JSONObject args = new JSONObject();
                JSONObject propsMessage = new JSONObject();
                propsMessage.put("cmpr", "gzip");
                propsMessage.put("device", "android");
                propsMessage.put("encr", "1");
                propsMessage.put("eph", eph);
                propsMessage.put("file_type", file_type);
                propsMessage.put("str", "0");
                propsMessage.put("ext", FilenameUtils.getExtension(elmensaje));

                args.put("sid",sessionIDFrom);
                args.put("rid",sessionIDTo);
                args.put("props",propsMessage);
                args.put("id",idnegative);
                args.put("push", pushMessage);

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
                                    file_type, eph, pushMessage);
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
            if(since == null || since.equals("0"))
                args.put("groups", 1);
            args.put("qty", ""+portionsMessages);
            //args.put("G", requestGroups ? 1 : 0);
            json.put("args", args);
            json.put("cmd", MessageTypes.MOKProtocolGet);

            if(asynConnSocket != null && asynConnSocket.isConnected()){
                System.out.println("MONKEY - Enviando Get:"+json.toString());
                asynConnSocket.sendMessage(json);
            }
            else
                System.out.println("MONKEY - no pudo enviar Get - socket desconectado");

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
            Log.d("Watchdog", "Watchdog ready");
            watchdog.start();
        }  else if (!watchdog.isWorking()) {
            Log.d("Watchdog", "Watchdog ready");
            watchdog.start();
        }
    }

    /**
     * Reconecta el socket con el session id ya existente y le manda un runnable con las acciones que
     * debe de ejecutar cuando se conecte
     * @param run
     */
    public void reconnectSocket(Runnable run){
        startSocketConnection(this.sessionid, run);
    }

}