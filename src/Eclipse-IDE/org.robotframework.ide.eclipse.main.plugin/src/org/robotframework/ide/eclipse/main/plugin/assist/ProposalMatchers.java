/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.assist;

import java.util.Optional;

import org.rf.ide.core.testdata.model.table.keywords.names.EmbeddedKeywordNamesSupport;

import com.google.common.collect.Range;

public class ProposalMatchers {

    public static ProposalMatcher prefixesMatcher() {
        return new ProposalMatcher() {

            @Override
            public Optional<ProposalMatch> matches(final String userContent, final String proposalContent) {
                if (proposalContent.toLowerCase().startsWith(userContent.toLowerCase())) {
                    return Optional.of(new ProposalMatch(Range.closedOpen(0, userContent.length())));
                } else {
                    return Optional.empty();
                }
            }
        };
    }

    public static ProposalMatcher embeddedKeywordsMatcher() {
        return new ProposalMatcher() {

            @Override
            public Optional<ProposalMatch> matches(final String userContent, final String proposalContent) {
                final int index = EmbeddedKeywordNamesSupport.startsWithIgnoreCase(proposalContent, userContent);
                if (index >= 0) {
                    return Optional.of(new ProposalMatch(Range.closedOpen(0, index)));
                } else {
                    return Optional.empty();
                }
            }
        };
    }

    public static ProposalMatcher pathsMatcher() {
        return new ProposalMatcher() {

            @Override
            public Optional<ProposalMatch> matches(final String userContent, final String proposalContent) {
                if (proposalContent.toLowerCase().startsWith(userContent.toLowerCase())) {
                    return Optional.of(new ProposalMatch(Range.closedOpen(0, userContent.length())));
                }
                final int index = proposalContent.toLowerCase().indexOf("/" + userContent.toLowerCase());
                if (index >= 0) {
                    return Optional
                            .of(new ProposalMatch(Range.closedOpen(index + 1, index + 1 + userContent.length())));
                }
                return Optional.empty();
            }
        };
    }
}
