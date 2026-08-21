package com.haruhi.botServer.picimagesearch;

import com.haruhi.botServer.HaruhiBotServer;
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

        SearchResponse response = picImageSearchFactory.ascii2D().search(SearchInput.byUrl("https://p3-passport.byteacctimg.com/img/user-avatar/05292526f2b3bd07a2151d119ba41ed8~300x300.image"));
//        SearchResponse response = picImageSearchFactory.ascii2D().search(SearchInput.byFile(new File("D:\\temp\\pic\\2S8sRGmW9L6.png")));
        System.out.println(response);
    }
}
