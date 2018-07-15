
package astify.token;

import astify.core.Position;
import astify.core.Source;

import java.util.HashSet;
import java.util.Set;

public class DefaultTokenGenerator implements TokenGenerator {
    protected final Source source;
    protected final String contentBuffer;
    protected final Set<String> keywords;

    protected Position currentPosition;
    protected int currentBufferPosition;

    public DefaultTokenGenerator(Source source, Set<String> keywords) {
        this.source = source;
        this.keywords = keywords;

        contentBuffer = source.getContent();
        currentPosition = new Position(source, 1, 1);
    }

    public DefaultTokenGenerator(Source source) {
        this(source, new HashSet<>());
    }

    @Override public Token getNext() throws TokenException {
        if (currentBufferPosition >= contentBuffer.length()) {
            return new Token(TokenType.EOF, "", currentPosition);
        }
        if (matchString("//")) {
            int pos = contentBuffer.indexOf('\n', currentBufferPosition) + 1;
            if (pos == 0) pos = contentBuffer.length();
            advance(pos - currentBufferPosition);
            return getNext();
        }
        if (matchString("/*")) {
            int pos = contentBuffer.indexOf("*/") + 2;

            if (pos == 1) {
                throw new TokenException("expected closing */ to close comment", currentPosition);
            }

            advance(pos - currentBufferPosition);
            return getNext();
        }
        if (matchString(" ") || matchString("\t") || matchString("\r") || matchString("\n")) {
            advance(1);
            return getNext();
        }

        if (matchString("'") || matchString("\"")) {
            return consumeString();
        }
        if (isDigit()) {
            Token integer = consumeInteger();

            if (matchString(".")) {
                advance(1);

                if (!isDigit()) {
                    throw new TokenException("expected digit after '.'", currentPosition);
                }

                Token decimal = consumeInteger();

                if (matchString("e")) {
                    String expSign = "";

                    advance(1);

                    if (matchString("+") || matchString("-")) {
                        expSign = read(1);
                        advance(1);
                    }

                    if (!isDigit()) {
                        throw new TokenException("expected digit after " + (expSign.equals("") ? "'e'" : expSign), currentPosition);
                    }

                    Token exponent = consumeInteger();

                    return new Token(TokenType.Float, integer.getValue() + "." + decimal.getValue() + "e" + expSign + exponent.getValue(), integer.getPosition().to(exponent.getPosition()));
                }

                return new Token(TokenType.Float, integer.getValue() + "." + decimal.getValue(), integer.getPosition().to(decimal.getPosition()));
            }

            return integer;
        }
        if (isAlpha()) {
            return consumeWord();
        }

        String s = read(1);
        Position p = currentPosition;
        advance(1);

        return new Token(TokenType.Symbol, s, p);
    }

    protected Token consumeString() throws TokenException {
        String open = read(1);
        StringBuilder result = new StringBuilder();
        Position startPosition = currentPosition, position;
        boolean escaped = false;

        result.append(open);
        advance(1);

        while (!read(1).equals(open)) {
            String c = read(1);

            if (c.equals("")) {
                throw new TokenException("expected closing " + open + " to close string", startPosition);
            }

            if (!escaped) {
                if (c.equals("\\")) {
                    escaped = true;
                }
                if (c.equals("\n")) {
                    throw new TokenException("unexpected newline", currentPosition);
                }
            }
            else {
                escaped = false;
            }

            result.append(c);
            advance(1);
        }

        position = startPosition.to(currentPosition);
        result.append(open);
        advance(1);

        return new Token(TokenType.String, result.toString(), position);
    }

    protected Token consumeInteger() {
        StringBuilder number = new StringBuilder();
        Position position = currentPosition;

        for (; isDigit(); advance(1)) {
            number.append(getCharacter());
            position = position.to(currentPosition);
        }

        return new Token(TokenType.Integer, number.toString(), position);
    }

    protected Token consumeWord() {
        StringBuilder word = new StringBuilder();
        Position position = currentPosition;

        for (; isAlpha(); advance(1)) {
            word.append(getCharacter());
            position = position.to(currentPosition);
        }

        return new Token(keywords.contains(word.toString()) ? TokenType.Keyword : TokenType.Word, word.toString(), position);
    }

    protected boolean matchString(String s) {
        return currentBufferPosition < contentBuffer.length() && contentBuffer.indexOf(s, currentBufferPosition) == currentBufferPosition;
    }

    protected String read(int length) {
        if (currentBufferPosition >= contentBuffer.length()) {
            return "";
        }
        return contentBuffer.substring(currentBufferPosition, Math.min(currentBufferPosition + length, contentBuffer.length()));
    }

    protected boolean isDigit() {
        char c = getCharacter();
        return c >= '0' && c <= '9';
    }

    protected boolean isAlpha() {
        char c = getCharacter();
        return isDigit() || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_';
    }

    protected char getCharacter() {
        return currentBufferPosition < contentBuffer.length() ? contentBuffer.charAt(currentBufferPosition) : 0;
    }

    protected void advance(int characters) {
        assert characters >= 0;
        if (currentBufferPosition >= contentBuffer.length()) return;
        int newlinePosition;
        int finalPosition = currentBufferPosition + characters;

        currentPosition = new Position(source, currentPosition.getLine(), currentPosition.char2 + characters);

        while ((newlinePosition = contentBuffer.indexOf('\n', currentBufferPosition)) != -1) {
            if (newlinePosition < finalPosition) {
                currentPosition = new Position(source, currentPosition.getLine() + 1, finalPosition - newlinePosition);
                currentBufferPosition = newlinePosition + 1;
            }
            else {
                break;
            }
        }

        currentBufferPosition = finalPosition;
    }

    @Override public Position getStartingPosition() {
        return new Position(source, 1, 1);
    }
}
