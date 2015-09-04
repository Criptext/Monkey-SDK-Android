package com.criptext.comunication;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
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

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.SocketHandler;

public class AsyncConnSocket implements ComServerDelegate{


	private String sessionId;
	private String urlPassword;
	public DarkStarClient socketClient;
	protected ComServerListener userServerListener;
	private HandlerThread handlerThread;
	private Void response;

	public static int isdisconectfinal=-1;

	public enum Status {sinIniciar, conectado, reconectando, desconectado};
	private Status socketStatus;
	public Handler mainMessageHandler;
	public Handler socketMessageHandler;
	private Runnable lastAction = null;
	//TIMEOUT
	private Timer longTimer;
	private int retries;
	private final int timeout = 2000;

	public AsyncConnSocket(String sessionId, String urlPassword, Handler mainMessageHandler) {
		this.sessionId=sessionId;
		this.urlPassword=urlPassword;
		this.mainMessageHandler=mainMessageHandler;
		socketStatus = Status.sinIniciar;
		this.retries = 0;
	}

	public AsyncConnSocket(String sessionId, String urlPassword, Handler mainMessageHandler, Runnable r) {
		this.sessionId=sessionId;
		this.urlPassword=urlPassword;
		this.mainMessageHandler=mainMessageHandler;
		this.lastAction = r;
		socketStatus = Status.sinIniciar;
		this.retries = 0;
	}


	@Override
	public void promptAction(String action) {}


	public void fireInTheHole(){
		if(socketStatus != Status.conectado)
			initConnection();
		if(handlerThread != null){
			handlerThread.quit();
		}
		handlerThread = new HandlerThread("AsyncConnSocketThread");
		handlerThread.start();
		socketMessageHandler = new Handler(handlerThread.getLooper()) {

			public void handleMessage(Message msg) {

				try {
					Thread.sleep(100);
					if(msg.obj.toString().compareTo("desconectar")==0){
						if(isConnected()){
							socketStatus = Status.desconectado;
							socketClient.logout(true);
							this.getLooper().quit();
						}
					}
					if(msg.obj.toString().compareTo("logout")==0){
						if(isConnected()){
							socketStatus = Status.desconectado;
							socketClient.sendToSession(MessageManager.encodeString("{\"cmd\":\"80\",\"args\":{}}"));
							socketClient.logout(false);
						}
					}
					else if(msg.obj.toString().compareTo("desconectarpull")==0){
						if(isConnected()){
							socketStatus = Status.conectado;
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
	}

	public void initConnection(){
    	System.out.println("INICIANDO CONEXION SOCKET");
		conexionRecursiva();

	}

	public void conexionRecursiva(){

		socketStatus = Status.reconectando;
		userServerListener=new ComServerListener((ComServerDelegate) this);//central.criptext.com
		socketClient = new DarkStarSocketClient("secure.criptext.com",1139,(DarkStarListener)userServerListener);
		retries = 0;
		Thread connThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {

					while (AsyncConnSocket.this.getSocketStatus() != Status.conectado) {
						System.out.println("RECONNECTING");
						socketClient.connect();
						AsyncConnSocket.this.socketClient.login(AsyncConnSocket.this.sessionId, urlPassword);

						Thread.sleep(timeout);
					}


					Handler handler = new Handler(Looper.getMainLooper());
					handler.post(new Runnable() {
						public void run() {
							System.out.println("SOCKET IS CONNECTED");
							fireInTheHole();
							if(lastAction != null)
								lastAction.run();

						}
					});


				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		connThread.start();


	}

	public boolean isConnected(){
		return socketStatus == Status.conectado;
	}

	@Override
	public void handleMessage(ComMessage message) {
		this.parseMessage(message);
	}

	public void parseMessage(ComMessage socketMessage) {
		if(socketMessage!=null){
			int cmd = socketMessage.getMessageCmd();
			if(socketStatus != Status.desconectado)
				socketStatus = Status.conectado;
			JsonObject args = socketMessage.getArgs().getAsJsonObject();

			/**if(cmd == ComMessageProtocol.MESSAGE_LIST){
				JsonArray array = args.get("messages").getAsJsonArray();
				for(int i=0; i<array.size(); i++){
					JsonElement jsonMessage = array.get(i);
					JsonObject currentMessage = jsonMessage.getAsJsonObject();
					buildMessage(MessageTypes.MOKProtocolMessage, currentMessage);
				}
			}
			else{*/
				System.out.println("parsing message: " + args.toString() + " cmd: " + cmd);
				buildMessage(cmd, args);
			//}
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
		if(args.has("params") && !parser.parse(args.get("params").getAsString()).isJsonNull())
			params=(JsonObject)parser.parse(args.get("params").getAsString());
		JsonObject props = new JsonObject();
		if(args.has("props") && !parser.parse(args.get("props").getAsString()).isJsonNull())
			props=(JsonObject)parser.parse(args.get("props").getAsString());
		switch (cmd) {
		case MessageTypes.MOKProtocolMessage:{

			if(args.get("type").getAsString().compareTo(MessageTypes.MOKText)==0
				|| args.get("type").getAsString().compareTo(MessageTypes.MOKFile)==0){
				remote=new MOKMessage(args.get("id").getAsString(), 
						args.get("sid").getAsString(), 
						args.get("rid").getAsString(),
						args.get("msg").getAsString(),
						args.get("datetime").getAsString(), 
						args.get("type").getAsString(),params,props);
				Message msg = mainMessageHandler.obtainMessage();
				msg.what=MessageTypes.MOKProtocolMessage;
				msg.obj =remote;
				mainMessageHandler.sendMessage(msg);	
			}
			else if(args.get("type").getAsString().compareTo(MessageTypes.MOKTempNote)==0){
				remote=new MOKMessage("",args.get("sid").getAsString(), 
						args.get("rid").getAsString(),"",
						args.get("datetime").getAsString(), 
						args.get("type").getAsString(), params, props);
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
						args.get("type").getAsString(), params, props);
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
						"", params, props);
				Message msg = mainMessageHandler.obtainMessage();			      
				msg.what=MessageTypes.MOKProtocolOpen;
				msg.obj =remote;
				mainMessageHandler.sendMessage(msg);
			}
			break;
		}
		case MessageTypes.MOKProtocolAck:{			
			if(args.get("type").getAsString().equals(MessageTypes.MOKText)
				|| args.get("type").getAsString().equals(MessageTypes.MOKNotif)){
				remote=new MOKMessage(props.get("new_id").getAsString(), //En el atributo message_id va el nuevo id
						args.get("sid").getAsString(),args.get("rid").getAsString(),
						props.get("old_id").getAsString(), //En el atributo msg mando en el old_id
						args.get("datetime").getAsString(), 
						args.get("type").getAsString(), params, props);
				Message msg = mainMessageHandler.obtainMessage();			      
				msg.what=MessageTypes.MOKProtocolAck;
				msg.obj =remote;
				mainMessageHandler.sendMessage(msg);
			}
			else if(args.get("type").getAsString().compareTo(MessageTypes.MOKOpen)==0){
				remote=new MOKMessage(args.get("id").getAsString(),
						args.get("sid").getAsString(),args.get("rid").getAsString(),"",
						args.get("datetime").getAsString(), 
						args.get("type").getAsString(), params, props);
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
					args.get("type").getAsString(), params, props);
			Message msg = mainMessageHandler.obtainMessage();			      
			msg.what=MessageTypes.MOKProtocolDelete;
			msg.obj =remote;
			mainMessageHandler.sendMessage(msg);
			break;
		}
			case MessageTypes.MOKProtocolGet:
				System.out.println("MOK PROTOCOL GET");
				if(args.get("type").getAsInt() == 1) {
					JsonArray array = args.get("messages").getAsJsonArray();
					for (int i = 0; i < array.size(); i++) {
						JsonElement jsonMessage = array.get(i);
						JsonObject currentMessage = jsonMessage.getAsJsonObject();
						buildMessage(MessageTypes.MOKProtocolMessage, currentMessage);
					}
				} else {
					//PARSE GROUPS UPDATES
					remote=new MOKMessage("","","",args.get("messages").getAsString(), "",
							args.get("type").getAsString(), params, props);
					remote.setMonkeyAction(MessageTypes.MOKGroupJoined);
					Message msg = mainMessageHandler.obtainMessage();
					msg.what=MessageTypes.MOKProtocolGet;
					msg.obj = remote;
					mainMessageHandler.sendMessage(msg);

				}
		default:
			break;
		}

		return remote;
	}

	@Override
	public void handleEvent(short evid) {

		if(evid==ComMessageProtocol.LOGGINMSG_ID){
			//Este es el mensaje que indica que ya estoy conectado
			socketStatus = Status.conectado;
			System.out.println("Conectado al Socket!");
			Message msg = mainMessageHandler.obtainMessage();			      
			msg.what=MessageTypes.MessageSocketConnected;
			mainMessageHandler.sendMessage(msg);
		}
		else if(evid==ComMessageProtocol.FAILLOGGINMSG){
			//REMOTE LOGOUT DESDE EL SOCKET
			socketStatus = Status.desconectado;
			System.out.println("Desconectado del Socket!!");
			Message msg = mainMessageHandler.obtainMessage();			      
			msg.what=MessageTypes.MessageSocketDisconnected;
			mainMessageHandler.sendMessage(msg);
		}else if(evid==ComMessageProtocol.FAILLOGGINMSGDISCONECT){
			//REMOTE LOGOUT DESDE EL SOCKET
			socketStatus = Status.desconectado;
			disconnected();
			System.out.println("Desconectado del Socket!");
			Message msg = mainMessageHandler.obtainMessage();			      
			msg.what=MessageTypes.MessageSocketDisconnected;
			mainMessageHandler.sendMessage(msg);
		}

	}

	@Override
	public void disconnected(){
		if(socketStatus != Status.desconectado)
			initConnection();

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
			else {
				System.out.println("MONKEY - AsyncConnSocket - socketMessageHandler es null");
				fireInTheHole();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void disconectSocket(){
		try {
			if(socketStatus != Status.desconectado){
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

	public void conectSocket(Runnable r){
		try {
			lastAction = r;
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
			if(socketStatus != Status.desconectado){
				socketStatus = Status.desconectado;
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
			if(socketStatus != Status.desconectado){
				Message msg = socketMessageHandler.obtainMessage();			      
				msg.obj ="desconectarpull";
				socketMessageHandler.sendMessage(msg);
			}
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	public Status getSocketStatus(){
		return socketStatus;
	}

	public void removeContext(){
		if(handlerThread != null)
			handlerThread.quit();

	}

}

