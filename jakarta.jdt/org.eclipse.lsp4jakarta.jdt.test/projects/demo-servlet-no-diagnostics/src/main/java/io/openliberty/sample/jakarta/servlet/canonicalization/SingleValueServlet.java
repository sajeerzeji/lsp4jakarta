package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.fake.annotation.WebServlet;
import jakarta.fake.http.HttpServlet;

@WebServlet("foo")
public class SingleValueServlet extends HttpServlet {
}
