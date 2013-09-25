package com.teg.tegcontacts;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import net.maxters.android.ntlm.NTLM;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;

public final class SPQueries {
	
	private String postResponse = "";
	private String soapBody = "";
	private Activity act;

	public enum WSOperation {
		GETLISTITEMS("GetListItems", "_vti_bin/Lists.asmx", "http://schemas.microsoft.com/sharepoint/soap/GetListItems");
		
		private String opName;
    private String soapAction;
    private String webServiceURL;
 
    private WSOperation(String opName, String webServiceURL, String soapAction) {
    	this.opName = opName;
    	this.webServiceURL = webServiceURL;
      this.soapAction = soapAction;
    }
    
    public String getOpName() {
    	return opName;
    }
    
    public String getSoapAction() {
      return soapAction;
    }
    
    public String getWebServiceURL() {
      return webServiceURL;
    }
	}
	
	static class SOAPEnvelope {
		public static String header = "";
		public static String footer = "";
		public static String payload = "";
		public static String opheader = "";
		public static String opfooter = "";
	}
	
	/**
     * Issue a SOAP request to the server.
     *
     * @param webURL server url.
     * @param operation enum.
     * @param listName Name of List or ListGUID.
     * @param viewName Name of View for List.
     * @param query Query to List (include &lt;Query&gt;&lt;/Query&gt;).
     * @param viewFields Which fields to return.
     * @param rowLimit Number of rows to return.
     * 
  	 * @throws ExecutionException 
  	 * @throws InterruptedException 
     * @throws IOException propagated from POST.
     */
	public String SPQuery(String webURL, WSOperation operation, String listName, String viewName, String query, String viewFields, int rowLimit, Activity act) {
		soapBody = "";
		this.act = act;
		
		SOAPEnvelope.header = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
				"<soap:Envelope xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'><soap:Body>";
		SOAPEnvelope.footer = "</soap:Body></soap:Envelope>";
		SOAPEnvelope.payload = "";
		SOAPEnvelope.opheader = "<" + operation.getOpName() +" xmlns=\"http://schemas.microsoft.com/sharepoint/soap/\">";
		SOAPEnvelope.opfooter = "</"+ operation.getOpName() +">";
		
		String listNameSoap     = "<listName>"+ listName +"</listName>";
		String viewNameSoap     = "<viewName>"+ viewName +"</viewName>";
		String querySoap        = "<query>"   + query    +"</query>";
		String viewFieldsSoap   = "<viewFields>" + viewFields + "</viewFields>";
		String rowLimitSoap     = "<rowLimit>"+ rowLimit +"</rowLimit>";
		String queryOptionsSoap = "<queryOptions><QueryOptions /></queryOptions>";
		
		SOAPEnvelope.payload += listNameSoap;
		SOAPEnvelope.payload += viewNameSoap;
		SOAPEnvelope.payload += querySoap;
		SOAPEnvelope.payload += viewFieldsSoap;
		SOAPEnvelope.payload += rowLimitSoap;
		SOAPEnvelope.payload += queryOptionsSoap;
		
		soapBody += SOAPEnvelope.header;
		soapBody += SOAPEnvelope.opheader;
		soapBody += SOAPEnvelope.payload;
		soapBody += SOAPEnvelope.opfooter;
		soapBody += SOAPEnvelope.footer;
		
		final String url = webURL + "/" + operation.getWebServiceURL();
		Log.i("SOAP URL", url);
		
		RunnableFuture<String> sendSoapRequest = new FutureTask<String>(new Callable<String>() {
			@Override
			public String call() throws Exception {
				try {
					return post(url);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			}
		});
		
    Thread thread =  new Thread(null, sendSoapRequest, "MagentoBackground");
    thread.start();
        
    try {
    	String result = sendSoapRequest.get();
			return result;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
        
		return "";
	}
	
  private String post(String endpoint) throws UnsupportedEncodingException {   	
      
    SharedPreferences p = act.getSharedPreferences("%VALUE%", 0);
    String username = p.getString("username", "").toLowerCase(Locale.US);
    String password = p.getString("password", "");
    String domain = p.getString("domain", "");
    
    DefaultHttpClient httpClient = new DefaultHttpClient();
    NTLM.setNTLM(httpClient, username, password, domain);
    HttpPost httpPost = new HttpPost(endpoint);
    
    httpPost.addHeader("SOAPAction", operation.getWebServiceURL());
    httpPost.setHeader("Content-Type","text/xml;charset='UTF-8'");
    
    Log.i("SOAP Body", soapBody);
    
    StringEntity entity = new StringEntity(soapBody, HTTP.UTF_8);
    entity.setContentType("text/xml");
    httpPost.setEntity(entity);
    
    /*
     * Making HTTP Request
     */
    try {
        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity respEntity = response.getEntity();

        if (respEntity != null) {
            // EntityUtils to get the response content
            postResponse = EntityUtils.toString(respEntity);
        	Log.i("SOAP Response", postResponse);
        	return postResponse;
        }
    } catch (ClientProtocolException e) {
        // writing exception to log
        e.printStackTrace();
    } catch (IOException e) {
        // writing exception to log
        e.printStackTrace();
    }
		return "";
  }
}
