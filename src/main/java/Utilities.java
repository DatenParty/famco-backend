import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class Utilities {

    static DBConnection connection = null;
    static final HashMap<Integer, Boolean> intToBool = new HashMap<Integer, Boolean>() {{
        put(0, false);
        put(1, true);
    }};

    static void log(String text) {
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + " > " + text);
    }

    static void write(String text, int responseCode, HttpExchange e) {
        try {
            e.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            e.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            e.sendResponseHeaders(responseCode, 0);
            OutputStream os = e.getResponseBody();
            os.write(text.getBytes("UTF-8"));
            os.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
	
	/**
	 * @param query
	 * @return map
	 */
    static HashMap<String, String> queryToMap(String query) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        String[] e = query.split("&");
        for (String el : e) {
            map.put(el.substring(0, el.indexOf("=")), el.substring(el.indexOf("=") + 1));
        }
        return map;
    }

    static boolean exists(ResultSet set) {
        try {
            return set.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    static boolean nullOrEmpty(String string) {
        return string == null || string.equals("");
    }

    static String hash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] array = digest.digest(text.getBytes("UTF-8"));

            StringBuilder buffer = new StringBuilder();
            for (byte anArray : array)
                buffer.append(Integer.toString((anArray & 0xff) + 0x100, 16).substring(1));
            return buffer.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    static String generateSignature(String username, String password) {
        return hash(username + hash(password) + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dMMyyy.hh")));
    }
}
