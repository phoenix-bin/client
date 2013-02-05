import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

public class generatedata {

	private static final String FILENAME = "data.csv";
	private static Random r = new Random();
	public static void main(String[] args) throws FileNotFoundException, IOException {
		String[] host = {"NA","CS","EU"};
		String[] domain = {"Salesforce.com","Apple.com","Google.com"};
		String[] feature = {"Login","Report","Dashboard"};
		Calendar now = GregorianCalendar.getInstance();
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		int rowCount = Integer.parseInt(args[0]);
		for (int i=0; i<rowCount; i++) {
			now.add(Calendar.SECOND, 1);
			getFileOutputStream()
					.write((	
							host[r.nextInt(3)] + "," + 
					        domain[r.nextInt(3)] + "," + 
					        feature[r.nextInt(3)] + "," + 
					        sdf.format(now.getTime()) + "," + 
					        r.nextInt(500) + "," + 
					        r.nextInt(2000)+"," + 
					        r.nextInt(10000) + 
					        "\n")
							.getBytes());
		}
		
		getFileOutputStream().close();
		fostream = null;
		
	}

	private static FileOutputStream fostream = null;
	private static FileOutputStream getFileOutputStream() throws FileNotFoundException {
		if (fostream == null) {
			fostream = new FileOutputStream(FILENAME);
		}

		return fostream;
	}

}
