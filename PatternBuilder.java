package astify;

import astify.token.Token;
import astify.token.TokenType;

import java.util.*;

public class PatternBuilder {
    private final Map<String, Pattern> environment;
    private final Set<String> keywords;
    private final Set<String> operators;

    protected TokenType Word = TokenType.Word;
    protected TokenType String = TokenType.String;
    protected TokenType Integer = TokenType.Integer;
    protected TokenType Float = TokenType.Float;
    protected TokenType Symbol = TokenType.Symbol;
    protected TokenType Keyword = TokenType.Keyword;
    protected TokenType Boolean = TokenType.Boolean;
    protected TokenType EOF = TokenType.EOF;
    protected TokenType Special = TokenType.Special;
    protected TokenType Other = TokenType.Other;

    public PatternBuilder() {
        environment = new HashMap<>();
        keywords = new HashSet<>();
        operators = new HashSet<>();
    }

    public Set<String> getKeywords() {
        return new HashSet<>(keywords);
    }

    public Set<String> getOperators() { return new HashSet<>(operators); }

    // a token with the given type
    public Pattern.TokenPattern token(TokenType type) {
        assert type != null;
        return new Pattern.TokenPattern(type);
    }

    // a token with the given type and value
    public Pattern.TokenPattern token(TokenType type, String value) {
        assert type != null;
        assert value != null;
        return new Pattern.TokenPattern(type, value);
    }

    // token(TokenType) but defines the resulting pattern using the given name
    public Pattern.SequencePattern token(String name, TokenType type) {
        assert name != null;
        assert type != null;
        return define(name, token(type));
    }

    // token(TokenType, String) but defines the resulting pattern using the given name
    public Pattern.SequencePattern token(String name, TokenType type, String value) {
        assert name != null;
        assert type != null;
        assert value != null;
        return define(name, token(type, value));
    }

    // a token with type Keyword and the given word value
    public astify.Pattern.TokenPattern keyword(String word) {
        assert word != null;
        keywords.add(word);
        return new Pattern.TokenPattern(TokenType.Keyword, word);
    }

    // a sequence of consecutive tokens with the type Symbol matching the symbol given
    public Pattern symbol(String symbol) {
        assert symbol != null;

        if (symbol.length() == 1) {
            return new Pattern.TokenPattern(TokenType.Symbol, symbol);
        }

        List<Pattern> patterns = new ArrayList<>();

        patterns.add(symbol(symbol.substring(0, 1)));

        for (int i = 1; i < symbol.length(); ++i) {
            patterns.add(symbol(symbol.substring(i, i + 1)).addPredicate(MatchPredicate.noSpace()));
        }

        return new Pattern.SequencePattern(null, patterns, (captures) -> new Capture.TokenCapture(new Token(
                Symbol,
                symbol,
                captures.get(0).spanningPosition.to(captures.get(captures.size() - 1).spanningPosition)
        )));
    }

    public Pattern operator(String symbol) {
        assert symbol != null;
        operators.add(symbol);
        return symbol(symbol);
    }

    // matches either the pattern given or nothing
    public Pattern.OptionalPattern optional(Pattern opt) {
        assert opt != null;
        return new Pattern.OptionalPattern(opt);
    }

    // optional(Pattern) but defines the resulting pattern using the given name
    public Pattern.SequencePattern optional(String name, Pattern opt) {
        assert name != null;
        assert opt != null;
        return define(name, optional(opt));
    }

    // matches 0 or more occurrences of the pattern given, greedily
    public Pattern.ListPattern list(Pattern pattern) {
        assert pattern != null;
        return new Pattern.ListPattern(pattern);
    }

    // list(Pattern) but defines the resulting pattern using the given name
    public Pattern.SequencePattern list(String name, Pattern pattern) {
        assert name != null;
        assert pattern != null;
        return define(name, list(pattern));
    }

    // matches the pattern followed by a list of 0 or more (the delimiter followed by the pattern)
    public Pattern.DelimitedPattern delim(Pattern pattern, Pattern delim) {
        assert pattern != null;
        return new Pattern.DelimitedPattern(pattern, delim);
    }

    // delim(Pattern, Pattern) but defines the resulting pattern using the given name
    public Pattern.SequencePattern delim(String name, Pattern pattern, Pattern delim) {
        assert name != null;
        assert pattern != null;
        return define(name, delim(pattern, delim));
    }

    // matches a sequence of the patterns given
    // creates a capture based on the capture generator
    public Pattern.SequencePattern sequence(CaptureGenerator generator, Pattern... parts) {
        assert generator != null;
        assert parts.length > 0;
        return new Pattern.SequencePattern(null, Arrays.asList(parts), generator);
    }

    // sequence(CaptureGenerator, Pattern...) but defines the resulting pattern using the given name
    public Pattern.SequencePattern sequence(String name, CaptureGenerator generator, Pattern... parts) {
        assert name != null;
        assert generator != null;
        assert parts.length > 0;
        return define(name, new Pattern.SequencePattern(name, Arrays.asList(parts), generator));
    }

    // sequence(CaptureGenerator, Pattern...) but creates a list capture containing all sub-captures
    public Pattern.SequencePattern sequence(Pattern... parts) {
        assert parts.length > 0;
        return sequence(Capture.ListCapture::createFrom, parts);
    }

    // sequence(Pattern...) but defines the resulting pattern using the given name
    public Pattern.SequencePattern sequence(String name, Pattern... parts) {
        assert name != null;
        assert parts.length > 0;
        return sequence(name, Capture.ListCapture::createFrom, parts);
    }

    // matches any number of the given patterns
    // note that this can lead to syntax ambiguity, resulting in many distinct matches for the same token stream
    public Pattern one_of(Pattern... options) {
        assert options.length > 0;
        if (options.length == 1) return options[0];
        return new Pattern.BranchPattern(Arrays.asList(options));
    }

    // one_of(Pattern...) but defines the resulting pattern using the given name
    public Pattern.SequencePattern one_of(String name, Pattern... options) {
        assert name != null;
        assert options.length > 0;
        return define(name, one_of(options));
    }

    // matches the pattern defined using the given name
    public Pattern.GeneratorPattern ref(String name) {
        assert name != null;
        return new Pattern.GeneratorPattern(() -> lookup(name).getMatcher());
    }

    // matches the end of the file
    public Pattern.TokenPattern eof() {
        return token(TokenType.EOF);
    }

    // creates an empty capture dependent on the given predicate
    public Pattern predicate(MatchPredicate predicate) {
        return new Pattern.NothingPattern().addPredicate(predicate);
    }

    // defines a pattern such that it behaves as if inline when referred to
    // see define() for contrast
    public <T extends Pattern> T defineInline(String name, T pattern) {
        assert name != null;
        assert pattern != null;
        assert !environment.containsKey(name) : "Redefinition of " + name;

        environment.put(name, pattern);

        return pattern;
    }

    // defines a pattern such that it will show `name` in the matcher stack of any parser exceptions
    public <T extends Pattern> Pattern.SequencePattern define(String name, T pattern) {
        assert name != null;
        assert pattern != null;

        if (!(pattern instanceof Pattern.SequencePattern)) {
            return defineInline(name, new Pattern.SequencePattern(name, Collections.singletonList(pattern), Capture.nth(0)));
        }
        else {
            return defineInline(name, (Pattern.SequencePattern) pattern);
        }
    }

    // looks up a pattern with the given name
    public Pattern lookup(String name) {
        assert name != null;
        assert environment.containsKey(name) : "Lookup of '" + name + "' failed";
        return environment.get(name);
    }

    public Pattern getMain() {
        return lookup("main");
    }
}
