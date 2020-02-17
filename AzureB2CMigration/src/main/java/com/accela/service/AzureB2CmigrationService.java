package com.accela.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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

	private static final String GRAPH_URL = "https://graph.microsoft.com/v1.0/users";

	public void migrateUserFromFile(String pathToUserFile, String pathToB2CApplicationProp, String clientSecret) {
		LOGGER.info("AT migrateUserFromFile ");
		try {
			String b2cBearerToken = getB2CBearerToken(getB2CApplicationProp(pathToB2CApplicationProp), clientSecret);
			List<B2CUser> b2cUsers = mapUserFromJsonFile(pathToUserFile);
			for(B2CUser b2cUser: b2cUsers) {
				migrateUser(b2cUser, b2cBearerToken);
			}
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.info(e.getMessage());
		}
	}

	private void migrateUser(B2CUser b2cUser, String b2cBearerToken) {
		String json= "{ \"accountEnabled\": true, "
						+ "\"displayName\": \""+b2cUser.getLoginName()+"\", "
						+ "\"mailNickname\": \"testNickname7\", "
						+ "\"userPrincipalName\": \""+StringUtils.substringBefore(b2cUser.getEmail(), "@")+"@dublindevb2c.onmicrosoft.com\", "
						+ "\"passwordProfile\" : { "
							+ "\"forceChangePasswordNextSignIn\": true, "
							+ "\"password\": \"Password123!\""
						+ " } "
					+ "}";
		LOGGER.info("json : {}", json);
		try {
			StringEntity postingString = new StringEntity(json);
			LOGGER.info("json : {}", json);
			CloseableHttpClient client = HttpClients.createDefault();
			HttpPost postMethod = new HttpPost(GRAPH_URL);
			postMethod.setHeader("Content-Type", "application/json");
			postMethod.setHeader("Authorization", "Bearer " + b2cBearerToken);
			LOGGER.info("Set Authorization: {}", b2cBearerToken);
			postMethod.setEntity(postingString);
			CloseableHttpResponse response = client.execute(postMethod);
			int statusCode = response.getStatusLine().getStatusCode();
			LOGGER.info("Response Code :{}", statusCode);
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

	private static String getB2CBearerToken(Properties b2cApplicationProp, String clientSecret) {
		String b2cBearerToken = "";


		try {
			List<BasicNameValuePair> postParameters = new ArrayList<>();
			postParameters.add(new BasicNameValuePair("client_id", b2cApplicationProp.getProperty("client_id")));
			postParameters.add(new BasicNameValuePair("scope", b2cApplicationProp.getProperty("scope")));
			postParameters.add(new BasicNameValuePair("grant_type", b2cApplicationProp.getProperty("grant_type")));
			postParameters.add(new BasicNameValuePair("client_secret", clientSecret));

			HttpPost postMethod = new HttpPost(b2cApplicationProp.getProperty("b2c_tenant_url"));
			postMethod.setHeader("Content-Type", "application/x-www-form-urlencoded");
			postMethod.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));

			CloseableHttpClient client = HttpClients.createDefault();
			CloseableHttpResponse response = client.execute(postMethod);

			String responseBody = EntityUtils.toString(response.getEntity());
			JSONObject responseObj = new JSONObject(responseBody);
			LOGGER.info("response object : {}", responseObj);

			int statusCode = response.getStatusLine().getStatusCode();
			LOGGER.info("Response Code :{}", statusCode);
			if (statusCode != 200) {
				responseBody = EntityUtils.toString(response.getEntity());
				LOGGER.info("errorMessage "+responseObj.getString("message"));
			}
			b2cBearerToken = responseObj.getString("access_token");
		} catch (MalformedURLException e) {
			LOGGER.error(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
			e.printStackTrace();
		}
		return b2cBearerToken;
	}

	private Properties getB2CApplicationProp(String pathToB2CApplicationProp) throws IOException {
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(pathToB2CApplicationProp);
		Properties props = new Properties();
		if (in == null) {
			throw new IOException("Stream null from "+pathToB2CApplicationProp);
		}
		props.load(in);
		return props;
	}
}
