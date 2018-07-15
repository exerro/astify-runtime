
package astify;

import astify.core.Position;
import astify.token.Token;
import astify.token.TokenType;

import java.util.List;

public interface MatchPredicate {
    class State {
        public final Token nextToken;
        public final Position lastTokenPosition;
        public final List<String> sources;

        public State(Token nextToken, Position lastTokenPosition, List<String> sources) {
            this.nextToken = nextToken;
            this.lastTokenPosition = lastTokenPosition;
            this.sources = sources;
        }
    }

    boolean test(State state);

    default ParserFailure getError(State state) {
        return new ParserFailure.PredicateFailure(state.sources, "failed predicate");
    }

    static MatchPredicate noSpace() {
        return new MatchPredicate() {
            @Override public boolean test(State state) {
                return state.nextToken.getPosition().isAfter(state.lastTokenPosition);
            }

            @Override public ParserFailure getError(State state) {
                return new ParserFailure.PredicateFailure(state.sources, "Unexpected space before " + state.nextToken.toString());
            }
        };
    }

    static MatchPredicate sameLine() {
        return new MatchPredicate() {
            @Override public boolean test(State state) {
                return state.nextToken.getPosition().line1 == state.lastTokenPosition.line2;
            }

            @Override public ParserFailure getError(State state) {
                return new ParserFailure.PredicateFailure(state.sources, "Unexpected newline before " + state.nextToken.toString());
            }
        };
    }

    static MatchPredicate nextLine() {
        return new MatchPredicate() {
            @Override public boolean test(State state) {
                return state.nextToken.getType() == TokenType.EOF || state.nextToken.getPosition().line1 > state.lastTokenPosition.line2;
            }

            @Override public ParserFailure getError(State state) {
                return new ParserFailure.PredicateFailure(state.sources, "Expected newline before " + state.nextToken.toString());
            }
        };
    }

    static MatchPredicate matches(Pattern.TokenPattern pattern) {
        return new MatchPredicate() {
            @Override public boolean test(State state) {
                return ((Matcher.TokenMatcher) pattern.getMatcher()).matches(state.nextToken);
            }

            @Override public ParserFailure getError(State state) {
                return ((Matcher.TokenMatcher) pattern.getMatcher()).getError(state.nextToken, state.sources);
            }
        };
    }
}
