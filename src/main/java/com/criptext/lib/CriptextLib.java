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
import com.google.gson.JsonObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Base64;

public class CriptextLib{

	//VARIABLES PARA REQUERIMIENTOS
	private AQuery aq;
	private BasicHandle handle;
	private String urlUser;
	private String urlPass;
	//VARIABLES PARA EL SOCKET
	public static Handler mainMessageHandler;
	private AsyncConnSocket asynConnSocket;
	public int secondsDelay=2;
	public int portionsMessages=15;
	public String lastMessageId="0";
	private AESUtil aesutil;	
	//VARIABLES DE LA ACTIVITY
	private Context context;
	private String fullname;
	private String sessionid;
	private String expiring;
	private List<MOKMessage> messagesToSendAfterOpen;

	//VARIALBES DE PERSISTENCIA
	public SharedPreferences prefs;

	//DELEGATE
	private List<CriptextLibDelegate> delegates;

	//SINGLETON
	static CriptextLib _sharedInstance=null;

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
				delegates.get(i).onConnectOK((String)info[0]);
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
	 * 
	 */	
	public void startSession(String fullname, final String sessionId, String expiring, String user, String pass) {

		this.fullname=fullname;
		this.sessionid=sessionId;
		this.expiring=expiring;
		this.prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
		this.messagesToSendAfterOpen=new ArrayList<MOKMessage>();
		this.urlUser = user;
		this.urlPass = pass;

		//EJECUTO ESTO EN UN ASYNCTASK PORQUE AL GENERAR LAS CLAVES AES SE INHIBE
		new AsyncTask<Void, Void, Void>(){

			@Override
			protected Void doInBackground(Void... params) {
				aesutil=new AESUtil(prefs,sessionId);
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				didGenerateAESKeys();
			}

		}.execute();

	}

	public void didGenerateAESKeys(){
		aq = new AQuery(context);
		handle = new BasicHandle(urlUser, urlPass);

		try{

			String url = "http://secure.criptext.com/user/session";
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

		if(asynConnSocket!=null){
			try{
				System.out.println("MONKEY - onResume SOCKET - isConnected:"+asynConnSocket.isConnected()+" - asynConnSocket.isInitiated:"+asynConnSocket.isInitiated+" - asynConnSocket.desconexionVerdadera:"+asynConnSocket.desconexionVerdadera);
				if(!asynConnSocket.isConnected() && !asynConnSocket.isInitiated && !asynConnSocket.desconexionVerdadera){
					asynConnSocket.desconexionVerdadera=false;	
					System.out.println("MONKEY - onResume SOCKET fireInTheHole");
					asynConnSocket.fireInTheHole();
				}
				else if(!asynConnSocket.isConnected() && asynConnSocket.isInitiated && asynConnSocket.desconexionVerdadera){
					asynConnSocket.desconexionVerdadera=false;
					System.out.println("MONKEY - onResume SOCKET - connect");
					asynConnSocket.conectSocket();
				}
				else{
					executeInDelegates("onSocketConnected",new Object[]{""});
				}
			}catch(Exception e){  
				e.printStackTrace();
			}	
		}
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
					String urlconnect = "http://secure.criptext.com/user/connect";
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
					executeInDelegates("onConnectOK", new Object[]{json.getString("sessionId")});
					//Get data from JSON
					final String sessionId=json.getString("sessionId");

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
									prefs.edit().putString(message.getSid(), "").apply();
									sendOpenConversation(sessionId,message.getSid());
								}
								catch (Exception e) {
									e.printStackTrace();
								}
								break;
							case MessageTypes.MOKProtocolAck:
								try {
									System.out.println("ack 205");
									executeInDelegates("onAcknowledgeRecieved", new Object[]{message});
								} catch (Exception e) {
									e.printStackTrace();
								}
								break;
							case MessageTypes.MOKProtocolOpen:{
								sendOpenConversation(sessionId,message.getRid());
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

					/****COMIENZA CONEXION CON EL SOCKET*****/
					if(asynConnSocket==null) {
						System.out.println("MONKEY - conectando con el socket - "+sessionId);
						asynConnSocket = new AsyncConnSocket(context, sessionId, urlUser + ":" + urlPass, mainMessageHandler);
					}
					try{
						System.out.println("MONKEY - onConnect - isConnected:"+asynConnSocket.isConnected()+" - asynConnSocket.isInitiated:"+asynConnSocket.isInitiated+" - asynConnSocket.desconexionVerdadera:"+asynConnSocket.desconexionVerdadera);
						if(!asynConnSocket.isConnected() && !asynConnSocket.isInitiated && !asynConnSocket.desconexionVerdadera){
							asynConnSocket.desconexionVerdadera=false;	
							System.out.println("MONKEY - onConnect SOCKET fireInTheHole");
							asynConnSocket.fireInTheHole();
						}
						else if(!asynConnSocket.isConnected() && asynConnSocket.isInitiated && asynConnSocket.desconexionVerdadera){
							asynConnSocket.desconexionVerdadera=false;
							System.out.println("MONKEY - onConnect SOCKET connect");
							asynConnSocket.conectSocket();
						}
						else{
							executeInDelegates("onSocketConnected", new Object[]{""});
						}
					}catch(Exception e){
						asynConnSocket.desconexionVerdadera=false;
						e.printStackTrace();
					}
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
	
	/************************************************************************/

	public void downloadFile(String filepath, final JsonObject props, final String sender_id,
							 final Runnable runnable){

		final String claves=prefs.getString(sender_id, ":");
		File target = new File(filepath);
		System.out.println("MONKEY - Descargando:"+ filepath + " " + "http://secure.criptext.com/file/open/"+target.getName());
		aq.auth(handle).download("http://secure.criptext.com/file/open/"+target.getName(), target, new AjaxCallback<File>(){
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
			String urlconnect = "http://secure.criptext.com/user/call";
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

		if(prefs.getString(sessionIdTo, "").compareTo("")!=0){
			executeInDelegates("onOpenConversationOK", new Object[]{sessionIdTo});
		}
		else{
			try {
				String urlconnect = "http://secure.criptext.com/user/open/secure";
				AjaxCallback<JSONObject> cb = new AjaxCallback<JSONObject>(); 

				JSONObject localJSONObject1 = new JSONObject();
				localJSONObject1.put("user_to",sessionIdTo);
				localJSONObject1.put("session_id",sessionId);

				Map<String, Object> params = new HashMap<String, Object>();
				params.put("data", localJSONObject1.toString());

				cb.url(urlconnect).type(JSONObject.class).weakHandler(CriptextLib.this, "onOpenConversation");
				cb.params(params);

				aq.auth(handle).ajax(cb);	

			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	public void onOpenConversation(String url, final JSONObject jo, com.androidquery.callback.AjaxStatus status) {

		if(jo!=null){
			try {
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
							MOKMessage message=messagesToSendAfterOpen.get(i);
							if(message.getSid().compareTo(json.getString("session_to"))==0){
								System.out.println("MONKEY - mensaje en espera de procesar");
								procesarMokMessage(message, desencriptConvKey);    							   
								messagesToDelete.add(message);	
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
			} catch (Exception e) {
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
			
			String urlconnect = "http://secure.criptext.com/push/subscribe";
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
			
			String urlconnect = "http://secure.criptext.com/group/create";
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
			
			String urlconnect = "http://secure.criptext.com/group/delete";
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
			
			String urlconnect = "http://secure.criptext.com/group/addmember";
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
			
			String urlconnect = "http://secure.criptext.com/group/info";
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

	public void sendMessage(String idnegative, String elmensaje, String sessionIDFrom, String sessionIDTo, String pushMessage,JSONObject params, JSONObject props){

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

				if(asynConnSocket.isConnected()){
					System.out.println("MONKEY - Enviando mensaje:"+json.toString());
					asynConnSocket.sendMessage(json);
				}	
				else
					System.out.println("MONKEY - no pudo enviar mensaje - socket desconectado");

			} catch (Exception e) {
				e.printStackTrace();
			}				            
		}
	}

	/**
	 * Envia una notificación.
	 * @param sessionIDFrom
	 * @param sessionIDTo
	 * @param paramsObject
	 */
	public void sendNotification(final String sessionIDFrom, final String sessionIDTo, JSONObject paramsObject){


		try {

			JSONObject args = new JSONObject();
			JSONObject json=new JSONObject();

			args.put("sid",sessionIDFrom);
			args.put("rid",sessionIDTo);
			args.put("params", paramsObject.toString());
			args.put("type", MessageTypes.MOKNotif);
			args.put("msg", "");

			json.put("args", args);

			json.put("cmd", MessageTypes.MOKProtocolMessage);


			if(asynConnSocket.isConnected()){
				System.out.println("MONKEY - Enviando mensaje:"+json.toString());
				asynConnSocket.sendMessage(json);
			}
			else
				System.out.println("MONKEY - no pudo enviar mensaje - socket desconectado");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public void sendFileMessage(final String idnegative, String elmensaje, final String sessionIDFrom, final String sessionIDTo, String file_type, String eph, String pushMessage){

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
				
				args.put("sid",sessionIDFrom);					
				args.put("rid",sessionIDTo);
				args.put("props",propsMessage);
				args.put("id",idnegative);

				Map<String, Object> params = new HashMap<String, Object>();
				params.put("data", args.toString());
				byte[] finalData=IOUtils.toByteArray(new FileInputStream(elmensaje));
				
				//COMPRIMIMOS CON GZIP
				Compressor compressor = new Compressor();
				finalData = compressor.gzipCompress(finalData);
				
				//ENCRIPTAMOS
				finalData=aesutil.encrypt(finalData);
				
				params.put("file", finalData);

				aq.auth(handle).ajax("http://secure.criptext.com/file/new", params, JSONObject.class, new AjaxCallback<JSONObject>() {
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

			} catch (Exception e) {
				e.printStackTrace();
			}				            
		}
	}

	public void sendGet(String since){

		try {

			JSONObject args=new JSONObject();
			JSONObject json=new JSONObject();

			args.put("messages_since",since);
			if(since == null || since.equals("0"))
				args.put("groups", 1);
			args.put("delay", ""+secondsDelay);
			args.put("blocks", ""+portionsMessages);
			//args.put("G", requestGroups ? 1 : 0);
			json.put("args", args);
			json.put("cmd", MessageTypes.MOKProtocolGet);

			if(asynConnSocket != null && asynConnSocket.isConnected()){
				System.out.println("MONKEY - Enviando Get:"+json.toString());
				asynConnSocket.sendMessage(json);
			}
			else
				System.out.println("MONKEY - no pudo enviar Get - socket desconectado");

		} catch (Exception e) {
			e.printStackTrace();
		}

		lastMessageId=since;
	}
	public void sendSet(String online){

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

		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public void sendClose(String sessionid){

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

		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public void sendDelete(String sessionid, String messageid){

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

		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	public boolean monkeyIsConnected(){
		if(asynConnSocket != null)
			return asynConnSocket.isConnected();
		return false;
	}

}
