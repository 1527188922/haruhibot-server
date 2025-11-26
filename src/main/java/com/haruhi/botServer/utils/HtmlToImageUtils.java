package com.haruhi.botServer.utils;

import cn.hutool.core.text.StrFormatter;
import com.haruhi.botServer.entity.ChatRecordSqlite;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.w3c.dom.Document;
import org.xhtmlrenderer.swing.Java2DRenderer;
import org.xml.sax.InputSource;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class HtmlToImageUtils {

    public static String generateHtmlTest(String title, String content, String param,Long uin) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_34);
        cfg.setClassForTemplateLoading(HtmlToImageUtils.class, "/templates"); // 模板目录
        Template template = cfg.getTemplate("test.ftl");
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("content", content);
        data.put("param", param);
        data.put("imgurl", CommonUtil.getAvatarUrl(uin, false));

        try (StringWriter writer = new StringWriter()){
            template.process(data, writer);
            return writer.toString();
        }
    }

    public static String generateHtmlTest2() throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_34);
        cfg.setClassForTemplateLoading(HtmlToImageUtils.class, "/templates"); // 模板目录
        Template template = cfg.getTemplate("test2.ftl");
        Map<String, Object> data = new HashMap<>();
        data.put("stringList", Arrays.asList("123","4656","4ff23f23"));
        ChatRecordSqlite chatRecordSqlite = new ChatRecordSqlite();
        chatRecordSqlite.setId(12312L);
        chatRecordSqlite.setCard("!3123123");
        chatRecordSqlite.setContent("你好");
        ChatRecordSqlite chatRecordSqlite2 = new ChatRecordSqlite();
        chatRecordSqlite2.setId(1231222L);
        chatRecordSqlite2.setCard("!放2分3分3分发");
        chatRecordSqlite2.setContent("你也好");
        data.put("chatList", Arrays.asList(chatRecordSqlite,chatRecordSqlite2));
        data.put("emptyList", Arrays.asList());

        try (StringWriter writer = new StringWriter()){
            template.process(data, writer);
            return writer.toString();
        }
    }

    public static Document htmlToDocument(String htmlContent) throws Exception {
        // 初始化XML解析器工厂
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // 关键设置：禁用DTD验证（避免网络加载DTD失败）
        factory.setValidating(false);
//        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setNamespaceAware(true); // 支持XHTML命名空间

        DocumentBuilder builder = factory.newDocumentBuilder();
        // 将HTML字符串转为输入流（指定UTF-8编码）
        InputSource inputSource = new InputSource(
                new ByteArrayInputStream(htmlContent.getBytes(StandardCharsets.UTF_8))
        );
        inputSource.setEncoding("UTF-8"); // 明确编码

        // 解析为Document对象
        return builder.parse(inputSource);
    }


    public static void main(String[] args) throws Exception {
        // 1. 生成带参数的HTML内容
        String htmlContent = HtmlToImageUtils.generateHtmlTest("标题1123",
                "内容3r3",
                "File output = new File(\"D:\\\\temp\\\\test\\\\output_flying.png\");",
                1527188922L);

//        String htmlContent = HtmlToImageUtils.generateHtmlTest2();


        Document document = HtmlToImageUtils.htmlToDocument(htmlContent);
        // 2. 渲染为图片（宽度800px，高度自动计算）
        Java2DRenderer renderer = new Java2DRenderer(document, 800);
        BufferedImage image = renderer.getImage();

        // 3. 保存图片
        File output = new File("D:\\temp\\test\\output_flying.png");
        ImageIO.write(image, "png", output);
        System.out.println("图片已保存到："+output.getAbsolutePath());
    }

}
