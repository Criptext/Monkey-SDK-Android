package com.criptext.database;

import com.criptext.lib.CriptextLib;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by criptext on 5/15/15.
 */
public class MessageModel extends RealmObject {
    @PrimaryKey
    private String key;
    //Hora de envio del mensaje en el servidor
    private long _datetime;

    //Hora de llegada del mensaje en el telefono
    private long _datetimeorden;

    //"1" Si el mensaje es efimero, de lo contrario "0" El nombre es bien retardado
    private String   _message_text;
    private String _file_type;

    private long _group_id;
    //Identificador del mensaje
    private String _message;
    private String _message_id;
    private String _request;
    //Status del mensaje, enviado, no enviado, leido
    private String _status;
    private String _type;
    //id de quien envia el mensaje (KEY_FROM_MESS)
    private String _uid_sent;
    //id de quien recibe el mensaje (KEY_TO_MESS)
    private String _uid_recive;
    private String _message_text_old;
    //JSON con params
    private String params;
    //JSON con props
    private String props;
    private boolean groupIdIsNull;


    public MessageModel(){
        _datetime = System.currentTimeMillis()/1000;
        groupIdIsNull = true;
        params = "";
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isGroupIdIsNull() {
        return groupIdIsNull;
    }

    public void setGroupIdIsNull(boolean groupIdIsNull) {
        this.groupIdIsNull = groupIdIsNull;
    }

    public MessageModel(String message_id, String ud_sent, String message, long datatime, String request, String file_type, String type,String uid_recive)
    {
        this._message_id = message_id;
        //this._group_id = null;
        groupIdIsNull = true;
        this._uid_sent = ud_sent;
        this._uid_recive = uid_recive;
        this._message = message;
        this._message_text = CriptextLib.null_ref;
        this._message_text_old = CriptextLib.null_ref;
        if(datatime!=0)
            this._datetime = datatime;
        else
            this._datetime = System.currentTimeMillis()/1000;
        this._request = request;

        this._file_type = file_type;
        this._type = type;
        this.key = ud_sent + this._datetime;
        this.params = "";
        this.props = "";
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }
    public String getProps() {
        return props;
    }

    public void setProps(String props) {
        this.props = props;
    }

    public String get_message_text() {
        return _message_text;
    }

    public void set_message_text(String _message_text) {
        this._message_text = _message_text;
    }

    public String get_file_type() {
        return _file_type;
    }

    public void set_file_type(String _file_type) {
        this._file_type = _file_type;
    }

    public long get_group_id() {
        return _group_id;
    }

    public void set_group_id(long _group_id) {
        this._group_id = _group_id;
    }

    public String get_message_id() {
        return _message_id;
    }

    public void set_message_id(String _message_id) {
        this._message_id = _message_id;
    }

    public String get_message() {
        return _message;
    }

    public void set_message(String _message) {
        this._message = _message;
    }

    public String get_status() {
        return _status;
    }

    public void set_status(String _status) {
        this._status = _status;
    }

    public String get_type() {
        return _type;
    }

    public void set_type(String _type) {
        this._type = _type;
    }

    public String get_uid_recive() {
        return _uid_recive;
    }

    public long get_datetimeorden() {
        return _datetimeorden;
    }

    public void set_datetimeorden(long _datetimeorden) {
        this._datetimeorden = _datetimeorden;
    }

    public void set_uid_recive(String _uid_recive) {
        this._uid_recive = _uid_recive;
    }

    public String get_message_text_old() {
        return _message_text_old;
    }

    public void set_message_text_old(String _message_text_old) {
        this._message_text_old = _message_text_old;
    }

    public String get_request() {
        return _request;
    }

    public void set_request(String _request) {
        this._request = _request;
    }

    public String get_uid_sent() {
        return _uid_sent;
    }

    public void set_uid_sent(String _uid_sent) {
        this._uid_sent = _uid_sent;
    }

    public long get_datetime() {

        return _datetime;
    }

    public void set_datetime(long _datetime) {
        this._datetime = _datetime;
    }
}
