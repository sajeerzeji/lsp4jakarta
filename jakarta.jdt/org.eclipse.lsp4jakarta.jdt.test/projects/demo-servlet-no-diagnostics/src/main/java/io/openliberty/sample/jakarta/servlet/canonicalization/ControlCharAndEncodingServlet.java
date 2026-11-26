package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.fake.annotation.WebServlet;
import jakarta.fake.http.HttpServlet;

@WebServlet(urlPatterns = { "/foo%00/bar", "/foo%XX/bar", "/foo%E2%82/bar", "/café%20x", "/foo\t/bar" })
public class ControlCharAndEncodingServlet extends HttpServlet {
}
