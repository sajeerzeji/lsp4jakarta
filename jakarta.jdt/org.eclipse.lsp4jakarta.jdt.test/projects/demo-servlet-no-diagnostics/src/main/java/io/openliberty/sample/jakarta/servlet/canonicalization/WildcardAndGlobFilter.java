package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.fake.Filter;
import jakarta.fake.annotation.WebFilter;

@WebFilter(urlPatterns = { "/**", "/foo/*/bar", "/api/**" })
public class WildcardAndGlobFilter implements Filter {
}
