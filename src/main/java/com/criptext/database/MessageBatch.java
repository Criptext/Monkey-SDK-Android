package com.criptext.database;

import com.criptext.comunication.MOKMessage;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by gesuwall on 1/27/16.
 */
public class MessageBatch {
    private int messageCount;
    private ArrayList<MOKMessage> messages;
    private LinkedList<String> pendingKeys;

    /**
     * Agrega un mensaje a la lista. Si el session id del remitente del mensaje esta en pendingKeys
     * se quita ese session de la lista de pendingKeys.
     * @param message Mensaje a agrega. Tiene que estar desencriptado.
     */
    public void addMessage(MOKMessage message){
       messages.add(message);

        if(!pendingKeys.isEmpty()){
            messages.remove(message.getSid());
        }

        if(messages.size() == messageCount){
            onBatchReady(messages);
            messages.clear();
            pendingKeys.clear();
        }

    }

    public void addPendingKey(String session){
        pendingKeys.add(session);
    }

    public MessageBatch(int maxSize){
        messageCount = maxSize;
        messages = new ArrayList<MOKMessage>();
        pendingKeys = new LinkedList<String>();
    }

    public void onBatchReady(ArrayList<MOKMessage> readymessages){

    }

    public void increaseBatchSize(int size){
        messageCount += size;
    }


}
