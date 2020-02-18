package com.accela.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import com.accela.model.B2CUser;
import com.fasterxml.jackson.databind.ObjectMapper;

@PropertySource("classpath:b2cApplication.properties")
@Service
public class AzureB2CmigrationService {

	@Value("${tenant_name}")
	private String tenantName;

	@Value("${client_id}")
	private String clientId;

	@Value("${scope}")
	private String scope;

	@Value("${grant_type}")
	private String grantType;

	@Value("${b2c_tenant_url}")
	private String b2cTenantUrl;

	private static final String GRAPH_URL = "https://graph.microsoft.com/v1.0/users";

	private static final Logger LOGGER = LoggerFactory
		.getLogger(AzureB2CmigrationService.class);

	public void migrateUserFromFile(String pathToUserFile, String clientSecret) {
		String b2cBearerToken = getB2CBearerToken(clientSecret);

		List<B2CUser> b2cUsers = mapUserFromJsonFile(pathToUserFile);
		for(B2CUser b2cUser: b2cUsers) {
			migrateUser(b2cUser, b2cBearerToken);
		}
	}

	private void migrateUser(B2CUser b2cUser, String b2cBearerToken) {
		String json = mapUserToB2CSocialUser(b2cUser);
		try {
			StringEntity postingString = new StringEntity(json);
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

	private String mapUserToB2CSocialUser(B2CUser b2cUser) {
		JSONObject jsonUserTemplate = readJsonFromFile("social-user-template.json");
		String tenantIssuer = tenantName+".onmicrosoft.com";

		jsonUserTemplate.put("displayName", b2cUser.getLoginName());

		JSONArray jsonArray = jsonUserTemplate.getJSONArray("identities");
		for(int i=0; i< jsonArray.length(); i++) {
			JSONObject jsonChild = jsonArray.getJSONObject(i);
			if(jsonChild.get("signInType").equals("emailAddress")) {
				jsonChild.put("issuerAssignedId", b2cUser.getEmail());
			}
			if(jsonChild.get("signInType").equals("userName")) {
				jsonChild.put("issuerAssignedId", b2cUser.getLoginName());
			}
			jsonChild.put("issuer", tenantIssuer);
		}
		return jsonUserTemplate.toString();
	}

	private JSONObject readJsonFromFile(String path) {
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
		String jsonin = "";
		StringBuilder resultStringBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
			String line;
			while ((line = br.readLine()) != null) {
				resultStringBuilder.append(line).append("\n");
				jsonin = resultStringBuilder.toString();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return new JSONObject(jsonin);
	}

	private List<B2CUser> mapUserFromJsonFile(String path) {
		ObjectMapper mapper = new ObjectMapper();
		List<B2CUser> b2cUsers = new ArrayList<>();
		try {
			String usersAsJsonString = new String(Files.readAllBytes(Paths.get(path)), 
				StandardCharsets.UTF_8);
			b2cUsers = Arrays.asList(mapper.readValue(usersAsJsonString, B2CUser[].class));
		} catch (IOException e) {
			LOGGER.error(e.getMessage());
			e.printStackTrace();
		}
		return b2cUsers;
	}

	private String getB2CBearerToken(String clientSecret) {
		String b2cBearerToken = "";

		try {
			List<BasicNameValuePair> postParameters = new ArrayList<>();
			postParameters.add(new BasicNameValuePair("client_id", clientId));
			postParameters.add(new BasicNameValuePair("scope", scope));
			postParameters.add(new BasicNameValuePair("grant_type", grantType));
			postParameters.add(new BasicNameValuePair("client_secret", clientSecret));

			HttpPost postMethod = new HttpPost(b2cTenantUrl);
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
}
