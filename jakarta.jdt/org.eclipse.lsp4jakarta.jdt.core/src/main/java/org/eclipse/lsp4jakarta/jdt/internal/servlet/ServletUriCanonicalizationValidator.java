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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4jakarta.jdt.core.java.diagnostics.JavaDiagnosticsContext;
import org.eclipse.lsp4jakarta.jdt.core.utils.PositionUtils;
import org.eclipse.lsp4jakarta.jdt.internal.DiagnosticUtils;
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

        List<String> urlPatterns = DiagnosticUtils.getAnnotationMemberValues(annotation, Constants.URL_PATTERNS, String.class);
        List<String> values = DiagnosticUtils.getAnnotationMemberValues(annotation, Constants.VALUE, String.class);

        List<String> allPatterns = new ArrayList<>();
        allPatterns.addAll(urlPatterns);
        allPatterns.addAll(values);

        if (allPatterns.isEmpty()) {
            return;
        }

        Range range = PositionUtils.toNameRange(annotation, context.getUtils());

        for (String pattern : allPatterns) {
            validateSinglePattern(context, uri, pattern, range, diagnostics);
        }
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

        // 1. Missing leading slash (§3.5.2 Rule 10, §12.2)
        if (!pattern.startsWith(Constants.FORWARD_SLASH) && !isExtensionMapping) {
            diagnostics.add(context.createDiagnostic(uri,
                                                     Messages.getMessage("ServletUriMissingLeadingSlash", pattern),
                                                     range, Constants.DIAGNOSTIC_SOURCE, null,
                                                     ErrorCode.ServletUriMissingLeadingSlash, DiagnosticSeverity.Error));
        }

        // 2. Fragment identifier (§3.5.2 Rule 10.1)
        if (pattern.contains(Constants.HASH)) {
            diagnostics.add(context.createDiagnostic(uri,
                                                     Messages.getMessage("ServletUriContainsFragment", pattern),
                                                     range, Constants.DIAGNOSTIC_SOURCE, null,
                                                     ErrorCode.ServletUriContainsFragment, DiagnosticSeverity.Error));
        }

        // 3. Query string delimiter (§3.5.2 Step 2, §12.2)
        if (pattern.contains(Constants.QUESTION_MARK)) {
            diagnostics.add(context.createDiagnostic(uri,
                                                     Messages.getMessage("ServletUriContainsQueryString", pattern),
                                                     range, Constants.DIAGNOSTIC_SOURCE, null,
                                                     ErrorCode.ServletUriContainsQueryString, DiagnosticSeverity.Error));
        }

        // 4. Backslash character (§3.5.2 Rule 10.9)
        if (pattern.contains(Constants.BACKSLASH) || containsIgnoreCase(pattern, "%5c")) {
            diagnostics.add(context.createDiagnostic(uri,
                                                     Messages.getMessage("ServletUriContainsBackslash", pattern),
                                                     range, Constants.DIAGNOSTIC_SOURCE, null,
                                                     ErrorCode.ServletUriContainsBackslash, DiagnosticSeverity.Error));
        }

        // 5. Encoded slash (§3.5.2 Rule 10.4)
        if (containsIgnoreCase(pattern, "%2f")) {
            diagnostics.add(context.createDiagnostic(uri,
                                                     Messages.getMessage("ServletUriEncodedSlash", pattern),
                                                     range, Constants.DIAGNOSTIC_SOURCE, null,
                                                     ErrorCode.ServletUriEncodedSlash, DiagnosticSeverity.Error));
        }

        boolean reportedControlChar = false;
        // 6. Control characters (raw in string) (§3.5.2 Rule 10.10)
        if (hasRawControlCharacters(pattern)) {
            diagnostics.add(context.createDiagnostic(uri,
                                                     Messages.getMessage("ServletUriContainsControlChar", pattern),
                                                     range, Constants.DIAGNOSTIC_SOURCE, null,
                                                     ErrorCode.ServletUriContainsControlChar, DiagnosticSeverity.Error));
            reportedControlChar = true;
        }

        // 7. Invalid percent-encoding & UTF-8 decoding (§3.5.2 Rules 10.10, 10.11, 10.12)
        validatePercentEncodingAndDecodedContent(context, uri, pattern, range, diagnostics, reportedControlChar);

        // 8. Spring/Ant glob /** pattern (§12.2)
        if (pattern.contains(Constants.SPRING_GLOB_PATTERN)) {
            diagnostics.add(context.createDiagnostic(uri,
                                                     Messages.getMessage("ServletUriSpringGlobPattern", pattern),
                                                     range, Constants.DIAGNOSTIC_SOURCE, null,
                                                     ErrorCode.ServletUriSpringGlobPattern, DiagnosticSeverity.Warning));
        }

        // 9. Misplaced wildcards (§12.2)
        if (hasMisplacedWildcard(pattern)) {
            diagnostics.add(context.createDiagnostic(uri,
                                                     Messages.getMessage("ServletUriMisplacedWildcard", pattern),
                                                     range, Constants.DIAGNOSTIC_SOURCE, null,
                                                     ErrorCode.ServletUriMisplacedWildcard, DiagnosticSeverity.Error));
        }

        // 10. Segment-level analysis (§3.5.2 Rules 10.3, 10.5, 10.6, 10.8)
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
            diagnostics.add(context.createDiagnostic(uri,
                                                     Messages.getMessage("ServletUriInvalidPercentEncoding", pattern),
                                                     range, Constants.DIAGNOSTIC_SOURCE, null,
                                                     ErrorCode.ServletUriInvalidPercentEncoding, DiagnosticSeverity.Error));
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
                diagnostics.add(context.createDiagnostic(uri,
                                                         Messages.getMessage("ServletUriContainsControlChar", pattern),
                                                         range, Constants.DIAGNOSTIC_SOURCE, null,
                                                         ErrorCode.ServletUriContainsControlChar, DiagnosticSeverity.Error));
            }
        } catch (CharacterCodingException | IllegalArgumentException e) {
            diagnostics.add(context.createDiagnostic(uri,
                                                     Messages.getMessage("ServletUriDecodeError", pattern),
                                                     range, Constants.DIAGNOSTIC_SOURCE, null,
                                                     ErrorCode.ServletUriDecodeError, DiagnosticSeverity.Error));
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

            // Empty segment with parameters (e.g. /;/ or /;param/ when not the last segment)
            if (!isLast && seg.startsWith(Constants.SEMICOLON) && !reportedEmptyWithParam) {
                diagnostics.add(context.createDiagnostic(uri,
                                                         Messages.getMessage("ServletUriEmptySegmentWithParam", pattern),
                                                         range, Constants.DIAGNOSTIC_SOURCE, null,
                                                         ErrorCode.ServletUriEmptySegmentWithParam, DiagnosticSeverity.Error));
                reportedEmptyWithParam = true;
            }

            // Dot segment with parameters (/..;/ or /.;/)
            int semicolonIdx = seg.indexOf(';');
            String segBeforeParam = semicolonIdx != -1 ? seg.substring(0, semicolonIdx) : seg;
            String segBeforeParamDecoded = segBeforeParam.replaceAll("(?i)%2e", ".");

            if (semicolonIdx != -1) {
                if ((".".equals(segBeforeParam) || "..".equals(segBeforeParam)
                     || ".".equals(segBeforeParamDecoded) || "..".equals(segBeforeParamDecoded))
                    && !reportedDotWithParam) {
                    diagnostics.add(context.createDiagnostic(uri,
                                                             Messages.getMessage("ServletUriDotSegmentWithParam", pattern),
                                                             range, Constants.DIAGNOSTIC_SOURCE, null,
                                                             ErrorCode.ServletUriDotSegmentWithParam, DiagnosticSeverity.Error));
                    reportedDotWithParam = true;
                }
            }

            // Encoded dot segment (/%2e/ or /%2e%2e/)
            if (ENCODED_DOT_PATTERN.matcher(segBeforeParam).find()
                && (".".equals(segBeforeParamDecoded) || "..".equals(segBeforeParamDecoded))
                && !reportedEncodedDot) {
                diagnostics.add(context.createDiagnostic(uri,
                                                         Messages.getMessage("ServletUriEncodedDotSegment", pattern),
                                                         range, Constants.DIAGNOSTIC_SOURCE, null,
                                                         ErrorCode.ServletUriEncodedDotSegment, DiagnosticSeverity.Error));
                reportedEncodedDot = true;
            }

            // Path normalization check (Leading dot-dot or normalizing above root)
            if (".".equals(segBeforeParamDecoded)) {
                // remove dot segment
                continue;
            } else if ("..".equals(segBeforeParamDecoded)) {
                if (normalizedStack.isEmpty() && !reportedLeadingDotDot) {
                    diagnostics.add(context.createDiagnostic(uri,
                                                             Messages.getMessage("ServletUriLeadingDotDotSegment", pattern),
                                                             range, Constants.DIAGNOSTIC_SOURCE, null,
                                                             ErrorCode.ServletUriLeadingDotDotSegment, DiagnosticSeverity.Error));
                    reportedLeadingDotDot = true;
                } else if (!normalizedStack.isEmpty()) {
                    normalizedStack.remove(normalizedStack.size() - 1);
                }
            } else if (!segBeforeParamDecoded.isEmpty()) {
                normalizedStack.add(segBeforeParamDecoded);
            }
        }
    }

    private static boolean containsIgnoreCase(String str, String search) {
        if (str == null || search == null) {
            return false;
        }
        return str.toLowerCase().contains(search.toLowerCase());
    }
}
