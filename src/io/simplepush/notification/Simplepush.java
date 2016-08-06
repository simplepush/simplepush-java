package io.simplepush.notification;

import java.io.IOException;
import java.net.URL;

public class Simplepush {

	private static String URL = "https://api.simplepush.io/send";

	private String key;
	private String title;
	private String message;

    /**
	 * Construct a {@link Simplepush}.
	 *
	 * @param key Unique SimplePush.io push notification key.
	 * @param message Push notification message.
	 */
    public Simplepush(String key, String message) {
		this(key, null, message);
    }

    /**
	 * Construct a {@link Simplepush}.
	 *
	 * @param key Unique SimplePush.io push notification key.
	 * @param title Push notification title.
	 * @param message Push notification message.
	 */
    public Simplepush(String key, String title, String message) {
        this.key = key;
		this.title = title;
		this.message = message;
    }

	/**
	 * Send push notification to simplepush.io
	 *
	 * @throws IOException
     */
    public void send() throws IOException {
		if (this.key == null || this.message == null) {
			throw new IllegalArgumentException("key and message argument must be set");
		}

		String request;
		if(this.title != null ) {
			request = URL + "/" + this.key + "/" + this.title + "/" + this.message;
		} else {
			request = URL + "/" + this.key + "/" + this.message;
		}

		new URL(request).openConnection();
    }
}
