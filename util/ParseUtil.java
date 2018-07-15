package astify.util;

import astify.*;
import astify.core.Source;
import astify.token.DefaultTokenGenerator;
import astify.token.TokenException;
import astify.token.TokenGenerator;

import java.util.List;

public class ParseUtil {
    public static List<Capture> parse(PatternBuilder patternBuilder, TokenGenerator tokenGenerator, String patternName) throws TokenException, ParserException {
        Parser parser = new Parser();
        Pattern pattern;

        if (patternName == null) {
            pattern = patternBuilder.getMain();
        }
        else {
            pattern = patternBuilder.lookup(patternName);
        }

        if (pattern == null) {
            return null;
        }

        parser.setup(pattern, tokenGenerator.getStartingPosition());
        parser.parse(tokenGenerator);

        if (parser.hasError()) {
            throw ParserException.combine(parser.getExceptions());
        }
        else {
            return parser.getResults();
        }
    }


    public static List<Capture> parse(PatternBuilder patternBuilder, TokenGenerator tokenGenerator) throws TokenException, ParserException {
        return ParseUtil.parse(patternBuilder, tokenGenerator, null);
    }

    public static List<Capture> parse(Source source, PatternBuilder patternBuilder, String patternName) throws TokenException, ParserException {
        return ParseUtil.parse(patternBuilder, new DefaultTokenGenerator(source, patternBuilder.getKeywords()), patternName);
    }

    public static List<Capture> parse(Source source, PatternBuilder patternBuilder) throws TokenException, ParserException {
        return parse(source, patternBuilder, null);
    }


    public static Capture parseSingle(PatternBuilder patternBuilder, TokenGenerator tokenGenerator, String patternName) throws TokenException, ParserException, AmbiguityException {
        List<Capture> captures = ParseUtil.parse(patternBuilder, tokenGenerator, patternName);

        if (captures == null) {
            return null;
        }

        if (captures.size() == 1) {
            return captures.get(0);
        }
        else if (captures.size() == 0) {
            // this should be impossible without exceptions being thrown but eh who knows
            System.out.println("wtf (astify/Util.java: parseSingle#1");
            return null;
        }
        else {
            // ambiguous syntax
            throw new AmbiguityException(captures);
        }
    }

    public static Capture parseSingle(PatternBuilder patternBuilder, TokenGenerator tokenGenerator) throws TokenException, ParserException, AmbiguityException {
        return parseSingle(patternBuilder, tokenGenerator, null);
    }

    public static Capture parseSingle(Source source, PatternBuilder patternBuilder, String patternName) throws TokenException, ParserException, AmbiguityException {
        return parseSingle(patternBuilder, new DefaultTokenGenerator(source, patternBuilder.getKeywords()), patternName);
    }

    public static Capture parseSingle(Source source, PatternBuilder patternBuilder) throws TokenException, ParserException, AmbiguityException {
        return parseSingle(source, patternBuilder, null);
    }


    public static class AmbiguityException extends Exception {
        private final List<Capture> results;

        public AmbiguityException(List<Capture> results) {
            super(getMessage(results));
            this.results = results;
        }

        private static String getMessage(List<Capture> captures) {
            StringBuilder errorBuilder = new StringBuilder("Ambiguous syntax detected: multiple parse results returned");
            int i = 0;

            for (Capture result : captures) {
                errorBuilder.append("\n\nResult ");
                errorBuilder.append(++i);
                errorBuilder.append(":\n\t");
                errorBuilder.append(result.toString().replace("\n", "\n\t"));
            }

            return errorBuilder.toString();
        }
    }
}
