package com.criptext.comunication;

/**
 * @author ***
 */
public class MessageTypes {
	// messages types
	public static  String MESSAGE_TYPE_CHAT = "";
	public static  String MESSAGE_TYPE_FILE = "file";
	public static  String MESSAGE_TYPE_FRIEND = "friend";
	public static  String MESSAGE_TYPE_TYPING = "typing";
	public static  String MESSAGE_TYPE_STATUS_MESSAGE = "status_message";
	public static  String MESSAGE_TYPE_AVATAR = "avatar";
	public static  String MESSAGE_TYPE_GROUP_ADD = "group_add";
	public static  String MESSAGE_TYPE_GROUP_REMOVE = "group_remove";
	public static  String MESSAGE_TYPE_GROUP_UPDATE = "group_update";
	public static  String MESSAGE_TYPE_GROUP_DELETE = "group_delete";
	public static  String MESSAGE_TYPE_GROUP_MESSAGE = "group_message";
	public static  String MESSAGE_TYPE_INVITE_ACCEPT = "invite_accept";
	public static  String MESSAGE_TYPE_INVITE_DENY = "invite_deny";
	public static  String MESSAGE_TYPE_INVITE_CANCEL = "invite_cancel";
	public static  String MESSAGE_TYPE_FRIEND_DELETE = "friend_delete";
	public static  String MESSAGE_TYPE_SHARE_FRIEND = "share_friend";
	public static  String MESSAGE_TYPE_CONVERSATION_OPEN = "conversation_open";
	public static  String MESSAGE_TYPE_CONVERSATION_CLOSE = "conversation_close";

	public static final String FILE_TYPE_AUDIO = "audio";
	public static final String FILE_TYPE_PHOTO = "photo";

	public static final int MessageSocketDisconnected = -2;
	public static final int MessageSocketConnected = -1;
	public static final int blMessageDefault = 0;
	public static final int blMessageFriendRequest = 1;
	public static final int blMessageTyping = 2;
	public static final int blMessageStatus = 3;
	public static final int blMessageAvatar = 4;
	public static final int blMessageGroupAdd = 5;
	public static final int blMessageGroupRemove = 6;
	public static final int blMessageGroupUpdate = 7;
	public static final int blMessageGroupDelete = 8;
	public static final int blMessageGroupMessage = 9;
	public static final int blMessageInviteAccepted = 10;
	public static final int blMessageInviteDenied = 11;
	public static final int blMessageInviteCanceled = 12;
	public static final int blMessageDeleteFriend = 13;
	public static final int blMessageShareAFriend = 14;
	public static final int blMessageConversationOpen = 15;
	public static final int blMessageConversationClose = 16;
	public static final int blMessagePhotoAttach = 18;
	public static final int blMessageAudio = 19;
	public static final int blMessageUntyping=20;
	public static final int blMessageOffline=47;
	public static final int blMessageOnline=48;
	public static final int EmailINbox=23;

	public static final int SuscribeToChannels=25;
	public static final int SuscribeToChannel=26;
	public static final int MessagesUpdates=27;
	public static final int EmailUpdates=28;
	public static final int MessagesUserOffline=29;

	public static final int blMessageReSend=30;
	public static final int blMessageReciveInvite=31;
	public static final int blMessageAcceptedInvite=32;
	public static final int blMessageMailOpened=33;
	public static final int blMessageElimineInvite=34;
	public static final int blMessageFriendDirect=36;
	public static final int OldBroadcastMessage=35;
	public static final int blMessageFriendActive=37;
	public static final int MessageremoteLogout=38;
	public static final int BroadcastMessage=39;
	public static final int MessageGroupRecall=41;
	public static final int MessageGroupCreate=42;
	public static final int NewGroupMemberNotification=57;

	public static final int MessageGroupRemoveMember=43;
	public static final int MessageRecall=44;

	public static final int blMessageDelivered=51;
	public static final int blMessageNotDelivered=50;
	public static final int blMessageNotView=52;
	public static final int blMessageNotSended=53;
	public static final int blMessageAudioAttachNew=54;
	public static final int blMessageScreenCapture=60;
	public static final int blMessageAlert=45;
	public static final int blMessageUserGroupsUpdate=46;

	//COMANDOS
	public static final int MOKProtocolMessage = 200;
	public static final int MOKProtocolGet = 201;
	public static final int MOKProtocolTransaction = 202;
	public static final int MOKProtocolOpen = 203;
	public static final int MOKProtocolSet = 204;
	public static final int MOKProtocolAck = 205;
	public static final int MOKProtocolDelete = 207;
	public static final int MOKProtocolClose = 208;

	//TIPOS DE MENSAJES
	public static final String MOKText = "1";
	public static final String MOKFile = "2";
	public static final String MOKTempNote = "3";
	public static final String MOKNotif = "4";
	public static final String MOKAlert = "5";
	public static final String MOKOpen = "203";

	//MONKEY_TYPES
	public static final int MOKGroupCreate = 1;
	public static final int MOKGroupDelete = 2;
	public static final int MOKGroupNewMember = 3;
	public static final int MOKGroupRemoveMember = 4;
	
}
