/*
 * Copyright 2017 dbeer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.copperarrow;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wicket.authroles.authentication.AuthenticatedWebSession;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

/**
 * @author dbeer
 */
public class LoginPage extends WebPage {

    private static Logger logger = LogManager.getLogger(LoginPage.class);

    public LoginPage(PageParameters parameters) {
        super(parameters);
        add(new LoginForm("loginForm"));
    }

    private class LoginForm extends Form<Void> {
        private transient RequestCache requestCache = new HttpSessionRequestCache();

        private String username;

        private String password;

        public LoginForm(String id) {
            super(id);
            setModel(new CompoundPropertyModel(this));
            add(new RequiredTextField<>("username"));
            add(new PasswordTextField("password"));
            add(new FeedbackPanel("feedback"));
        }

        @Override
        protected void onSubmit() {
            HttpServletRequest servletRequest = (HttpServletRequest) RequestCycle.get().getRequest().getContainerRequest();
            String originalUrl = getOriginalUrl(servletRequest.getSession());
            AuthenticatedWebSession session = AuthenticatedWebSession.get();
            try {
                boolean signin = session.signIn(username, password);
                if (signin) {
                    if (originalUrl != null) {
                        logger.info(String.format("redirecting to %s", originalUrl));
                        throw new RedirectToUrlException(originalUrl);
                    } else {
                        logger.info("redirecting to home page");
                        setResponsePage(getApplication().getHomePage());
                    }
                }
            } catch (AuthenticationException e) {
                error(e.getMessage());
            }
        }


        /**
         * Returns the URL the user accessed before he was redirected to the login page. This URL has been stored in the session by spring
         * security.
         *
         * @return the original URL the user accessed or null if no URL has been stored in the session.
         */
        private String getOriginalUrl(HttpSession session) {
            // TODO: The following session attribute seems to be null the very first time a user accesses a secured page. Find out why
            // spring security doesn't set this parameter the very first time.
            SavedRequest savedRequest = (SavedRequest) session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
            if (savedRequest != null) {
                return savedRequest.getRedirectUrl();
            } else {
                return null;
            }
        }
    }

}
