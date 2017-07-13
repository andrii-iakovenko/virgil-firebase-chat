/**
 * 
 */
package com.virgilsecurity.firebasechat.server.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.tasks.OnSuccessListener;
import com.google.firebase.tasks.Task;
import com.google.firebase.tasks.Tasks;
import com.virgilsecurity.firebasechat.server.exception.ServiceException;
import com.virgilsecurity.firebasechat.server.model.RegistrationData;
import com.virgilsecurity.firebasechat.server.model.User;
import com.virgilsecurity.sdk.client.RequestSigner;
import com.virgilsecurity.sdk.client.VirgilClient;
import com.virgilsecurity.sdk.client.model.CardModel;
import com.virgilsecurity.sdk.client.requests.PublishCardRequest;
import com.virgilsecurity.sdk.crypto.Crypto;
import com.virgilsecurity.sdk.crypto.PrivateKey;

/**
 * @author Andrii Iakovenko
 *
 */
@RestController
public class UserController {

	private static final Logger log = LoggerFactory.getLogger(UserController.class);

	@Autowired
	private Crypto crypto;

	@Autowired
	private VirgilClient virgilClient;

	@Autowired
	private PrivateKey appKey;

	@Value("#{systemProperties['appId']}")
	private String appId;

	private Map<String, User> users;

	public UserController() {
		users = new HashMap<String, User>();
	}

	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public String login(@RequestParam("email") String email, @RequestParam("password") String password) {
		User user = users.get(email);
		if (user == null || !password.equals(user.getPassword())) {
			throw new ServiceException(001001, "Login failed");
		}

		return authenticateWithFirebase(email);
	}

	@RequestMapping(value = "/signup", method = RequestMethod.POST)
	public RegistrationData signup(@RequestParam("email") String email, @RequestParam("password") String password,
			@RequestBody String stringifiedPublishCardRequest) {

		if (users.containsKey(email)) {
			throw new ServiceException(002001, "User is already registered");
		}

		PublishCardRequest publishCardRequest = new PublishCardRequest(stringifiedPublishCardRequest);
		// Register Virgil Card for new user
		CardModel card = registerVirgilCard(publishCardRequest);

		users.put(email, new User(card.getId(), email, password));

		String customToken = authenticateWithFirebase(email);

		return new RegistrationData(customToken, card.getId());
	}

	@RequestMapping("/users")
	public List<User> getUsers() {
		return new ArrayList<User>(users.values());
	}

	@ExceptionHandler(value = Exception.class)
	public ResponseEntity<String> exceptionHandler(HttpServletRequest req, Exception e) {
		log.debug("Request: " + req.getRequestURL() + " raised " + e);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
	}

	/**
	 * Authenticate user by email with Firebase Auth.
	 * 
	 * @param email
	 *            The user's email.
	 * @return The custom token.
	 */
	private String authenticateWithFirebase(String email) {
		final Task<String> task = FirebaseAuth.getInstance().createCustomToken(email)
				.addOnSuccessListener(new OnSuccessListener<String>() {

					public void onSuccess(String customToken) {
						log.debug("Generated custom token: {}", customToken);
					}
				});
		try {
			Tasks.await(task);
		} catch (ExecutionException | InterruptedException e) {
			throw new ServiceException(003001, "Firebase authentication failed");
		}

		return task.getResult();
	}

	/**
	 * Register new Virgil Card.
	 * 
	 * @param publishCardRequest
	 * @return The created Virgil Card.
	 */
	private CardModel registerVirgilCard(PublishCardRequest publishCardRequest) {
		RequestSigner requestSigner = new RequestSigner(crypto);
		requestSigner.authoritySign(publishCardRequest, appId, appKey);

		/** Publish a Virgil Card */
		CardModel card = virgilClient.publishCard(publishCardRequest);
		return card;
	}
}
