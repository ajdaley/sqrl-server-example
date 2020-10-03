package com.github.sqrlserverjava.example.controller;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;

import javax.servlet.RequestDispatcher;
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

import com.github.sqrlserverjava.AuthPageData;
import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlServerOperations;
import com.github.sqrlserverjava.backchannel.LoggingUtil;
import com.github.sqrlserverjava.enums.SqrlAuthenticationStatus;
import com.github.sqrlserverjava.exception.SqrlException;
import com.github.sqrlserverjava.persistence.SqrlCorrelator;
import com.github.sqrlserverjava.util.SqrlUtil;

import com.github.sqrlserverjava.example.Constants;
import com.github.sqrlserverjava.example.ErrorId;
import com.github.sqrlserverjava.example.Util;
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
public class SqrlController {

    private static final long serialVersionUID = 3182250009216737995L;

    private static SqrlConfig sqrlConfig;
    private static SqrlServerOperations sqrlServerOperations;

    private static final Logger logger = LoggerFactory.getLogger(SqrlController.class);

    static {
        initSqrlConfig();
        sqrlServerOperations = new SqrlServerOperations(sqrlConfig);
    }

    private static void initSqrlConfig() {
        sqrlConfig = new SqrlConfig();
        sqrlConfig.setBackchannelServletPath("sqrlbc");
        sqrlConfig.setAesKeyBase64("DhMncY4ErDcLRfwfyeN02Q==");
        sqrlConfig.setCpsCancelUri("login?error=8");
        sqrlConfig.setSecureRandom(new SecureRandom());
    }


    @RequestMapping(value = {"/login"}, method = RequestMethod.GET)
    public void login(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, SqrlException {
        try {
            displayLoginPage(req, resp);
        } catch (final RuntimeException | ServletException e) {
            redirectToLoginPageWithError(resp, ErrorId.SYSTEM_ERROR);
            LoggingUtil.cleanup();
        }
    }

    private void displayLoginPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute(Constants.JSP_SUBTITLE, "Login Page");
        // Default action, show the login page with a new SQRL QR code
        try {
            final AuthPageData pageData = sqrlServerOperations.browserFacingOperations()
                    .prepareSqrlAuthPageData(request, response, 150);
            final ByteArrayOutputStream baos = pageData.getQrCodeOutputStream();
            baos.flush();
            final byte[] imageInByteArray = baos.toByteArray();
            baos.close();
            // Since this is being passed to the browser, we use regular Base64 encoding, NOT SQRL specific
            // Base64URL encoding
            final String b64 = new StringBuilder("data:image/").append(pageData.getHtmlFileType(sqrlConfig))
                    .append(";base64, ").append(Base64.getEncoder().encodeToString(imageInByteArray)).toString();
            // TODO_DOC add doc FAQ link
            final int pageRefreshSeconds = sqrlConfig.getNutValidityInSeconds() / 2;
            request.setAttribute(Constants.JSP_PAGE_REFRESH_SECONDS, Integer.toString(pageRefreshSeconds));
            request.setAttribute("sqrlqr64", b64);
            final String sqrlUrl = pageData.getUrl().toString();
            request.setAttribute("sqrlurl", sqrlUrl);
            request.setAttribute("cpsEnabled", Boolean.toString(sqrlConfig.isEnableCps()));
            request.setAttribute("cpsNotEnabled", Boolean.toString(!sqrlConfig.isEnableCps()));
            // The url that will get sent to the SQRL client via CPS must include a cancel page (can) if case of failure
            final String sqrlurlWithCan = sqrlUrl;
            request.setAttribute("sqrlurlwithcan64", SqrlUtil.sqrlBase64UrlEncode(sqrlurlWithCan));
            request.setAttribute("sqrlqrdesc", "Scan with mobile SQRL app");
            request.setAttribute("correlator", pageData.getCorrelator());
            checkForErrorState(request, response);
        } catch (final Throwable e) { // need to catch everything, NoClassDefError etc so we don't end up looping
            displayErrorAndKillSession(request, "Rendering error", true);
        }
//        RequestDispatcher rd = request.getRequestDispatcher("/app");
        RequestDispatcher rd = request.getRequestDispatcher("WEB-INF/jsp/login.jsp");
        rd.forward(request, response);
    }

    private void checkForErrorState(final javax.servlet.http.HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException, SqrlException {
        final String errorParam = request.getParameter("error");
        if (Util.isBlank(errorParam)) {
            return;
        }
        final ErrorId errorId = ErrorId.lookup(errorParam);
        // If we have access to the correlator, append the first 5 chars to the message in case it gets reported
        final String correlatorString = sqrlServerOperations.extractSqrlCorrelatorStringFromRequestCookie(request);
        final String errorMessage = errorId.buildErrorMessage(correlatorString);

        displayErrorAndKillSession(request, errorMessage, errorId.isDisplayInRed());
    }

    private void displayErrorAndKillSession(final javax.servlet.http.HttpServletRequest request, final String errorText,
            final boolean displayInRed) {
        // Set it so it gets displayed
        String content = errorText;
        if (displayInRed) {
            content = Util.wrapErrorInRed(errorText);
        }
        request.setAttribute(Constants.JSP_SUBTITLE, content);
        // Since we are in an error state, kill the session
        final HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public String returnError() {
        return "Login Error!";
    }

//    @RequestMapping(value = "/sqrllogin", method = RequestMethod.GET)
//    public @ResponseBody
//    void getSqrlLogin(HttpServletRequest req, HttpServletResponse resp)
//            throws IOException, SqrlException {
//        doSqrlLogin(req, resp);
//    }

    public void doSqrlLogin(HttpServletRequest request, HttpServletResponse response) throws IOException, SqrlException {
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
            } else if (!authComplete) {
                System.out.println("Error processing login: SQRL auth incomplete");
            }
            redirectToLoginPageWithError(response, ErrorId.ERROR_SQRL_INTERNAL);
        } catch (final RuntimeException | SQLException | SqrlException | ServletException e) {
            System.out.println("Error processing username/password login");
            e.printStackTrace();
            redirectToLoginPageWithError(response, ErrorId.SYSTEM_ERROR);
        }
    }

    private boolean isSqrlWebRefreshAuthComplete(final javax.servlet.http.HttpServletRequest request, final HttpServletResponse response)
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
            redirectToLoginPageWithError(response, ErrorId.ERROR_SQRL_INTERNAL);
            return true;
        } else if (authStatus.isAuthComplete()) {
            return false;
            //return completeSqrlAuthentication(sqrlCorrelator, request, response);
        }
        return false;
    }

    public static void redirectToLoginPageWithError(final HttpServletResponse response, ErrorId errorId) {
        if (errorId == null) {
            errorId = ErrorId.GENERIC;
        }
        response.setHeader("Location", "login?error=" + errorId.getId());
        response.setStatus(302);
    }

    @RequestMapping(value = "/app", method = RequestMethod.GET)
    public @ResponseBody void app(HttpServletRequest req, HttpServletResponse resp) {
        logger.info(SqrlUtil.logEnterServlet(req));
        try {
            AppUser user = null;
            if (req.getSession(false) != null) {
                user = (AppUser) req.getSession(false).getAttribute(Constants.SESSION_NATIVE_APP_USER);
            }
            if (user == null || Util.isBlank(user.getGiven_Name()) || Util.isBlank(user.getWelcome_Phrase())) {
                logger.error("user is not in session, redirecting to login page");
                redirectToLoginPageWithError(resp, ErrorId.ATTRIBUTES_NOT_FOUND);
                return;
            }

            String accountType = "Username/password only";
            if (user.getUsername() == null) {
                accountType = "SQRL only";
            } else if (sqrlServerOperations.fetchSqrlIdentityByUserXref(Long.toString(user.getId())) != null) {
                accountType = "Both SQRL and username/password";
            }
            final HttpSession session = req.getSession(false);
            session.setAttribute("givenname", user.getGiven_Name());
            session.setAttribute("phrase", user.getWelcome_Phrase());
            session.setAttribute("accounttype", accountType);
            req.getRequestDispatcher("WEB-INF/jsp/app.jsp").forward(req, resp);
        } catch (final RuntimeException | ServletException | IOException e) {
            logger.error("Error rendering app page", e);
            redirectToLoginPageWithError(resp, ErrorId.SYSTEM_ERROR);
        }
    }

    @RequestMapping(value = "/jsp", method = RequestMethod.GET)
    public @ResponseBody void jsp(HttpServletRequest req, HttpServletResponse resp) {
        return;
    }
}
