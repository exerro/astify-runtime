package astify;

import astify.core.Position;
import astify.token.Token;

import java.util.*;

public class ParserException extends Exception {
    private final Position position;
    private final String message;

    public ParserException(Position position, String message) {
        this.position = position;
        this.message = message;
    }

    public Position getPosition() {
        return position;
    }

    public String getMessage() {
        return message;
    }

    public String toString() {
        return position.source.getName() + ": " + this.getClass().getName() + "\n\t" + message + "\n" + position.getLineAndCaret();
    }

    public static ParserException combine(List<ParserException> exceptions) {
        assert exceptions.size() > 0 : "Cannot combine 0 exceptions";

        if (exceptions.size() == 1) return exceptions.get(0);

        List<String> messages = new ArrayList<>();
        Position position = exceptions.get(0).position;

        for (ParserException exception : exceptions) {
            assert position.equals(exception.position) : "Cannot combine exceptions thrown at different positions";
            messages.add(exception.getMessage());
        }

        return new ParserException(position, String.join("\n\t", messages));
    }

    static List<ParserException> generateFrom(Set<ParserFailure> failures, Token token) {
        Map<List<String>, Set<ParserFailure>> groups = new HashMap<>();
        Map<List<String>, String> sourceNames = new HashMap<>();
        List<ParserException> results = new ArrayList<>();
        boolean includeSource;

        failures = ParserFailure.simplifySources(failures);

        for (ParserFailure failure : failures) {
            groups.computeIfAbsent(failure.sources, (ignored) -> new HashSet<>()).add(failure);
            sourceNames.put(failure.sources, failure.getSources());
        }

        includeSource = groups.size() > 1;

        for (List<String> group : groups.keySet()) {
            Set<ParserFailure> sourceFailures = groups.get(group);
            List<String> expected = new ArrayList<>();
            List<ParserFailure> otherFailures = new ArrayList<>();

            for (ParserFailure failure : sourceFailures) {
                if (failure.getExpected() != null) {
                    expected.add(failure.getExpected());
                }
                else {
                    otherFailures.add(failure);
                }
            }

            String message;

            if (expected.size() == 1) {
                message = "Expected " + expected.get(0);
            }
            else {
                message = "Expected one of {" + String.join(", ", expected) + "}";
            }

            if (expected.size() > 0) {
                results.add(new ParserException(token.getPosition(), message + ", got " + token.toString() + (includeSource ? " (in parse as " + sourceNames.get(group) + ")" : "")));
            }

            for (ParserFailure failure : otherFailures) {
                results.add(new ParserException(token.getPosition(), failure.getError() + (includeSource ? " (in parse as " + sourceNames.get(group) + ")" : "")));
            }
        }

        return results;
    }
}
