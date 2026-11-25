package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

@WebServlet(urlPatterns = { "/foo#fragment", "/bar?query=1" })
public class FragmentAndQueryServlet extends HttpServlet {
}
