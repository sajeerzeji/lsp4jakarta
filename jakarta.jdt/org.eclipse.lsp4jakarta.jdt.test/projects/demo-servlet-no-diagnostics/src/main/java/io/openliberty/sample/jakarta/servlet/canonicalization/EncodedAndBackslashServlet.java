package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.fake.annotation.WebServlet;
import jakarta.fake.http.HttpServlet;

@WebServlet(urlPatterns = { "/foo%2Fbar", "/foo\\bar", "/foo%5Cbar" })
public class EncodedAndBackslashServlet extends HttpServlet {
}
