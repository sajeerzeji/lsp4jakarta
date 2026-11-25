package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.fake.servlet.Filter;
import jakarta.fake.servlet.annotation.WebFilter;

@WebFilter(urlPatterns = { "/**", "/foo/*/bar", "/api/**" })
public class WildcardAndGlobFilter implements Filter {
}
