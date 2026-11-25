package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

@WebServlet(urlPatterns = { "/foo%2Fbar", "/foo\\bar", "/foo%5Cbar" })
public class EncodedAndBackslashServlet extends HttpServlet {
}
