package hello.suribot.abstracts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

/**
 * Classe factorisant les méthodes "send" sur le réseau.
 * L'utilisation de ces méthodes se fait en étandant ces méthodes.
 */
public abstract class AbstractHttpSender {

	/**
	 * Send GET request and return response body
	 * @param url
	 * @return
	 * @throws IOException 
	 * @throws Exception
	 */
	protected String sendGet(String url) throws IOException {

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		return response.toString();
	}

	/**
	 * Send POST request and return response body
	 * @param url
	 * @param text
	 * @param requestPropertyKey
	 * @param requestProperty
	 * @param params
	 * @return
	 * @throws Exception
	 */
	protected String sendPost(String url, String text, String requestPropertyKey, 
			String requestProperty, String...params) throws Exception {
		if(url==null || url.isEmpty()) return null;
		
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("POST");
		con.setDoOutput(true);
		if(requestPropertyKey!=null && requestProperty!=null && 
				!requestPropertyKey.isEmpty() && !requestProperty.isEmpty()){
			con.setRequestProperty(requestPropertyKey, requestProperty);
		}
		OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
		if(text!=null && !text.isEmpty()) out.write("text="+text);
		if(params != null && params.length!=0){
			for(String str : params){
				if(str!=null && !str.isEmpty()) out.write("&"+str);
			}
		}
		out.close();

		String response = null;
		int HttpResult = con.getResponseCode();  
		if(HttpResult == HttpURLConnection.HTTP_OK){  
			BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(),"utf-8"));  
			String line = null;  
			StringBuilder sb = new StringBuilder();
			while ((line = br.readLine()) != null) {  
				sb.append(line + "\n");  
			}  
			br.close();  

			response = sb.toString();

		}else{  
			//TODO: Log system ?
			System.out.println("sendPost : "+con.getResponseMessage());  
		}  

		return response;
	}

	/**
	 * Send POST request
	 * @param url
	 * @param text
	 * @throws Exception
	 */
	protected void sendPost(String url, JSONObject text) throws Exception {
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("POST");
		con.setDoOutput(true);
		con.setRequestProperty("Content-Type", "application/json");
		OutputStreamWriter out = new  OutputStreamWriter(con.getOutputStream());
		out.write(text.toString());
		out.flush();
		out.close();
		con.getResponseCode(); // send the request
	}

}
