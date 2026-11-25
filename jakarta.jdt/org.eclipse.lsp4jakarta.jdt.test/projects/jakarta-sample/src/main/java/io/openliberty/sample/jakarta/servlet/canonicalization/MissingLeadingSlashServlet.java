package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

@WebServlet(urlPatterns = { "foo/bar" })
public class MissingLeadingSlashServlet extends HttpServlet {
}
