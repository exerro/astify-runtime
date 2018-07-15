package astify.token;

import astify.core.Position;

public interface TokenGenerator {
    Token getNext() throws TokenException;
    Position getStartingPosition();
}
