package net.osmand;

import net.osmand.data.LatLon;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiClient {

    public interface ApiCallback {
        void onSuccess(LatLon latLon);
        void onError(Exception e);
    }

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static void sendGetRequest(String urlString, ApiCallback callback) {
        executorService.execute(() -> {
            StringBuilder response = new StringBuilder();
            HttpURLConnection urlConnection = null;
            try {
                String proxyEnv = System.getenv("HTTP_PROXY");
                Proxy proxy = Proxy.NO_PROXY;
                String proxyUser = null;
                String proxyPassword = null;
                if (proxyEnv != null && !proxyEnv.isEmpty()) {
                    proxyEnv = proxyEnv.replace("http://", "").replace("https://", ""); // Supprimer "http://"
                    String[] proxyParts = proxyEnv.split("@"); // Séparer auth et host

                    if (proxyParts.length == 2) { // Si authentification présente
                        String[] authParts = proxyParts[0].split(":");
                        proxyUser = authParts[0];
                        proxyPassword = authParts[1];
                        proxyEnv = proxyParts[1]; // Garder seulement "domaine:port"
                    }
                    String proxyHost = proxyEnv.split(":")[0];
                    int proxyPort = Integer.parseInt(proxyEnv.split(":")[1]);
                    proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                }

                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection(proxy);
                if (proxyUser != null && proxyPassword != null) {
                    String encodedAuth = Base64.getEncoder()
                            .encodeToString((proxyUser + ":" + proxyPassword).getBytes(StandardCharsets.UTF_8));
                    urlConnection.setRequestProperty("Proxy-Authorization", "Basic " + encodedAuth);
                }
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setConnectTimeout(5000);

                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) { // 200
                    BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                } else {
                    callback.onError(new Exception("Erreur HTTP : " + responseCode));
                    return;
                }
            } catch (Exception e) {
                callback.onError(e);
                return;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            try {
                String reponse = response.toString();
                JSONArray jsonArray = new JSONArray(reponse);
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                LatLon latlon = new LatLon(jsonObject.getDouble("lat"), jsonObject.getDouble("lon"));
                callback.onSuccess(latlon);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
}
