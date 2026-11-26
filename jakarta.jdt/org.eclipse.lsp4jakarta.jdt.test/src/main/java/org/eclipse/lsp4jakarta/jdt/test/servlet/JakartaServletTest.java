/*******************************************************************************
* Copyright (c) 2021, 2026 IBM Corporation and others.
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
package org.eclipse.lsp4jakarta.jdt.test.servlet;

import static org.eclipse.lsp4jakarta.jdt.test.core.JakartaForJavaAssert.assertJavaCodeAction;
import static org.eclipse.lsp4jakarta.jdt.test.core.JakartaForJavaAssert.assertJavaDiagnostics;
import static org.eclipse.lsp4jakarta.jdt.test.core.JakartaForJavaAssert.ca;
import static org.eclipse.lsp4jakarta.jdt.test.core.JakartaForJavaAssert.createCodeActionParams;
import static org.eclipse.lsp4jakarta.jdt.test.core.JakartaForJavaAssert.d;
import static org.eclipse.lsp4jakarta.jdt.test.core.JakartaForJavaAssert.te;

import java.util.Arrays;

import com.google.gson.JsonPrimitive;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.eclipse.lsp4jakarta.jdt.core.utils.IJDTUtils;
import org.eclipse.lsp4jakarta.jdt.internal.core.ls.JDTUtilsLSImpl;
import org.eclipse.lsp4jakarta.jdt.test.core.BaseJakartaTest;
import org.junit.Test;

public class JakartaServletTest extends BaseJakartaTest {

    protected static IJDTUtils IJDT_UTILS = JDTUtilsLSImpl.getInstance();

    @Test
    public void ExtendWebServlet() throws Exception {
        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/io/openliberty/sample/jakarta/servlet/DontExtendHttpServlet.java"));
        String uri = javaFile.getLocation().toFile().toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // expected
        Diagnostic d = d(5, 13, 34, "Annotated classes with @WebServlet must extend the HttpServlet class.",
                         DiagnosticSeverity.Error, "jakarta-servlet",
                         "WebServletAnnotatedClassDoesNotExtendHttpServlet");

        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, d);

        // test associated quick-fix code action
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
        TextEdit te = te(2, 45, 5, 34, "\nimport jakarta.servlet.http.HttpServlet;\n\n"
                                       + "@WebServlet(name = \"demoServlet\", urlPatterns = { \"/demo\" })\npublic class DontExtendHttpServlet extends HttpServlet");

        CodeAction ca = ca(uri, "Let 'DontExtendHttpServlet' extend 'HttpServlet'", d, te);
        assertJavaCodeAction(codeActionParams, IJDT_UTILS, ca);
    }

    @Test
    public void CompleteWebServletAnnotation() throws Exception {
        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/io/openliberty/sample/jakarta/servlet/InvalidWebServlet.java"));
        String uri = javaFile.getLocation().toFile().toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d = d(9, 0, 13,
                         "The @WebServlet annotation must define the attribute 'urlPatterns' or 'value'.",
                         DiagnosticSeverity.Error, "jakarta-servlet", "WebServletAnnotationMissingAttributes");

        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, d);

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
        TextEdit te1 = te(9, 0, 10, 0, "@WebServlet(value = {})\n");
        CodeAction ca1 = ca(uri, "Add the `value` attribute to @WebServlet", d, te1);

        TextEdit te2 = te(9, 0, 10, 0, "@WebServlet(urlPatterns = {})\n");
        CodeAction ca2 = ca(uri, "Add the `urlPatterns` attribute to @WebServlet", d, te2);
        assertJavaCodeAction(codeActionParams, IJDT_UTILS, ca2, ca1);
    }

    @Test
    public void implementFilter() throws Exception {
        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/io/openliberty/sample/jakarta/servlet/DontImplementFilter.java"));
        String uri = javaFile.getLocation().toFile().toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d = d(5, 13, 32,
                         "Annotated classes with @WebFilter must implement the Filter interface.",
                         DiagnosticSeverity.Error, "jakarta-servlet", "ClassWebFilterAnnotatedNoFilterInterfaceImpl");

        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, d);

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
        TextEdit te = te(2, 0, 5, 32, "import jakarta.servlet.Filter;\nimport jakarta.servlet.annotation.WebFilter;\n\n@WebFilter(urlPatterns = "
                                      + "{ \"/filter\" })\npublic class DontImplementFilter implements Filter");
        CodeAction ca = ca(uri, "Let 'DontImplementFilter' implement 'Filter'", d, te);
        assertJavaCodeAction(codeActionParams, IJDT_UTILS, ca);
    }

    @Test
    public void implementListener() throws Exception {
        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/io/openliberty/sample/jakarta/servlet/DontImplementListener.java"));
        String uri = javaFile.getLocation().toFile().toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d = d(5, 13, 34,
                         "Annotated classes with @WebListener must implement one or more of the following interfaces: ServletContextListener, "
                                    + "ServletContextAttributeListener, ServletRequestListener, ServletRequestAttributeListener, HttpSessionListener, "
                                    + "HttpSessionAttributeListener, or HttpSessionIdListener.",
                         DiagnosticSeverity.Error, "jakarta-servlet", "WebFilterAnnotatedClassReqIfaceNoImpl");

        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, d);

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);

        TextEdit te1 = te(2, 46, 5, 34, "\nimport jakarta.servlet.http.HttpSessionAttributeListener;\n\n"
                                        + "@WebListener\npublic class DontImplementListener implements HttpSessionAttributeListener");
        CodeAction ca1 = ca(uri, "Let 'DontImplementListener' implement 'HttpSessionAttributeListener'", d, te1);

        TextEdit te2 = te(2, 46, 5, 34, "\nimport jakarta.servlet.http.HttpSessionIdListener;\n\n"
                                        + "@WebListener\npublic class DontImplementListener implements HttpSessionIdListener");
        CodeAction ca2 = ca(uri, "Let 'DontImplementListener' implement 'HttpSessionIdListener'", d, te2);

        TextEdit te3 = te(2, 46, 5, 34, "\nimport jakarta.servlet.http.HttpSessionListener;\n\n"
                                        + "@WebListener\npublic class DontImplementListener implements HttpSessionListener");
        CodeAction ca3 = ca(uri, "Let 'DontImplementListener' implement 'HttpSessionListener'", d, te3);

        TextEdit te4 = te(2, 0, 5, 34, "import jakarta.servlet.ServletContextAttributeListener;\nimport jakarta.servlet.annotation.WebListener;\n\n"
                                       + "@WebListener\npublic class DontImplementListener implements ServletContextAttributeListener");
        CodeAction ca4 = ca(uri, "Let 'DontImplementListener' implement 'ServletContextAttributeListener'", d, te4);

        TextEdit te5 = te(2, 0, 5, 34, "import jakarta.servlet.ServletContextListener;\nimport jakarta.servlet.annotation.WebListener;\n\n"
                                       + "@WebListener\npublic class DontImplementListener implements ServletContextListener");
        CodeAction ca5 = ca(uri, "Let 'DontImplementListener' implement 'ServletContextListener'", d, te5);

        TextEdit te6 = te(2, 0, 5, 34, "import jakarta.servlet.ServletRequestAttributeListener;\nimport jakarta.servlet.annotation.WebListener;\n\n"
                                       + "@WebListener\npublic class DontImplementListener implements ServletRequestAttributeListener");
        CodeAction ca6 = ca(uri, "Let 'DontImplementListener' implement 'ServletRequestAttributeListener'", d, te6);

        TextEdit te7 = te(2, 0, 5, 34, "import jakarta.servlet.ServletRequestListener;\nimport jakarta.servlet.annotation.WebListener;\n\n"
                                       + "@WebListener\npublic class DontImplementListener implements ServletRequestListener");
        CodeAction ca7 = ca(uri, "Let 'DontImplementListener' implement 'ServletRequestListener'", d, te7);

        assertJavaCodeAction(codeActionParams, IJDT_UTILS, ca1, ca2, ca3, ca4, ca5, ca6, ca7);
    }

    @Test
    public void RemoveDuplicateAttribute() throws Exception {

        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/io/openliberty/sample/jakarta/servlet/DuplicateAttributeWebServlet.java"));
        String uri = javaFile.getLocation().toFile().toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d = d(5, 0, 41,
                         "The @WebServlet annotation cannot have both 'value' and 'urlPatterns' attributes specified at once.",
                         DiagnosticSeverity.Error, "jakarta-servlet", "WebServletAnnotationAttributeConflict");

        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, d);

        String newText = "@WebServlet(value = \"\")\n";
        String newText1 = "@WebServlet(urlPatterns = \"\")\n";

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);

        TextEdit te1 = te(5, 0, 6, 0, newText);
        CodeAction ca1 = ca(uri, "Remove the `urlPatterns` attribute from @WebServlet", d, te1);

        TextEdit te2 = te(5, 0, 6, 0, newText1);
        CodeAction ca2 = ca(uri, "Remove the `value` attribute from @WebServlet", d, te2);

        assertJavaCodeAction(codeActionParams, IJDT_UTILS, ca1, ca2);
    }

    @Test
    public void CompleteWebFilterAnnotation() throws Exception {

        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/io/openliberty/sample/jakarta/servlet/InvalidWebFilter.java"));
        String uri = javaFile.getLocation().toFile().toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d = d(5, 0, 12,
                         "The annotation @WebFilter must define the attribute 'urlPatterns', 'servletNames' or 'value'.",
                         DiagnosticSeverity.Error, "jakarta-servlet", "WebFilterAnnotationMissingAttributes");

        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, d);

        String newText = "@WebFilter(servletNames = {})\n";
        String newText1 = "@WebFilter(urlPatterns = {})\n";
        String newText2 = "@WebFilter(value = {})\n";

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);

        TextEdit te1 = te(5, 0, 6, 0, newText);
        CodeAction ca1 = ca(uri, "Add the `servletNames` attribute to @WebFilter", d, te1);

        TextEdit te2 = te(5, 0, 6, 0, newText1);
        CodeAction ca2 = ca(uri, "Add the `urlPatterns` attribute to @WebFilter", d, te2);

        TextEdit te3 = te(5, 0, 6, 0, newText2);
        CodeAction ca3 = ca(uri, "Add the `value` attribute to @WebFilter", d, te3);

        assertJavaCodeAction(codeActionParams, IJDT_UTILS, ca1, ca2, ca3);
    }

    @Test
    public void RemoveDuplicateWebFilterAttributes() throws Exception {

        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/io/openliberty/sample/jakarta/servlet/DuplicateAttributeWebFilter.java"));
        String uri = javaFile.getLocation().toFile().toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d = d(5, 0, 40,
                         "The annotation @WebFilter can not have both 'value' and 'urlPatterns' attributes specified at once.",
                         DiagnosticSeverity.Error, "jakarta-servlet", "WebFilterAnnotationAttributeConflict");

        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, d);

        String newText1 = "@WebFilter(value = \"\")\n";
        String newText2 = "@WebFilter(urlPatterns = \"\")\n";

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);

        TextEdit te1 = te(5, 0, 6, 0, newText1);
        CodeAction ca1 = ca(uri, "Remove the `urlPatterns` attribute from @WebFilter", d, te1);

        TextEdit te2 = te(5, 0, 6, 0, newText2);
        CodeAction ca2 = ca(uri, "Remove the `value` attribute from @WebFilter", d, te2);

        assertJavaCodeAction(codeActionParams, IJDT_UTILS, ca1, ca2);
    }

    @Test
    public void DeclareRolesWithoutServlet() throws Exception {
        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/io/openliberty/sample/jakarta/servlet/declareroles/DeclareRolesWithoutServlet.java"));
        String uri = javaFile.getLocation().toFile().toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // expected
        Diagnostic d = d(5, 13, 39, "Annotated classes with @DeclareRoles must implement the Servlet interface or its subclasses.",
                         DiagnosticSeverity.Error, "jakarta-servlet",
                         "DeclareRolesOnNonServletClass");

        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, d);

        // test associated quick-fix code action
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
        TextEdit te = te(2, 48, 5, 39, "\nimport jakarta.servlet.http.HttpServlet;\n\n"
                                       + "@DeclareRoles(\"Administrator\")\npublic class DeclareRolesWithoutServlet extends HttpServlet");

        CodeAction ca = ca(uri, "Let 'DeclareRolesWithoutServlet' extend 'HttpServlet'", d, te);
        assertJavaCodeAction(codeActionParams, IJDT_UTILS, ca);
    }

    @Test
    public void DeclareRolesWithGenericServlet() throws Exception {
        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/io/openliberty/sample/jakarta/servlet/declareroles/DeclareRolesWithGenericServlet.java"));
        String uri = javaFile.getLocation().toFile().toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Should not trigger any diagnostic - GenericServlet is a valid Servlet subclass
        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS);
    }

    @Test
    public void DeclareRolesWithServletInterface() throws Exception {
        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/io/openliberty/sample/jakarta/servlet/declareroles/DeclareRolesWithServletInterface.java"));
        String uri = javaFile.getLocation().toFile().toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Should not trigger any diagnostic - implements Servlet interface directly
        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS);
    }

    @Test
    public void DeclareRolesWithHttpServlet() throws Exception {
        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/io/openliberty/sample/jakarta/servlet/declareroles/DeclareRolesWithHttpServlet.java"));
        String uri = javaFile.getLocation().toFile().toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Should not trigger any diagnostic - HttpServlet is a valid Servlet subclass
        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS);
    }

    @Test
    public void MissingLeadingSlashServlet() throws Exception {
        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/io/openliberty/sample/jakarta/servlet/canonicalization/MissingLeadingSlashServlet.java"));
        String uri = javaFile.getLocation().toFile().toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d = d(5, 0, 40,
                         "URL pattern 'foo/bar' must start with a '/' (unless an extension mapping starting with '*.').",
                         DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriMissingLeadingSlash", "foo/bar");

        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, d);

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
        TextEdit te = te(5, 28, 5, 37, "\"/foo/bar\"");
        CodeAction ca = ca(uri, "Add leading '/' to URL pattern", d, te);
        assertJavaCodeAction(codeActionParams, IJDT_UTILS, ca);
    }

    @Test
    public void QuickFixWithJsonPrimitiveData() throws Exception {
        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/io/openliberty/sample/jakarta/servlet/canonicalization/WildcardAndGlobFilter.java"));
        String uri = javaFile.getLocation().toFile().toURI().toString();

        Diagnostic d = d(5, 0, 60,
                         "URL pattern '/api/**' uses '/**', which in Jakarta Servlet is treated as an exact literal path match and does not match sub-paths.",
                         DiagnosticSeverity.Warning, "jakarta-servlet", "ServletUriSpringGlobPattern", new JsonPrimitive("/api/**"));

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
        TextEdit te = te(5, 48, 5, 57, "\"/api/*\"");
        CodeAction ca = ca(uri, "Replace '/**' with '/*'", d, te);
        assertJavaCodeAction(codeActionParams, IJDT_UTILS, ca);
    }

    @Test
    public void ValidExtensionAndRootServlet() throws Exception {
        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/io/openliberty/sample/jakarta/servlet/canonicalization/ValidExtensionAndRootServlet.java"));
        String uri = javaFile.getLocation().toFile().toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS);
    }

    private static final String CANON_DIR = "src/main/java/io/openliberty/sample/jakarta/servlet/canonicalization/";

    private String canonUri(String className) throws Exception {
        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path(CANON_DIR + className + ".java"));
        return javaFile.getLocation().toFile().toURI().toString();
    }

    private static final String MISSING_SLASH_MSG = "URL pattern '%s' must start with a '/' (unless an extension mapping starting with '*.').";

    @Test
    public void MixedLiteralAndConstantServlet() throws Exception {
        String uri = canonUri("MixedLiteralAndConstantServlet");

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic literal = d(5, 0, 89, String.format(MISSING_SLASH_MSG, "lit"),
                               DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriMissingLeadingSlash", "lit");
        Diagnostic constant = d(5, 0, 89, String.format(MISSING_SLASH_MSG, "const"),
                                DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriMissingLeadingSlash", "const");

        // "/a" is valid; the literal "lit" and the constant "const" are both reported
        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, literal, constant);

        // The fix on the literal edits only that literal, even though an invalid constant sits in the same annotation
        CodeAction fix = ca(uri, "Add leading '/' to URL pattern", literal, te(5, 34, 5, 39, "\"/lit\""));
        assertJavaCodeAction(createCodeActionParams(uri, literal), IJDT_UTILS, fix);

        // The constant has no literal to rewrite
        assertJavaCodeAction(createCodeActionParams(uri, constant), IJDT_UTILS);
    }

    @Test
    public void SingleValueServlet() throws Exception {
        String uri = canonUri("SingleValueServlet");

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d = d(5, 0, 18, String.format(MISSING_SLASH_MSG, "foo"),
                         DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriMissingLeadingSlash", "foo");
        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, d);

        CodeAction fix = ca(uri, "Add leading '/' to URL pattern", d, te(5, 12, 5, 17, "\"/foo\""));
        assertJavaCodeAction(createCodeActionParams(uri, d), IJDT_UTILS, fix);
    }

    @Test
    public void ValueAttributeFilter() throws Exception {
        String uri = canonUri("ValueAttributeFilter");

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d = d(5, 0, 25, String.format(MISSING_SLASH_MSG, "bar"),
                         DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriMissingLeadingSlash", "bar");
        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, d);

        CodeAction fix = ca(uri, "Add leading '/' to URL pattern", d, te(5, 19, 5, 24, "\"/bar\""));
        assertJavaCodeAction(createCodeActionParams(uri, d), IJDT_UTILS, fix);
    }

    @Test
    public void MoreCanonicalizationServlet() throws Exception {
        String uri = canonUri("MoreCanonicalizationServlet");

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d1 = d(5, 0, 79, "URL pattern '/a%09b' must not contain control characters.",
                          DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriContainsControlChar", "/a%09b");
        Diagnostic d2 = d(5, 0, 79, "URL pattern '/a%7Fb' must not contain control characters.",
                          DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriContainsControlChar", "/a%7Fb");
        Diagnostic d3 = d(5, 0, 79, "URL pattern '/a%' contains an invalid percent-encoding sequence.",
                          DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriInvalidPercentEncoding", "/a%");
        Diagnostic d4 = d(5, 0, 79, "URL pattern '/.;/x' must not contain dot segments with parameters.",
                          DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriDotSegmentWithParam", "/.;/x");
        Diagnostic d5 = d(5, 0, 79, "URL pattern '/a/../../b' must not contain a leading dot-dot segment or resolve above the root.",
                          DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriLeadingDotDotSegment", "/a/../../b");

        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, d1, d2, d3, d4, d5);
    }

    @Test
    public void QuoteInPatternServlet() throws Exception {
        String uri = canonUri("QuoteInPatternServlet");

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d = d(5, 0, 40, String.format(MISSING_SLASH_MSG, "foo'bar"),
                         DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriMissingLeadingSlash", "foo'bar");
        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, d);

        CodeAction fix = ca(uri, "Add leading '/' to URL pattern", d, te(5, 28, 5, 37, "\"/foo'bar\""));
        assertJavaCodeAction(createCodeActionParams(uri, d), IJDT_UTILS, fix);
    }

    @Test
    public void ConstantReferenceServlet() throws Exception {
        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/io/openliberty/sample/jakarta/servlet/canonicalization/ConstantReferenceServlet.java"));
        String uri = javaFile.getLocation().toFile().toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Constants are validated by their resolved value: "/api" and "/api" + "/v1" are valid, "api" is not
        Diagnostic d = d(5, 0, 150,
                         "URL pattern 'api' must start with a '/' (unless an extension mapping starting with '*.').",
                         DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriMissingLeadingSlash", "api");

        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, d);

        // The offending value lives in a constant, not in a string literal, so there is nothing to rewrite
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
        assertJavaCodeAction(codeActionParams, IJDT_UTILS);
    }

    @Test
    public void FragmentAndQueryServlet() throws Exception {
        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/io/openliberty/sample/jakarta/servlet/canonicalization/FragmentAndQueryServlet.java"));
        String uri = javaFile.getLocation().toFile().toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d1 = d(5, 0, 62,
                          "URL pattern '/foo#fragment' must not contain a fragment identifier ('#').",
                          DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriContainsFragment", "/foo#fragment");
        Diagnostic d2 = d(5, 0, 62,
                          "URL pattern '/bar?query=1' must not contain a query string ('?').",
                          DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriContainsQueryString", "/bar?query=1");

        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, d1, d2);

        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d1);
        TextEdit te1 = te(5, 28, 5, 43, "\"/foo\"");
        CodeAction ca1 = ca(uri, "Remove fragment from URL pattern", d1, te1);
        assertJavaCodeAction(codeActionParams1, IJDT_UTILS, ca1);

        JakartaJavaCodeActionParams codeActionParams2 = createCodeActionParams(uri, d2);
        TextEdit te2 = te(5, 45, 5, 59, "\"/bar\"");
        CodeAction ca2 = ca(uri, "Remove query string from URL pattern", d2, te2);
        assertJavaCodeAction(codeActionParams2, IJDT_UTILS, ca2);
    }

    @Test
    public void EncodedAndBackslashServlet() throws Exception {
        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/io/openliberty/sample/jakarta/servlet/canonicalization/EncodedAndBackslashServlet.java"));
        String uri = javaFile.getLocation().toFile().toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d1 = d(5, 0, 70,
                          "URL pattern '/foo%2Fbar' must not contain an encoded slash ('%2F').",
                          DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriEncodedSlash", "/foo%2Fbar");
        Diagnostic d2 = d(5, 0, 70,
                          "URL pattern '/foo\\bar' must not contain a backslash character ('\\').",
                          DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriContainsBackslash", "/foo\\bar");
        Diagnostic d3 = d(5, 0, 70,
                          "URL pattern '/foo%5Cbar' must not contain a backslash character ('\\').",
                          DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriContainsBackslash", "/foo%5Cbar");

        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, d1, d2, d3);
    }

    @Test
    public void DotSegmentsAndTraversalServlet() throws Exception {
        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/io/openliberty/sample/jakarta/servlet/canonicalization/DotSegmentsAndTraversalServlet.java"));
        String uri = javaFile.getLocation().toFile().toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d1 = d(5, 0, 89,
                          "URL pattern '/../foo' must not contain a leading dot-dot segment or resolve above the root.",
                          DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriLeadingDotDotSegment", "/../foo");
        Diagnostic d2 = d(5, 0, 89,
                          "URL pattern '/foo/..;/bar' must not contain dot segments with parameters.",
                          DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriDotSegmentWithParam", "/foo/..;/bar");
        Diagnostic d3 = d(5, 0, 89,
                          "URL pattern '/foo/%2e%2e/bar' must not contain encoded dot segments.",
                          DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriEncodedDotSegment", "/foo/%2e%2e/bar");
        Diagnostic d4 = d(5, 0, 89,
                          "URL pattern '/;/foo/bar' must not contain empty segments with parameters.",
                          DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriEmptySegmentWithParam", "/;/foo/bar");

        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, d1, d2, d3, d4);
    }

    @Test
    public void ControlCharAndEncodingServlet() throws Exception {
        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/io/openliberty/sample/jakarta/servlet/canonicalization/ControlCharAndEncodingServlet.java"));
        String uri = javaFile.getLocation().toFile().toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d1 = d(5, 0, 104,
                          "URL pattern '/foo%00/bar' must not contain control characters.",
                          DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriContainsControlChar", "/foo%00/bar");
        Diagnostic d2 = d(5, 0, 104,
                          "URL pattern '/foo%XX/bar' contains an invalid percent-encoding sequence.",
                          DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriInvalidPercentEncoding", "/foo%XX/bar");
        Diagnostic d3 = d(5, 0, 104,
                          "URL pattern '/foo%E2%82/bar' contains a malformed UTF-8 sequence.",
                          DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriDecodeError", "/foo%E2%82/bar");
        Diagnostic d4 = d(5, 0, 104,
                          "URL pattern '/foo\t/bar' must not contain control characters.",
                          DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriContainsControlChar", "/foo\t/bar");

        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, d1, d2, d3, d4);
    }

    @Test
    public void WildcardAndGlobFilter() throws Exception {
        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path("src/main/java/io/openliberty/sample/jakarta/servlet/canonicalization/WildcardAndGlobFilter.java"));
        String uri = javaFile.getLocation().toFile().toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d1 = d(5, 0, 60,
                          "URL pattern '/**' uses '/**', which in Jakarta Servlet is treated as an exact literal path match and does not match sub-paths.",
                          DiagnosticSeverity.Warning, "jakarta-servlet", "ServletUriSpringGlobPattern", "/**");
        Diagnostic d2 = d(5, 0, 60,
                          "URL pattern '/foo/*/bar' contains an invalid wildcard placement. Wildcards are only permitted as '/*' suffix or '*.' prefix.",
                          DiagnosticSeverity.Error, "jakarta-servlet", "ServletUriMisplacedWildcard", "/foo/*/bar");
        Diagnostic d3 = d(5, 0, 60,
                          "URL pattern '/api/**' uses '/**', which in Jakarta Servlet is treated as an exact literal path match and does not match sub-paths.",
                          DiagnosticSeverity.Warning, "jakarta-servlet", "ServletUriSpringGlobPattern", "/api/**");

        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, d1, d2, d3);

        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d1);
        TextEdit te1 = te(5, 27, 5, 32, "\"/*\"");
        CodeAction ca1 = ca(uri, "Replace '/**' with '/*'", d1, te1);
        assertJavaCodeAction(codeActionParams1, IJDT_UTILS, ca1);

        JakartaJavaCodeActionParams codeActionParams3 = createCodeActionParams(uri, d3);
        TextEdit te3 = te(5, 48, 5, 57, "\"/api/*\"");
        CodeAction ca3 = ca(uri, "Replace '/**' with '/*'", d3, te3);
        assertJavaCodeAction(codeActionParams3, IJDT_UTILS, ca3);
    }
}
