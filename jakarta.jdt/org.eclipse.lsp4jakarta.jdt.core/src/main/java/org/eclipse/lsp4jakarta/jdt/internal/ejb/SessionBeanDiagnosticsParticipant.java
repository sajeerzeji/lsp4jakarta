/*******************************************************************************
* Copyright (c) 2023, 2026 IBM Corporation and others.
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

package org.eclipse.lsp4jakarta.jdt.internal.ejb;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4jakarta.jdt.core.java.diagnostics.IJavaDiagnosticsParticipant;
import org.eclipse.lsp4jakarta.jdt.core.java.diagnostics.JavaDiagnosticsContext;
import org.eclipse.lsp4jakarta.jdt.core.java.diagnostics.helpers.ConstructorInfoDiagnosticHelper;
import org.eclipse.lsp4jakarta.jdt.core.utils.PositionUtils;
import org.eclipse.lsp4jakarta.jdt.internal.DiagnosticUtils;
import org.eclipse.lsp4jakarta.jdt.internal.Messages;

/**
 * EJB diagnostic participant that validates session beans have a public no-arg constructor.
 */
public class SessionBeanDiagnosticsParticipant implements IJavaDiagnosticsParticipant {

    @Override
    public List<Diagnostic> collectDiagnostics(JavaDiagnosticsContext context, IProgressMonitor monitor) throws CoreException {
        ITypeRoot typeRoot = context.getTypeRoot();
        String uri = context.getUri();
        IJavaElement[] elements = typeRoot.getChildren();
        List<Diagnostic> diagnostics = new ArrayList<>();

        for (IJavaElement element : elements) {
            if (monitor.isCanceled()) {
                return null;
            }

            if (element.getElementType() == IJavaElement.TYPE) {
                IType type = (IType) element;

                if (!type.isClass()) {
                    continue;
                }

                boolean isSessionBean = false;
                IAnnotation[] annotations = type.getAnnotations();

                for (IAnnotation annotation : annotations) {
                    String matchedAnnotation = DiagnosticUtils.getMatchedJavaElementName(type,
                            annotation.getElementName(),
                            Constants.SESSION_BEAN_ANNOTATIONS);
                    if (matchedAnnotation != null) {
                        isSessionBean = true;
                        break;
                    }
                }

                if (isSessionBean) {
                    ConstructorInfoDiagnosticHelper constructorInfo = ConstructorInfoDiagnosticHelper.initialize();

                    IMethod[] methods = type.getMethods();
                    for (IMethod method : methods) {
                        constructorInfo.mergeConstructorInfo(ConstructorInfoDiagnosticHelper.getConstructorInfo(method));
                    }

                    if (constructorInfo.hasConstructor() && !constructorInfo.hasValidPublicNoArgsConstructor()) {
                        String message = Messages.getMessage("SessionBeanNoPublicNoArgConstructor");
                        Range range = PositionUtils.toNameRange(type, context.getUtils());
                        diagnostics.add(context.createDiagnostic(uri, message, range,
                                Constants.DIAGNOSTIC_SOURCE,
                                ErrorCode.MissingPublicNoArgConstructor,
                                DiagnosticSeverity.Error));
                    }
                }
            }
        }

        return diagnostics;
    }
}