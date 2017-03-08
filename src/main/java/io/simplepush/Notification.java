package io.simplepush;

import main.java.io.simplepush.SimplepushException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class Notification{

	private static String API = "https://api.simplepush.io/send";
    private static String SALT_COMPAT = "1789F0B8C4A051E5";

    private Notification() {}

	/**
	 * Send push notification to simplepush.io
	 *
	 * @throws IllegalArgumentException
     * @throws SimplepushException
     */
    public static void send(String key, String title, String message, String event) throws IllegalArgumentException, SimplepushException {
		if (key == null || message == null) {
			throw new IllegalArgumentException("Simplepush key and message must be set");
		}

        try {
		    List<NameValuePair> params = getParams(key, title, message, event, null, null);
            sendHttpPost(params);
        } catch (Exception e) {
            throw new SimplepushException(e.getMessage());
        }
    }

	/**
	 * Send encrypted push notification to simplepush.io
	 *
     * @throws IllegalArgumentException
     * @throws SimplepushException
     */
    public static void sendEncrypted(String key, String title, String message, String event, String password, String salt) throws IllegalArgumentException, SimplepushException {
		if (key == null || message == null || password == null) {
			throw new IllegalArgumentException("Simplepush key, message and password must be set");
		}

        try {
            List<NameValuePair> params;

            if(salt != null) {
                params = getParams(key, title, message, event, password, salt);
            } else {
                // For compatibility reasons
                params = getParams(key, title, message, event, password, SALT_COMPAT);
            }

            sendHttpPost(params);
        } catch (Exception e) {
            throw new SimplepushException(e.getMessage());
        }
    }

    private static void sendHttpPost(List<NameValuePair> params) throws IOException, KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        CloseableHttpClient httpClient;
        httpClient = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost(API);
        httpPost.setEntity(new UrlEncodedFormEntity(params));
        httpClient.execute(httpPost);

        if(httpClient != null) {
            httpClient.close();
        }
    }

    private static List<NameValuePair> getParams(String key, String title, String message, String event, String password, String salt) throws UnsupportedEncodingException, NoSuchAlgorithmException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, InvalidAlgorithmParameterException, NoSuchPaddingException {
        List<NameValuePair> params = new ArrayList<NameValuePair>(6);
        params.add(new BasicNameValuePair("key", key));

        if(password == null) {
            params.add(new BasicNameValuePair("msg", message));

            if(title != null) {
                params.add(new BasicNameValuePair("title", title));
            }

        } else {
            SecretKey encKey = getEncryptionKey(password, salt);

            IvParameterSpec iv = getRandomIV();
            String ivHex = Hex.encodeHexString(iv.getIV());
            params.add(new BasicNameValuePair("iv", ivHex));
            params.add(new BasicNameValuePair("encrypted", "true"));

            if(title != null) {
                title = encrypt(encKey, iv, title);
                params.add(new BasicNameValuePair("title", title));
            }

            message = encrypt(encKey, iv, message);
            params.add(new BasicNameValuePair("msg", message));
        }

        if(event != null) {
            params.add(new BasicNameValuePair("event", event));
        }

        return params;
	}

	private static IvParameterSpec getRandomIV() {
	    final byte[] iv = new byte[16];
        final SecureRandom rng = new SecureRandom();
        rng.nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    private static SecretKey getEncryptionKey(String password, String salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String saltedPassword = password + salt;
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        byte[] sha1sum = sha1.digest(saltedPassword.getBytes("UTF-8"));
        byte[] key = Arrays.copyOfRange(sha1sum, 0, 16);

        return new SecretKeySpec(key, 0, 16, "AES");
    }

    private static String encrypt(SecretKey encryptionKey, IvParameterSpec iv, String data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, iv);

        byte[] cipherText = cipher.doFinal(data.getBytes("UTF-8"));

        return Base64.encodeBase64URLSafeString(cipherText);
    }
}
