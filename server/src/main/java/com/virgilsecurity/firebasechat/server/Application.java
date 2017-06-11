package com.virgilsecurity.firebasechat.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseCredentials;
import com.virgilsecurity.sdk.client.VirgilClient;
import com.virgilsecurity.sdk.client.utils.ConvertionUtils;
import com.virgilsecurity.sdk.crypto.Crypto;
import com.virgilsecurity.sdk.crypto.PrivateKey;
import com.virgilsecurity.sdk.crypto.VirgilCrypto;
import com.virgilsecurity.sdk.crypto.exception.CryptoException;

@SpringBootApplication
public class Application {
	private static final Logger log = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		initFirebase();
		SpringApplication.run(Application.class, args);
	}

	private static void initFirebase() {
		String serviceAccountKeyPath = System.getProperty("serviceAccountKey");
		if (StringUtils.isEmpty(serviceAccountKeyPath)) {
			throw new RuntimeException("Service account key is not defined. Use `serviceAccountKey` parameter");
		}
		String databaseUrl = System.getProperty("databaseUrl");
		if (StringUtils.isEmpty(databaseUrl)) {
			throw new RuntimeException("Firebase database URL is not defined. Use `databaseUrl` parameter");
		}
		try {
			FileInputStream serviceAccount = new FileInputStream(serviceAccountKeyPath);
			FirebaseOptions options = new FirebaseOptions.Builder()
					.setCredential(FirebaseCredentials.fromCertificate(serviceAccount)).setDatabaseUrl(databaseUrl)
					.build();

			FirebaseApp.initializeApp(options);
		} catch (Exception e) {
			log.error("Firebase Admin SDK is not initalized", e);
		}
	}

	@Bean
	public Crypto crypto() {
		return new VirgilCrypto();
	}

	@Bean
	@Autowired
	public PrivateKey appKey(@Value("#{systemProperties['appKey']}") String appKey, @Value("#{systemProperties['appKeyFile']}") String appKeyFileName,
			@Value("#{systemProperties['appKeyPwd']}") String appKeyPwd, Crypto crypto) {
		byte[] keyData = null;
		if (!StringUtils.isEmpty(appKey)) {
			keyData = ConvertionUtils.base64ToBytes(appKey);
		} else if (!StringUtils.isEmpty(appKeyFileName)) {
			File file = new File(appKeyFileName);
			try (FileInputStream fis = new FileInputStream(file)) {
				keyData = new byte[(int) file.length()];
				fis.read(keyData);
			} catch (FileNotFoundException e) {
				throw new RuntimeException("Application key file not found");
			} catch (IOException e) {
				throw new RuntimeException("Application key file read error");
			}
		} else {
			throw new RuntimeException("Application key is not defined");
		}
		if (StringUtils.isEmpty(appKeyPwd)) {
			throw new RuntimeException("Application key password is not defined");
		}
		try {
			PrivateKey key = crypto.importPrivateKey(keyData, appKeyPwd);
			return key;
		} catch (CryptoException e) {
			throw new RuntimeException("Private key importing error: " + e.getMessage());
		}
	}

	@Bean
	public VirgilClient virgilClient(@Value("#{systemProperties['accessToken']}") String accessToken) {
		VirgilClient client = new VirgilClient(accessToken);
		return client;
	}

}