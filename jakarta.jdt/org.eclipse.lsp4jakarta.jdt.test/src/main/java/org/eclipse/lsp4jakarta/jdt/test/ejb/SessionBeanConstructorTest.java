/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
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

package org.eclipse.lsp4jakarta.jdt.test.ejb;

import static org.eclipse.lsp4jakarta.jdt.test.core.JakartaForJavaAssert.assertJavaCodeAction;
import static org.eclipse.lsp4jakarta.jdt.test.core.JakartaForJavaAssert.assertJavaDiagnostics;
import static org.eclipse.lsp4jakarta.jdt.test.core.JakartaForJavaAssert.ca;
import static org.eclipse.lsp4jakarta.jdt.test.core.JakartaForJavaAssert.createCodeActionParams;
import static org.eclipse.lsp4jakarta.jdt.test.core.JakartaForJavaAssert.d;
import static org.eclipse.lsp4jakarta.jdt.test.core.JakartaForJavaAssert.te;

import java.util.Arrays;

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

public class SessionBeanConstructorTest extends BaseJakartaTest {

    protected static IJDTUtils IJDT_UTILS = JDTUtilsLSImpl.getInstance();

    @Test
    public void testInvalidStatelessBean() throws Exception {
        assertMissingPublicNoArgConstructor("src/main/java/io/openliberty/sample/jakarta/ejb/InvalidStatelessBean.java",
                33, "public InvalidStatelessBean() {\n\t}\n\n\t");
    }

    @Test
    public void testInvalidStatefulBean() throws Exception {
        assertMissingPublicNoArgConstructor("src/main/java/io/openliberty/sample/jakarta/ejb/InvalidStatefulBean.java",
                32, "public InvalidStatefulBean() {\n\t}\n\n\t");
    }

    @Test
    public void testInvalidSingletonBean() throws Exception {
        assertMissingPublicNoArgConstructor("src/main/java/io/openliberty/sample/jakarta/ejb/InvalidSingletonBean.java",
                33, "public InvalidSingletonBean() {\n\t}\n\n\t");
    }

    @Test
    public void testValidStatelessBean() throws Exception {
        assertNoDiagnostics("src/main/java/io/openliberty/sample/jakarta/ejb/ValidStatelessBean.java");
    }

    private void assertMissingPublicNoArgConstructor(String projectRelativePath, int endCharacter,
                                                     String insertedConstructor) throws Exception {
        String uri = getJavaFileUri(projectRelativePath);
        JakartaJavaDiagnosticsParams diagnosticsParams = createDiagnosticsParams(uri);

        Diagnostic d = d(9, 13, endCharacter,
                "Session beans must have a public no-argument constructor.",
                DiagnosticSeverity.Error, "jakarta-ejb", "MissingPublicNoArgConstructor");

        assertJavaDiagnostics(diagnosticsParams, IJDT_UTILS, d);

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
        TextEdit te = te(11, 4, 11, 4, insertedConstructor);
        CodeAction ca = ca(uri, "Add a default 'public' constructor to this class", d, te);
        assertJavaCodeAction(codeActionParams, IJDT_UTILS, ca);
    }

    private void assertNoDiagnostics(String projectRelativePath) throws Exception {
        assertJavaDiagnostics(createDiagnosticsParams(getJavaFileUri(projectRelativePath)), IJDT_UTILS);
    }

    private JakartaJavaDiagnosticsParams createDiagnosticsParams(String uri) {
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));
        return diagnosticsParams;
    }

    private String getJavaFileUri(String projectRelativePath) throws Exception {
        IJavaProject javaProject = loadJavaProject("jakarta-sample", "");
        IFile javaFile = javaProject.getProject().getFile(new Path(projectRelativePath));
        return javaFile.getLocation().toFile().toURI().toString();
    }
}
