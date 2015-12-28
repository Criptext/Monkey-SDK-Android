package com.criptext.lib;

import android.content.Context;
import android.security.KeyPairGeneratorSpec;
import android.util.Base64;

import com.criptext.database.KeyChain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.security.auth.x500.X500Principal;

/**
 * Created by daniel on 12/11/15.
 */
public class KeyStoreCriptext {

    public static void putString(Context context, String key, String value){
        KeyStoreCriptext.createNewKey(context, key);
        KeyChain.putString(context, key, KeyStoreCriptext.encryptString(key, value));
    }

    public static String getString(Context context,String key){
        if(KeyChain.getString(context,key).equals(""))
            return "";
        else
            return KeyStoreCriptext.decryptString(key,KeyChain.getString(context,key));
    }

    public static void putInt(Context context, String key, int value){
        KeyStoreCriptext.createNewKey(context, key);
        KeyChain.putString(context, key, KeyStoreCriptext.encryptString(key, String.valueOf(value)));
    }

    public static int getInt(Context context,String key){
        if(KeyChain.getString(context, key).equals(""))
            return 0;
        else
            return Integer.parseInt(KeyStoreCriptext.decryptString(key,KeyChain.getString(context, key)));
    }

    public static ArrayList<String> getAllAlias(){
        ArrayList<String> keyAliases = new ArrayList<>();
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                keyAliases.add(aliases.nextElement());
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        return keyAliases;
    }

    public static void createNewKey(Context context, String alias) {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            // Create new key if needed
            if (!keyStore.containsAlias(alias)) {
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                end.add(Calendar.YEAR, 1);
                KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                        .setAlias(alias)
                        .setSubject(new X500Principal("CN=Sample Name, O=Android Authority"))
                        .setSerialNumber(BigInteger.ONE)
                        .setStartDate(start.getTime())
                        .setEndDate(end.getTime())
                        .build();
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
                generator.initialize(spec);
                KeyPair keyPair = generator.generateKeyPair();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String encryptString(String alias, String initialText) {
        String encryptedText="";
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(alias, null);
            RSAPublicKey publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();

            // Encrypt the text
            if(initialText.isEmpty()) {
                return "";
            }

            Cipher input = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
            input.init(Cipher.ENCRYPT_MODE, publicKey);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    outputStream, input);
            cipherOutputStream.write(initialText.getBytes("UTF-8"));
            cipherOutputStream.close();

            byte [] vals = outputStream.toByteArray();
            encryptedText=Base64.encodeToString(vals, Base64.DEFAULT);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptedText;
    }

    public static String decryptString(String alias, String cipherText) {
        String decryptedText="";
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(alias, null);
            //RSAPrivateKey privateKey = (RSAPrivateKey) privateKeyEntry.getPrivateKey();
            //Cipher output = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
            //output.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());
            Cipher output = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            output.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());

            CipherInputStream cipherInputStream = new CipherInputStream(
                    new ByteArrayInputStream(Base64.decode(cipherText, Base64.DEFAULT)), output);
            ArrayList<Byte> values = new ArrayList<>();
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                values.add((byte)nextByte);
            }

            byte[] bytes = new byte[values.size()];
            for(int i = 0; i < bytes.length; i++) {
                bytes[i] = values.get(i).byteValue();
            }

            decryptedText = new String(bytes, 0, bytes.length, "UTF-8");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return decryptedText;
    }

}
