package com.criptext.database;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.criptext.comunication.MOKMessage;
import com.criptext.comunication.MessageTypes;
import com.criptext.lib.CriptextLib;
import com.criptext.lib.R;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * Created by daniel on 12/28/15.
 */
public class CriptextDBHandler {

    /**
     * Saca todas las fotos de una conversacion
     * @param id id de la conversacion cuya fotos se van a sacar
     * @return una lista de strings con el nombre del  archivo en el app cache
     */
    public static ArrayList<String> getConversationPhotos(Context context, String id){
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        RealmResults<MessageModel> myMessages;
        if(id.startsWith("G:")){
            //SI ESTOY SACANDO MENSAJES DE UN GRUPO
            myMessages = realm.where(MessageModel.class).equalTo("_type", "" + MessageTypes.blMessagePhoto)
                    .equalTo("_message_text", "0")
                    .beginGroup()
                    .equalTo("_uid_recive", id)
                    .or()
                    .equalTo("_uid_sent", id)
                    .endGroup()
                    .findAll();
        } else{
            //SI ESTOY SACANDO MENSAJES DE CONVERSACIONES NORMALES
            myMessages = realm.where(MessageModel.class).equalTo("_type", "" + MessageTypes.blMessagePhoto)
                    .equalTo("_message_text", "0")
                    .beginGroup()
                    .beginGroup()
                    .contains("_uid_recive", id).not().beginsWith("_uid_sent", "G:", Case.SENSITIVE)
                    .endGroup()
                    .or()
                    .beginGroup()
                    .contains("_uid_sent", id).not().beginsWith("_uid_recive", "G:", Case.SENSITIVE)
                    .endGroup()
                    .endGroup().findAll();

        }

        ArrayList<String> paths = new ArrayList<>();
        for(MessageModel message : myMessages) {
            paths.add(Uri.fromFile(new File(context.getCacheDir().getAbsolutePath()
                    + "/" + message.get_message())).toString());
        }

        return paths;
    }

    /**
     * Obtiene n (size) cantidad de mensajes de la conversacion
     * @param id identificador de la conversacion
     * @param size cantidad de mensajes
     * @param offset donde me quede la ulitma vez que llame este metodo?
     * @return lista con mensajes a mostrar en la conversacion.
     */

    public static LinkedList<RemoteMessage> getTopMessages(String id, int size, int offset){
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        RealmResults<MessageModel> myMessages;
        int messageCount;
        if(id.startsWith("G:")){
            //SI ESTOY SACANDO MENSAJES DE UN GRUPO
            myMessages = realm.where(MessageModel.class).equalTo("_uid_recive", id).or().equalTo("_uid_sent", id).findAll();
        } else{
            //SI ESTOY SACANDO MENSAJES DE CONVERSACIONES NORMALES
            myMessages = realm.where(MessageModel.class).beginGroup()
                    .equalTo("_uid_recive", id).not().beginsWith("_uid_sent", "G:", Case.SENSITIVE)
                    .endGroup()
                    .or()
                    .beginGroup()
                    .contains("_uid_sent", id).not().beginsWith("_uid_recive", "G:", Case.SENSITIVE)
                    .endGroup().findAll();

        }
        messageCount = myMessages.size();
        LinkedList<RemoteMessage> orderedmessages;


        orderedmessages = RemoteMessage.insertSortCopy(myMessages);
        int available = Math.min(messageCount - offset, size);
        System.out.println("CRIPTEXTDBHANDLER1 - THERE ARE " + available + " MESSAGES AVAILABLE. OFFSET = " + offset + " SIZE = "+size);
        return new LinkedList(orderedmessages.subList(messageCount - offset - available, messageCount - offset));

    }

    /**
     * Obtiene n (size) cantidad de mensajes de la conversacion. Este metodo abre una nueva
     * referencia de Realm encriptada para poder modificar el mensaje desde cualquier thread, por
     * lo tanto esta funcion NUNCA debe de ser llamada en el thread UI.
     * @param id identificador de la conversacion
     * @return lista con mensajes a mostrar en la conversacion.
     */
    public static RealmResults<MessageModel> getTopMessagesBG(String id){
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        final RealmResults<MessageModel> myMessages;
        int messageCount;
            if (id.startsWith("G:")) {
                //SI ESTOY SACANDO MENSAJES DE UN GRUPO
                myMessages = realm.where(MessageModel.class).equalTo("_uid_recive", id).or().equalTo("_uid_sent", id).findAllAsync();
            } else {
                //SI ESTOY SACANDO MENSAJES DE CONVERSACIONES NORMALES
                myMessages = realm.where(MessageModel.class).beginGroup()
                        .contains("_uid_recive", id).not().beginsWith("_uid_sent", "G:", Case.SENSITIVE)
                        .endGroup()
                        .or()
                        .beginGroup()
                        .contains("_uid_sent", id).not().beginsWith("_uid_recive", "G:", Case.SENSITIVE)
                        .endGroup().findAllAsync();

            }

        return myMessages;

    }

    /**
     * Borra un mensaje de la base, Si el mensaje ha sido enviado a varios destinatarios (Broadcast),
     * simplemente se borra en session id del destinatario al cual se le hace unsend.
     * @param id id del mensaje a borrar
     * @param receiver_id session id del destinatario al cual se le hace unsend. Si es null, quiere
     *                    decir que es un mensaje recibido, por lo cual debe de ser borrado
     *                    inmediatamente.
     */
    public static  String deleteMessage(String id, String receiver_id){
        Log.d("UpdateRemoteMessage", "deleteMessage");
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        MessageModel result = realm.where(MessageModel.class).equalTo("_message_id", id).findFirst();
        String path=null;

        if (result != null){
            if(receiver_id == null || result.get_uid_recive().equals(receiver_id)) {
                realm.beginTransaction();

                if(result.get_type().equals("" + MessageTypes.blMessageAudio) ||
                        result.get_type().equals("" + MessageTypes.blMessagePhoto)) //Si es audio o foto borrar el archivo
                    path=result.get_message();
                result.removeFromRealm();

                realm.commitTransaction();
            } else {
                realm.beginTransaction();

                String new_rid = deleteSessionFromRid(result.get_uid_recive(), receiver_id);

                result.set_uid_recive(new_rid);

                realm.commitTransaction();
            }
        }

        return path;
    }

    public static RemoteMessage getMessageBYid(String id){

        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        MessageModel result = realm.where(MessageModel.class).equalTo("_message_id", id).findFirst();
        RemoteMessage message = null;
        if(result != null){
            message = new RemoteMessage(result);
        }
        return message;

    }

    /**
     * Marca el estado de un mensaje en la base como leído.
     * @param id el id del mensaje a marcar como leído.
     */
    public static void updateMessageReadStatus(String id) {

        Log.d("UpdateRemoteMessage", "updateMessageReadStatus");
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        MessageModel result = realm.where(MessageModel.class).equalTo("_message_id", id).findFirst();
        realm.beginTransaction();
        if(result != null){
            result.set_status("leido");
        }
        realm.commitTransaction();

    }

/**
     * Marca el estado de un mensaje en la base como leído.
     * @param id el id del mensaje a marcar como leído.
     */
    public static void updateMessageReadStatusBG(String id) {

        Log.d("UpdateRemoteMessage", "updateMessageReadStatusBG");
        Realm realm = CriptextLib.instance().getNewMonkeyRealm();
        MessageModel result = realm.where(MessageModel.class).equalTo("_message_id", id).findFirst();
        realm.beginTransaction();
        if(result != null){
            result.set_status("leido");
        }
        realm.commitTransaction();
        realm.close();

    }
    /**
     * Actualiza el mensaje en Realm.
     * @param message El mensaje a actualizar. El contenido de este mensaje reemplaza por completo
     *                al que estaba anteriormente en la base de datos. Hay que tener cuidado porque
     *                puede darse el caso de que se pierda información si la base tiene información
     *                más reciente que la que entra como argumento.
     */
    public static void updateMessage(RemoteMessage message)
    {
        Log.d("UpdateRemoteMessage", "updateMessage");
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(message.getModel());
        realm.commitTransaction();
    }

    public static void deleteAllMessageFrom(String id) {
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        realm.beginTransaction();
        realm.where(MessageModel.class).equalTo("_uid_sent", id).findAll().clear();
        //result.removeFromRealm();
        realm.commitTransaction();

    }
    public static void deleteAllMessageTo(String id) {
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        realm.beginTransaction();
        realm.where(MessageModel.class).equalTo("_uid_recive", id).findAll().clear();
        //result.removeFromRealm();
        realm.commitTransaction();

    }

    /**
     * Borra todos los mensajes enviados por un usuario con cierto session id
     * @param id Session Id del usuario que envió los mensajes a borrar.
     */
    public static  void deleteAllMessagesFrom(String id) {
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        realm.beginTransaction();
        realm.where(MessageModel.class).equalTo("_uid_sent", id).findAll().clear();
        realm.commitTransaction();
    }

    /**
     * Borra todos los mensajes enviados a un usuario con cierto session id
     * @param id Session Id del usuario al que se le enviaron los mensajes a borrar.
     */
    public static  void deleteAllMessagesTo(String id) {
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        realm.beginTransaction();
        realm.where(MessageModel.class).equalTo("_uid_recive", id).findAll().clear();
        realm.commitTransaction();

    }

    public static int getTotalWithoutRead(String sessionid) {
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        RealmResults<MessageModel> results = realm.where(MessageModel.class).equalTo("_uid_sent", sessionid).equalTo("_status", "porabrir").findAll();
        return results.size();
    }

    public static void addMessage(RemoteMessage remote)
    {
        Log.d("UpdateRemoteMessage", "addMessage");
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        realm.beginTransaction();
        remote.printValues();
        realm.copyToRealmOrUpdate(remote.getModel());
        realm.commitTransaction();

    }

    public static void addMessageWrite(RemoteMessage remote)
    {
        addMessage(remote);
    }

    /**
    * Obtiene todos los mensajes de Realm que aún se están enviando.
    * @return lista con todos los mensajes que aún se están enviando.
    */
    public static ArrayList<RemoteMessage> getAllMessageSending()
    {
         Realm realm = CriptextLib.instance().getMonkeyKitRealm();
         RealmResults<MessageModel> mess = realm.where(MessageModel.class).equalTo("_status", "sending").findAll();
         ArrayList messages = new ArrayList<RemoteMessage>(RemoteMessage.insertSortCopy(mess)); //ARRAYLIST WTF!??
         return messages;
    }

    public static LinkedList<RemoteMessage> getAllMessageDeliveredById(String idfriend)
    {
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        RealmResults<MessageModel> mess = realm.where(MessageModel.class).equalTo("_uid_recive", idfriend).equalTo("_status", "entregado").findAll();
        LinkedList messages = RemoteMessage.insertSortCopy(mess);
        return messages;
    }

    public static boolean existMessage(String id) {

        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        MessageModel mess = realm.where(MessageModel.class).equalTo("_message_id", id).findFirst();
        boolean exists = mess == null ? false : true;
        return exists;
    }

    public static void updateMessageReciveThread(RemoteMessage message)
    {
        Log.d("UpdateRemoteMessage", "updateMessageReciveThread A");
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(message.getModel());
        realm.commitTransaction();
    }

    /**
     * Actualiza un mensaje en la base de datos. Este metodo abre una nueva referencia de Realm
     * encriptada para poder modificar el mensaje desde cualquier thread, por lo tanto esta funcion
     * NUNCA debe de ser llamada en el thread UI.
     * @param message el mensaje a modificar
     */
    public static void updateMessageReciveThreadBG(RemoteMessage message)
    {
        Log.d("UpdateRemoteMessage", "updateMessageReciveThreadBG");
        Realm realm = CriptextLib.instance().getNewMonkeyRealm();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(message.getModel());
        realm.commitTransaction();
        realm.close();
    }

    /**
     * Guarda un mensaje desencriptado en la base, una vez dentro le coloca el contenido encriptado.
     * @param message Mensaje a guardar
     * @param encrypted contenido del mensaje encriptado
     */
    public static void updateMessageReciveThread(RemoteMessage message, String encrypted)
    {
        Log.d("UpdateRemoteMessage", "updateMessageReciveThread B");
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        realm.beginTransaction();
        MessageModel decrypted = realm.copyToRealmOrUpdate(message.getModel());
        decrypted.set_message(encrypted);
        realm.commitTransaction();
    }

    /**
     * Toma una lista de mensajes y borra un session id en particular entre varios del Rid. Este
     * metodo deberia de llamarse despues de borrar todos los mensajes enviados al usuario 'id' que
     * no son broadcast.
     * @param id id a borrar
     */
    public static void removeSessionFromMessageList(String id){
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        realm.beginTransaction();
        RealmResults broadcastMessages = realm.where(MessageModel.class).contains("_uid_recive", id).findAll();
        if(broadcastMessages.size() > 0)
            for(int i = broadcastMessages.size() - 1;i > -1; i-- ){
                MessageModel broadcast = (MessageModel)broadcastMessages.get(i);
                String newRid = deleteSessionFromRid(broadcast.get_uid_recive(), id);
                broadcast.set_uid_recive(newRid);

            }
    }

    /**
     * borra un session id de un String que tiene varios session id's de destinatarios de un mensaje.
     * @param rid String con los session id's de todos los destinatarios de un mensaje
     * @param id_to_remove session id que debe de ser borrado
     * @return un String que es basicamente 'rid' sin 'id_to_remove'.
     */
    private static String deleteSessionFromRid(String rid, String id_to_remove){
        String[] array = rid.split(",");
        String new_rid = "";
        for(String s : array){
            if(!s.equals(id_to_remove))
                new_rid += "," + s;
        }

        return new_rid.length()>0?new_rid.substring(1):"";
    }


    /**
     * Borra todos los mensajes enviados por un usuario a otro
     * @param sender El usuario que envio los mensajes que van a ser borrados
     * @param reciever El usuario/grupo que recibio los mensajes que van a ser borrados
     */
    public static LinkedList<String> unsendRemoteMessages(String sender, String reciever){

        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        LinkedList<String> listaPaths;
        realm.beginTransaction();
        RealmResults<MessageModel> results = realm.where(MessageModel.class).equalTo("_uid_recive", reciever).equalTo("_uid_sent", sender).findAll();

        listaPaths=deleteMessagesList(results);

        realm.commitTransaction();

        return listaPaths;
    }

    /**
     * Borra una lista de mensajes de realm. Si el mensaje es una foto o Audio, adicionalmente
     * borra el archivo. Para llamar a esta funcion, debe estar en medio de un RealmTransaction.
     * @param deletedMessages lista con los mensajes sacados de Realm a ser borrado
     */
    private static LinkedList<String> deleteMessagesList(RealmResults<MessageModel> deletedMessages){
        LinkedList<String> listaPaths = new LinkedList<String>();
        for(int i = deletedMessages.size() - 1;i > -1; i-- ) {
            MessageModel deletedMsg = deletedMessages.get(i);

            if(deletedMsg.get_type().equals("" + MessageTypes.blMessageAudio) ||
                    deletedMsg.get_type().equals("" + MessageTypes.blMessagePhoto)) //Si es audio o foto borra el file
                listaPaths.add(deletedMsg.get_message());

            deletedMessages.get(i).removeFromRealm();
        }
        return listaPaths;
    }

    /**
     * Crea una nueva instancia de RemoteMessage en base a un MOKMessage recibido. Si el mensaje es
     * de legacy lo desencripta de la vieja forma. Marca el mensaje como 'por abrir'
     * @param message MOKMessage recien recibido de MonketKit
     * @param tipo el MonkeyTypeAction que dice que tipo de mensaje es
     * @param context Context el activity que lo ejecuta.
     * @return
     */
    public static RemoteMessage createIncomingRemoteMessage(MOKMessage message, int tipo, Context context){

        //VERIFICO SI ES UN MENSAJE DE GRUPO
        String sid=message.getRid().contains("G")?message.getRid():message.getSid();
        String rid=message.getRid().contains("G")?message.getSid():message.getRid();


        RemoteMessage remote = new RemoteMessage(message.getMessage_id(),sid,
                message.getMsg(),Long.parseLong(message.getDatetime()), "", "",""+tipo,rid);
        remote.set_datetimeorden(System.currentTimeMillis());
        remote.set_status("porabrir");
        if(remote.get_uid_sent().startsWith("legacy:") ||remote.get_uid_recive().startsWith("legacy:"))
            remote.set_message(RemoteMessage.desencrypt(message.getMsg(), context));
        remote.setDesencript(true);
        remote.set_message_text(message.getProps().get("eph").getAsString());

        switch(tipo){
            case MessageTypes.blMessageDocument:
                remote.set_message_text_old(message.getProps().get("file_name").getAsString()+":"+message.getProps().get("ext").getAsString());
                break;
            case MessageTypes.blMessageShareAFriend:
                remote.setParams(message.getParams().toString());
                break;
            case MessageTypes.blMessagePhoto: case MessageTypes.blMessageAudio:
                remote.setProps("" + message.getProps());
                remote.setParams("" + message.getParams());
                break;
            case MessageTypes.blMessageScreenCapture:
                break;
        }

        return remote;
    }

    /**
     * Retorna el tipo de accion de un mensaje de MonkeyKit. La acción puede estar en props o en params.
     * props tiene prioridad, si no encuentra el action ahi, lo busca en params. Después se mapean los tipos
     * de MonkeyKit con los valores de Criptext
     * @param message el mensaje recibido desde MonkeyKit
     * @return el tipo de acción del mensaje
     */
    public static int getMonkeyActionType(MOKMessage message){
        int tipo=0;
        if(message.getProps() != null) {
            if (message.getProps().has("type"))
                tipo = Integer.parseInt(message.getProps().get("type").getAsString());
            if (message.getProps().has("file_type"))
                tipo = Integer.parseInt(message.getProps().get("file_type").getAsString());
            if (message.getProps().has("monkey_action"))
                tipo = message.getProps().get("monkey_action").getAsInt();
        }
        //El tipo de shareContat se encuentra en params
        if(message.getParams() != null && message.getParams().has("type")){
            return message.getParams().get("type").getAsInt();//Puede ser blMessageShareAFriend, blMessageChangeAvatar, etc
        }
        System.out.println("msj de tipo: " + message.getMonkeyAction());
        switch(message.getMonkeyAction()) {
            case com.criptext.comunication.MessageTypes.MOKGroupCreate: //MOKGroupCreate
                return MessageTypes.blMessageGroupAdded;
            case 2: //
                break;
            case com.criptext.comunication.MessageTypes.MOKGroupNewMember:
                return MessageTypes.blMessageGroupNewMember;
            case com.criptext.comunication.MessageTypes.MOKGroupRemoveMember:
                return MessageTypes.blMessageGroupRemovedMember;
            case com.criptext.comunication.MessageTypes.MOKGroupJoined:
                return MessageTypes.blMessageUserGroupsUpdate;
        }

        return tipo;
    }

    public static String get_LastMessage()
    {
        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        AnonData model = realm.where(AnonData.class).findFirst();
        if(model != null && model.getLastMessage()!= null && !model.getLastMessage().isEmpty()){
            Log.d("lastMessage", "GET " + model.getLastMessage());
            return model.getLastMessage();
        }
        return "0";
    }

    /**
     * Coloca el timestamp del ultimo mensaje de la base de datos de UserData de forma asincrona.
     * @param paramString
     */
    public static void set_LastMessage(final String paramString) {
        if (paramString == null)
            return; //ERROR

        Realm realm = CriptextLib.instance().getMonkeyKitRealm();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm bgRealm) {
                AnonData user = bgRealm.where(AnonData.class).findFirst();
                if (user == null) {
                    user = bgRealm.createObject(AnonData.class);
                    user.set_id("1");
                }

                user.setLastMessage(paramString);
                Log.d("lastMessage", user.getLastMessage());
            }
        }, new Realm.Transaction.Callback() {
            @Override
            public void onSuccess() {
                Log.d("AccountModel", "set lastMessage: SUCCESS");
            }

            @Override
            public void onError(Exception e) {
                Log.d("AccountModel", "set lastMessage: ERROR\n");
                e.printStackTrace();
                // transaction is automatically rolled-back, do any cleanup here
            }
        });
    }

    /**
     * Borra permanente la base de datos de MonkeyKit y todos sus mensajes.
     */
    public static void deleteDB(){
        Realm.deleteRealm(CriptextLib.instance().getMonkeyConfig());
    }

    }
