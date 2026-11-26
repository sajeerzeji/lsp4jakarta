/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.lsp4jakarta.jdt.internal.servlet;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4jakarta.jdt.core.java.diagnostics.JavaDiagnosticsContext;
import org.eclipse.lsp4jakarta.jdt.core.utils.PositionUtils;
import org.eclipse.lsp4jakarta.jdt.internal.Messages;

/**
 * Validates URI path patterns in {@code @WebServlet} and {@code @WebFilter} annotations
 * for canonicalization violations in accordance with Jakarta Servlet 6.0/6.1 specification
 * (Sections 3.5.2, 3.5.3, and 12.2).
 */
public class ServletUriCanonicalizationValidator {

    private static final Pattern INVALID_PERCENT_PATTERN = Pattern.compile("%(?![0-9A-Fa-f]{2})");
    private static final Pattern ENCODED_DOT_PATTERN = Pattern.compile("(?i)(%2e)");

    /**
     * Validates URL patterns declared on {@code @WebServlet} or {@code @WebFilter} annotations.
     *
     * @param context the Java diagnostics context
     * @param uri the file URI
     * @param annotation the servlet or filter annotation
     * @param diagnostics the diagnostics accumulator list
     * @throws JavaModelException if model resolution fails
     */
    public static void validatePatterns(JavaDiagnosticsContext context, String uri,
                                        IAnnotation annotation, List<Diagnostic> diagnostics) throws JavaModelException {

        List<String> allPatterns = getPatternValues(context, annotation);

        if (allPatterns.isEmpty()) {
            return;
        }

        Range range = PositionUtils.toNameRange(annotation, context.getUtils());

        for (String pattern : allPatterns) {
            validateSinglePattern(context, uri, pattern, range, diagnostics);
        }
    }

    /**
     * Collects the URL patterns declared in the {@code urlPatterns} and {@code value} members.
     *
     * <p>Values are resolved through the AST so that compile-time constants
     * (e.g. {@code Paths.API} or {@code Paths.API + "/v1"}) are validated by their value.
     * The Java model cannot be used for this: it reports a constant reference as a
     * {@code String} holding the expression text, not the constant's value.</p>
     *
     * <p>If the annotation node cannot be located, only the plain string literals reported by
     * the Java model are returned.</p>
     */
    private static List<String> getPatternValues(JavaDiagnosticsContext context,
                                                 IAnnotation annotation) throws JavaModelException {
        Annotation node = findAnnotationNode(context, annotation);
        if (node == null) {
            return getStringLiteralValues(annotation);
        }

        List<String> patterns = new ArrayList<>();
        if (node instanceof SingleMemberAnnotation single) {
            collectPatterns(single.getValue(), patterns);
        } else if (node instanceof NormalAnnotation normal) {
            for (Object obj : normal.values()) {
                if (obj instanceof MemberValuePair pair) {
                    String name = pair.getName().getIdentifier();
                    if (Constants.URL_PATTERNS.equals(name) || Constants.VALUE.equals(name)) {
                        collectPatterns(pair.getValue(), patterns);
                    }
                }
            }
        }
        return patterns;
    }

    /**
     * Adds the String value of the expression, or of each element if it is an array initializer.
     * Expressions that are not String compile-time constants are skipped.
     */
    private static void collectPatterns(Expression expr, List<String> patterns) {
        if (expr instanceof ArrayInitializer array) {
            for (Object element : array.expressions()) {
                collectPatterns((Expression) element, patterns);
            }
        } else if (expr instanceof StringLiteral literal) {
            patterns.add(literal.getLiteralValue());
        } else if (expr != null && expr.resolveConstantExpressionValue() instanceof String value) {
            patterns.add(value);
        }
    }

    private static Annotation findAnnotationNode(JavaDiagnosticsContext context,
                                                 IAnnotation annotation) throws JavaModelException {
        if (!(context.getTypeRoot() instanceof ICompilationUnit)) {
            return null;
        }
        ISourceRange range = annotation.getSourceRange();
        if (range == null || range.getOffset() < 0) {
            return null;
        }
        ASTNode node = NodeFinder.perform(context.getASTRoot(), range.getOffset(), range.getLength());
        while (node != null && !(node instanceof Annotation)) {
            node = node.getParent();
        }
        return (Annotation) node;
    }

    /**
     * Fallback that returns the plain string literals reported by the Java model. Only
     * {@link IMemberValuePair#K_STRING} values are accepted because the model reports
     * constant references as {@code String}s holding the expression text.
     */
    private static List<String> getStringLiteralValues(IAnnotation annotation) throws JavaModelException {
        List<String> result = new ArrayList<>();
        for (IMemberValuePair pair : annotation.getMemberValuePairs()) {
            String name = pair.getMemberName();
            if ((Constants.URL_PATTERNS.equals(name) || Constants.VALUE.equals(name))
                && pair.getValueKind() == IMemberValuePair.K_STRING) {
                Object value = pair.getValue();
                if (value instanceof Object[] array) {
                    for (Object item : array) {
                        if (item instanceof String s) {
                            result.add(s);
                        }
                    }
                } else if (value instanceof String s) {
                    result.add(s);
                }
            }
        }
        return result;
    }

    /**
     * Validates a single URL pattern against Jakarta Servlet canonicalization rules.
     */
    private static void validateSinglePattern(JavaDiagnosticsContext context, String uri,
                                              String pattern, Range range, List<Diagnostic> diagnostics) {
        if (pattern == null || pattern.isEmpty()) {
            // Empty string is valid context root mapping per §12.2
            return;
        }

        boolean isExtensionMapping = pattern.startsWith(Constants.EXTENSION_MAPPING_PREFIX);

        // §3.5.2 Rejecting Suspicious Sequences: Leading slash / Section 12.2 Mapping Rules
        if (!pattern.startsWith(Constants.FORWARD_SLASH) && !isExtensionMapping) {
            report(context, uri, range, diagnostics, ErrorCode.ServletUriMissingLeadingSlash,
                   "ServletUriMissingLeadingSlash", pattern);
        }

        // §3.5.2 Rejecting Suspicious Sequences: URI Fragment (#)
        if (pattern.contains(Constants.HASH)) {
            report(context, uri, range, diagnostics, ErrorCode.ServletUriContainsFragment,
                   "ServletUriContainsFragment", pattern);
        }

        // §3.5.2 Rejecting Suspicious Sequences: Query String Delimiter (?) / Section 12.2
        if (pattern.contains(Constants.QUESTION_MARK)) {
            report(context, uri, range, diagnostics, ErrorCode.ServletUriContainsQueryString,
                   "ServletUriContainsQueryString", pattern);
        }

        // §3.5.2 Rejecting Suspicious Sequences: Backslash (\ or %5C)
        if (pattern.contains(Constants.BACKSLASH) || containsIgnoreCase(pattern, "%5c")) {
            report(context, uri, range, diagnostics, ErrorCode.ServletUriContainsBackslash,
                   "ServletUriContainsBackslash", pattern);
        }

        // §3.5.2 Rejecting Suspicious Sequences: Encoded Forward Slash (%2F)
        if (containsIgnoreCase(pattern, "%2f")) {
            report(context, uri, range, diagnostics, ErrorCode.ServletUriEncodedSlash,
                   "ServletUriEncodedSlash", pattern);
        }

        boolean reportedControlChar = false;
        // §3.5.2 Rejecting Suspicious Sequences: Encoded and Raw Control Characters
        if (hasRawControlCharacters(pattern)) {
            report(context, uri, range, diagnostics, ErrorCode.ServletUriContainsControlChar,
                   "ServletUriContainsControlChar", pattern);
            reportedControlChar = true;
        }

        // §3.5.2 Rejecting Suspicious Sequences: Invalid Percent-Encoding & UTF-8 Decoding Errors
        validatePercentEncodingAndDecodedContent(context, uri, pattern, range, diagnostics, reportedControlChar);

        // Section 12.2: Spring Glob Pattern (/**)
        if (pattern.contains(Constants.SPRING_GLOB_PATTERN)) {
            report(context, uri, range, diagnostics, ErrorCode.ServletUriSpringGlobPattern,
                   DiagnosticSeverity.Warning, "ServletUriSpringGlobPattern", pattern);
        }

        // Section 12.2: Misplaced Wildcard (*)
        if (hasMisplacedWildcard(pattern)) {
            report(context, uri, range, diagnostics, ErrorCode.ServletUriMisplacedWildcard,
                   "ServletUriMisplacedWildcard", pattern);
        }

        // §3.5.2 Rejecting Suspicious Sequences: Segment-level analysis
        validateSegments(context, uri, pattern, range, diagnostics);
    }

    private static boolean hasRawControlCharacters(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                return true;
            }
        }
        return false;
    }

    private static void validatePercentEncodingAndDecodedContent(JavaDiagnosticsContext context, String uri,
                                                                 String pattern, Range range,
                                                                 List<Diagnostic> diagnostics,
                                                                 boolean alreadyReportedControlChar) {
        if (!pattern.contains(Constants.PERCENT)) {
            return;
        }

        if (INVALID_PERCENT_PATTERN.matcher(pattern).find()) {
            report(context, uri, range, diagnostics, ErrorCode.ServletUriInvalidPercentEncoding,
                   "ServletUriInvalidPercentEncoding", pattern);
            return;
        }

        // Decode percent-encoded byte sequence with UTF-8
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int i = 0;
            while (i < pattern.length()) {
                char c = pattern.charAt(i);
                if (c == '%') {
                    int b = Integer.parseInt(pattern.substring(i + 1, i + 3), 16);
                    buffer.write(b);
                    i += 3;
                } else {
                    byte[] charBytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
                    buffer.write(charBytes, 0, charBytes.length);
                    i++;
                }
            }
            byte[] bytes = buffer.toByteArray();
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            String decoded = decoder.decode(ByteBuffer.wrap(bytes)).toString();

            // Check if decoded content has control characters (if not already reported on raw pattern)
            if (!alreadyReportedControlChar && hasRawControlCharacters(decoded)) {
                report(context, uri, range, diagnostics, ErrorCode.ServletUriContainsControlChar,
                       "ServletUriContainsControlChar", pattern);
            }
        } catch (CharacterCodingException | IllegalArgumentException e) {
            report(context, uri, range, diagnostics, ErrorCode.ServletUriDecodeError,
                   "ServletUriDecodeError", pattern);
        }
    }

    private static boolean hasMisplacedWildcard(String pattern) {
        if (!pattern.contains("*") || pattern.contains(Constants.SPRING_GLOB_PATTERN)) {
            return false;
        }
        if (pattern.startsWith(Constants.EXTENSION_MAPPING_PREFIX)) {
            // Extension mapping: only one * allowed at the beginning
            return pattern.indexOf('*', 2) != -1;
        }
        if (pattern.endsWith(Constants.PATH_MAPPING_SUFFIX)) {
            // Path mapping ending in /*
            // Valid if no other * appears before the last character
            return pattern.indexOf('*') != pattern.length() - 1;
        }
        return true;
    }

    private static void validateSegments(JavaDiagnosticsContext context, String uri,
                                         String pattern, Range range, List<Diagnostic> diagnostics) {
        // Strip fragment and query before segment parsing
        String cleanPath = pattern;
        int fragmentIdx = cleanPath.indexOf('#');
        if (fragmentIdx != -1) {
            cleanPath = cleanPath.substring(0, fragmentIdx);
        }
        int queryIdx = cleanPath.indexOf('?');
        if (queryIdx != -1) {
            cleanPath = cleanPath.substring(0, queryIdx);
        }

        if (cleanPath.startsWith(Constants.FORWARD_SLASH)) {
            cleanPath = cleanPath.substring(1);
        }

        String[] rawSegments = cleanPath.split(Constants.FORWARD_SLASH, -1);
        List<String> normalizedStack = new ArrayList<>();
        boolean reportedLeadingDotDot = false;
        boolean reportedDotWithParam = false;
        boolean reportedEncodedDot = false;
        boolean reportedEmptyWithParam = false;

        for (int i = 0; i < rawSegments.length; i++) {
            String seg = rawSegments[i];
            boolean isLast = (i == rawSegments.length - 1);

            // §3.5.2 Rejecting Suspicious Sequences: Empty segment with parameters (/;/ or /;param/)
            if (!isLast && seg.startsWith(Constants.SEMICOLON) && !reportedEmptyWithParam) {
                report(context, uri, range, diagnostics, ErrorCode.ServletUriEmptySegmentWithParam,
                       "ServletUriEmptySegmentWithParam", pattern);
                reportedEmptyWithParam = true;
            }

            // §3.5.2 Rejecting Suspicious Sequences: Dot segments with parameters (/..;/ or /.;/)
            int semicolonIdx = seg.indexOf(';');
            String segBeforeParam = semicolonIdx != -1 ? seg.substring(0, semicolonIdx) : seg;
            String segBeforeParamDecoded = segBeforeParam.replaceAll("(?i)%2e", ".");

            if (semicolonIdx != -1) {
                if ((".".equals(segBeforeParam) || "..".equals(segBeforeParam)
                     || ".".equals(segBeforeParamDecoded) || "..".equals(segBeforeParamDecoded))
                    && !reportedDotWithParam) {
                    report(context, uri, range, diagnostics, ErrorCode.ServletUriDotSegmentWithParam,
                           "ServletUriDotSegmentWithParam", pattern);
                    reportedDotWithParam = true;
                }
            }

            // §3.5.2 Rejecting Suspicious Sequences: Encoded dot segment (/%2e/ or /%2e%2e/)
            if (ENCODED_DOT_PATTERN.matcher(segBeforeParam).find()
                && (".".equals(segBeforeParamDecoded) || "..".equals(segBeforeParamDecoded))
                && !reportedEncodedDot) {
                report(context, uri, range, diagnostics, ErrorCode.ServletUriEncodedDotSegment,
                       "ServletUriEncodedDotSegment", pattern);
                reportedEncodedDot = true;
            }

            // §3.5.2 Rejecting Suspicious Sequences: Traversing above root (/../)
            if (".".equals(segBeforeParamDecoded)) {
                // remove dot segment
                continue;
            } else if ("..".equals(segBeforeParamDecoded)) {
                if (normalizedStack.isEmpty() && !reportedLeadingDotDot) {
                    report(context, uri, range, diagnostics, ErrorCode.ServletUriLeadingDotDotSegment,
                           "ServletUriLeadingDotDotSegment", pattern);
                    reportedLeadingDotDot = true;
                } else if (!normalizedStack.isEmpty()) {
                    normalizedStack.remove(normalizedStack.size() - 1);
                }
            } else if (!segBeforeParamDecoded.isEmpty()) {
                normalizedStack.add(segBeforeParamDecoded);
            }
        }
    }

    private static void report(JavaDiagnosticsContext context, String uri, Range range,
                               List<Diagnostic> diagnostics, ErrorCode errorCode,
                               DiagnosticSeverity severity, String messageKey, String pattern) {
        diagnostics.add(context.createDiagnostic(uri,
                                                 Messages.getMessage(messageKey, pattern),
                                                 range, Constants.DIAGNOSTIC_SOURCE, pattern,
                                                 errorCode, severity));
    }

    private static void report(JavaDiagnosticsContext context, String uri, Range range,
                               List<Diagnostic> diagnostics, ErrorCode errorCode,
                               String messageKey, String pattern) {
        report(context, uri, range, diagnostics, errorCode, DiagnosticSeverity.Error, messageKey, pattern);
    }

    private static boolean containsIgnoreCase(String str, String search) {
        if (str == null || search == null) {
            return false;
        }
        return str.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT));
    }
}
