/*******************************************************************************
* Copyright (c) 2026 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/
package org.eclipse.lsp4jakarta.jdt.core.java.diagnostics.helpers;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.lsp4jakarta.jdt.internal.DiagnosticUtils;

/**
 * Constructor information diagnostics helper for a given method.
 *
 * @author Archana Iyer
 *
 */
public final class ConstructorInfoDiagnosticHelper {

    private boolean hasConstructor;
    private boolean hasValidPublicNoArgsConstructor;
    private boolean hasValidProtectedNoArgsConstructor;
    private boolean hasParameterizedConstructor;

    private ConstructorInfoDiagnosticHelper(boolean hasConstructor,
                                            boolean hasValidPublicNoArgsConstructor,
                                            boolean hasValidProtectedNoArgsConstructor,
                                            boolean hasParameterizedConstructor) {
        this.hasConstructor = hasConstructor;
        this.hasValidPublicNoArgsConstructor = hasValidPublicNoArgsConstructor;
        this.hasValidProtectedNoArgsConstructor = hasValidProtectedNoArgsConstructor;
        this.hasParameterizedConstructor = hasParameterizedConstructor;
    }

    public boolean hasConstructor() {
        return hasConstructor;
    }

    public boolean hasValidPublicNoArgsConstructor() {
        return hasValidPublicNoArgsConstructor;
    }

    public boolean hasValidProtectedNoArgsConstructor() {
        return hasValidProtectedNoArgsConstructor;
    }

    public boolean hasParameterizedConstructor() {
        return hasParameterizedConstructor;
    }

    @Override
    public String toString() {
        return "ConstructorInfoDiagnosticHelper [hasConstructor=" + hasConstructor
               + ", hasValidPublicNoArgsConstructor=" + hasValidPublicNoArgsConstructor
               + ", hasValidProtectedNoArgsConstructor=" + hasValidProtectedNoArgsConstructor
               + ", hasParameterizedConstructor=" + hasParameterizedConstructor + "]";
    }

    /**
     * Factory utility method checks the constructor existence and returns the constructor information
     *
     * @param method
     * @return
     * @throws JavaModelException
     */
    public static ConstructorInfoDiagnosticHelper getConstructorInfo(IMethod method) throws JavaModelException {
        boolean isUserDefinedConstructor = false;
        boolean isPublicNoArgsConstructor = false;
        boolean isProtectedNoArgsConstructor = false;
        boolean isParameterizedConstructor = false;

        if (DiagnosticUtils.isConstructorMethod(method)) {
            isUserDefinedConstructor = true; // Check explicit constructor declaration
            String[] params = method.getParameterTypes();
            int flags = method.getFlags();

            if (params.length == 0) { // User-defined no-args constructor
                if (Flags.isPublic(flags)) {
                    isPublicNoArgsConstructor = true;
                }
                if (Flags.isProtected(flags)) {
                    isProtectedNoArgsConstructor = true;
                }
            } else {
                // Constructor with parameters
                isParameterizedConstructor = true;
            }
        }
        return new ConstructorInfoDiagnosticHelper(isUserDefinedConstructor, isPublicNoArgsConstructor, isProtectedNoArgsConstructor, isParameterizedConstructor);
    }

    /**
     * Analyzes all methods of a type to determine constructor information.
     *
     * @param type
     * @return ConstructorInfoDiagnosticHelper
     * @throws JavaModelException
     */
    public static ConstructorInfoDiagnosticHelper getConstructorInfo(IType type) throws JavaModelException {
        ConstructorInfoDiagnosticHelper constructorInfo = ConstructorInfoDiagnosticHelper.initialize();

        for (IMethod method : type.getMethods()) {
            constructorInfo.mergeConstructorInfo(ConstructorInfoDiagnosticHelper.getConstructorInfo(method));
        }

        return constructorInfo;
    }

    /**
     * This method merges the constructor check info and retains the constructor information
     *
     * @param info
     * @return
     */
    public ConstructorInfoDiagnosticHelper mergeConstructorInfo(ConstructorInfoDiagnosticHelper calculatedValue) {
        this.hasConstructor = this.hasConstructor || calculatedValue.hasConstructor;
        this.hasValidPublicNoArgsConstructor = this.hasValidPublicNoArgsConstructor || calculatedValue.hasValidPublicNoArgsConstructor;
        this.hasValidProtectedNoArgsConstructor = this.hasValidProtectedNoArgsConstructor || calculatedValue.hasValidProtectedNoArgsConstructor;
        this.hasParameterizedConstructor = this.hasParameterizedConstructor || calculatedValue.hasParameterizedConstructor;
        return this;
    }

    /**
     * This is to return default values for constructor information
     *
     * @return
     */
    public static ConstructorInfoDiagnosticHelper initialize() {
        return new ConstructorInfoDiagnosticHelper(false, false, false, false);
    }
}