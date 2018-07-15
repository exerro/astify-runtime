package astify;

import java.util.ArrayList;
import java.util.List;

class MatcherSequence {

    static class CompletionPair {
        public final MatcherSequence sequence;
        public final Capture result;

        private CompletionPair(MatcherSequence sequence, Capture result) {
            this.sequence = sequence;
            this.result = result;
        }
    }

    private final MatcherSequence parent;
    private final Matcher.SequenceMatcher matcher;
    private final List<Capture> captures;

    private int children = 0;

    MatcherSequence(MatcherSequence parent, Matcher.SequenceMatcher matcher) {
        this.parent = parent;
        this.matcher = matcher;

        captures = new ArrayList<>();
    }

    private MatcherSequence(MatcherSequence copy) {
        this.parent = copy.parent;
        this.matcher = copy.matcher;
        this.captures = new ArrayList<>(copy.captures);

        if (parent != null) parent.children++;
    }

    MatcherSequence getParent() {
        return parent;
    }

    String getMatcherName() {
        return matcher.name;
    }

    void notifyBranch(int branches) {
        children = branches;
    }

    void notifySubSequence() {
        if (children == 0) children = 1;
    }

    MatcherSequence addCapture(Capture capture) {
        assert captures.size() < matcher.getMatcherCount() : matcher.name;
        assert capture != null;

        MatcherSequence self = this;

        if (children > 1) {
            self = new MatcherSequence(this);
        }

        if (children > 0) {
            children--;
        }

        self.captures.add(capture);

        return self;
    }

    CompletionPair complete() {
        assert captures.size() == matcher.getMatcherCount();
        return new CompletionPair(parent, matcher.generate(captures));
    }

    boolean isFinished() {
        return captures.size() == matcher.getMatcherCount();
    }

    Matcher getNextMatcher() {
        assert captures.size() < matcher.getMatcherCount();
        return matcher.getMatcher(captures.size());
    }
}
