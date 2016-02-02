package com.criptext.database;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;

import com.criptext.comunication.Base64Coder;
import com.criptext.comunication.MOKMessage;
import com.criptext.lib.CriptextLib;
import com.criptext.lib.R;

import javax.crypto.Cipher;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;

public class RemoteMessage implements Comparable<RemoteMessage>{

	//Modelo con todos los atributos que se guardan en la base de datos
	MessageModel model;

	public long get_datetimeorden() {
		return model.get_datetimeorden();
	}
	public void set_datetimeorden(long param) {
		model.set_datetimeorden(param);
	}

	private boolean desencript=false;
	private boolean anidescript=true;
	public  boolean isready=false;
	public  boolean isfirthprivateaudio=true;
	public boolean animateexit=false;
	public  String id_miembro="";
	public  boolean isgroup=false;
	public int audioProgress;

	private CountDownTimer privateCounter = null;

	private String dirtemp_images=null;
	boolean istap_private=true;
	public long counter=0L;

	public boolean isIstap_private() {
		return istap_private;
	}
	public void setIstap_private(boolean istap_private) {
		this.istap_private = istap_private;
	}
	public String get_message_text_old() {
		return model.get_message_text_old().equals(CriptextLib.null_ref) ? null : model.get_message_text_old();
	}
	public void set_message_text_old(String paramString) {
		model.set_message_text_old(paramString == null ? CriptextLib.null_ref : paramString);
	}
	public String getDirtemp_images() {
		return dirtemp_images;
	}
	public void setDirtemp_images(String dirtemp_images) {
		this.dirtemp_images = dirtemp_images;
	}
	public boolean isAnidescript() {
		return anidescript;
	}
	public void setAnidescript(boolean anidescript) {
		this.anidescript = anidescript;
	}
	public boolean isDesencript() {
		return desencript;
	}
	public void setDesencript(boolean desencript) {
		this.desencript = desencript;
	}
	public String get_message_text() {
		if(model.get_message_text() != null)
			return model.get_message_text().equals(CriptextLib.null_ref) ? null : model.get_message_text();
		else
			return null;
	}
	public void set_message_text(String paramString) {
		model.set_message_text(paramString == null ? CriptextLib.null_ref : paramString);
	}


	public CountDownTimer getPrivateCounter() {
		return privateCounter;
	}

	public void setPrivateCounter(CountDownTimer privateCounter) {
		this.privateCounter = privateCounter;
	}
	public 	boolean is_ready_audio=false;
	public 	boolean is_playing_audio=false;

	public String get_uid_recive() {
		return model.get_uid_recive().equals(CriptextLib.null_ref) ? null : model.get_uid_recive();
	}
	public void set_uid_recive(String _uid_recive) {
		model.set_uid_recive(_uid_recive);
	}

	public RemoteMessage()
	{
		model = new MessageModel();
		this.audioProgress = 0;
	}

	public RemoteMessage(String message_id, String ud_sent, String message, long datatime, String request, String file_type, String type,String uid_recive)
	{
		model = new MessageModel(message_id, ud_sent, message, datatime, request, file_type, type, uid_recive);
		this.audioProgress = 0;
	}

	public RemoteMessage(MessageModel mmodel){

		model = mmodel;
		this.audioProgress = 0;
	}
	public MessageModel getModel() {
		return model;
	}

	public void setModel(MessageModel model) {
		this.model = model;
	}

	public int compareTo(RemoteMessage message)
	{
		//synchronized (message) {
		try{
			long stamp1 = getValidTimestamp(getModel()), stamp2 = getValidTimestamp(message.getModel());
			if(stamp1 < stamp2)
				return -1;
			else if(stamp1 > stamp2)
				return 1;
			else
				return 0;
		}catch(Exception e){
			System.out.println("Se cae en el orden de los mensajes");
			e.printStackTrace();
			return 0;
		}
		//}
	}

	public long datetime()
	{
		return model.get_datetime();
	}

	public String file_type()
	{
		return model.get_file_type().equals(CriptextLib.null_ref) ? null : model.get_file_type();
	}

	public long get_datetime()
	{
		return  model.get_datetime();
	}

	public Long get_group_id()
	{
		return model.isGroupIdIsNull() ? null : model.get_group_id();
	}

	public String get_message()
	{
		return message();
	}

	public String get_message_id()
	{
		return model.get_message_id().equals(CriptextLib.null_ref) ? null : model.get_message_id();
	}

	public String get_request()
	{
		return model.get_request().equals(CriptextLib.null_ref) ? null : model.get_request();
	}

	public String get_status()
	{
		if(model.get_status() == null)
			return null;
		return model.get_status().equals(CriptextLib.null_ref) ? null : model.get_status();
	}

	public String get_type()
	{
		return model.get_type().equals(CriptextLib.null_ref) ? null : model.get_type();
	}

	public String get_uid_sent()
	{
		return model.get_uid_sent().equals(CriptextLib.null_ref) ? null : model.get_uid_sent();
	}



	public String message()
	{
		return model.get_message().equals(CriptextLib.null_ref) ? null : model.get_message();
	}

	public String message_id()
	{
		return model.get_message_id().equals(CriptextLib.null_ref) ? null : model.get_message_id();
	}

	public String request()
	{
		return model.get_request().equals(CriptextLib.null_ref) ?  null : model.get_request();
	}

	public void set_datetime(long param)

	{
		model.set_datetime(param);
		if((model.getKey() == null || model.getKey().equals("")) && !(model.get_uid_sent() == null || model.get_uid_sent().equals("")))
			model.setKey(model.get_uid_sent() + model.get_datetime());
	}

	public void set_file_type(String paramString)
	{
		model.set_file_type(paramString == null ? CriptextLib.null_ref : paramString);
	}

	public void set_group_id(Long paramLong)
	{
		if(paramLong != null) {
			model.setGroupIdIsNull(false);
			model.set_group_id(paramLong);
		} else{
			model.setGroupIdIsNull(true);
		}
	}

	public void set_message(String paramString)
	{
		model.set_message(paramString == null ? CriptextLib.null_ref : paramString);
	}

	public void set_message_id(String paramString)
	{
		model.set_message_id(paramString == null ? CriptextLib.null_ref : paramString);
	}

	public void set_request(String paramString)
	{
		model.set_request(paramString == null ? CriptextLib.null_ref : paramString);
	}

	public void set_status(String paramString)
	{
		model.set_status(paramString == null ? CriptextLib.null_ref : paramString);
	}

	public void set_type(String paramString)
	{
		model.set_type(paramString == null ? CriptextLib.null_ref : paramString);
	}

	public void set_uid_sent(String paramString)
	{
		model.set_uid_sent(paramString == null ? CriptextLib.null_ref : paramString);
		if((model.getKey() == null || model.getKey().equals("")) && !(model.get_datetime() == 0 ))
			model.setKey(model.get_uid_sent() + model.get_datetime());
	}

	public String type()
	{
		return model.get_type().equals(CriptextLib.null_ref) ? null : model.get_type();
	}

	public String uid_sent()
	{
		return model.get_uid_sent().equals(CriptextLib.null_ref) ? null : model.get_uid_sent();


	}

	public String getParams(){
		return model.getParams();
	}

	public void setParams(String params){
		if(params != null)
			model.setParams(params);
	}

	public String getProps(){
		return model.getProps();
	}

	public void setProps(String props){
		if(props != null)
			model.setProps(props);
	}
	public void printValues(){
		System.out.println( "MESSAGE MODEL:" + " status: " + model.get_status() + " uid_sent: " + model.get_uid_sent()
						+ " datetime: " + model.get_datetime()	+ " datatimeorder: " + model.get_datetimeorden() +
						" uid_recive: " + model.get_uid_recive() + " filetype: " + model.get_file_type() + " id: " + model.get_message_id()
						+ " message: " + model.get_message() + " message_text: " + model.get_message_text() +
						" message_old: " + model.get_message_text_old() + " request: " + model.get_request() + " type: " + model.get_type()
		);
	}

	/**
	 * Algoritmo Insert Sort para los mensajes
	 * @param realmlist lista con mensajes sacados de la base de datos
	 * @return una LinkedList de RemoteMessage que es una copia exacta ordenada de la
	 * lista que pasó como argumento
	 */
	public static LinkedList<RemoteMessage> insertSortCopy(RealmList<MessageModel> realmlist){
		LinkedList<RemoteMessage> linkedlist = new LinkedList<RemoteMessage>();
		int total = realmlist.size();
		for(int i = 0; i < total; i++ ){
			MessageModel temp = realmlist.get(i);
			linkedlist.add(new RemoteMessage(temp));
			ListIterator it = linkedlist.listIterator(i);
			int j;
			for(j = i - 1; j >= 0 && getValidTimestamp(temp) < getValidTimestamp(realmlist.get(j)); j-- )
				Collections.swap(linkedlist, j, j + 1);
		}

		return linkedlist;
	}

	/**
	 * Algoritmo Insert Sort para los mensajes
	 * @param realmlist lista con mensajes sacados de la base de datos
	 * @return una LinkedList de RemoteMessage que es una copia exacta ordenada de la
	 * lista que pasó como argumento
	 */
	public static LinkedList<RemoteMessage> insertSortCopy(RealmResults<MessageModel> realmlist){
		LinkedList<RemoteMessage> linkedlist = new LinkedList<RemoteMessage>();
		int total = realmlist.size();
		for(int i = 0; i < total; i++ ){
			MessageModel temp = realmlist.get(i);
			linkedlist.add(new RemoteMessage(temp));
			ListIterator it = linkedlist.listIterator(i);
			int j;
			for(j = i - 1; j >= 0 && getValidTimestamp(temp) < getValidTimestamp(realmlist.get(j)); j-- )
				Collections.swap(linkedlist, j, j + 1);
		}

		return linkedlist;
	}

	private static long getValidTimestamp(MessageModel mi_model){
		long date_order = mi_model.get_datetimeorden(), datetime = mi_model.get_datetime();
		if(date_order != 0)
			return date_order;
		else return datetime;
	}
	/**
	 * Convierte una LinkedList de RemoteMessages a un RealmList de MessageModel para asegurarnos
	 * de tener los mensajes actualizados antes de guardarlos en la base.
	 * @param messages
	 * @return
	 */
	public static RealmList<MessageModel> getRemoteMessageList(LinkedList<RemoteMessage> messages){
		Iterator it  = messages.iterator();
		RealmList<MessageModel> results = new RealmList<MessageModel>();
		while(it.hasNext()){
			RemoteMessage message = (RemoteMessage)it.next();
			results.add(message.getModel());
		}
		return results;
	}

    public static String encrypt(String original,Context context) {

        try {
            System.out.println("ANDROID  ORIGINAL: "+original);
            InputStream instream= context.getResources().openRawResource(R.raw.public_key);

            byte[] encodedKey = new byte[instream.available()];
            instream.read(encodedKey);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey pkPublic = kf.generatePublic(publicKeySpec);

            Cipher pkCipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
            pkCipher.init(Cipher.ENCRYPT_MODE, pkPublic);
            //String stemp=Uri.parse("").buildUpon().appendQueryParameter("x", original).build().toString();
            //stemp=stemp.substring(3, stemp.length());

            String stemp=encodeURIComponent(original);
            //System.out.println("ANDROID UTF8: "+stemp);
            //original=Charset.forName("UTF-8").encode(original);

//			Charset charsetE = Charset.forName("iso-8859-1");
//			CharsetEncoder encoder = charsetE.newEncoder();
//
//			ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(original));
//			byte[] b = new byte[bbuf.remaining()];
//			bbuf.get(b);

            //byte ptext[] = original.getBytes("UTF_8");
            //String value = new String(ptext, "UTF_8");
            //System.out.println("ANDROID UTF8: "+value);
            //byte[] encryptedInByte = pkCipher.doFinal(ptext);
            byte[] encryptedInByte = pkCipher.doFinal(stemp.getBytes());
            String encryptedInString = new String(Base64Coder.encode(encryptedInByte));
            System.out.println("ANDROID ENCRIPTADOR : "+encryptedInString);
            instream.close();

            //desencrypt(encryptedInString,context);

            return encryptedInString;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";

    }

    /**
     * Actualiza el estado de un mensaje, colocandole un nuevo ID
     * @param newID el nuevo ID del mensaje (positivo, si ya no esta en sending)
     * @param status 52 es para leido, 51 es para entregado y el usuario esta dentro del app, y 50 es
     *               entregado pero el usuario esta fuera del app
     */
    public void updateStatus(String newID, int status){
        set_message_id(newID);
        if(status < 52)
            set_status("entregado");
        else
            set_status("leido");
    }

	/**
	 * Desencriptar legacy
	 * @param original
	 * @param context
	 * @return
	 */
    public static String desencrypt(String original,Context context){

        try {

            InputStream instream= context.getResources().openRawResource(R.raw.private_key);

            byte[] encodedKey = new byte[instream.available()];
            instream.read(encodedKey);
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey pkPrivate= kf.generatePrivate(privateKeySpec);
            //System.out.println("ANDROID String: "+original);
            byte[] rsaEncodedMessage = Base64Coder.decode(original);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
            cipher.init(Cipher.DECRYPT_MODE, pkPrivate);
            byte[] desencryptedInByte  = cipher.doFinal(rsaEncodedMessage);


            String desencryptedInString = new String(desencryptedInByte,"UTF_8");
            //System.out.println("ANDROID   mas: "+desencryptedInString);
            desencryptedInString=stripGarbage(desencryptedInString);

            if(desencryptedInString.endsWith("%"))
                desencryptedInString=desencryptedInString.substring(0, desencryptedInString.length()-1);

            String stemp=decodeURIComponent(desencryptedInString);

            instream.close();
            //System.out.println("ANDROID   DESENCRIPTADO : "+stemp);
            return stemp;

        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public static String encodeURIComponent(String input) {
        if(input.isEmpty()) {
            return input;
        }
        int l = input.length();
        StringBuilder o = new StringBuilder(l * 3);
        try {
            for (int i = 0; i < l; i++) {
                String e = input.substring(i, i + 1);
                if (ALLOWED_CHARS.indexOf(e) == -1) {
                    byte[] b = e.getBytes("utf-8");
                    o.append(getHex(b));
                    continue;
                }
                o.append(e);
            }
            return o.toString();
        } catch(UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return input;
    }

    public static String decodeURIComponent(String encodedURI) {
        char actualChar;

        StringBuffer buffer = new StringBuffer();
        int bytePattern, sumb = 0;
        for (int i = 0, more = -1; i < encodedURI.length(); i++) {
            actualChar = encodedURI.charAt(i);

            switch (actualChar) {
                case '%': {
                    actualChar = encodedURI.charAt(++i);
                    int hb = (Character.isDigit(actualChar) ? actualChar - '0'
                            : 10 + Character.toLowerCase(actualChar) - 'a') & 0xF;
                    actualChar = encodedURI.charAt(++i);
                    int lb = (Character.isDigit(actualChar) ? actualChar - '0'
                            : 10 + Character.toLowerCase(actualChar) - 'a') & 0xF;
                    bytePattern = (hb << 4) | lb;
                    break;
                }
                case '+': {
                    bytePattern = ' ';
                    break;
                }
                default: {
                    bytePattern = actualChar;
                }
            }

            if ((bytePattern & 0xc0) == 0x80) { // 10xxxxxx
                sumb = (sumb << 6) | (bytePattern & 0x3f);
                if (--more == 0)
                    buffer.append((char) sumb);
            } else if ((bytePattern & 0x80) == 0x00) { // 0xxxxxxx
                buffer.append((char) bytePattern);
            } else if ((bytePattern & 0xe0) == 0xc0) { // 110xxxxx
                sumb = bytePattern & 0x1f;
                more = 1;
            } else if ((bytePattern & 0xf0) == 0xe0) { // 1110xxxx
                sumb = bytePattern & 0x0f;
                more = 2;
            } else if ((bytePattern & 0xf8) == 0xf0) { // 11110xxx
                sumb = bytePattern & 0x07;
                more = 3;
            } else if ((bytePattern & 0xfc) == 0xf8) { // 111110xx
                sumb = bytePattern & 0x03;
                more = 4;
            } else { // 1111110x
                sumb = bytePattern & 0x01;
                more = 5;
            }
        }
        return buffer.toString();
    }

    private static String getHex(byte buf[]) {
        StringBuilder o = new StringBuilder(buf.length * 3);
        for (int i = 0; i < buf.length; i++) {
            int n = (int) buf[i] & 0xff;
            o.append("%");
            if (n < 0x10) {
                o.append("0");
            }
            o.append(Long.toString(n, 16).toUpperCase());
        }
        return o.toString();
    }

    public static final String ALLOWED_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.!~*'()";

    public static String stripGarbage(String s) {
        s=s.replace("+", "%2B");
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'A' && ch <= 'Z') ||
                    (ch >= 'a' && ch <= 'z') ||
                    (ch >= '0' && ch <= '9') ||
                    ch == '%' || ch == '_' ||
                    ch == '-' || ch == '!' ||
                    ch == '.' || ch == '~' ||
                    ch == '(' || ch == ')' ||
                    ch == '*' || ch == '\'' ||
                    ch == ';' || ch == '/' ||
                    ch == '?' || ch == ':' ||
                    ch == '@' || ch == '=' ||
                    ch == '&' || ch == '$' ||
                    ch == '+' || ch == ',') {
                sb.append(ch);
            }
            else
                break;
        }
        return sb.toString();
    }
}
