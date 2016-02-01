package com.criptext.lib;

import org.json.JSONObject;

import com.criptext.comunication.MOKMessage;
import com.criptext.database.RemoteMessage;

import java.util.ArrayList;

public interface CriptextLibDelegate {

	public void onSessionOK();
	public void onSessionError(String errmsg);
	
	public void onConnectOK(String sessionID, String lastMessageID);
	public void onConnectError(String errmsg);
	
	public void onSocketConnected();
	public void onSocketDisconnected();
	
	public void onGetOK();
	
	public void onOpenConversationOK(String sessionID);
	public void onOpenConversationError(String errmsg);
	
	public void onCreateGroupOK(String grupoID);
	public void onCreateGroupError(String errmsg);
	
	public void onDeleteGroupOK(String grupoID);
	public void onDeleteGroupError(String errmsg);
	
	public void onAddMemberToGroupOK();
	public void onAddMemberToGroupError(String errmsg);
	
	public void onGetGroupInfoOK(JSONObject json);
	public void onGetGroupInfoError(String errmsg);
	
	public void onMessageRecieved(MOKMessage message);
	public void onAcknowledgeRecieved(MOKMessage message);
	public void onDeleteRecieved(MOKMessage message);
    public void onMessageSaved(RemoteMessage remote);
	
	public void onContactOpenMyConversation(String sessionID);
	public void onNotificationReceived(MOKMessage message);

	public void onMessageBatchReady(ArrayList<MOKMessage> messages);
}
