package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.fake.annotation.WebServlet;
import jakarta.fake.http.HttpServlet;

@WebServlet(urlPatterns = { "/a", "lit", MixedLiteralAndConstantServlet.Paths.NO_SLASH })
public class MixedLiteralAndConstantServlet extends HttpServlet {

    static final class Paths {
        static final String NO_SLASH = "const";
    }
}
