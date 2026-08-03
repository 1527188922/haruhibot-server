package com.haruhi.botServer.picimagesearch;

public class PicImageSearchException extends RuntimeException {
        public PicImageSearchException(String message) {
            super(message);
        }

        public PicImageSearchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
