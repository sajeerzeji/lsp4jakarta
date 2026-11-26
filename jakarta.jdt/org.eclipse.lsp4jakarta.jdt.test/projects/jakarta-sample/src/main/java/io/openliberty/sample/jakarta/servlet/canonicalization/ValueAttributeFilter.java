package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.servlet.Filter;
import jakarta.servlet.annotation.WebFilter;

@WebFilter(value = "bar")
public class ValueAttributeFilter implements Filter {
}
