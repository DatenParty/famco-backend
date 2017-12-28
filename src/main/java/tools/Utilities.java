package tools;

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

    public static DBConnection connection = null;
    static public final HashMap<Integer, Boolean> intToBool = new HashMap<Integer, Boolean>() {{
        put(0, false);
        put(1, true);
    }};

    public static void log(String text) {
        System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + " > " + text);
    }

    static public void write(String text, int responseCode, HttpExchange e) {
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

    static public void unauthorized(HttpExchange e) {
        write("{\"code\": 401}", 401, e);
    }

	/**
	 * @param query
	 * @return map
	 */
    public static  HashMap<String, String> queryToMap(String query) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        String[] e = query.split("&");
        for (String el : e) {
            map.put(el.substring(0, el.indexOf("=")), el.substring(el.indexOf("=") + 1));
        }
        return map;
    }

    public static boolean exists(ResultSet set) {
        try {
            return set.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean nullOrEmpty(String string) {
        return string == null || string.equals("");
    }

    public static boolean nullOrEmpty(String ... strings) {
        for (String string : strings) {
            if (string == null || string.equals("")) return true;
        }
        return false;
    }

    public static String hash(String text) {
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

    public static String generateSignature(String username, String password) {
        return hash(username + hash(password) + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dMMyyy.hh")));
    }
    
    /**
     Verification System
     
     example verifaction query:
     famco.datenparty.org/handler?signature=username:b109f3bbbc244eb82441917ed06d...
     required data:
     1. username
     2. signature
     
     generate signature:
     hash(username + hash(password) + time(dMMyyy.hh))
     
     Responses:
     true/	authorized:		200 and requested data
     false/unauthorized:	401 and no data
     */
    public static boolean verify(HashMap<String, String> query) {
        
        String pwd = null;
        String[] signature = query.get("signature").split(":");
        //signature[0] = username
        //signature[1] = signature
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dMMyyy.hh"));
        
        try {
            ResultSet set = Utilities.connection.execute("SELECT pwd FROM users WHERE username = ?", signature[0]);
            set.next();
            pwd = set.getString("pwd");
        } catch (SQLException e) {
            e.getErrorCode();
        }
        
        //Comparing the signature generated from the server with the signature from the query
        String signatureServer = Utilities.hash(signature[0] + pwd + time);
        return signature[1].equals(signatureServer);
    }
}
