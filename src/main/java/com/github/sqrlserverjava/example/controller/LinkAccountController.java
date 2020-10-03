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
 * Controller to handle Link Account requests.
 *
 * @author Dave Badia
 * @author Alun Daley
 *
 */
@RestController
public class LinkAccountController {

    private static final long serialVersionUID = 5609899766821704630L;

    private static final Logger logger	= LoggerFactory.getLogger(LinkAccountController.class);
    private final SqrlServerOperations sqrlServerOperations	= new SqrlServerOperations(
            SqrlConfigHelper.loadFromClasspath());

    @RequestMapping(value = {"/linkaccount"}, method = RequestMethod.GET)
    public void login(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, SqrlException, ServletException {
        doPost(req, resp);
    }

    @RequestMapping(value = {"/linkaccount"}, method = RequestMethod.POST)
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        logger.info(SqrlUtil.logEnterServlet(request));
        request.setAttribute(Constants.JSP_SUBTITLE, "Link Account Option");
        final HttpSession session = request.getSession(true);
        try {
            final AppUser user = validateRequestAndAuthenticateAppUser(request, response);
            if (user == null) {
                // validateRequestAndAuthenticateAppUser set error text and forwarded as needed
                return;
            } else {
                // We have a valid user to link
                final SqrlIdentity sqrlIdentity = (SqrlIdentity) session.getAttribute(Constants.SESSION_SQRL_IDENTITY);
                sqrlServerOperations.updateNativeUserXref(sqrlIdentity, Long.toString(user.getId()));
                session.setAttribute(Constants.SESSION_NATIVE_APP_USER, user);
                // All done, send them to the app page
                response.setHeader("Location", "app");
                response.setStatus(302); // we use 302 to make it easy to understand what the example app is doing, but
                // a real app might do a server side redirect instead
                return;
            }
        } catch (final RuntimeException | SQLException e) {
            logger.error("Error in LinkAccountServlet", e);
            SqrlController.redirectToLoginPageWithError(response, ErrorId.SYSTEM_ERROR);
        }
    }

    private AppUser validateRequestAndAuthenticateAppUser(final HttpServletRequest request,
            final HttpServletResponse response) throws SQLException, ServletException, IOException {
        if (Util.isBlank(request.getParameter("username")) && Util.isBlank(request.getParameter("password"))) {
            request.setAttribute(Constants.JSP_SUBTITLE, Util.wrapErrorInRed("Invalid username or password"));
            request.getRequestDispatcher("WEB-INF/jsp/linkaccountoption.jsp").forward(request, response);
            return null;
        }
        // Check for login credentials
        final String username = Util.sanitizeString(request.getParameter("username"), Constants.MAX_LENGTH_GIVEN_NAME);
        final String password = Util.sanitizeString(request.getParameter("password"), Constants.MAX_LENGTH_GIVEN_NAME);

        if (!password.equals(Constants.PASSWORD_FOR_ALL_USERS)) {
            request.setAttribute(Constants.JSP_SUBTITLE, Util.wrapErrorInRed("Invalid username or password"));
            request.getRequestDispatcher("WEB-INF/jsp/linkaccountoption.jsp").forward(request, response);
            return null;
        }

        final AppUser appUser = AppDatastore.getInstance().fetchUserByUsername(username);
        if(appUser == null) {
            // No such user
            request.setAttribute(Constants.JSP_SUBTITLE, Util.wrapErrorInRed("Invalid username or password"));
            request.getRequestDispatcher("WEB-INF/jsp/linkaccountoption.jsp").forward(request, response);
            return null;
        } else if (sqrlServerOperations.fetchSqrlIdentityByUserXref(Long.toString(appUser.getId())) != null) {
            request.setAttribute(Constants.JSP_SUBTITLE,
                    Util.wrapErrorInRed(" Another SQRL ID has already been linked to this username"));
            request.getRequestDispatcher("WEB-INF/jsp/linkaccountoption.jsp").forward(request, response);
            return null;
        } else {
            // All good, let the user by linked
            return appUser;
        }
    }
}
