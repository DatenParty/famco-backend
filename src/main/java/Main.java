import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class Main {

    private static DBConnection connection = null;
    private final static HashMap<Integer, Boolean> intToBool = new HashMap<Integer, Boolean>() {{
        put(0, false);
        put(1, true);
    }};

    public static void main(String[] args) {
        try {
            connection = new DBConnection();
            new Main();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Main() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            HttpServer server = HttpServer.create(new InetSocketAddress(1337), 0);
            server.createContext("/", new Handler());
            server.createContext("/login", new LoginHandler());

            server.createContext("/newuser", new NewUserHandler());

            System.out.println("Server wird gestartet...");
            server.setExecutor(null);
            server.start();
            System.out.println("Server ist betriebsbereit");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private class Handler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) {
            write("{\"response\": \"Wohoo!\"}", 200, exchange);
        }
    }


    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                String query = exchange.getRequestURI().getQuery();
                HashMap<String, String> userdata = queryToMap(query);
                String username = userdata.get("username");
                String password = userdata.get("password");
                ResultSet resultSet = connection.execute("SELECT * FROM users WHERE username=? AND pwd=?", username, hash(password));
                JSONObject responseObject = new JSONObject();
                responseObject.put("response", false);
                responseObject.put("isAdmin", false);
                if (nullOrEmpty(username) || nullOrEmpty(password)) {
                    write(responseObject.toJSONString(), 401, exchange);
                } else {
                    while (resultSet.next()) {
                        responseObject.put("response", true);
                        responseObject.put("isAdmin", intToBool.get(resultSet.getInt("isAdmin")));
                    }
                    write(responseObject.toJSONString(), 200, exchange);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                write("{\"error\": \"Es wurden nicht alle Felder ausgefüllt.\"}", 400, exchange);
            }
        }
    }

    private class NewUserHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) {
            try {
                String query = exchange.getRequestURI().getQuery();
                HashMap<String, String> map = queryToMap(query);
                ResultSet set = connection.execute("SELECT email FROM users WHERE email = ?", map.get("email"));

                String name = map.get("name");
                name = name.replace('+', ' ');
                String password = map.get("password");
                String group = map.get("groupId");
                String email = map.get("email");
                if (exists(set)) {
                    write("{\"error\": \"Dieser Benutzer ist schon vorhanden\"}", 401, exchange);
                } else if (nullOrEmpty(name) || nullOrEmpty(password) || nullOrEmpty(group) || nullOrEmpty(email) || map.isEmpty()) {
                    write("{\"error\": \"Es wurden nicht alle Felder ausgefüllt\"}", 400, exchange);
                } else {
                    connection.update("INSERT INTO users VALUES (DEFAULT, ?, ?, ?, ?)", name, password, group, email);
                    write("{\"result\": \"Der Benutzer mit der E-Mail-Adresse " + map.get("email") + " wird erstellt\", \"response\": true}", 201, exchange);
                }
            } catch (NullPointerException e) {
                write("{\"error\": \"Es wurden nicht alle Felder ausgefüllt\"}", 400, exchange);
            }
        }
    }

    private void write(String text, int responseCode, HttpExchange e) {
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

    private HashMap<String, String> queryToMap(String s) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        String[] e = s.split("&");
        for (String el : e) {
            map.put(el.substring(0, el.indexOf("=")), el.substring(el.indexOf("=") + 1));
        }
        return map;
    }

    private boolean exists(ResultSet set) {
        try {
            return set.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean nullOrEmpty(String string) {
        return string == null || string.equals("");
    }

    private String hash(String text) {
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

    private static boolean intToBool(int i) {
        switch (i) {
            case 1: return true;
            default: return false;
        }
    }
}
