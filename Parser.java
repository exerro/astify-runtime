package astify;

import astify.core.Position;
import astify.token.Token;
import astify.token.TokenException;
import astify.token.TokenGenerator;
import astify.token.TokenType;

import java.util.*;

public class Parser {
    private static final boolean debugPrint = false;

    private static class PartialResult {
        private final Capture result;
        private final List<MatchPredicate> predicates;
        private final List<String> sources;

        private PartialResult(Capture result, List<MatchPredicate> predicates, List<String> sources) {
            this.result = result;
            this.predicates = predicates;
            this.sources = sources;
        }
    }

    private final List<Matcher> matchers = new ArrayList<>();
    private final List<MatcherSequence> sequences = new ArrayList<>();
    private final List<List<MatchPredicate>> predicates = new ArrayList<>();
    private final List<PartialResult> partialResults = new ArrayList<>();

    private boolean finished = false;
    private List<Capture> results;
    private Position lastPosition;
    private List<ParserException> exceptions;

    public Parser() {
        exceptions = new ArrayList<>();
        results = new ArrayList<>();
    }

    public void setup(Pattern pattern, Position begin) {
        assert pattern != null;
        assert begin != null;

        Matcher.SequenceMatcher sequenceMatcher = pattern.getMatcher() instanceof Matcher.SequenceMatcher
                ? (Matcher.SequenceMatcher) pattern.getMatcher()
                : new Matcher.SequenceMatcher(null, Collections.singletonList(pattern.getMatcher()), (captures) -> captures.get(0));

        matchers.clear();
        sequences.clear();
        predicates.clear();

        sequences.add(new MatcherSequence(null, sequenceMatcher));
        matchers.add(pattern.getMatcher());
        predicates.add(new ArrayList<>());
        results = new ArrayList<>();
        exceptions = new ArrayList<>();
        lastPosition = begin;
        finished = false;
    }

    public List<Capture> getResults() {
        return results;
    }

    public void parse(TokenGenerator generator) throws TokenException {
        Token token;

        while ((token = generator.getNext()).getType() != TokenType.EOF) {
            feedToken(token);

            if (hasError()) {
                return;
            }
        }

        finish();
    }

    public void feedToken(Token token) {
        Set<ParserFailure> failures = new HashSet<>();

        if (sequences.size() == 0) return;

        prepare();

        for (PartialResult partial : partialResults) {
            for (MatchPredicate predicate : partial.predicates) {
                MatchPredicate.State state = new MatchPredicate.State(token, lastPosition, partial.sources);

                if (predicate.test(state)) {
                    results.add(partial.result);
                }
                else {
                    failures.add(predicate.getError(state));
                }
            }
        }

        partialResults.clear();

        for (int i = matchers.size() - 1; i >= 0; --i) {
            Matcher.TokenMatcher matcher = (Matcher.TokenMatcher) matchers.get(i);
            MatcherSequence sequence = sequences.get(i);
            MatchPredicate.State predicateState = new MatchPredicate.State(token, lastPosition, getSources(sequence));
            boolean failed = false;

            if (matcher.matches(token)) {
                sequences.set(i, sequence.addCapture(new Capture.TokenCapture(token)));
            }
            else {
                failed = true;
                failures.add(matcher.getError(token, predicateState.sources));
            }

            if (!failed) {
                for (MatchPredicate predicate : predicates.get(i)) {
                    if (!predicate.test(predicateState)) {
                        failed = true;
                        failures.add(predicate.getError(predicateState));
                    }
                }
            }

            if (failed) {
                matchers.remove(i);
                sequences.remove(i);
                predicates.remove(i);
            }
            else {
                predicates.get(i).clear();
            }
        }

        lastPosition = token.getPosition();

        if (sequences.size() == 0) {
            exceptions = ParserException.generateFrom(failures, token);
        }
    }

    public void finish() {
        feedToken(new Token(TokenType.EOF, "", lastPosition.after(1)));

        for (int i = sequences.size() - 1; i >= 0; --i) {
            updateMatcher(i);
        }
    }

    public boolean hasError() {
        return exceptions.size() > 0 && sequences.size() == 0 && results.size() == 0;
    }

    public List<ParserException> getExceptions() {
        return exceptions;
    }

    private List<String> getSources(MatcherSequence sequence) {
        List<String> sources = new ArrayList<>();

        while (sequence != null) {
            if (sequence.getMatcherName() != null && !sequence.getMatcherName().equals(""))
                sources.add(sequence.getMatcherName());
            sequence = sequence.getParent();
        }

        return sources;
    }

    private void prepare() {
        assert !finished;

        if (debugPrint) System.out.println("prepare()");

        for (int i = sequences.size() - 1; i >= 0; --i) {
            updateMatcher(i);
        }

        for (int i = sequences.size() - 1; i >= 0; --i) {
            boolean first = true;

            if (debugPrint) System.out.print(i + " / " + (sequences.size() - 1) + " :: ");

            while (true) {
                MatcherSequence sequence = sequences.get(i);
                Matcher matcher = matchers.get(i);

                predicates.get(i).addAll(matcher.getPredicates());

                if (!first) { if (debugPrint) System.out.print(" -> "); }
                else first = false;

                if (debugPrint) System.out.print(matcher.toString() + " [" + matcher.getPredicates().size() + "/" + predicates.get(i).size() + "]");

                if (matcher instanceof Matcher.NothingMatcher) {
                    sequences.set(i, sequence.addCapture(new Capture.EmptyCapture(new Position(lastPosition.source, lastPosition.line2, lastPosition.char2))));

                    if (updateMatcher(i)) break;
                }
                else if (matcher instanceof Matcher.BranchMatcher) {
                    Matcher.BranchMatcher branchMatcher = (Matcher.BranchMatcher) matcher;
                    List<MatchPredicate> predicateList = predicates.get(i);
                    sequence.notifyBranch(branchMatcher.getBranchCount());
                    sequences.remove(i);
                    matchers.remove(i);
                    predicates.remove(i);

                    for (int j = 0; j < branchMatcher.getBranchCount(); ++j) {
                        sequences.add(i + j, sequence);
                        matchers.add(i + j, branchMatcher.getBranch(j));
                        predicates.add(i + j, new ArrayList<>(predicateList));
                    }

                    i += branchMatcher.getBranchCount();

                    break;
                }
                else if (matcher instanceof Matcher.SequenceMatcher) {
                    Matcher.SequenceMatcher sequenceMatcher = (Matcher.SequenceMatcher) matcher;
                    sequence.notifySubSequence();
                    sequences.set(i, new MatcherSequence(sequence, sequenceMatcher));
                    if (updateMatcher(i)) break;
                }
                else if (matcher instanceof Matcher.GeneratorMatcher) {
                    Matcher.GeneratorMatcher generatorMatcher = (Matcher.GeneratorMatcher) matcher;
                    matchers.set(i, generatorMatcher.generate());
                }
                else {
                    assert matcher instanceof Matcher.TokenMatcher;
                    break;
                }
            }

            if (debugPrint) System.out.println();
        }
    }

    private boolean updateMatcher(int i) {
        MatcherSequence sequence = sequences.get(i);

        if (sequence.isFinished()) {
            MatcherSequence.CompletionPair pair = sequence.complete();

            if (pair.sequence == null) {

                if (predicates.get(i).size() > 0) {
                    partialResults.add(new PartialResult(pair.result, predicates.get(i), getSources(sequence)));
                }
                else {
                    results.add(pair.result);
                }

                sequences.remove(i);
                matchers.remove(i);
                predicates.remove(i);

                return true;
            }
            else {
                sequences.set(i, pair.sequence.addCapture(pair.result));
                return updateMatcher(i);
            }
        }
        else {
            matchers.set(i, sequence.getNextMatcher());
        }
        return false;
    }
}
