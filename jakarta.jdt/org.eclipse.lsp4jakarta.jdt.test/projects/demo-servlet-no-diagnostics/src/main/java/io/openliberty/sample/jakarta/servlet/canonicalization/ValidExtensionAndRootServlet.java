package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.fake.annotation.WebServlet;
import jakarta.fake.http.HttpServlet;

@WebServlet(urlPatterns = { "*.jsp", "", "/valid/path", "/valid/*" })
public class ValidExtensionAndRootServlet extends HttpServlet {
}
