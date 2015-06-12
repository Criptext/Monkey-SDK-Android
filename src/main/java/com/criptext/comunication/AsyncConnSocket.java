package com.criptext.comunication;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.criptext.lib.CriptextLib;
import com.criptext.socket.DarkStarClient;
import com.criptext.socket.DarkStarListener;
import com.criptext.socket.DarkStarSocketClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AsyncConnSocket extends AsyncTask<Void, Void, Void> implements ComServerDelegate{

	static Object syncObject = new Object();
	private static Context context;	 
	private String sessionId;
	private String urlPassword;
	private Void response;
	public DarkStarClient socketClient;
	public Thread threadservices;
	protected ComServerListener userServerListener;

	boolean play=true;
	boolean start=false;

	public boolean desconexionVerdadera=false;
	public boolean isInitiated=false;
	public static boolean _isConnected = false;
	public static boolean isreadorwrite=false;	
	public static boolean reconnecting=false;
	public static int isdisconectfinal=-1;

	public Handler mainMessageHandler;
	public Handler socketMessageHandler;

	public AsyncConnSocket(Context context, String sessionId, String urlPassword, Handler mainMessageHandler) {
		AsyncConnSocket.context=context;
		this.sessionId=sessionId;
		this.urlPassword=urlPassword;
		this.mainMessageHandler=mainMessageHandler;
	}

	@Override
	protected void onCancelled() {
		isInitiated=false;
		super.onCancelled();
	}

	@Override
	public void promptAction(String action) {}

	@Override
	protected void onPostExecute(Void result) {
		isInitiated=false;
		super.onPostExecute(result);
	}

	@SuppressLint("NewApi") 
	public void fireInTheHole(Void... params){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			this.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,params);
		else
			this.execute(params);
	}

	public void stateWriteorRead(boolean flag){
		if(_isConnected){
			if(flag)
				isreadorwrite=true;
			else
				isreadorwrite=false;
		}
	}

	private boolean checkInternetConnection(){
		ConnectivityManager localConnectivityManager = (ConnectivityManager)context.getSystemService("connectivity");
		return (localConnectivityManager.getActiveNetworkInfo() != null) && (localConnectivityManager.getActiveNetworkInfo().isAvailable()) && (localConnectivityManager.getActiveNetworkInfo().isConnected());
	}

	public void initConnection(){

		isdisconectfinal=0;
		play=true;
		while(play){
			try {        		
				start=false;
				if(checkInternetConnection()){
					play=false;
					conexionRecursiva();
				}else{
					play=true;
					Thread.sleep(100);
				}

			} catch (Exception e) {
				play=false;
				e.printStackTrace();
			}  
		} 	

	}

	public void conexionRecursiva(){

		if(desconexionVerdadera==false){

			reconnecting=true;
			userServerListener=new ComServerListener((ComServerDelegate) this);//central.criptext.com
			socketClient = new DarkStarSocketClient("secure.criptext.com",1139,(DarkStarListener)userServerListener);
			boolean active=true;

			while(active){
				try {
					socketClient.connect();
					this.socketClient.login(this.sessionId, urlPassword);
					active=false;				     
				} catch (Exception e) {	
					active=true;					
				}				
			}
		}
	}

	public void set_WriteOrRead(boolean flag){
		AsyncConnSocket.isreadorwrite=flag;
	}

	public boolean isConnected(){
		return _isConnected;
	}

	@Override
	public void handleMessage(ComMessage message) {
		this.parseMessage(message);
	}

	public void parseMessage(ComMessage socketMessage) {
		if(socketMessage!=null){
			int cmd = socketMessage.getMessageCmd();			
			_isConnected = true;
			JsonObject args = socketMessage.getArgs().getAsJsonObject();

			if(cmd == ComMessageProtocol.MESSAGE_LIST){
				JsonArray array = args.get("messages").getAsJsonArray();
				for(int i=0; i<array.size(); i++){
					JsonElement jsonMessage = array.get(i);
					JsonObject currentMessage = jsonMessage.getAsJsonObject();
					buildMessage(MessageTypes.MOKProtocolMessage, currentMessage);
				}
			}
			else{
				buildMessage(cmd, args);
			} 
		}
		else{
			//LLEGARON MUCHOS MENSAJES POR ENDE LLAMO UPDATES CON VALORES MENORES
			CriptextLib.instance().secondsDelay++;
			CriptextLib.instance().portionsMessages--;
			if(CriptextLib.instance().secondsDelay>5)
				CriptextLib.instance().secondsDelay=5;
			if(CriptextLib.instance().portionsMessages<10)
				CriptextLib.instance().portionsMessages=10;
			CriptextLib.instance().sendGet(CriptextLib.instance().lastMessageId);
		}
	}

	public MOKMessage buildMessage(int cmd, JsonObject args){

		MOKMessage remote = null;
		JsonParser parser = new JsonParser();
		JsonObject params = new JsonObject();
		if(!parser.parse(args.get("params").getAsString()).isJsonNull())
			params=(JsonObject)parser.parse(args.get("params").getAsString());

		switch (cmd) {
		case MessageTypes.MOKProtocolMessage:{

			if(args.get("type").getAsString().compareTo(MessageTypes.MOKText)==0
				|| args.get("type").getAsString().compareTo(MessageTypes.MOKFile)==0){
				remote=new MOKMessage(args.get("id").getAsString(), 
						args.get("sid").getAsString(), 
						args.get("rid").getAsString(),
						args.get("msg").getAsString(),
						args.get("datetime").getAsString(), 
						args.get("type").getAsString(),params);
				Message msg = mainMessageHandler.obtainMessage();
				msg.what=MessageTypes.MOKProtocolMessage;
				msg.obj =remote;
				mainMessageHandler.sendMessage(msg);	
			}
			else if(args.get("type").getAsString().compareTo(MessageTypes.MOKTempNote)==0){
				remote=new MOKMessage("",args.get("sid").getAsString(), 
						args.get("rid").getAsString(),"",
						args.get("datetime").getAsString(), 
						args.get("type").getAsString(),params);
				Message msg = mainMessageHandler.obtainMessage();			      
				msg.what=MessageTypes.MOKProtocolMessage;
				msg.obj =remote;
				mainMessageHandler.sendMessage(msg);
			}
			else if(args.get("type").getAsString().compareTo(MessageTypes.MOKNotif)==0){
				remote=new MOKMessage(args.get("id").getAsString(), 
						args.get("sid").getAsString(), 
						args.get("rid").getAsString(),
						args.get("msg").getAsString(),
						args.get("datetime").getAsString(), 
						args.get("type").getAsString(),params);
				Message msg = mainMessageHandler.obtainMessage();
				msg.what=MessageTypes.MOKProtocolMessage;
				msg.obj =remote;
				mainMessageHandler.sendMessage(msg);
			}
			else{
				System.out.println("MONKEY - Tipo "+args.get("type").getAsString()+" no soportado");
			}
			
			break;
		}		
		case MessageTypes.MOKProtocolOpen:{
			if(args.get("type").getAsString().compareTo(MessageTypes.MOKOpen)==0){
				remote=new MOKMessage(args.get("id").getAsString(),
						args.get("sid").getAsString(),args.get("rid").getAsString(),"",
						args.get("datetime").getAsString(), 
						"",params);
				Message msg = mainMessageHandler.obtainMessage();			      
				msg.what=MessageTypes.MOKProtocolOpen;
				msg.obj =remote;
				mainMessageHandler.sendMessage(msg);
			}
			break;
		}
		case MessageTypes.MOKProtocolAck:{			
			if(args.get("type").getAsString().compareTo("50")==0
				|| args.get("type").getAsString().compareTo("51")==0
					|| args.get("type").getAsString().compareTo("52")==0){
				remote=new MOKMessage(params.get("new_id").getAsString(), //En el atributo message_id va el nuevo id
						args.get("sid").getAsString(),args.get("rid").getAsString(),
						params.get("old_id").getAsString(), //En el atributo msg mando en el old_id
						args.get("datetime").getAsString(), 
						args.get("type").getAsString(),params);
				Message msg = mainMessageHandler.obtainMessage();			      
				msg.what=MessageTypes.MOKProtocolAck;
				msg.obj =remote;
				mainMessageHandler.sendMessage(msg);
			}
			else if(args.get("type").getAsString().compareTo(MessageTypes.MOKOpen)==0){
				remote=new MOKMessage(args.get("id").getAsString(),
						args.get("sid").getAsString(),args.get("rid").getAsString(),"",
						args.get("datetime").getAsString(), 
						args.get("type").getAsString(),params);
				Message msg = mainMessageHandler.obtainMessage();			      
				msg.what=MessageTypes.MOKProtocolAck;
				msg.obj =remote;
				mainMessageHandler.sendMessage(msg);
			}
			break;
		}
		case MessageTypes.MOKProtocolDelete:{
			remote=new MOKMessage("",
					args.get("sid").getAsString(),args.get("rid").getAsString(),
					"",args.get("datetime").getAsString(), 
					args.get("type").getAsString(),params);
			Message msg = mainMessageHandler.obtainMessage();			      
			msg.what=MessageTypes.MOKProtocolDelete;
			msg.obj =remote;
			mainMessageHandler.sendMessage(msg);
			break;
		}
		default:
			break;
		}

		return remote;
	}

	@Override
	public void handleEvent(short evid) {

		if(evid==ComMessageProtocol.LOGGINMSG_ID){
			reconnecting=false;
			_isConnected=true;
			System.out.println("Conectado al Socket!");
			Message msg = mainMessageHandler.obtainMessage();			      
			msg.what=MessageTypes.MessageSocketConnected;
			mainMessageHandler.sendMessage(msg);
		}
		else if(evid==ComMessageProtocol.FAILLOGGINMSG){
			//REMOTE LOGOUT DESDE EL SOCKET
			_isConnected=false;
			desconexionVerdadera=true;
			reconnecting=false;
			System.out.println("Desconectado del Socket!!");
			Message msg = mainMessageHandler.obtainMessage();			      
			msg.what=MessageTypes.MessageSocketDisconnected;
			mainMessageHandler.sendMessage(msg);
		}else if(evid==ComMessageProtocol.FAILLOGGINMSGDISCONECT){
			//REMOTE LOGOUT DESDE EL SOCKET
			_isConnected=false;
			disconnected();
			reconnecting=false;
			System.out.println("Desconectado del Socket!");
			Message msg = mainMessageHandler.obtainMessage();			      
			msg.what=MessageTypes.MessageSocketDisconnected;
			mainMessageHandler.sendMessage(msg);
		}

	}

	@Override
	public void disconnected(){
		_isConnected=false;
		if(!desconexionVerdadera)
			initConnection();
		else{
			isdisconectfinal=1;
		}

	}

	@SuppressLint("HandlerLeak") 
	@Override
	protected Void doInBackground(Void... params) {

		isInitiated=true;
		initConnection();

		Looper.prepare();
		socketMessageHandler = new Handler() {

			public void handleMessage(Message msg) {

				try {  								

					Thread.sleep(100);

					if(msg.obj.toString().compareTo("desconectar")==0){
						if(isConnected()){
							desconexionVerdadera=true;
							socketClient.logout(true);
						}
					}
					if(msg.obj.toString().compareTo("logout")==0){
						if(isConnected()){	                		
							desconexionVerdadera=true;
							socketClient.sendToSession(MessageManager.encodeString("{\"cmd\":\"80\",\"args\":{}}"));
							socketClient.logout(false);
						}
					}
					else if(msg.obj.toString().compareTo("desconectarpull")==0){
						if(isConnected()){	                		
							desconexionVerdadera=false;
							socketClient.logout(true);
						}
					}
					else if(msg.obj.toString().compareTo("conectar")==0){

						if(!isConnected()){
							initConnection();
						}	

					}
					else{

						if(isConnected()){
							System.out.println("Socket - Sending Message: "+msg.obj.toString());
							socketClient.sendToSession(MessageManager.encodeString(msg.obj.toString()));	 
						}
						else{
							System.out.println("NO se pudo enviar mensaje Socket desconectado");
						}

					}  					

				}
				catch(Exception ex){
					ex.printStackTrace();
				}
			}
		};

		Looper.loop();

		return response;
	}

	/*****************************/
	/****FUNCIONES DEL SOCKET*****/
	/*****************************/

	public void sendMessage(JSONObject params){
		try{
			if(socketMessageHandler!=null){
				Message msg = socketMessageHandler.obtainMessage();
				msg.obj =params.toString();
				System.out.println("MONKEY - AsyncConnSocket - enviando mensaje");
				socketMessageHandler.sendMessage(msg);
			}
			else
				System.out.println("MONKEY - AsyncConnSocket - socketMessageHandler es null");
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void disconectSocket(){
		try {
			if(isInitiated){
				desconexionVerdadera=true;
				if(socketMessageHandler!=null){
					Message msg = socketMessageHandler.obtainMessage();			      
					msg.obj ="desconectar";
					socketMessageHandler.sendMessage(msg);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void conectSocket(){
		try {
			if(!isConnected()){
				if(socketMessageHandler==null){
					System.out.println("MONKEY - mandaron a conectar pero no esta inicializado el socketMessageHandler");
					fireInTheHole();
				}
				else{
					System.out.println("MONKEY - mandaron a conectar y SI esta inicializado el socketMessageHandler");
					Message msg = socketMessageHandler.obtainMessage();			      
					msg.obj ="conectar";
					socketMessageHandler.sendMessage(msg);	
				}				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}	     
	}

	public void sendLogout(){
		try {
			if(isInitiated){
				desconexionVerdadera=true;
				Message msg = socketMessageHandler.obtainMessage();			      
				msg.obj ="logout";
				socketMessageHandler.sendMessage(msg);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendDisconectFromPull(){
		try {
			if(isInitiated){
				Message msg = socketMessageHandler.obtainMessage();			      
				msg.obj ="desconectarpull";
				socketMessageHandler.sendMessage(msg);
			}
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

}

