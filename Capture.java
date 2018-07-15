
package astify;

import astify.core.Positioned;
import astify.core.Position;
import astify.token.Token;
import astify.token.TokenType;

import java.util.*;

public abstract class Capture implements Positioned {
    public final Position spanningPosition;

    protected Capture(Position spanningPosition) {
        assert spanningPosition != null;
        this.spanningPosition = spanningPosition;
    }

    // return a callback to return the nth capture of a list of captures
    // for example `sequence(Capture.nth(0), ref('something'), symbol(';'))` to get the `something` and ignore the ';'
    public static CaptureGenerator nth(int index) {
        return captures -> captures.get(index);
    }

    @Override public Position getPosition() {
        return spanningPosition;
    }

    // base class to inherit custom capture types from
    public static abstract class ObjectCapture extends Capture {
        protected ObjectCapture(Position spanningPosition) {
            super(spanningPosition);
        }

        @Override public String toString() {
            return "<object-capture>";
        }
    }

    public static final class TokenCapture extends Capture {
        private final Token token;

        public TokenCapture(Token token) {
            super(token.getPosition());
            this.token = token;
        }

        public Token getToken() {
            return token;
        }

        public TokenType getType() {
            return token.getType();
        }

        public String getValue() {
            return token.getValue();
        }

        @Override public String toString() {
            return "<token-capture " + token.toString() + ">";
        }
    }

    public static final class EmptyCapture extends Capture {
        public EmptyCapture(Position spanningPosition) {
            super(spanningPosition);
        }

        @Override public String toString() {
            return "<empty-capture>";
        }
    }

    public static class ListCapture extends Capture {
        private final List<Capture> elements;

        public ListCapture(Position spanningPosition, List<Capture> elements) {
            super(spanningPosition);
            this.elements = elements;
        }

        public Capture get(int i) {
            return elements.get(i);
        }

        public int size() {
            return elements.size();
        }

        public boolean isEmpty() {
            return size() == 0;
        }

        public List<Capture> all() {
            return new ArrayList<>(elements);
        }

        public Iterator<Capture> iterator() {
            return new Iterator<Capture>() {
                int i = 0;

                @Override public boolean hasNext() {
                    return i < size();
                }

                @Override public Capture next() {
                    return get(i++);
                }
            };
        }

        // creates an empty list capture spanning the given position
        public static ListCapture createEmpty(Position spanningPosition) {
            return new ListCapture(spanningPosition, new ArrayList<>());
        }

        // returns a list capture containing all given elements, spanning the position defined by the first and last element given
        public static ListCapture createFrom(List<Capture> elements) {
            assert elements != null;
            assert !elements.isEmpty();

            return new ListCapture(elements.get(0).getPosition().to(elements.get(elements.size() - 1).getPosition()), elements);
        }

        // returns an empty list capture spanning the given position if elements is empty, otherwise returns createFrom(elements)
        public static ListCapture createFrom(List<Capture> elements, Position spanningPosition) {
            return elements.isEmpty() ? createEmpty(spanningPosition) : createFrom(elements);
        }

        @Override public String toString() {
            if (elements.isEmpty()) return "<list-capture>";
            StringBuilder builder = new StringBuilder("<list-capture\n");

            for (int i = 0; i < elements.size(); ++i) {
                builder.append("\t")
                        .append(elements.get(i).toString().replace("\n", "\n\t"))
                        .append("\n");
            }

            return builder.append(">").toString();
        }
    }
}
