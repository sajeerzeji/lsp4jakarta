package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.servlet.Filter;
import jakarta.servlet.annotation.WebFilter;

@WebFilter(urlPatterns = { "/**", "/foo/*/bar" })
public class WildcardAndGlobFilter implements Filter {
}
