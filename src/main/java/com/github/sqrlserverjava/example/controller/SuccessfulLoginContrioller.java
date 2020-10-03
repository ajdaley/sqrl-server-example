package com.github.sqrlserverjava.example.controller;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlServerOperations;
import com.github.sqrlserverjava.enums.SqrlAuthenticationStatus;
import com.github.sqrlserverjava.exception.SqrlException;
import com.github.sqrlserverjava.persistence.SqrlCorrelator;
import com.github.sqrlserverjava.persistence.SqrlIdentity;
import com.github.sqrlserverjava.util.SqrlConfigHelper;

import com.github.sqrlserverjava.example.Constants;
import com.github.sqrlserverjava.example.ErrorId;
import com.github.sqrlserverjava.example.data.AppDatastore;
import com.github.sqrlserverjava.example.data.AppUser;

/**
 * Once SQRL auth is initiated, the browser polls the server to understand when SQRL auth is complete; once the browser
 * receives that message, it sends the user here so we can setup the app session based on the SQRL ID.
 * </p>
 * If this is the first time the user has authenticated via SQRL, the user will be sent to the linkaccountoption.jsp
 * where they can optionally link their SQRL account to an existing username/password account.
 * </p>
 * If the user has previously authenticated via SQRL, then the user is sent to the app page
 *
 * @author Dave Badia
 * @author Alun Daley
 *
 */
@RestController
public class SuccessfulLoginContrioller {

    private static final long serialVersionUID = 5609899766821704630L;

    private final SqrlConfig sqrlConfig = SqrlConfigHelper.loadFromClasspath();
    private final SqrlServerOperations sqrlServerOperations = new SqrlServerOperations(sqrlConfig);

    @RequestMapping(value = "/sqrllogin", method = RequestMethod.GET)
    public void successfullogin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, SqrlException, ServletException {
        doPost(req, resp);
    }

    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        try {
            // TODO: all requests that get here must contain a correlator, right? requestContainsCorrelatorCookie

            // Web polling SQRL auth (non-CPS)
            final boolean requestContainsCorrelatorCookie = sqrlServerOperations
                    .extractSqrlCorrelatorStringFromRequestCookie(request) != null;
            final boolean authComplete = isSqrlWebRefreshAuthComplete(request, response);
            if (requestContainsCorrelatorCookie && authComplete) {
                // All good, nothing else to do
                return;
            } else if (!requestContainsCorrelatorCookie) {
                System.out.println("Error processing login after SQRL auth: correlator cookie not found");
//                logger.error("Error processing login after SQRL auth: correlator cookie not found");
            } else if (!authComplete) {
                System.out.println("Error processing login: SQRL auth incomplete");
//                logger.error("Error processing login: SQRL auth incomplete");
            }
            SqrlController.redirectToLoginPageWithError(response, ErrorId.ERROR_SQRL_INTERNAL);
        } catch (final RuntimeException | SQLException | SqrlException e) {
            System.out.println("Error processing username/password login: " + e);
//            logger.error("Error processing username/password login ", e);
            SqrlController.redirectToLoginPageWithError(response, ErrorId.SYSTEM_ERROR);
        }
    }

    private boolean isSqrlWebRefreshAuthComplete(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, SQLException, SqrlException {
        final SqrlCorrelator sqrlCorrelator = sqrlServerOperations.fetchSqrlCorrelator(request);
        sqrlServerOperations.cleanSqrlAuthData(request, response);
        if (sqrlCorrelator == null) {
            return false;
        }
        final SqrlAuthenticationStatus authStatus = sqrlCorrelator.getAuthenticationStatus();
        if (authStatus.isUpdatesForThisCorrelatorComplete()) {
            // Now that we are done using the correlator, we can delete the correlator
            // note that If we didn't it would still get cleaned up later
            sqrlServerOperations.deleteSqrlCorrelator(sqrlCorrelator);
        }
        if (!authStatus.isHappyPath()) {
            SqrlController.redirectToLoginPageWithError(response, ErrorId.ERROR_SQRL_INTERNAL);
            return true;
        } else if (authStatus.isAuthComplete()) {
            return completeSqrlAuthentication(sqrlCorrelator, request, response);
        }
        return false;
    }

    private boolean completeSqrlAuthentication(final SqrlCorrelator sqrlCorrelator, final HttpServletRequest request,
            final HttpServletResponse response) throws SqrlException, ServletException, IOException {
        sqrlServerOperations.browserFacingOperations().valdateCpsParamIfNecessary(sqrlCorrelator, request);
        final SqrlIdentity sqrlIdentity = sqrlCorrelator.getAuthenticatedIdentity();
        if (sqrlIdentity == null) {
            System.out.println("Correlator status return AUTH_COMPLETE but user isn't authenticated");
//            logger.warn("Correlator status return AUTH_COMPLETE but user isn't authenticated");
            return false;
        }
        final
        HttpSession session = request.getSession(true); // TODO: shoult thsi be true?
        session.setAttribute(Constants.SESSION_SQRL_IDENTITY, sqrlIdentity);
        // The user has been SQRL authenticated, is this an existing app user?
        final String nativeUserXref = sqrlIdentity.getNativeUserXref();
        AppUser nativeAppUser = null;
        if (nativeUserXref != null) {
            nativeAppUser = AppDatastore.getInstance().fetchUserById(Long.parseLong(nativeUserXref));
        }
        final boolean existingAppUser = nativeAppUser != null;
        if (existingAppUser) {
            // The example app relies on server side session attribute to signal that the user has
            // been authenticated and to indentify the user
            session.setAttribute(Constants.SESSION_NATIVE_APP_USER, nativeAppUser);
            request.getRequestDispatcher("/app").forward(request, response);
            return true;
        } else {
            // sqrlIdentity exists but NativeAppUser doesn't. Send them to enrollment page to see if they have a
            // user name and password or are completely new
            request.setAttribute(Constants.JSP_SUBTITLE, "Link SQRL to existing username?");
            request.getRequestDispatcher("WEB-INF/jsp/linkaccountoption.jsp").forward(request, response);
            return true;
        }
    }

}
