package com.github.sqrlserverjava.example.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.github.sqrlserverjava.SqrlServerOperations;
import com.github.sqrlserverjava.exception.SqrlException;
import com.github.sqrlserverjava.persistence.SqrlIdentity;
import com.github.sqrlserverjava.util.SqrlConfigHelper;
import com.github.sqrlserverjava.util.SqrlUtil;

import com.github.sqrlserverjava.example.Constants;
import com.github.sqrlserverjava.example.ErrorId;
import com.github.sqrlserverjava.example.Util;
import com.github.sqrlserverjava.example.data.AppDatastore;
import com.github.sqrlserverjava.example.data.AppUser;

/**
 * Controller to handle user settings
 *
 * @author Dave Badia
 * @author Alun Daley
 *
 */
@RestController
public class UserSettingsController {

    private static final long serialVersionUID		= 7534356830225738651L;
    private static final Logger logger = LoggerFactory.getLogger(UserSettingsController.class);
    private final SqrlServerOperations sqrlServerOperations	= new SqrlServerOperations(
            SqrlConfigHelper.loadFromClasspath());

    @RequestMapping(value = "/usersettings", method = RequestMethod.GET)
    public @ResponseBody void usersettings(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, SqrlException, ServletException {
        req.getRequestDispatcher("WEB-INF/jsp/usersettings.jsp").forward(req, resp);
    }

    @RequestMapping(value = "/usersettings", method = RequestMethod.POST)
    protected void postusersettings(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        logger.info(SqrlUtil.logEnterServlet(request));
        try {
            final HttpSession session = request.getSession();
            final String givenName = Util.sanitizeString(request.getParameter("givenname"),
                    Constants.MAX_LENGTH_GIVEN_NAME);
            final String welcomePhrase = Util.sanitizeString(request.getParameter("phrase"),
                    Constants.MAX_LENGTH_WELCOME_PHRASE);

            if (Util.isBlank(givenName) || Util.isBlank(welcomePhrase)) {
                SqrlController.redirectToLoginPageWithError(response, ErrorId.MISSING_PARAM_FOR_NEW_USER);
                return;
            }

            // appUser and sqrlIdentity may or may not exist yet depending on how the user authenticated
            AppUser appUser = (AppUser) session.getAttribute(Constants.SESSION_NATIVE_APP_USER);
            final SqrlIdentity sqrlIdentity = (SqrlIdentity) session.getAttribute(Constants.SESSION_SQRL_IDENTITY);

            if (appUser == null && sqrlIdentity == null) {
                SqrlController.redirectToLoginPageWithError(response, ErrorId.INVALID_USERNAME_OR_PASSWORD);
                return;
            } else if (sqrlIdentity != null && appUser == null) {
                appUser = enrollSqrlOnlyUser(sqrlIdentity, givenName, welcomePhrase);
                session.setAttribute(Constants.SESSION_NATIVE_APP_USER, appUser);
            } else {
                if (sqrlIdentity != null && appUser != null) {
                    logger.warn("Both sqrlIdentity and appUser are non null for enrollment");
                }
                enrollUsernameOnlyUserOrModify(appUser, givenName, welcomePhrase);
            }
        } catch (final RuntimeException | SQLException e) {
            logger.error("Error processing user settings", e);
            SqrlController.redirectToLoginPageWithError(response, ErrorId.SYSTEM_ERROR);
        }
        // Send them to the app screen
        response.setHeader("Location", "app");
        response.setStatus(302);
    }

    private void enrollUsernameOnlyUserOrModify(final AppUser appUser, final String givenName,
            final String welcomePhrase) throws SQLException {
        // Username / password only user: sqrlIdentity == null && appUser != null
        // OR both sqrlIdentity and authUser exist, which shouldn't happen

        appUser.setGiven_Name(givenName);
        appUser.setWelcome_Phrase(welcomePhrase);
        AppDatastore.getInstance().updateUser(appUser);
    }

    private AppUser enrollSqrlOnlyUser(final SqrlIdentity sqrlIdentity, final String givenName,
            final String welcomePhrase) throws SQLException {
        // This is a SQRL only user so we will create a new app user with a null username (since there
        // is no username/password authentication)
        final AppUser appUser = new AppUser(givenName, welcomePhrase);
        AppDatastore.getInstance().createUser(appUser);

        // Link our sqrlIdentity to the new Appuser
        sqrlServerOperations.updateNativeUserXref(sqrlIdentity, Long.toString(appUser.getId()));

        return appUser;
    }


}
