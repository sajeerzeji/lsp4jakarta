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
import org.eclipse.jdt.core.dom.Expression;
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
    private final String targetPattern;

    public ServletPatternTransformationProposal(String label, ICompilationUnit cu,
                                                int relevance, ASTNode annotationNode,
                                                UnaryOperator<String> transformer,
                                                String targetPattern) {
        super(label, CodeActionKind.QuickFix, cu, null, relevance);
        this.annotationNode = annotationNode;
        this.transformer = transformer;
        this.targetPattern = targetPattern;
    }

    /**
     * Returns whether the annotation declares a string literal equal to {@code pattern} in its
     * {@code value} or {@code urlPatterns} member, i.e. whether this proposal can edit it.
     * A pattern coming from a constant reference has no literal to rewrite.
     *
     * @param annotationNode the {@code @WebServlet}/{@code @WebFilter} annotation node, may be null
     * @param pattern the URL pattern value to look for
     */
    public static boolean hasLiteral(ASTNode annotationNode, String pattern) {
        if (annotationNode instanceof SingleMemberAnnotation single) {
            return hasLiteral(single.getValue(), pattern);
        }
        if (annotationNode instanceof NormalAnnotation normal) {
            for (Object obj : normal.values()) {
                if (obj instanceof MemberValuePair pair) {
                    String name = pair.getName().getIdentifier();
                    if (("value".equals(name) || "urlPatterns".equals(name)) && hasLiteral(pair.getValue(), pattern)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean hasLiteral(Expression expr, String pattern) {
        if (expr instanceof StringLiteral literal) {
            return literal.getLiteralValue().equals(pattern);
        }
        if (expr instanceof ArrayInitializer arrayInit) {
            for (Object elem : arrayInit.expressions()) {
                if (elem instanceof StringLiteral literal && literal.getLiteralValue().equals(pattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ASTRewrite getRewrite() {
        AST ast = annotationNode.getAST();
        ASTRewrite rewrite = ASTRewrite.create(ast);

        if (annotationNode instanceof SingleMemberAnnotation single) {
            transformExpression(ast, rewrite, single.getValue(), single, SingleMemberAnnotation.VALUE_PROPERTY);
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
            rewriteLiteral(ast, rewrite, oldLiteral, parent, property);
        } else if (expr instanceof ArrayInitializer arrayInit) {
            for (Object elem : arrayInit.expressions()) {
                if (elem instanceof StringLiteral oldLiteral) {
                    rewriteLiteral(ast, rewrite, oldLiteral, null, null);
                }
            }
        }
    }

    private void rewriteLiteral(AST ast, ASTRewrite rewrite, StringLiteral oldLiteral,
                                ASTNode parent, org.eclipse.jdt.core.dom.StructuralPropertyDescriptor property) {
        String oldValue = oldLiteral.getLiteralValue();
        if (targetPattern != null && !targetPattern.equals(oldValue)) {
            return;
        }
        String newValue = transformer.apply(oldValue);
        if (newValue.equals(oldValue)) {
            return;
        }
        StringLiteral newLiteral = ast.newStringLiteral();
        newLiteral.setLiteralValue(newValue);
        if (parent != null && property != null) {
            rewrite.set(parent, property, newLiteral, null);
        } else {
            rewrite.replace(oldLiteral, newLiteral, null);
        }
    }
}
