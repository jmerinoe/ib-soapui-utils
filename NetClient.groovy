import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

class ServiceInfo {
    private String method;
    private String url;
    private Map<String, String> parameters;
    private Map<String, String> headers;
    private String requestBody;
    private int responseCode;
    private String responseMessage;
    private String responseBody;

    public ServiceInfo(String method, String url, Map<String, String> parameters, Map<String, String> headers, String requestBody, int responseCode,
            String responseBody, String responseMessage) {
        this.method = method;
        this.url = url;
        this.parameters = parameters;
        this.headers = headers;
        this.responseCode = responseCode;
        this.responseBody = responseBody;
        this.responseMessage = responseMessage;
        this.requestBody = requestBody;
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public String getRequestBody() {
        return requestBody;
    }
}


class NetClient {
	def CONTENTTYPE_JSON = "application/json";
	def CONTENTTYPE_FORMURLENCODED = "application/x-www-form-urlencoded";
	def METHOD_GET = "GET";
	def METHOD_PUT = "PUT";
	def METHOD_POST = "POST";
	def ENCODING = "UTF-8";
	def ACCEPT = "Accept";
	def CONTENT_TYPE = "Content-Type";
	def CONTENT_LENGTH = "Content-Length";

	def log
	def context
	def testRunner

	def NetClient(log, context, testRunner) {
		this.log = log
		this.context = context
		this.testRunner = testRunner
	}

    public ServiceInfo executeGET(String serviceURL, Map<String, String> params, Map<String, String> properties) {
        try {
            URL url = getFinalUrl(serviceURL, params);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(METHOD_GET);
            conn.setRequestProperty(ACCEPT, CONTENTTYPE_JSON);
            for (Map.Entry<String, String> property : properties.entrySet()) {
                conn.addRequestProperty(property.getKey(), property.getValue());
            }

            String responseBody = null;
            int responseCode = conn.getResponseCode();
            if (isOK(responseCode)) {
                responseBody = inputStream2String(conn.getInputStream());
            }
            String responseMessage = conn.getResponseMessage();

            conn.disconnect();

            return new ServiceInfo(METHOD_GET, String.valueOf(url), params, properties, null, responseCode, responseBody, responseMessage);
        } catch (IOException e) {
            return null;
        }
    }

    public ServiceInfo executePOSTwJson(String serviceURL, Map<String, String> params, Map<String, String> requestProperties) {
        return executePOST(serviceURL, params, requestProperties, null, CONTENTTYPE_JSON);
    }

    public ServiceInfo executePOSTwJson(String serviceURL, Map<String, String> params, Map<String, String> requestProperties, String body) {
        return executePOST(serviceURL, params, requestProperties, body, CONTENTTYPE_JSON);
    }

    public ServiceInfo executePOSTwFormUrl(String serviceURL, Map<String, String> params, Map<String, String> requestProperties) {
        return executePOST(serviceURL, params, requestProperties, null, CONTENTTYPE_FORMURLENCODED);
    }

    public ServiceInfo executePOSTwFormUrl(String serviceURL, Map<String, String> params, Map<String, String> requestProperties, String body) {
        return executePOST(serviceURL, params, requestProperties, body, CONTENTTYPE_FORMURLENCODED);
    }

    private ServiceInfo executePOST(String serviceURL, Map<String, String> params, Map<String, String> requestProperties, String body,
            String contentType) {
        return executePOSTPUT(METHOD_POST, serviceURL, params, requestProperties, body, contentType);
    }

    public ServiceInfo executePUT(String serviceURL, Map<String, String> params, Map<String, String> requestProperties) {
        return executePUT(serviceURL, params, requestProperties, null);
    }

    public ServiceInfo executePUT(String serviceURL, Map<String, String> params, Map<String, String> requestProperties, String body) {
        return executePOSTPUT(METHOD_PUT, serviceURL, params, requestProperties, body, CONTENTTYPE_JSON);
    }

    public boolean isOK(int responseCode) {
        return responseCode >= 200 && responseCode <= 299;
    }

    private URL getFinalUrl(String serviceURL, Map<String, String> params) throws MalformedURLException, UnsupportedEncodingException {
        StringBuilder urlParameters = new StringBuilder();
        boolean firstParam = true;
        for (Map.Entry<String, String> parameter : params.entrySet()) {
            if (!firstParam) {
                urlParameters.append("&");
            }
            firstParam = false;
            urlParameters.append(parameter.getKey()).append("=").append(URLEncoder.encode(parameter.getValue(), "UTF-8"));
        }

		String urlResultado = urlParameters.toString();
		return urlResultado == null || urlResultado.length() == 0 ? new URL(serviceURL) : new URL(serviceURL + "?" + urlParameters.toString());
    }

    private ServiceInfo executePOSTPUT(String method, String serviceURL, Map<String, String> params, Map<String, String> requestProperties,
            String body, String contentType) {
        try {
            URL url = getFinalUrl(serviceURL, params);
            String bodyText = body == null || body.length() == 0 ? "" : body;
            byte[] postData = bodyText.getBytes(StandardCharsets.UTF_8);
            int postDataLength = postData.length;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod(method);
            for (Map.Entry<String, String> property : requestProperties.entrySet()) {
                conn.setRequestProperty(property.getKey(), property.getValue());
            }
            conn.setRequestProperty(CONTENT_LENGTH, Integer.toString(postDataLength));
            conn.setRequestProperty(CONTENT_TYPE, contentType);
            conn.setUseCaches(false);

            DataOutputStream wr;
		  wr = new DataOutputStream(conn.getOutputStream())
            wr.write(postData);
            String responseBody = null;
            int responseCode = conn.getResponseCode();
            if (isOK(responseCode)) {
            	responseBody = inputStream2String(conn.getInputStream());
            }
            String responseMessage = conn.getResponseMessage();
            conn.disconnect();

            return new ServiceInfo(method, String.valueOf(url), params, requestProperties, body, responseCode, responseBody, responseMessage);
        } catch (Exception e) {
            return null;
        } finally{
        	wr.close();
        }
    }

	private String inputStream2String(InputStream inputStream) throws IOException {
      	InputStreamReader isReader = new InputStreamReader(inputStream);
      	BufferedReader reader = new BufferedReader(isReader);
      	StringBuffer sb = new StringBuffer();
      	String str;
      	while((str = reader.readLine())!= null){
     	    	sb.append(str);
   	   	}
      	return sb.toString();
   	}
}

NetClient initObj = context.getProperty("NetClient")
if (initObj == null) {
    initObj = new NetClient(log, context, context.getTestRunner())
    context.setProperty(initObj.getClass().getName(), initObj)
}