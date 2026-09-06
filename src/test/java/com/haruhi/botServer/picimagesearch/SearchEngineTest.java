package com.haruhi.botServer.picimagesearch;

import com.haruhi.botServer.HaruhiBotServer;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.HtmlToImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;

@Slf4j
@ActiveProfiles("dev")
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = HaruhiBotServer.class)
public class SearchEngineTest {
    @Autowired
    private PicImageSearchFactory picImageSearchFactory;

    @Test
    public void ascii2DTest() {
        try {
            SearchInput searchInput = SearchInput.byUrl("https://p3-passport.byteacctimg.com/img/user-avatar/05292526f2b3bd07a2151d119ba41ed8~300x300.image");
        SearchResponse search = picImageSearchFactory.ascii2D().search(searchInput);
//        SearchResponse search = picImageSearchFactory.ascii2D().search(SearchInput.byFile(new File("D:\\temp\\pic\\2S8sRGmW9L6.png")));
            System.out.println(search.getOrigin());
        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Test
    public void baiduTest() {
        try {
            SearchInput searchInput = SearchInput.byUrl("https://p3-passport.byteacctimg.com/img/user-avatar/05292526f2b3bd07a2151d119ba41ed8~300x300.image");
            SearchResponse search = picImageSearchFactory.baidu().search(searchInput);
            System.out.println(search.getOrigin());
        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Test
    public void bingTest() {
        try {
            SearchInput searchInput = SearchInput.byUrl("https://p3-passport.byteacctimg.com/img/user-avatar/05292526f2b3bd07a2151d119ba41ed8~300x300.image");
            SearchResponse search = picImageSearchFactory.bing().search(searchInput);
            System.out.println(search.getOrigin());
        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Test
    public void exHentaiTest() {
        try {
            SearchInput searchInput = SearchInput.byUrl("https://p3-passport.byteacctimg.com/img/user-avatar/05292526f2b3bd07a2151d119ba41ed8~300x300.image");
            SearchResponse search = picImageSearchFactory.exHentai().search(searchInput);
            System.out.println(search.getOrigin());
        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    @Test
    public void eHentaiTest() {
        try {
            SearchInput searchInput = SearchInput.byUrl("https://p3-passport.byteacctimg.com/img/user-avatar/05292526f2b3bd07a2151d119ba41ed8~300x300.image");
            SearchResponse search = picImageSearchFactory.eHentai().search(searchInput);
            System.out.println(search.getOrigin());
        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Test
    public void googleTest() {
        try {
            SearchInput searchInput = SearchInput.byUrl("https://p3-passport.byteacctimg.com/img/user-avatar/05292526f2b3bd07a2151d119ba41ed8~300x300.image");
            SearchResponse search = picImageSearchFactory.google().search(searchInput);
            System.out.println(search.getOrigin());
        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    @Test
    public void yandexTest() {
        try {
            SearchInput searchInput = SearchInput.byUrl("https://p3-passport.byteacctimg.com/img/user-avatar/05292526f2b3bd07a2151d119ba41ed8~300x300.image");
            SearchResponse search = picImageSearchFactory.yandex().search(searchInput);
            System.out.println(search.getOrigin());
            HtmlToImageUtils.htmlToImage(search.getOrigin().toString(), FileUtil.getAppTempDir() + File.separator + "yd.png",
                    new int[]{1000, 700});

        }catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
