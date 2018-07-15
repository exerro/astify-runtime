package astify;

import astify.token.Token;
import astify.token.TokenType;

import java.util.ArrayList;
import java.util.List;

abstract class Matcher {
    private final List<MatchPredicate> predicates;

    Matcher() {
        predicates = new ArrayList<>();
    }

    Matcher addPredicate(MatchPredicate predicate) {
        predicates.add(predicate);
        return this;
    }

    List<MatchPredicate> getPredicates() {
        return new ArrayList<>(predicates);
    }

    static class TokenMatcher extends Matcher {
        private final TokenType type;
        private final String value;

        TokenMatcher(TokenType type, String value) {
            assert type != null;
            assert value != null;
            this.type = type;
            this.value = value;
        }

        TokenMatcher(TokenType type) {
            assert type != null;
            this.type = type;
            this.value = null;
        }

        boolean matches(Token token) {
            return value == null ? token.matches(type) : token.matches(type, value);
        }

        ParserFailure getError(Token token, List<String> sources) {
            if (type == token.getType()) {
                return new ParserFailure.TokenValueMatchFailure(sources, value);
            }
            return new ParserFailure.TokenTypeMatchFailure(sources, type, value);
        }

        @Override public String toString() {
            return "<token-matcher " + type.toString() + (value == null ? "" : ", " + value) + ">";
        }
    }

    static class NothingMatcher extends Matcher {
        NothingMatcher() {
            // empty
        }

        @Override public String toString() {
            return "<nothing-matcher>";
        }
    }

    static class SequenceMatcher extends Matcher {
        final String name;

        private final List<Matcher> matchers;
        private final CaptureGenerator generator;

        SequenceMatcher(String name, List<Matcher> matchers, CaptureGenerator generator) {
            this.name = name;
            this.matchers = matchers;
            this.generator = generator;
        }

        Matcher getMatcher(int i) {
            return matchers.get(i);
        }

        int getMatcherCount() {
            return matchers.size();
        }

        Capture generate(List<Capture> captures) {
            return generator.generate(captures);
        }

        @Override public String toString() {
            return name == null ? "<sequence-matcher>" : "<sequence-matcher '" + name + "'>";
        }
    }

    static class BranchMatcher extends Matcher {
        private final List<Matcher> branches;

        BranchMatcher(List<Matcher> branches) {
            this.branches = branches;
        }

        Matcher getBranch(int i) {
            return branches.get(i);
        }

        int getBranchCount() {
            return branches.size();
        }

        @Override public String toString() {
            return "<branch-matcher>";
        }
    }

    static class GeneratorMatcher extends Matcher {
        interface MatcherGenerator {
            Matcher generate();
        }

        private final MatcherGenerator generator;

        GeneratorMatcher(MatcherGenerator generator) {
            this.generator = generator;
        }

        Matcher generate() {
            return generator.generate();
        }

        @Override public String toString() {
            return "<generator-matcher>";
        }
    }
}
