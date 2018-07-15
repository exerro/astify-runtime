package astify.util;

import astify.*;
import astify.core.Source;
import astify.token.DefaultTokenGenerator;
import astify.token.TokenException;
import astify.token.TokenGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Util {
    public static<T> String concatList(List<T> elements, String delimiter) {
        List<String> elementStrings = new ArrayList<>();

        for (T element : elements) {
            elementStrings.add(element.toString());
        }

        return String.join(delimiter, elementStrings);
    }

    public static<T> String concatList(List<T> elements) {
        return concatList(elements, ", ");
    }


    public static<T, R> List<R> map(Function<T, R> func, List<T> list) {
        List<R> result = new ArrayList<>();

        for (T value : list) {
            result.add(func.apply(value));
        }

        return result;
    }


    public static<T, R> Function<List<T>, List<R>> mapf(Function<T, R> func) {
        return ts -> map(func, ts);
    }



    public static String unformatString(String str) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;

        for (int i = 1; i < str.length() - 1; ++i) {
            char c = str.charAt(i);

            if (escaped) {
                result.append(c);
                escaped = false;
            }
            else if (c == '\\') {
                escaped = true;
            }
            else {
                result.append(c);
            }
        }

        return result.toString();
    }
}
