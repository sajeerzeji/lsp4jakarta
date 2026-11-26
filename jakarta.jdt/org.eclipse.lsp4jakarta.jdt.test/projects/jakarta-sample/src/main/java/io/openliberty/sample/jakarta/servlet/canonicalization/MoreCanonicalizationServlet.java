package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

@WebServlet(urlPatterns = { "/a%09b", "/a%7Fb", "/a%", "/.;/x", "/a/../../b" })
public class MoreCanonicalizationServlet extends HttpServlet {
}
