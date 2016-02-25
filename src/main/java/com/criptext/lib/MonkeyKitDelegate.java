package com.criptext.lib;

import com.criptext.comunication.MOKMessage;
import com.google.gson.JsonObject;
import java.util.ArrayList;

public interface MonkeyKitDelegate {

	/**
	 * Cuando MonkeyKit se conecta, realiza varios requerimientos HTTP antes de conectar el socket.
	 * Si alguno de estos falla por errores de conexion, MonkeyKit mostrara la excepcion al desarrollador
	 * a traves de este callback. MonkeyKit automaticamente tratara de reconectarse. Si tienes problemas
	 * de conexion, seria muy util ver las excepciones que se arrojan aqui y contactar soporte.
	 * @param exception La excepcion que se arrojo durante el error de conexion.
	 */
	void onNetworkError(Exception exception);

	/**
	 * Cuando MonkeyKit logra conectar el socket, y esta listo para enviar y recibir mensajes, ejecuta
	 * este callback. Este es un buen momento para hacer "sendSet" para decirle a los otros usuarios
	 * que estas online.
	 */
	void onSocketConnected();

	/**
	 * Periodicamente el socket de MonkeyKit se desconecta. MonkeyKit automaticamente se volvera a
	 * conectar, pero este es un buen momento para mostrarle al usuario que el socket se esta
	 * reconectando.
	 */
	void onSocketDisconnected();
	
	void onCreateGroupOK(String grupoID);
	void onCreateGroupError(String errmsg);

	void onDeleteGroupOK(String grupoID);
	void onDeleteGroupError(String errmsg);

	void onAddMemberToGroupOK();
	void onAddMemberToGroupError(String errmsg);

	void onGetGroupInfoOK(JsonObject json);
	void onGetGroupInfoError(String errmsg);
	
	void onMessageRecieved(MOKMessage message);
	void onAcknowledgeRecieved(MOKMessage message);
	void onDeleteRecieved(MOKMessage message);

	void onContactOpenMyConversation(String sessionID);
	void onNotificationReceived(MOKMessage message);

	void onMessageBatchReady(ArrayList<MOKMessage> messages);
}
