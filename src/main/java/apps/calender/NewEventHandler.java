package apps.calender;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import tools.Utilities;

import java.sql.ResultSet;
import java.util.HashMap;

import static tools.Utilities.verify;

public class NewEventHandler implements HttpHandler {
	
	
	/**
	 * @param exchange
	 *
	 * Query:
	 * 		*signature=...
	 * 		*title=...
	 * 		*date=YYYY-MM-DD
	 *
	 * 		createdBy
	 * 		assignedFamilyMembers
	 *
	 * 		Comment
	 * 		Time
	 * 		Location
	 */
	@Override
	public void handle(HttpExchange exchange) {
		HashMap<String, String> query = Utilities.queryToMap(exchange.getRequestURI().getQuery());
		if (verify(query)) {
			String title = query.get("title");
			String date = query.get("date");
			String comment = null;
			String location = null;
			String time = null;
			String createdBy = null;
			String assigned = null;
			
			ResultSet set = Utilities.connection.execute("SELECT eventTitle FROM `app:calender` WHERE eventTitle = ?", query.get("title"));
			if (Utilities.exists(set)) {
				Utilities.write("{\"error\": \"Dieser Benutzer ist schon vorhanden\"}", 409, exchange);
			} else if (Utilities.nullOrEmpty(title, date)) {
				Utilities.write("{\"error\": \"Es wurden nicht alle Felder ausgefüllt\"}", 400, exchange);
			} else {
				Utilities.connection.update("INSERT INTO `app:calender` VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?)", title, date, comment, location, time, createdBy, assigned);
				Utilities.write("{\"result\": \"Das Event " + title + " wird erstellt\", \"response\": true}", 201, exchange);
			}
		} else {
			Utilities.unauthorized(exchange);
		}
	}
}

/*
    After you have read my code I recommend you to look for help,
    here are numbers of Suicide-Prevention Hotlines:
        -> Germany: 0800 1110111
        -> USA:     1-800-273-8255
*/