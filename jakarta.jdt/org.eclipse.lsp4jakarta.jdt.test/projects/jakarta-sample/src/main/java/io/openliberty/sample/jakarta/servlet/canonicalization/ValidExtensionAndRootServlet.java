package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

@WebServlet(urlPatterns = { "*.jsp", "", "/valid/path", "/valid/*" })
public class ValidExtensionAndRootServlet extends HttpServlet {
}
