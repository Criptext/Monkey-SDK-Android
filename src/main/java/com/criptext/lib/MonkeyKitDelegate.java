package com.criptext.lib;

import com.criptext.comunication.MOKMessage;
import com.google.gson.JsonObject;
import java.util.ArrayList;

public interface MonkeyKitDelegate {

	void onConnectOK(String sessionID, String lastMessageID);
	void onConnectError(String errmsg);
	
	void onSocketConnected();
	void onSocketDisconnected();
	
	void onOpenConversationOK(String sessionID);
	void onOpenConversationError(String errmsg);
	
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
