package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

@WebServlet(urlPatterns = { "/../foo", "/foo/..;/bar", "/foo/%2e%2e/bar", "/;/foo/bar" })
public class DotSegmentsAndTraversalServlet extends HttpServlet {
}
