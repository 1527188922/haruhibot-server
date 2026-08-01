package com.haruhi.botServer.handler.message.image;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ImageSearchProviderFactory {

    private final Map<ImageSearchProviderType, ImageSearchProvider> providerMap = new EnumMap<>(ImageSearchProviderType.class);

    public ImageSearchProviderFactory(List<ImageSearchProvider> providers) {
        for (ImageSearchProvider provider : providers) {
            ImageSearchProvider oldProvider = providerMap.put(provider.type(), provider);
            if (oldProvider != null) {
                throw new IllegalStateException("Duplicate image search provider: " + provider.type());
            }
        }
    }

    public ImageSearchProvider getProvider(ImageSearchProviderType type) {
        ImageSearchProvider provider = providerMap.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("Image search provider not found: " + type);
        }
        return provider;
    }

    public List<ImageSearchProvider> getAllProviders() {
        return providerMap.values().stream().toList();
    }
}
