package com.accela;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.accela.model.B2CUser;
import com.accela.service.AzureB2CmigrationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
public class AzureB2CMigrationApplication implements CommandLineRunner {

	@Autowired
	private AzureB2CmigrationService azureB2CmigrationService;

	private static final Logger LOGGER = LoggerFactory
		.getLogger(AzureB2CMigrationApplication.class);

	private final String filePath = "C:\\Users\\sdornan\\Documents\\workspace-sts-3.9.7.RELEASE\\AzureB2CMigration\\src\\main\\resources\\CivicUser.json";

	public static void main(String[] args) {
		LOGGER.info("STARTING THE APPLICATION");
		SpringApplication.run(AzureB2CMigrationApplication.class, args);
		LOGGER.info("APPLICATION FINISHED");
	}

	@Override
	public void run(String... args) {
		LOGGER.info("User Migration initiated... \n\n");
		//createUser();
		azureB2CmigrationService.migrateUserFromFile(filePath);
	}

	private void createUser() {
		ObjectMapper mapper = new ObjectMapper();
		String usersJsonString = readJsonFile();
		try {
			List<B2CUser> list = Arrays.asList(mapper.readValue(usersJsonString, B2CUser[].class));
			migrateUser(list);
		} catch (JsonProcessingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	//---------------------------------------------------------------------------------------------------------------------------
	private String getBearerToken() {
		//Literals --move to prop file...
		String path = "https://login.microsoftonline.com/e9fef58a-e29f-4b41-ae71-fe8ec078462d/oauth2/v2.0/token";

		String clientId ="20d010b8-d0c8-4a7c-b2ef-3c6017869b9b";
		String clientSecret = "";
		try {
			clientSecret = URLEncoder.encode("2VRPJCSXWc1rZWdNntrsjsY0m5+J5TUW5N+EBBkFCwc=", StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		String scope ="https://graph.microsoft.com/.default";
		String grantType ="client_credentials";
		String token = "";
		
		String params = 
				"client_id=" + clientId + 
				"&client_secret=" + clientSecret+
				"&grant_type=" + grantType +
				"&scope=" + scope;
		byte[] data = params.getBytes(StandardCharsets.UTF_8);

		//Create client...
		URL url;
		try {
		/*
			url = new URL(path);
			HttpURLConnection conn = (HttpURLConnection) 
					url.openConnection();
			
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			try(OutputStream outStream = conn.getOutputStream();) {
				outStream.write(data);
				BufferedReader br = null;
				if (conn.getResponseCode() != 200) {
					br = new BufferedReader(new InputStreamReader((conn.getErrorStream())));
				} else {
					br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
				}
				JSONObject myObject = new JSONObject(br.readLine());
				if (myObject.has("access_token")) {
					token = myObject.getString("access_token");
					LOGGER.info("========== getBearToken========"+token);
				} else {
					LOGGER.error(myObject.toString());
				}
				
				br.close();
			}
		*/
			CloseableHttpClient client = null;
			client = HttpClients.createDefault();
			List<BasicNameValuePair> postParameters = new ArrayList<>();
			HttpPost postMethod = new HttpPost(path);
			postMethod.setHeader("Content-Type", "application/x-www-form-urlencoded");

			postParameters.add(new BasicNameValuePair("client_id", clientId));
			postParameters.add(new BasicNameValuePair("client_secret", "2VRPJCSXWc1rZWdNntrsjsY0m5+J5TUW5N+EBBkFCwc="));
			postParameters.add(new BasicNameValuePair("scope", scope));
			postParameters.add(new BasicNameValuePair("grant_type", grantType));
			postMethod.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));

			CloseableHttpResponse response = client.execute(postMethod);
			int statusCode = response.getStatusLine().getStatusCode();
			LOGGER.info("Response Code :{}", statusCode);

			if (statusCode == 200) {
				String responseBody = EntityUtils.toString(response.getEntity());
				JSONObject responseObj = new JSONObject(responseBody);
				token = responseObj.getString("access_token");
				LOGGER.info("========== getBearToken========"+token);
				
			} else {
				String responseBody = EntityUtils.toString(response.getEntity());
				LOGGER.info("responseBody ===>> "+ responseBody.toString());
				/*
				JSONObject responseObj = new JSONObject(responseBody);
				String errorMessage = responseObj.getString("message");
				LOGGER.info("response object : {}", responseObj);
				LOGGER.info("errorMessage "+errorMessage);
				*/
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return token;
	}

	private void migrateUser(List<B2CUser> list) {
		final String bearerToken = getBearerToken();
		CloseableHttpClient client = null;
		//JSONObject jsonRequest = new JSONObject(list.get(0));
		String graphUrl = "https://graph.microsoft.com/v1.0/users";
		String displayName = list.get(1).getLoginName();
		String userPrincipalName = list.get(1).getEmail();

		//Set user name and email
		String json= "";
		
		json= "{ \"accountEnabled\": true, "
						+ "\"displayName\": \""+displayName+"\", "
						+ "\"mailNickname\": \"testNickname7\", "
						+ "\"userPrincipalName\": \""+StringUtils.substringBefore(userPrincipalName, "@")+"@dublindevb2c.onmicrosoft.com\", "
						+ "\"passwordProfile\" : { "
							+ "\"forceChangePasswordNextSignIn\": true, "
							+ "\"password\": \"Password123!\""
						+ " } "
					+ "}";
		try {
			StringEntity postingString = new StringEntity(json);
			LOGGER.info("json : {}", json);
			client = HttpClients.createDefault();
			HttpPost postMethod = new HttpPost(graphUrl);
			postMethod.setHeader("Content-Type", "application/json");
			postMethod.setHeader("Authorization", "Bearer " + bearerToken);
			LOGGER.info("Set Authorization: {}", bearerToken);
			postMethod.setEntity(postingString);
			CloseableHttpResponse response = client.execute(postMethod);
			int statusCode = response.getStatusLine().getStatusCode();
			LOGGER.info("Response Code :{}", statusCode);

			//Debug
			//String responseBody = EntityUtils.toString(response.getEntity());
			//JSONObject responseObj = new JSONObject(responseBody);
			//String errorMessage = responseObj.getJSONObject("error").getString("message");
			//LOGGER.info("response object : {}", responseObj);
			//LOGGER.info("errorMessage "+errorMessage);

		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOGGER.info(e.getMessage());
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOGGER.info(e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOGGER.info(e.getMessage());
		}
	}

	private String readJsonFile() {
		String text = "";
		String path = "C:\\Users\\sdornan\\Documents\\workspace-sts-3.9.7.RELEASE\\AzureB2CMigration\\src\\main\\resources\\CivicUser.json";
		try {
			text = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
			LOGGER.info("JsonFile -------------->>>> "+text);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return text;
	}

}
