import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
            Utilities.log("starting server");
    
            HttpServer server = HttpServer.create(new InetSocketAddress(1337), 0);
            server.createContext("/", new Handler());
            server.createContext("/login", new LoginHandler());
            server.createContext("/test", new VerificationHandler());
    
            //server.createContext("/newuser", new NewUserHandler());

            server.setExecutor(null);
            server.start();
            Utilities.log("server started");
        } catch (IOException e) {
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
    
    private class VerificationHandler implements HttpHandler {
    	
    	@Override
			public void handle(HttpExchange exchange) {
    		try {
					HashMap<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
					JSONObject responseObject = new JSONObject();
			
					boolean verified = verify(query.get("signature"));
					responseObject.put("verified", verified);
					
					if (verified) {
						write(responseObject.toJSONString(), 200, exchange);
					} else {
						write(responseObject.toJSONString(), 400, exchange);
					}
				} catch (Exception e) {
    			e.printStackTrace();
				}
			}
		}

    /*
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
		private boolean verify(String signature) {
		
			String pwd = null;
			String[] query = signature.split(":");
			String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dMMyyy.hh"));
	
      try {
          ResultSet set = connection.execute("SELECT pwd FROM users WHERE username = ?", query[0]);
          set.next();
          pwd = set.getString("pwd");
      } catch (SQLException e) {
          e.getErrorCode();
      }
	
      //Comparing the signature generated from the server with the signature from the query
      String signatureServer = hash(query[0] + pwd + time);
      return signature.equals(signatureServer);
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
            case 1:
                return true;
            default:
                return false;
        }
    }
}
