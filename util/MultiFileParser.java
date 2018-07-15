package astify.util;

import astify.Capture;
import astify.core.Source;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public abstract class MultiFileParser<T extends Capture.ObjectCapture> extends MultiSourceParser<T> {
    public MultiFileParser() {

    }

    public void parseFileDeferred(String filename) {
        String resolvedFilePath = null;
        List<String> basePaths = getBasePaths();

        for (String basePath : basePaths) {
            if ((resolvedFilePath = resolveFilePath(filename, basePath)) != null) {
                break;
            }
        }

        if (resolvedFilePath != null) {
            try {
                parseSourceDeferred(new Source.FileSource(resolvedFilePath, filename));
            }
            catch (FileNotFoundException e) {
                error(e);
            }
        }
        else {
            error(new FileNotFoundException("File '" + filename + "' not found\nLooked in:\n\t" + Util.concatList(basePaths, "\n\t")));
        }
    }

    public String resolveFilePath(String filename, String basePath) {
        Path filePath = basePath == null ? Paths.get(filename) : Paths.get(basePath, filename);
        File file = new File(filePath.toAbsolutePath().toString());

        if (file.exists() && file.isFile()) {
            return filePath.toString();
        }
        else {
            return null;
        }
    }

    public abstract List<String> getBasePaths();

    public static abstract class SingleBasePathMultiPathParser<T extends Capture.ObjectCapture> extends MultiFileParser<T> {
        protected final String basePath;

        protected SingleBasePathMultiPathParser(String basePath) {
            this.basePath = basePath;
        }

        @Override
        public List<String> getBasePaths() {
            return Collections.singletonList(basePath);
        }
    }
}
