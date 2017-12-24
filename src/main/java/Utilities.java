import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Utilities {
	
	static void log(String text) {
		
		System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + " > " + text);
	}
}
