
package astify.core;

public final class Position {
    private static int getLength(int n1, int n2) {
        String s1 = String.valueOf(n1);
        String s2 = String.valueOf(n2);
        return Math.max(s1.length(), s2.length());
    }

    private static String formatNumber(int n, int length) {
        StringBuilder s = new StringBuilder(String.valueOf(n));
        while (s.length() < length) s.insert(0, " ");
        return s.toString();
    }

    private static String rep(String s, int n) {
        StringBuilder b = new StringBuilder();
        for (; n > 0; --n) b.append(s);
        return b.toString();
    }

    private static String rep(String s, int n, String src) {
        return rep(s, src.substring(0, n).replace("\t", "    ").length());
    }

    private static String spaces(int characters, String line) {
        return rep(" ", characters, line);
    }

    public final Source source;
    public final int line1, line2;
    public final int char1, char2;

    public Position(Source source, int line1, int line2, int char1, int char2) {
        assert source != null;
        this.source = source;
        this.line1 = line1;
        this.line2 = line2;
        this.char1 = char1;
        this.char2 = char2;
    }

    public Position(Source source, int line, int char1, int char2) {
        this(source, line, line, char1, char2);
    }

    public Position(Source source, int line, int c) {
        this(source, line, line, c, c);
    }

    public int getLine() {
        return line2;
    }

    public Position to(Position other) {
        assert source == other.source;
        return new Position(source, line1, other.line2, char1, other.char2);
    }

    public Position after(int n) {
        return new Position(source, line2, char2 + n);
    }

    // returns true if this position is directly after the first parameter's position
    public boolean isAfter(Position otherPosition) {
        return source.equals(otherPosition.source) && line1 == otherPosition.line2 && char1 == otherPosition.char2 + 1;
    }

    // returns a string representation of the position, showing its source lines and pointers to the characters it spans
    public String getLineAndCaret() {
        int lineNumberLength = Math.max(1, getLength(line1, line2));

        if (line1 == line2) {
            String l1 = source.getLine(line1);
            return formatNumber(line1, lineNumberLength) + " | " + l1 + "\n"
                    + rep(" ", lineNumberLength + 2) + spaces(char1, l1) + rep("^", char2 - char1 + 1);
        }
        else {
            String l1 = source.getLine(line1), l2 = source.getLine(line2);
            return formatNumber(line1, lineNumberLength) + " | " + l1.replace("\t", "    ") + "\n"
                    + rep(" ", lineNumberLength + 2) + spaces(char1, l1) + rep("^", l1.length() - char1 + 1) + " ...\n"
                    + formatNumber(line2, lineNumberLength) + " | " + l2.replace("\t", "    ") + "\n"
                    + rep(" ", lineNumberLength - 1) + "... " + rep("^", char2, l2);
        }
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Position position = (Position) o;

        if (line1 != position.line1) return false;
        if (line2 != position.line2) return false;
        if (char1 != position.char1) return false;
        if (char2 != position.char2) return false;
        return source.equals(position.source);
    }

    @Override public int hashCode() {
        int result = source.hashCode();
        result = 31 * result + line1;
        result = 31 * result + line2;
        result = 31 * result + char1;
        result = 31 * result + char2;
        return result;
    }

    @Override public String toString() {
        if (line1 == line2) return source.toString() + "[" + line1 + " : " + char1 + " .. " + char2 + "]";
        else return source.toString() + "[" + line1 + ", " + char1 + " .. " + line2 + ", " + char2 + "]";
    }
}
