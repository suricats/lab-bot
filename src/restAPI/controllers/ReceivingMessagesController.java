package restAPI.controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpStatus;
import org.bots4j.msbotframework.beans.ChannelAccount;
import org.bots4j.msbotframework.beans.Message;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import utils.msbotframework.HelloSuribotConnectorClient;

@RestController
public class ReceivingMessagesController {
	
	// TODO To change every hour : make a daemon to update the token, and put it in the cache
	private static final String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6Ikk2b0J3NFZ6QkhPcWxlR3JWMkFKZEE1RW1YYyIsImtpZCI6Ikk2b0J3NFZ6QkhPcWxlR3JWMkFKZEE1RW1YYyJ9.eyJhdWQiOiJodHRwczovL2dyYXBoLm1pY3Jvc29mdC5jb20iLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC83MmY5ODhiZi04NmYxLTQxYWYtOTFhYi0yZDdjZDAxMWRiNDcvIiwiaWF0IjoxNDc4MTg2OTkzLCJuYmYiOjE0NzgxODY5OTMsImV4cCI6MTQ3ODE5MDg5MywiYXBwX2Rpc3BsYXluYW1lIjoiTEFCLUJPVCIsImFwcGlkIjoiNzg2ZmJiZDQtMjZjZS00Y2I4LTg0MWYtYzE3NGIyNTJhMDRhIiwiYXBwaWRhY3IiOiIxIiwiaWRwIjoiaHR0cHM6Ly9zdHMud2luZG93cy5uZXQvNzJmOTg4YmYtODZmMS00MWFmLTkxYWItMmQ3Y2QwMTFkYjQ3LyIsInRpZCI6IjcyZjk4OGJmLTg2ZjEtNDFhZi05MWFiLTJkN2NkMDExZGI0NyIsInZlciI6IjEuMCJ9.bFJbq2fGfu5YlIiAJlSG-W5h08D6mcSyfOSGu8DoLJE4rNh6grwTLuLrPFdzLiH29T4e-B94dM-wWN_WruLhoBLQPJSKdv8w-DVA4FTM3gxAcljMC97EOXHIsN-aTR80f7bfrTm2mC-MOufvX7VS76zj6GNY7JN1-ZCYGZ7tU6M7I-PpSf_24hxODGEmCm6uustrwMuuzqhhF79J7MTnprCiOa1LycuFG-cjMCObgWOz5JmDRGnk8fQ2b5m_ii6QnxT-YBMg_n5fQQtNCdkfqI6f8Qbj_-a7MtJVTs1s5smCXOdGPE5fv9WJs2oMH4sWxHawDVMKYK2FjlRnzWY9Tg";
	
	private static final String appid = System.getenv("APPID");
	private static final String appsecret = System.getenv("APPSECRET");
	
	@RequestMapping(value ="/")
	public int receivingMessage(HttpServletRequest request){
	    
		JSONObject json = null;
	    try {
			Reader body = request.getReader();
			BufferedReader reader = new BufferedReader(body);
			StringBuilder sb = new StringBuilder();
		    int cp;
		    while ((cp = reader.read()) != -1) {
		      sb.append((char) cp);
		    }
		    json = new JSONObject(sb.toString());
		    printUserMessage(json);
		    
		    System.out.println("\n====sendResponse=====\n");
		    HelloSuribotConnectorClient client = new HelloSuribotConnectorClient(appid, appsecret, token);
		    Message message = new Message()
		            .withFrom(from())
		            .withTo(to())
		            .withText("This is a test message from Hello Suribot")
		            .withLanguage("en");

		    printEntireJSONMessage(json);
		    Message reply = client.Messages.sendMessage(message);
		    System.out.println("Response : "+reply.getText());
		    
	    } catch (JSONException e){
	    	System.out.println("No user message but a request has been received.");
	    	printEntireJSONMessage(json);
	    } catch (RuntimeException | IOException e){
	    	e.printStackTrace();
	    	return HttpStatus.SC_UNAUTHORIZED;
	    }
			
	    return HttpStatus.SC_OK;
	}
	
	private void printEntireJSONMessage(JSONObject json){
		System.out.println("Entire JSON Body : \n"+ json.toString(3) );
	}
	
	private void printUserMessage(JSONObject json) throws JSONException {
		System.out.println("User message : "+json.getString("text"));
	}
	
	public static ChannelAccount to(){
        return new ChannelAccount()
                .withName("julien.margarido")
                .withId("U2LJMA17F")
//                .withAddress("julien.margarido")
//                .withChannelId("slack")
                .withIsBot(false);
    }

    public static ChannelAccount from(){
        return new ChannelAccount()
                .withName("lab_bot")
                .withId("B2S5SJQ3Y")
//                .withAddress("lab-bot")
//                .withChannelId("slack")
                .withIsBot(true);
    }
} 
