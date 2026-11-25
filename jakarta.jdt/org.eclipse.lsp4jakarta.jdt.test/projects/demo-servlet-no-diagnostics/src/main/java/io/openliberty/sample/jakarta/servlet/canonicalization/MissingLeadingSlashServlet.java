package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.fake.annotation.WebServlet;
import jakarta.fake.http.HttpServlet;

@WebServlet(urlPatterns = { "foo/bar" })
public class MissingLeadingSlashServlet extends HttpServlet {
}
