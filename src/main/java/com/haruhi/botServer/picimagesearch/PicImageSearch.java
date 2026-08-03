package com.haruhi.botServer.picimagesearch;

import java.io.File;

/**
 * Factory facade for the Java PicImageSearch port.
 */
public final class PicImageSearch {

    private PicImageSearch() {
    }

    public static SearchInput byUrl(String url) {
        return SearchInput.byUrl(url);
    }

    public static SearchInput byFile(File file) {
        return SearchInput.byFile(file);
    }

    public static SearchInput byBase64(String base64) {
        return SearchInput.byBase64(base64);
    }
}
