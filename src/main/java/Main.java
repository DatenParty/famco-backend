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

    

    public static void main(String[] args) {
        try {
            Utilities.connection = new DBConnection();
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
            Utilities.write("{\"response\": \"Wohoo!\"}", 200, exchange);
        }
    }


    private class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                String query = exchange.getRequestURI().getQuery();
                HashMap<String, String> userdata = Utilities.queryToMap(query);
                String username = userdata.get("username");
                String password = userdata.get("password");
                ResultSet resultSet = Utilities.connection.execute("SELECT * FROM users WHERE username=? AND pwd=?", username, Utilities.hash(password));
                JSONObject responseObject = new JSONObject();
                responseObject.put("response", false);
                responseObject.put("isAdmin", false);
                if (Utilities.nullOrEmpty(username) || Utilities.nullOrEmpty(password)) {
                    Utilities.write(responseObject.toJSONString(), 401, exchange);
                } else {
                    while (resultSet.next()) {
                        responseObject.put("response", true);
                        responseObject.put("isAdmin", Utilities.intToBool.get(resultSet.getInt("isAdmin")));
                    }
                    Utilities.write(responseObject.toJSONString(), 200, exchange);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                Utilities.write("{\"error\": \"Es wurden nicht alle Felder ausgefüllt.\"}", 400, exchange);
            }
        }
    }

    private class NewUserHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) {
            try {
                String query = exchange.getRequestURI().getQuery();
                HashMap<String, String> map = Utilities.queryToMap(query);
                ResultSet set = Utilities.connection.execute("SELECT email FROM users WHERE email = ?", map.get("email"));

                String name = map.get("name");
                name = name.replace('+', ' ');
                String password = map.get("password");
                String group = map.get("groupId");
                String email = map.get("email");
                if (Utilities.exists(set)) {
                    Utilities.write("{\"error\": \"Dieser Benutzer ist schon vorhanden\"}", 401, exchange);
                } else if (Utilities.nullOrEmpty(name) || Utilities.nullOrEmpty(password) || Utilities.nullOrEmpty(group) || Utilities.nullOrEmpty(email) || map.isEmpty()) {
                    Utilities.write("{\"error\": \"Es wurden nicht alle Felder ausgefüllt\"}", 400, exchange);
                } else {
                    Utilities.connection.update("INSERT INTO users VALUES (DEFAULT, ?, ?, ?, ?)", name, password, group, email);
                    Utilities.write("{\"result\": \"Der Benutzer mit der E-Mail-Adresse " + map.get("email") + " wird erstellt\", \"response\": true}", 201, exchange);
                }
            } catch (NullPointerException e) {
                Utilities.write("{\"error\": \"Es wurden nicht alle Felder ausgefüllt\"}", 400, exchange);
            }
        }
    }
    
    private class VerificationHandler implements HttpHandler {
        /*
        Example for the implementation of the authorizer (verify(query)) in a Httphandler
         */
    	
    	@Override
			public void handle(HttpExchange exchange) {
    		try {
    		    //converting query to map wih "Utilities.queryToMap"
					HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());
			
					/*
					  Basicly everything you have to do is calling the verify methode with the httpexchange-query
					 */
					if (verify(query)) {
						Utilities.write("{\"code\": 200}", 200, exchange);
					} else {
						Utilities.write("{\"code\": 401}", 401, exchange);
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
		private boolean verify(HashMap<String, String> query) {
		
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
