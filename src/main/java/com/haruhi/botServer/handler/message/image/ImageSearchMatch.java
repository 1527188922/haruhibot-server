package com.haruhi.botServer.handler.message.image;

import com.haruhi.botServer.dto.qqclient.Message;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImageSearchMatch {

    private final boolean matched;
    private final String imageUrl;
    private final Message replyMessage;
    private final String cacheKey;
    private final boolean waitingImage;

    public static ImageSearchMatch notMatched() {
        return new ImageSearchMatch(false, null, null, null, false);
    }

    public static ImageSearchMatch waiting(String cacheKey) {
        return new ImageSearchMatch(true, null, null, cacheKey, true);
    }

    public static ImageSearchMatch ready(String imageUrl, Message replyMessage, String cacheKey) {
        return new ImageSearchMatch(true, imageUrl, replyMessage, cacheKey, false);
    }
}
