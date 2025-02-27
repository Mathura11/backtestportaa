package org.patriques;

import org.patriques.input.ApiParameter;
import org.patriques.input.ApiParameterBuilder;
import org.patriques.output.AlphaVantageException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Connection to Alpha Vantage API.
 *
 * @see ApiConnector
 */
public class AlphaVantageConnector implements ApiConnector {
  private static final String BASE_URL = "https://www.alphavantage.co/query?";
  private final String apiKey;
  private final int timeOut;
  private int connCount;


  /**
   * Creates an AlphaVantageConnector.
   *
   * @param apiKey the secret key to access the api.
   * @param timeOut the timeout for when reading the connection should give up.
   */
  public AlphaVantageConnector(String apiKey, int timeOut) {
    this.apiKey = apiKey;
    this.timeOut = timeOut;
    this.connCount = 0;
  }

  public void setSSLConfiguration() {
	  try {
		  TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() 
		  {
          public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
          public void checkClientTrusted(X509Certificate[] certs, String authType) { }
          public void checkServerTrusted(X509Certificate[] certs, String authType) { }

		  } };

		  SSLContext sc = SSLContext.getInstance("SSL");
		  sc.init(null, trustAllCerts, new java.security.SecureRandom());
		  HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		  // Create all-trusting host name verifier
		  HostnameVerifier allHostsValid = new HostnameVerifier() {
			  public boolean verify(String hostname, SSLSession session) { return true; }
		  };
		  // Install the all-trusting host verifier
		  HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	  }
	  catch (NoSuchAlgorithmException e) {
		  throw new AlphaVantageException("failure sending request, NoSuchAlgorithmException error", e);
	  } catch (KeyManagementException e) {
		throw new AlphaVantageException("failure sending request, KeyManagementException error", e);
	  }
  }
  
  @Override
  public String getRequest(ApiParameter... apiParameters) {
    String params = getParameters(apiParameters);
    try {
      URL request = new URL(BASE_URL + params);
      
      setSSLConfiguration();
      
      URLConnection connection = request.openConnection();
      connection.setConnectTimeout(timeOut);
      connection.setReadTimeout(timeOut);

      InputStreamReader inputStream = new InputStreamReader(connection.getInputStream(), "UTF-8");
      BufferedReader bufferedReader = new BufferedReader(inputStream);
      StringBuilder responseBuilder = new StringBuilder();

      String line;
      while ((line = bufferedReader.readLine()) != null) {
        responseBuilder.append(line);
      }
      bufferedReader.close();
      return responseBuilder.toString();
    } catch (IOException e) {
      throw new AlphaVantageException("failure sending request", e);
    }
  }

  /**
   * Builds up the url query from the api parameters used to append to the base url.
   *
   * @param apiParameters the api parameters used in the query
   * @return the query string to use in the url
   */
  private String getParameters(ApiParameter... apiParameters) {
    ApiParameterBuilder urlBuilder = new ApiParameterBuilder();
    for (ApiParameter parameter : apiParameters) {
      urlBuilder.append(parameter);
    }
    urlBuilder.append("apikey", apiKey);
    return urlBuilder.getUrl();
  }
  
  public String getapiKey() {
	  return apiKey;
  }
  
  public void countPlus() {
	  connCount = connCount + 1;
  }
  
  public int getConnCount() {
	  return connCount;
  }
  
}
