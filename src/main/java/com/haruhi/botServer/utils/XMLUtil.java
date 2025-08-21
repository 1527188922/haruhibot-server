package com.haruhi.botServer.utils;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class XMLUtil {
    private static final Logger logger = LoggerFactory.getLogger(XMLUtil.class);
    /**
     * 对象转xml
     * @param obj
     * @return
     */
    public static String convertToXml(Object obj) {
        try (StringWriter sw = new StringWriter()){
            // 利用jdk中自带的转换类实现    
            JAXBContext context = JAXBContext.newInstance(obj.getClass());

            Marshaller marshaller = context.createMarshaller();
            // 格式化xml输出的格式    
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                    Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            // 将对象转换成输出流形式的xml    
            marshaller.marshal(obj, sw);
            return sw.toString();
        } catch (JAXBException | IOException e) {
            logger.error("java对象转xml异常 {}", obj,e);
            return null;
        }
    }


    /**
     * xml转换成对象
     */
    public static <T> T convertXmlToObject(Class<T> clazz, String xmlStr) {
        try (Reader reader = new StringReader(xmlStr)){
            JAXBContext context = JAXBContext.newInstance(clazz);
            // 进行将Xml转成对象的核心接口
            Unmarshaller unmarshal = context.createUnmarshaller();
            return (T)unmarshal.unmarshal(reader);
        } catch (Exception e) {
            logger.error("xml转java对象异常 {}",xmlStr,e);
        }
        return null;
    }

    /**
     * 将对象转xml文件
     *
     * @param obj
     * @param path
     * @return
     */
    public static void convertToXml(Object obj, String path) throws JAXBException, IOException {
        JAXBContext context = JAXBContext.newInstance(obj.getClass());

        Marshaller marshaller = context.createMarshaller();
        // 格式化xml输出的格式
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "utf-8");
        // 将对象转换成输出流形式的xml
        // 创建输出流

        try (FileWriter fw = new FileWriter(path)){
            marshaller.marshal(obj, fw);
        }
    }


    /**
     * xml文件转对象
     */
    public static <T> T convertXmlFileToObject(Class<T> clazz, String xmlPath) throws JAXBException, IOException {
        JAXBContext context = JAXBContext.newInstance(clazz);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        try(FileInputStream fileInputStream = new FileInputStream(xmlPath);
            InputStreamReader isr = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8)) {
            return (T)unmarshaller.unmarshal(isr);
        }
    }

}
