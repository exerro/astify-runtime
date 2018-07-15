package astify;

import astify.token.TokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class Pattern {

    private List<MatchPredicate> predicates = new ArrayList<>();

    public Pattern addPredicate(MatchPredicate predicate) {
        predicates.add(predicate);
        return this;
    }

    Matcher addPredicates(Matcher matcher) {
        for (MatchPredicate predicate : predicates) {
            matcher.addPredicate(predicate);
        }

        return matcher;
    }

    abstract Matcher getMatcher();

    public static final class TokenPattern extends Pattern {
        private final TokenType type;
        private final String value;

        TokenPattern(TokenType type, String value) {
            assert type != null;
            assert value != null;
            this.type = type;
            this.value = value;
        }

        TokenPattern(TokenType type) {
            assert type != null;
            this.type = type;
            this.value = null;
        }

        @Override Matcher getMatcher() {
            return addPredicates(value == null ? new Matcher.TokenMatcher(type) : new Matcher.TokenMatcher(type, value));
        }
    }

    public static final class NothingPattern extends Pattern {
        NothingPattern() {
            // empty
        }

        @Override Matcher getMatcher() {
            return addPredicates(new Matcher.NothingMatcher());
        }
    }

    public final static class SequencePattern extends Pattern {
        final String name;
        private final List<Pattern> patterns;
        private final CaptureGenerator generator;

        SequencePattern(String name, List<Pattern> patterns, CaptureGenerator generator) {
            assert patterns != null;
            assert generator != null;
            this.name = name;
            this.patterns = patterns;
            this.generator = generator;
        }

        @Override Matcher getMatcher() {
            List<Matcher> matchers = new ArrayList<>();

            for (Pattern pattern : patterns) {
                matchers.add(pattern.getMatcher());
            }

            return addPredicates(new Matcher.SequenceMatcher(name, matchers, generator));
        }
    }

    public static final class BranchPattern extends Pattern {
        private final List<Pattern> branches;

        BranchPattern(List<Pattern> branches) {
            assert branches != null;
            this.branches = branches;
        }

        @Override Matcher getMatcher() {
            List<Matcher> matchers = new ArrayList<>();

            for (Pattern pattern : branches) {
                matchers.add(pattern.getMatcher());
            }

            return addPredicates(new Matcher.BranchMatcher(matchers));
        }
    }

    public static final class GeneratorPattern extends Pattern {
        private final Matcher.GeneratorMatcher.MatcherGenerator generator;

        GeneratorPattern(Matcher.GeneratorMatcher.MatcherGenerator generator) {
            assert generator != null;
            this.generator = generator;
        }

        @Override Matcher getMatcher() {
            return addPredicates(new Matcher.GeneratorMatcher(generator));
        }
    }

    public static final class OptionalPattern extends Pattern {
        private final Pattern pattern;
        private final CaptureGenerator generator;

        OptionalPattern(Pattern pattern) {
            assert pattern != null;
            this.pattern = pattern;
            this.generator = null;
        }

        OptionalPattern(Pattern pattern, CaptureGenerator generator) {
            this.pattern = pattern;
            this.generator = generator;
        }

        @Override Matcher getMatcher() {
            return addPredicates(new Matcher.BranchMatcher(Arrays.asList(
                pattern.getMatcher(),
                generator == null ? new Matcher.NothingMatcher() : new Matcher.SequenceMatcher(null, Collections.singletonList(new Matcher.NothingMatcher()), generator)
            )));
        }
    }

    public static final class ListPattern extends Pattern {
        private final Pattern pattern;

        ListPattern(Pattern pattern) {
            assert pattern != null;
            this.pattern = pattern;
        }

        // unwraps a list of `captures`, adds `capture` to the beginning, and returns the resulting list
        static Capture generateFromList(Capture capture, Capture captures) {
            assert captures instanceof Capture.ListCapture;

            Capture.ListCapture listCapture = (Capture.ListCapture) captures;
            List<Capture> allCaptures = new ArrayList<>();

            allCaptures.add(capture);
            allCaptures.addAll((List<Capture>) listCapture.all());

            return Capture.ListCapture.createFrom(allCaptures);
        }

        static Capture generateFromList(List<Capture> captures) {
            assert captures.size() == 2;
            return generateFromList(captures.get(0), captures.get(1));
        }

        Matcher generateMatcher() {
            return new OptionalPattern(
                    new Pattern.SequencePattern(null,
                            Arrays.asList(pattern, new GeneratorPattern(this::generateMatcher)),
                            ListPattern::generateFromList
                    ),
                    (captures) -> Capture.ListCapture.createEmpty(captures.get(0).spanningPosition)
            ).getMatcher();
        }

        @Override public Matcher getMatcher() {
            Matcher matcher = pattern.getMatcher();
            return addPredicates(generateMatcher());
        }
    }

    public static final class DelimitedPattern extends Pattern {
        private final Pattern pattern, delim;

        DelimitedPattern(Pattern pattern, Pattern delim) {
            assert pattern != null;
            assert delim != null;
            this.pattern = pattern;
            this.delim = delim;
        }

        @Override public Matcher getMatcher() {
            return addPredicates(new Matcher.SequenceMatcher(null, Arrays.asList(
                    pattern.getMatcher(),
                    new ListPattern(new SequencePattern(null, Arrays.asList(delim, pattern), Capture.nth(1))).getMatcher()
            ), ListPattern::generateFromList));
        }
    }
}
