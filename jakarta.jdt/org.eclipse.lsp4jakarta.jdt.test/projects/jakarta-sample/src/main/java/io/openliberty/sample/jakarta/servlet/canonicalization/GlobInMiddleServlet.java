package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

@WebServlet(urlPatterns = "/**/x")
public class GlobInMiddleServlet extends HttpServlet {
}
