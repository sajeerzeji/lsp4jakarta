package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.fake.annotation.WebServlet;
import jakarta.fake.http.HttpServlet;

@WebServlet(urlPatterns = { "/../foo", "/foo/..;/bar", "/foo/%2e%2e/bar", "/;/foo/bar" })
public class DotSegmentsAndTraversalServlet extends HttpServlet {
}
