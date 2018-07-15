package astify;

import astify.token.TokenType;

import java.util.List;
import java.util.Set;

abstract class ParserFailure {
    final List<String> sources;

    ParserFailure(List<String> sources) {
        this.sources = sources;
    }

    abstract String getExpected();
    abstract String getError();

    String getSources() {
        if (sources.size() == 0) return "";

        StringBuilder builder = new StringBuilder(sources.get(sources.size() - 1));

        for (int i = sources.size() - 2; i >= 0; --i) {
            builder.append(".");
            builder.append(sources.get(i));
        }

        return builder.toString();
    }

    static Set<ParserFailure> simplifySources(Set<ParserFailure> failures) {
        if (failures.isEmpty()) return failures;

        ParserFailure someFailure = failures.iterator().next();
        List<String> compareTo = someFailure.sources;
        boolean removeNext = false;

        while (compareTo.size() >= 0) {
            String comparingTo = compareTo.get(compareTo.size() - 1);
            boolean allMatch = compareTo.size() > 0;

            for (ParserFailure failure : failures) {
                if (removeNext) failure.sources.remove(failure.sources.size() - 1);
                allMatch = allMatch && failure.sources.size() > 1 && failure.sources.get(failure.sources.size() - 1).equals(comparingTo);
            }

            removeNext = allMatch;

            if (!allMatch) break;
        }

        for (ParserFailure failure : failures) {
            for (int i = failure.sources.size() - 1; i > 0; --i) {
                String s1 = failure.sources.get(i);
                String s2 = failure.sources.get(i - 1);

                if (s1.equals(s2) || s1.equals(s2 + "*")) {
                    failure.sources.remove(i);
                    failure.sources.set(i - 1, s2 + "*");
                }
            }

            for (boolean set = true; failure.sources.size() > 3; set = false) {
                failure.sources.remove(failure.sources.size() - 1);
                if (set) failure.sources.set(2, "..." + failure.sources.get(2));
            }
        }

        return failures;
    }

    static class TokenTypeMatchFailure extends ParserFailure {
        private final TokenType expectedType;
        private final String expectedValue;

        TokenTypeMatchFailure(List<String> sources, TokenType expectedType, String expectedValue) {
            super(sources);
            assert expectedType != null;
            this.expectedType = expectedType;
            this.expectedValue = expectedValue;
        }

        @Override String getExpected() {
            return expectedValue == null ? expectedType.toString() : expectedType.toString() + " \"" + expectedValue + "\"";
        }

        @Override String getError() {
            return "Expected " + getExpected();
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TokenTypeMatchFailure that = (TokenTypeMatchFailure) o;

            if (expectedType != that.expectedType) return false;
            return expectedValue != null ? expectedValue.equals(that.expectedValue) : that.expectedValue == null;
        }

        @Override public int hashCode() {
            int result = expectedType.hashCode();
            result = 31 * result + (expectedValue != null ? expectedValue.hashCode() : 0);
            return result;
        }
    }

    static class TokenValueMatchFailure extends ParserFailure {
        private final String expectedValue;

        TokenValueMatchFailure(List<String> sources, String expectedValue) {
            super(sources);
            assert expectedValue != null;
            this.expectedValue = expectedValue;
        }

        @Override public String getExpected() {
            return "'" + expectedValue + "'";
        }

        @Override String getError() {
            return "Expected " + getExpected();
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TokenValueMatchFailure that = (TokenValueMatchFailure) o;

            return expectedValue.equals(that.expectedValue);
        }

        @Override public int hashCode() {
            return expectedValue.hashCode();
        }
    }

    static class PredicateFailure extends ParserFailure {
        private final String predicateFailure;

        PredicateFailure(List<String> sources, String predicateFailure) {
            super(sources);
            assert predicateFailure != null;
            this.predicateFailure = predicateFailure;
        }

        @Override String getExpected() {
            return null;
        }

        @Override String getError() {
            return predicateFailure;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PredicateFailure that = (PredicateFailure) o;

            return predicateFailure.equals(that.predicateFailure);
        }

        @Override
        public int hashCode() {
            return predicateFailure.hashCode();
        }
    }
}
