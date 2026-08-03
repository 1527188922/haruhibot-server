package com.haruhi.botServer.picimagesearch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Command line demos for each PicImageSearch engine.
 * Usage: PicImageSearchDemo <engine> <imageUrl> [saucenaoApiKey]
 */
@Component
public final class PicImageSearchDemo implements CommandLineRunner {

    public static void main(String[] args) {


    }

    @Autowired
    private PicImageSearchFactory picImageSearchFactory;

    private static void printSummary(SearchResponse response) {
        System.out.println("engine=" + response.getEngine());
        System.out.println("statusCode=" + response.getStatusCode());
        System.out.println("resultUrl=" + response.getUrl());
        System.out.println("resultCount=" + response.getRaw().size());
        response.getRaw().stream().limit(5).forEach(item -> {
            System.out.println("----");
            System.out.println("title=" + item.getTitle());
            System.out.println("url=" + item.getUrl());
            System.out.println("thumbnail=" + item.getThumbnail());
            System.out.println("similarity=" + item.getSimilarity());
            System.out.println("extra=" + item.getExtra());
        });
    }

    @Override
    public void run(String... args) throws Exception {
        EngineType engine = EngineType.SAUCENAO;
        SearchInput input = PicImageSearch.byUrl("https://gchat.qpic.cn/gchatpic_new/0/0-0-BDF4CEB854C1B5A422207DCCA3F5FB7B/0");
        String sauceNaoApiKey = "9d4597b2e67058e4d40625087afb66a7be4c6852";

        try {
//            SearchResponse response = picImageSearchFactory.sauceNao().apiKey(sauceNaoApiKey).search(input);
            SearchResponse response = picImageSearchFactory.animeTrace().search(input);
            printSummary(response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
