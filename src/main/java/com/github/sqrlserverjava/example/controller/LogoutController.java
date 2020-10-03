package com.github.sqrlserverjava.example.controller;

import java.io.IOException;

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

import com.github.sqrlserverjava.BrowserFacingOperations;
import com.github.sqrlserverjava.SqrlServerOperations;
import com.github.sqrlserverjava.exception.SqrlException;
import com.github.sqrlserverjava.util.SqrlConfigHelper;

import com.github.sqrlserverjava.example.ErrorId;
import com.github.sqrlserverjava.example.Util;

/**
 * Controller to handle logout requests
 *
 * @author Dave Badia
 * @author Alun Daley
 *
 */
@RestController
public class LogoutController {

    private static final long serialVersionUID = 2107859031515432927L;
    private static final Logger logger = LoggerFactory.getLogger(LogoutController.class);
    private final BrowserFacingOperations sqrlbrowserFacingOperations = new SqrlServerOperations(
            SqrlConfigHelper.loadFromClasspath()).browserFacingOperations();

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public void logout(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, SqrlException, ServletException {
        doPost(req, resp);
    }

    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        try {
            final HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            sqrlbrowserFacingOperations.deleteSqrlAuthCookies(request, response);
            Util.deleteAllCookies(request, response);
            response.setStatus(302);
            response.setHeader("Location", "login");
        } catch (final RuntimeException e) {
            logger.error("Error in LinkAccountServlet", e);
            SqrlController.redirectToLoginPageWithError(response, ErrorId.SYSTEM_ERROR);
        }
    }
}
