package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.fake.annotation.WebServlet;
import jakarta.fake.http.HttpServlet;

@WebServlet(urlPatterns = { "/foo#fragment", "/bar?query=1" })
public class FragmentAndQueryServlet extends HttpServlet {
}
