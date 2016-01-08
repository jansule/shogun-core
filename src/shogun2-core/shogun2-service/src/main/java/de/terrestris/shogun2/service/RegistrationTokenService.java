package de.terrestris.shogun2.service;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import de.terrestris.shogun2.model.User;
import de.terrestris.shogun2.model.token.RegistrationToken;
import de.terrestris.shogun2.util.application.Shogun2ContextUtil;
import de.terrestris.shogun2.util.mail.MailPublisher;

/**
 *
 * @author Daniel Koch
 * @author Nils Bühner
 *
 */
@Service("registrationTokenService")
public class RegistrationTokenService extends AbstractUserTokenService<RegistrationToken> {

	/**
	 * The Logger
	 */
	private static final Logger LOG =
			Logger.getLogger(RegistrationTokenService.class);

	/**
	 * The relative path for the SHOGun2 user activation interface.
	 */
	@Value("${login.accountActivationPath}")
	private String accountActivationPath;

	/**
	 * The registration token expiration time in minutes
	 */
	@Value("${login.registrationTokenExpirationTime}")
	private int registrationTokenExpirationTime;

	/**
	 *
	 */
	@Autowired
	private MailPublisher mailPublisher;

	/**
	 *
	 */
	@Autowired
	@Qualifier("registrationMailMessageTemplate")
	private SimpleMailMessage registrationMailMessageTemplate;

	/**
	 * @throws InvocationTargetException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SecurityException
	 * @throws NoSuchMethodException
	 * @throws URISyntaxException
	 * @throws UnsupportedEncodingException
	 *
	 */
	public void sendRegistrationActivationMail(HttpServletRequest request,
			User user) throws NoSuchMethodException, SecurityException,
			InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException,
			URISyntaxException, UnsupportedEncodingException {

		// generate and save the unique registration token for the user
		RegistrationToken registrationToken = getValidTokenForUser(user, registrationTokenExpirationTime);

		// create the reset-password URI that will be send to the user
		URI resetPasswordURI = createRegistrationActivationURI(request,
				registrationToken);

		// create a thread safe "copy" of the template message
		SimpleMailMessage registrationActivationMsg = new SimpleMailMessage(
				registrationMailMessageTemplate);

		// prepare a personalized mail with the given token
		final String email = user.getEmail();
		registrationActivationMsg.setTo(email);
		registrationActivationMsg.setText(
				String.format(
						registrationActivationMsg.getText(),
						UriUtils.decode(resetPasswordURI.toString(), "UTF-8")
				)
		);

		// and send the mail
		mailPublisher.sendMail(registrationActivationMsg);

	}

	/**
	 *
	 * @param request
	 * @param registrationToken
	 * @return
	 * @throws URISyntaxException
	 */
	private URI createRegistrationActivationURI(HttpServletRequest request,
			RegistrationToken registrationToken) throws URISyntaxException {

		// get the webapp URI
		URI appURI = Shogun2ContextUtil.getApplicationURIFromRequest(request);

		// build the registration activation link URI
		URI tokenURI = new URIBuilder(appURI)
				.setPath(appURI.getPath() + accountActivationPath)
				.setParameter("token", registrationToken.getToken())
				.build();

		LOG.trace("Created the following URI for account activation: " + tokenURI);

		return tokenURI;
	}

	/**
	 * This method has no {@link PreAuthorize} annotation and should only be
	 * used after an user account has been activated.
	 *
	 * @param token
	 */
	public void deleteTokenAfterActivation(RegistrationToken token) {
		dao.delete(token);
		LOG.trace("The registration token has been deleted.");
	}

	/**
	 * @return the accountActivationPath
	 */
	public String getAccountActivationPath() {
		return accountActivationPath;
	}

	/**
	 * @param accountActivationPath the accountActivationPath to set
	 */
	public void setAccountActivationPath(String accountActivationPath) {
		this.accountActivationPath = accountActivationPath;
	}

	/**
	 * @return the registrationTokenExpirationTime
	 */
	public int getRegistrationTokenExpirationTime() {
		return registrationTokenExpirationTime;
	}

	/**
	 * @param registrationTokenExpirationTime the registrationTokenExpirationTime to set
	 */
	public void setRegistrationTokenExpirationTime(
			int registrationTokenExpirationTime) {
		this.registrationTokenExpirationTime = registrationTokenExpirationTime;
	}

	/**
	 * @return the mailPublisher
	 */
	public MailPublisher getMailPublisher() {
		return mailPublisher;
	}

	/**
	 * @param mailPublisher the mailPublisher to set
	 */
	public void setMailPublisher(MailPublisher mailPublisher) {
		this.mailPublisher = mailPublisher;
	}

	/**
	 * @return the registrationMailMessageTemplate
	 */
	public SimpleMailMessage getRegistrationMailMessageTemplate() {
		return registrationMailMessageTemplate;
	}

	/**
	 * @param registrationMailMessageTemplate the registrationMailMessageTemplate to set
	 */
	public void setRegistrationMailMessageTemplate(
			SimpleMailMessage registrationMailMessageTemplate) {
		this.registrationMailMessageTemplate = registrationMailMessageTemplate;
	}

}
