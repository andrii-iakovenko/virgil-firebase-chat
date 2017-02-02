package com.google.firebase.codelab.friendlychat.utils;

import com.virgilsecurity.sdk.client.utils.ConvertionUtils;
import com.virgilsecurity.sdk.client.utils.StreamUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Created by Andrii Iakovenko.
 */

public class HttpUtils {

    /**
     * Create and configure http connection.
     *
     * @param url
     *            The URL.
     * @param method
     *            The HTTP method.
     * @return The connection.
     * @throws IOException
     */
    private static HttpURLConnection createConnection(URL url, String method) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod(method);
        urlConnection.setUseCaches(false);

        switch (method) {
            case "DELETE":
            case "POST":
            case "PUT":
            case "PATCH":
                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);
                break;
            default:
        }
        urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        return urlConnection;
    }

    public static <T> T execute(URL url, String method, Map<String, String> properties, InputStream inputStream, Class<T> clazz) {
        try {
            HttpURLConnection urlConnection = createConnection(url, method);
            if (properties != null) {
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    urlConnection.addRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            if (inputStream != null) {
                StreamUtils.copyStream(inputStream, urlConnection.getOutputStream());
            }
            try {
                if (urlConnection.getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
                    // Get error code from request
                    try (InputStream in = new BufferedInputStream(urlConnection.getErrorStream())) {
                        String body = ConvertionUtils.toString(in);
                        throw new RuntimeException(body);
                    }
                } else if (clazz.isAssignableFrom(Void.class)) {
                    return null;
                } else {
                    try (InputStream instream = new BufferedInputStream(urlConnection.getInputStream())) {
                        String body = ConvertionUtils.toString(instream);
                        return ConvertionUtils.getGson().fromJson(body, clazz);
                    }
                }
            } finally {
                urlConnection.disconnect();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
