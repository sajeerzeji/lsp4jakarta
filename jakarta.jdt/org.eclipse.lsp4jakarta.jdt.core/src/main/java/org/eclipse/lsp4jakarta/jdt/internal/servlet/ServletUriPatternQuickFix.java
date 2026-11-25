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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4jakarta.commons.codeaction.CodeActionResolveData;
import org.eclipse.lsp4jakarta.commons.codeaction.ICodeActionId;
import org.eclipse.lsp4jakarta.commons.codeaction.JakartaCodeActionId;
import org.eclipse.lsp4jakarta.jdt.core.java.codeaction.ExtendedCodeAction;
import org.eclipse.lsp4jakarta.jdt.core.java.codeaction.IJavaCodeActionParticipant;
import org.eclipse.lsp4jakarta.jdt.core.java.codeaction.JavaCodeActionContext;
import org.eclipse.lsp4jakarta.jdt.core.java.codeaction.JavaCodeActionResolveContext;
import org.eclipse.lsp4jakarta.jdt.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4jakarta.jdt.core.java.corrections.proposal.ServletPatternTransformationProposal;
import org.eclipse.lsp4jakarta.jdt.internal.Messages;

/**
 * Consolidated QuickFix participant for Jakarta Servlet URL pattern transformations.
 */
public class ServletUriPatternQuickFix implements IJavaCodeActionParticipant {

    private static final Logger LOGGER = Logger.getLogger(ServletUriPatternQuickFix.class.getName());

    @Override
    public String getParticipantId() {
        return ServletUriPatternQuickFix.class.getName();
    }

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic,
                                                     IProgressMonitor monitor) throws CoreException {
        if (diagnostic.getCode() == null || diagnostic.getCode().getLeft() == null) {
            return Collections.emptyList();
        }

        String diagnosticCode = diagnostic.getCode().getLeft();
        ICodeActionId id = getCodeActionId(diagnosticCode);
        String label = getLabel(id);

        if (id == null || label == null) {
            return Collections.emptyList();
        }

        Map<String, Object> extendedData = null;
        String message = diagnostic.getMessage() != null && diagnostic.getMessage().isLeft() ? diagnostic.getMessage().getLeft() : null;
        String pattern = extractPattern(message);
        if (pattern != null) {
            extendedData = Collections.singletonMap(Constants.PATTERN, pattern);
        }

        ExtendedCodeAction codeAction = new ExtendedCodeAction(label);
        codeAction.setRelevance(0);
        codeAction.setKind(CodeActionKind.QuickFix);
        codeAction.setDiagnostics(Collections.singletonList(diagnostic));
        codeAction.setData(new CodeActionResolveData(context.getUri(), getParticipantId(), context.getParams().getRange(), extendedData, context.getParams().isResourceOperationSupported(), context.getParams().isCommandConfigurationUpdateSupported(), id));

        return Collections.singletonList(codeAction);
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        CodeAction toResolve = context.getUnresolved();
        if (!(toResolve.getData() instanceof CodeActionResolveData)) {
            return toResolve;
        }

        CodeActionResolveData data = (CodeActionResolveData) toResolve.getData();
        String actionId = data.getCodeActionId();
        UnaryOperator<String> transformer = getTransformer(actionId);
        String label = toResolve.getTitle();

        if (transformer == null) {
            return toResolve;
        }

        ASTNode node = context.getCoveredNode();
        String targetPattern = data.getExtendedDataEntry(Constants.PATTERN) instanceof String s ? s : null;

        ChangeCorrectionProposal proposal = new ServletPatternTransformationProposal(label, context.getCompilationUnit(), 0, node, transformer, targetPattern);

        try {
            toResolve.setEdit(context.convertToWorkspaceEdit(proposal));
        } catch (CoreException e) {
            LOGGER.log(Level.SEVERE, "Unable to create workspace edit for servlet pattern transformation", e);
        }
        return toResolve;
    }

    private static String extractPattern(String message) {
        if (message == null) {
            return null;
        }
        int firstQuote = message.indexOf('\'');
        if (firstQuote != -1) {
            int secondQuote = message.indexOf('\'', firstQuote + 1);
            if (secondQuote != -1) {
                return message.substring(firstQuote + 1, secondQuote);
            }
        }
        return null;
    }

    private static ICodeActionId getCodeActionId(String diagnosticCode) {
        if (ErrorCode.ServletUriMissingLeadingSlash.name().equals(diagnosticCode)) {
            return JakartaCodeActionId.ServletPrefixSlashUrlPattern;
        } else if (ErrorCode.ServletUriContainsFragment.name().equals(diagnosticCode)) {
            return JakartaCodeActionId.ServletRemoveFragmentUrlPattern;
        } else if (ErrorCode.ServletUriContainsQueryString.name().equals(diagnosticCode)) {
            return JakartaCodeActionId.ServletRemoveQueryStringUrlPattern;
        } else if (ErrorCode.ServletUriSpringGlobPattern.name().equals(diagnosticCode)) {
            return JakartaCodeActionId.ServletReplaceGlobUrlPattern;
        }
        return null;
    }

    private static String getLabel(ICodeActionId id) {
        if (JakartaCodeActionId.ServletPrefixSlashUrlPattern.equals(id)) {
            return Messages.getMessage("PrefixSlashToUrlPattern");
        } else if (JakartaCodeActionId.ServletRemoveFragmentUrlPattern.equals(id)) {
            return Messages.getMessage("RemoveFragmentFromUrlPattern");
        } else if (JakartaCodeActionId.ServletRemoveQueryStringUrlPattern.equals(id)) {
            return Messages.getMessage("RemoveQueryStringFromUrlPattern");
        } else if (JakartaCodeActionId.ServletReplaceGlobUrlPattern.equals(id)) {
            return Messages.getMessage("ReplaceGlobInUrlPattern");
        }
        return null;
    }

    private static UnaryOperator<String> getTransformer(String actionId) {
        if (JakartaCodeActionId.ServletPrefixSlashUrlPattern.getId().equals(actionId)) {
            return old -> (!old.startsWith(Constants.FORWARD_SLASH)
                           && !old.startsWith(Constants.EXTENSION_MAPPING_PREFIX)
                           && !old.isEmpty()) ? Constants.FORWARD_SLASH + old : old;
        } else if (JakartaCodeActionId.ServletRemoveFragmentUrlPattern.getId().equals(actionId)) {
            return old -> old.contains(Constants.HASH) ? old.substring(0, old.indexOf(Constants.HASH)) : old;
        } else if (JakartaCodeActionId.ServletRemoveQueryStringUrlPattern.getId().equals(actionId)) {
            return old -> old.contains(Constants.QUESTION_MARK) ? old.substring(0, old.indexOf(Constants.QUESTION_MARK)) : old;
        } else if (JakartaCodeActionId.ServletReplaceGlobUrlPattern.getId().equals(actionId)) {
            return old -> old.endsWith(Constants.SPRING_GLOB_PATTERN) ? old.substring(0, old.length() - Constants.SPRING_GLOB_PATTERN.length())
                                                                        + Constants.PATH_MAPPING_SUFFIX : old;
        }
        return null;
    }
}
