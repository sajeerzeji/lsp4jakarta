package io.openliberty.sample.jakarta.servlet.canonicalization;

import jakarta.fake.Filter;
import jakarta.fake.annotation.WebFilter;

@WebFilter(value = "bar")
public class ValueAttributeFilter implements Filter {
}
