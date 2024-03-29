package astify.token;

import astify.core.Position;
import astify.core.Positioned;

import static java.util.Objects.hash;

public class Token implements Positioned {
    private final TokenType type;
    private final String value;
    private final Position position;

    public Token(TokenType type, String value, Position position) {
        this.type = type;
        this.value = value;
        this.position = position;
    }

    public TokenType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public boolean matches(TokenType type) {
        return this.type == type;
    }

    public boolean matches(TokenType type, String value) {
        return matches(type) && this.value.equals(value);
    }

    @Override public Position getPosition() {
        return position;
    }

    @Override public String toString() {
        if (type == TokenType.EOF) return "<EOF>";
        return type.toString() + " \"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof Token)) return false;
        Token otherCasted = (Token) other;
        return type == otherCasted.type && value.equals(otherCasted.value);
    }

    @Override public int hashCode() {
        return hash(type, value);
    }
}
