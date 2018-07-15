package astify.token;

import astify.core.Position;
import astify.core.Positioned;

public class TokenException extends Exception implements Positioned {
    private final String message;
    private final Position position;

    public TokenException(String message, Position position) {
        this.message = message;
        this.position = position;
    }

    @Override public Position getPosition() {
        return position;
    }

    @Override public String toString() {
        return this.getClass().getName() + ":\n" + message + " (in " + position.source.toString() + ")\n" + position.getLineAndCaret();
    }
}
