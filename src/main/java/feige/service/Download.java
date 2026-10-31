package feige.service;

import java.io.File;

public class Download {

    private final File file;
    private final String originalName;
    private final boolean inline;

    public Download(File file, String originalName, boolean inline) {
        this.file = file;
        this.originalName = originalName;
        this.inline = inline;
    }

    public File getFile() {
        return file;
    }

    public String getOriginalName() {
        return originalName;
    }

    public boolean isInline() {
        return inline;
    }
}
