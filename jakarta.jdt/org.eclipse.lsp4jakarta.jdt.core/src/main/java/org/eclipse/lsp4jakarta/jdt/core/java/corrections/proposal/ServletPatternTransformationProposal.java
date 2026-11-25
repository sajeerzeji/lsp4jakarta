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
package org.eclipse.lsp4jakarta.jdt.core.java.corrections.proposal;

import java.util.function.UnaryOperator;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.lsp4j.CodeActionKind;

/**
 * CodeAction proposal to transform or sanitize URL pattern string literals in
 * {@code @WebServlet} or {@code @WebFilter} annotations.
 */
public class ServletPatternTransformationProposal extends ASTRewriteCorrectionProposal {

    private final ASTNode annotationNode;
    private final UnaryOperator<String> transformer;

    public ServletPatternTransformationProposal(String label, ICompilationUnit cu, CompilationUnit invocationNode,
                                                int relevance, IBinding binding, ASTNode annotationNode,
                                                UnaryOperator<String> transformer) {
        super(label, CodeActionKind.QuickFix, cu, null, relevance);
        this.annotationNode = annotationNode;
        this.transformer = transformer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ASTRewrite getRewrite() {
        AST ast = annotationNode.getAST();
        ASTRewrite rewrite = ASTRewrite.create(ast);

        if (annotationNode instanceof SingleMemberAnnotation single) {
            Expression valueExpr = single.getValue();
            transformExpression(ast, rewrite, valueExpr, single, SingleMemberAnnotation.VALUE_PROPERTY);
        } else if (annotationNode instanceof NormalAnnotation normal) {
            for (Object obj : normal.values()) {
                if (obj instanceof MemberValuePair pair) {
                    String name = pair.getName().getIdentifier();
                    if ("value".equals(name) || "urlPatterns".equals(name)) {
                        transformExpression(ast, rewrite, pair.getValue(), pair, MemberValuePair.VALUE_PROPERTY);
                    }
                }
            }
        }
        return rewrite;
    }

    private void transformExpression(AST ast, ASTRewrite rewrite, Expression expr,
                                     ASTNode parent, org.eclipse.jdt.core.dom.StructuralPropertyDescriptor property) {
        if (expr instanceof StringLiteral oldLiteral) {
            String oldValue = oldLiteral.getLiteralValue();
            String newValue = transformer.apply(oldValue);
            if (!newValue.equals(oldValue)) {
                StringLiteral newLiteral = ast.newStringLiteral();
                newLiteral.setLiteralValue(newValue);
                rewrite.set(parent, property, newLiteral, null);
            }
        } else if (expr instanceof ArrayInitializer arrayInit) {
            for (Object elem : arrayInit.expressions()) {
                if (elem instanceof StringLiteral oldLiteral) {
                    String oldValue = oldLiteral.getLiteralValue();
                    String newValue = transformer.apply(oldValue);
                    if (!newValue.equals(oldValue)) {
                        StringLiteral newLiteral = ast.newStringLiteral();
                        newLiteral.setLiteralValue(newValue);
                        rewrite.replace(oldLiteral, newLiteral, null);
                    }
                }
            }
        }
    }
}
