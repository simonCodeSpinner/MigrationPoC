package com.accela.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
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
import org.springframework.stereotype.Service;

import com.accela.model.B2CUser;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AzureB2CmigrationService {
	
	private static final Logger LOGGER = LoggerFactory
		.getLogger(AzureB2CmigrationService.class);

	private static String b2cBearerToken = getB2CBearerToken();

	public String migrateUserFromFile(String path) {
		List<B2CUser> b2cUsers = mapUserFromJsonFile(path); 

		for(B2CUser b2cUser: b2cUsers) {
			migrateUser(b2cUser);
		}
		return null;
	}

	private void migrateUser(B2CUser b2cUser) {
		CloseableHttpClient client = null;
		String graphUrl = "https://graph.microsoft.com/v1.0/users";
		String displayName = b2cUser.getLoginName();
		String userPrincipalName = b2cUser.getEmail();

		String json= "{ \"accountEnabled\": true, "
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
			postMethod.setHeader("Authorization", "Bearer " + b2cBearerToken);
			LOGGER.info("Set Authorization: {}", b2cBearerToken);
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
			e.printStackTrace();
			LOGGER.info(e.getMessage());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			LOGGER.info(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.info(e.getMessage());
		}
	}

	private List<B2CUser> mapUserFromJsonFile(String path) {
		ObjectMapper mapper = new ObjectMapper();
		List<B2CUser> b2cUsers = new ArrayList<>();
		String usersAsJsonString = "";
		try {
			usersAsJsonString = new String(Files.readAllBytes(Paths.get(path)), 
				StandardCharsets.UTF_8);
			b2cUsers = Arrays.asList(mapper.readValue(usersAsJsonString, B2CUser[].class));
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
			e.printStackTrace();
		}
		return b2cUsers;
	}

	private static String getB2CBearerToken() {
		String b2cTenantUrl = "https://login.microsoftonline.com/e9fef58a-e29f-4b41-ae71-fe8ec078462d/oauth2/v2.0/token";
		String clientId ="20d010b8-d0c8-4a7c-b2ef-3c6017869b9b";
		String clientSecret = "2VRPJCSXWc1rZWdNntrsjsY0m5+J5TUW5N+EBBkFCwc=";
		String scope ="https://graph.microsoft.com/.default";
		String grantType ="client_credentials";
		String b2cBearerToken = "";

		try {
			CloseableHttpClient client = null;
			client = HttpClients.createDefault();
			List<BasicNameValuePair> postParameters = new ArrayList<>();
			HttpPost postMethod = new HttpPost(b2cTenantUrl);
			postMethod.setHeader("Content-Type", "application/x-www-form-urlencoded");

			postParameters.add(new BasicNameValuePair("client_id", clientId));
			postParameters.add(new BasicNameValuePair("client_secret", clientSecret));
			postParameters.add(new BasicNameValuePair("scope", scope));
			postParameters.add(new BasicNameValuePair("grant_type", grantType));
			postMethod.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));

			CloseableHttpResponse response = client.execute(postMethod);
			int statusCode = response.getStatusLine().getStatusCode();
			LOGGER.info("Response Code :{}", statusCode);

			if (statusCode == 200) {
				String responseBody = EntityUtils.toString(response.getEntity());
				JSONObject responseObj = new JSONObject(responseBody);
				b2cBearerToken = responseObj.getString("access_token");
				LOGGER.info("========== getBearToken========"+b2cBearerToken);
				
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
			LOGGER.error(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
			e.printStackTrace();
		}
		return b2cBearerToken;
	}

}
