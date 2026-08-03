package com.haruhi.botServer.picimagesearch;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Optional;

public final class SearchInput {
        private final String url;
        private final File file;
        private final String base64;

        private SearchInput(String url, File file, String base64) {
            this.url = url;
            this.file = file;
            this.base64 = base64;
        }

        public static SearchInput byUrl(String url) {
            if (StringUtils.isBlank(url)) {
                throw new IllegalArgumentException("url must not be blank");
            }
            return new SearchInput(url, null, null);
        }

        public static SearchInput byFile(File file) {
            if (file == null || !file.exists()) {
                throw new IllegalArgumentException("file must exist");
            }
            return new SearchInput(null, file, null);
        }

        public static SearchInput byBase64(String base64) {
            if (StringUtils.isBlank(base64)) {
                throw new IllegalArgumentException("base64 must not be blank");
            }
            return new SearchInput(null, null, base64);
        }

        public Optional<String> url() {
            return Optional.ofNullable(url);
        }

        public Optional<File> file() {
            return Optional.ofNullable(file);
        }

        public Optional<String> base64() {
            return Optional.ofNullable(base64);
        }

        public boolean hasUrl() {
            return url != null;
        }

        public boolean hasFile() {
            return file != null;
        }

        public boolean hasBase64() {
            return base64 != null;
        }
    }
