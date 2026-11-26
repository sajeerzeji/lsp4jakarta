package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.fake.annotation.WebServlet;
import jakarta.fake.http.HttpServlet;

@WebServlet(urlPatterns = { ConstantReferenceServlet.Paths.API, ConstantReferenceServlet.Paths.NO_SLASH, ConstantReferenceServlet.Paths.API + "/v1" })
public class ConstantReferenceServlet extends HttpServlet {

    static final class Paths {
        static final String API = "/api";
        static final String NO_SLASH = "api";
    }
}
