package com.github.sqrlserverjava.example.controller;

import static org.atmosphere.annotation.AnnotationUtil.logger;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.github.sqrlserverjava.SqrlClientFacingOperations;
import com.github.sqrlserverjava.SqrlConfig;
import com.github.sqrlserverjava.SqrlServerOperations;
import com.github.sqrlserverjava.exception.SqrlException;
import com.github.sqrlserverjava.util.SqrlConfigHelper;

/**
 * Controller to handle SQRL client calls only. No user side html is served from here.
 *
 * @author Dave Badia
 * @author Alun Daley
 *
 */
@RestController
public class SqrlBackchannelController {

    private static final long serialVersionUID = -5867534423636409159L;
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    private static SqrlClientFacingOperations sqrlClientFacingOps = null;

    @RequestMapping(value = "/sqrlbc", method = RequestMethod.POST)
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if (!initialized.get()) {
                initialize();
            }
            sqrlClientFacingOps.handleSqrlClientRequest(request, response);
        } catch (final SqrlException e) {
            logger.error("Error occured trying to process SQRL client request", e);
        }
    }

    private synchronized void initialize() throws SqrlException {
        if (!initialized.get()) {
            final SqrlConfig sqrlConfig = SqrlConfigHelper.loadFromClasspath();
            sqrlClientFacingOps = new SqrlServerOperations(sqrlConfig).clientFacingOperations();
            initialized.set(true);
        }
    }

}
