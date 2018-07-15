package astify.util;

import astify.Capture;
import astify.ParserException;
import astify.core.Source;
import astify.token.TokenException;

import java.util.*;

public abstract class MultiSourceParser<T extends Capture.ObjectCapture> {
    private final Map<Source, T> parsed = new HashMap<>();
    private final List<Source> additionList = new LinkedList<>();
    private int firstUnresolved = 0;

    private final List<Exception> exceptions = new ArrayList<>();

    public MultiSourceParser() {

    }

    public abstract T parseSource(Source source) throws TokenException, ParserException;

    public void onSourceParsed(Source source, T result) {
        // do nothing
    }

    public void onError(Exception error) {
        // do nothing
    }

    public void parseSourceDeferred(Source source) {
        if (!additionList.contains(source)) {
            additionList.add(source);
        }
    }

    public void parseSources() {
        while (firstUnresolved < additionList.size()) {
            try {
                Source source = additionList.get(firstUnresolved++);
                T result = parseSource(source);
                parsed.put(source, result);
                onSourceParsed(source, result);
            }
            catch (ParserException | TokenException e) {
                error(e);
            }
        }
    }

    public void importFrom(MultiSourceParser<T> parser) {
        exceptions.addAll(parser.exceptions);

        for (Source source : parser.parsed.keySet()) {
            parsed.put(source, parser.parsed.get(source));
        }

        for (Source source : parser.additionList) {
            additionList.add(firstUnresolved, source);
        }
    }

    public List<Source> listSources() {
        return new ArrayList<>(additionList);
    }

    public T getResult(Source source) {
        if (parsed.containsKey(source)) {
            return parsed.get(source);
        }
        else {
            throw new IllegalArgumentException("No result for source " + source.toString());
        }
    }

    public boolean hasError() {
        return exceptions.size() > 0;
    }

    public List<Exception> getErrors() {
        return new ArrayList<>(exceptions);
    }

    protected void error(Exception exception) {
        exceptions.add(exception);
        onError(exception);
    }
}
