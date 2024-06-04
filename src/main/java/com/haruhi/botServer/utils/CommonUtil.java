package com.haruhi.botServer.utils;

import cn.hutool.core.img.gif.AnimatedGifEncoder;
import cn.hutool.core.img.gif.GifDecoder;
import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.simplerobot.modules.utils.KQCodeUtils;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.CollectionUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class CommonUtil {
    private CommonUtil(){}

    private static final Random random = new Random();
    public static int randomInt(int start,int end){
        return random.nextInt(end - start + 1) + start;
    }

    public static boolean isAt(Long userId,final String context) {
        List<String> qqs = getCqParams(context, CqCodeTypeEnum.at, "qq");
        if(CollectionUtils.isEmpty(qqs)){
            return false;
        }
        for (String qq : qqs) {
            if(String.valueOf(userId).equals(qq)){
                return true;
            }
        }
        return false;
    }
    public static String commandReplaceFirst(final String command, RegexEnum regexEnum){
        String[] split = regexEnum.getValue().split("\\|");
        for (String s : split) {
            if (command.startsWith(s)) {
                return command.replaceFirst(s,"");
            }
        }
        return null;
    }

    public static boolean commandStartsWith(final String command, RegexEnum regexEnum){
        String[] split = regexEnum.getValue().split("\\|");
        for (String s : split) {
            if (command.startsWith(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据cq码类型 参数类型 获取参数的值
     * @param message
     * @param typeEnum
     * @param paramKey
     * @return
     */
    public static List<String> getCqParams(String message,CqCodeTypeEnum typeEnum,String paramKey){
        List<String> params = null;
        KQCodeUtils instance = KQCodeUtils.getInstance();
        String[] cqs = instance.getCqs(message, typeEnum.getType());
        if (cqs != null && cqs.length > 0) {
            params = new ArrayList<>(cqs.length);
            for (String cq : cqs) {
                String paramVal = instance.getParam(cq, paramKey);
                if(Strings.isNotBlank(paramVal)){
                    params.add(paramVal);
                }
            }
        }
        return params;
    }

    /**
     * 将源List按照指定元素数量拆分为多个List
     *
     * @param source 源List
     * @param splitItemNum 每个List中元素数量
     */
    public static <T> List<List<T>> averageAssignList(List<T> source, int splitItemNum) {
        List<List<T>> result = new ArrayList<List<T>>();
        if (!CollectionUtils.isEmpty(source) && splitItemNum > 0) {
            if (source.size() <= splitItemNum) {
                // 源List元素数量小于等于目标分组数量
                result.add(source);
            } else {
                // 计算拆分后list数量
                int splitNum = (source.size() % splitItemNum == 0) ? (source.size() / splitItemNum) : (source.size() / splitItemNum + 1);

                List<T> value = null;
                for (int i = 0; i < splitNum; i++) {
                    if (i < splitNum - 1) {
                        value = source.subList(i * splitItemNum, (i + 1) * splitItemNum);
                    } else {
                        // 最后一组
                        value = source.subList(i * splitItemNum, source.size());
                    }
                    result.add(value);
                }
            }
        }
        return result;
    }
    public static String uuid(){
        return UUID.randomUUID().toString().replace("-","");
    }
    public static int averageAssignNum(int num, int divisor){
        if(num % divisor == 0){
            return num / divisor;
        }else{
            return num / divisor + 1;
        }
    }


    // 方法2
    public static String getNowIP2() throws IOException {
        String ip = null;
        BufferedReader br = null;
        try {
            URL url = new URL("https://v6r.ipip.net/?format=callback");
            br = new BufferedReader(new InputStreamReader(url.openStream()));
            String s = "";
            StringBuffer sb = new StringBuffer("");
            String webContent = "";
            while ((s = br.readLine()) != null) {
                sb.append(s + "\r\n");
            }
            webContent = sb.toString();
            int start = webContent.indexOf("(") + 2;
            int end = webContent.indexOf(")") - 1;
            webContent = webContent.substring(start, end);
            ip = webContent;
        } finally {
            if (br != null)
                br.close();
        }
        return ip;
    }

    // 方法4
    public static String getNowIP4() throws IOException {
        String ip = null;
        String objWebURL = "https://bajiu.cn/ip/";
        BufferedReader br = null;
        try {
            URL url = new URL(objWebURL);
            br = new BufferedReader(new InputStreamReader(url.openStream()));
            String s = "";
            String webContent = "";
            while ((s = br.readLine()) != null) {
                if (s.indexOf("互联网IP") != -1) {
                    ip = s.substring(s.indexOf("'") + 1, s.lastIndexOf("'"));
                    break;
                }
            }
        } finally {
            if (br != null)
                br.close();
        }
        return ip;
    }

    public static String getAvatarUrl(Long qq, boolean origin){
        return origin ? MessageFormat.format("https://q1.qlogo.cn/g?b=qq&nk={0}&s=0",String.valueOf(qq))
                : MessageFormat.format("https://q2.qlogo.cn/headimg_dl?dst_uin={0}&spec=100",String.valueOf(qq));
    }

    public static void main(String[] args) {
        // GIF图片文件路径  
        String gifFilePath = "D:\\my\\bot\\resources\\download.gif";
        // 叠加的图片文件路径  
        String overlayImagePath = "D:\\my\\bot\\resources\\g.jpg"; // 或 .png  
        // 新的GIF图片输出文件路径  
        String outputGifPath = "D:\\my\\bot\\resources\\download1.gif";
        

        try {

            BufferedImage bufferedImage = Thumbnails.of(overlayImagePath)
                    .size(110, 110) // 设置目标图片的宽度和高度为200x200像素  
                    .asBufferedImage();

            GifDecoder gifDecoder = new GifDecoder();
            gifDecoder.read(new FileInputStream(gifFilePath));
            int n = gifDecoder.getFrameCount();
            List<BufferedImage> frames = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                BufferedImage frame = gifDecoder.getFrame(i);  // 原gif的帧
                // 在指定位置叠加图片（这里假设是(100, 100)）  
                Graphics2D g2d = frame.createGraphics();
//                BufferedImage overlayImage = ImageIO.read(new File(overlayImagePath));
                BufferedImage circularOverlay = makeImageCircular(bufferedImage);

                g2d.drawImage(frame, 0, 0, null);
//                g2d.drawImage(overlayImage, 100, 100, null);
                g2d.drawImage(circularOverlay, 100, 100, null);
                g2d.dispose();

                frames.add(frame);
            }

            File output = new File(outputGifPath);
            AnimatedGifEncoder animatedGifEncoder = new AnimatedGifEncoder();
            animatedGifEncoder.start(new FileOutputStream(output));
            animatedGifEncoder.setDelay(gifDecoder.getDelay(0));
            animatedGifEncoder.setRepeat(0);
            for (BufferedImage image : frames) {
                animatedGifEncoder.addFrame(image);
            }
            animatedGifEncoder.finish();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private static BufferedImage makeImageCircular(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();

        // 取较短的一边作为圆的直径  
        int diameter = Math.min(w,h);

        // 创建一个新的正方形BufferedImage，用于存放圆形图片  
        BufferedImage result = new BufferedImage(diameter, diameter, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = result.createGraphics();

        // 绘制圆形  
        Shape shape = new Ellipse2D.Double(0, 0, diameter, diameter);
        g2d.setClip(shape);

        // 将原始图片绘制到圆形区域  
        g2d.drawImage(image, (w - diameter) / 2, (h - diameter) / 2, null);
        g2d.dispose();

        // 裁剪多余部分  
        return result.getSubimage(0, 0, diameter, diameter);
    }

    public static BufferedImage cutImage1(BufferedImage originalImage){
        BufferedImage canvas = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        int centerX = originalImage.getWidth() / 2;
        int centerY = originalImage.getHeight() / 2;
        int radius = Math.min(centerX, centerY);
        graphics.setComposite(AlphaComposite.Clear);
        graphics.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
        graphics.setComposite(AlphaComposite.Src);
        graphics.drawImage(originalImage, 0, 0, null);
        graphics.dispose();
        return canvas;
    }


    public static BufferedImage cutImages(BufferedImage avatarImage) {
        try {
            avatarImage = scaleByPercentage(avatarImage, avatarImage.getWidth(),  avatarImage.getWidth());
            int width = avatarImage.getWidth();
            // 透明底的图片
            BufferedImage formatAvatarImage = new BufferedImage(width, width, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D graphics = formatAvatarImage.createGraphics();
            //把图片切成一个园
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            //留一个像素的空白区域，这个很重要，画圆的时候把这个覆盖
            int border = 1;
            //图片是一个圆型
            Ellipse2D.Double shape = new Ellipse2D.Double(border, border, width - border * 2, width - border * 2);
            //需要保留的区域
            graphics.setClip(shape);
            graphics.drawImage(avatarImage, border, border, width - border * 2, width - border * 2, null);
            graphics.dispose();
            //在圆图外面再画一个圆
            //新创建一个graphics，这样画的圆不会有锯齿 带白边
//            graphics = formatAvatarImage.createGraphics();
//            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int border1 = 3;
            //画笔是4.5个像素，BasicStroke的使用可以查看下面的参考文档
            //使画笔时基本会像外延伸一定像素，具体可以自己使用的时候测试
//            Stroke s = new BasicStroke(5F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
//            graphics.setStroke(s);
//            graphics.setColor(Color.WHITE);
//            graphics.drawOval(border1, border1, width - border1 * 2, width - border1 * 2);
//            graphics.dispose();
            return formatAvatarImage;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 缩小Image，此方法返回源图像按给定宽度、高度限制下缩放后的图像
     *
     * @param inputImage
     *            ：压缩后宽度
     *            ：压缩后高度
     * @throws java.io.IOException
     *             return
     */
    public static BufferedImage scaleByPercentage(BufferedImage inputImage, int newWidth, int newHeight){
        // 获取原始图像透明度类型
        try {
            int type = inputImage.getColorModel().getTransparency();
            int width = inputImage.getWidth();
            int height = inputImage.getHeight();
            // 开启抗锯齿
            RenderingHints renderingHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 使用高质量压缩
            renderingHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            BufferedImage img = new BufferedImage(newWidth, newHeight, type);
            Graphics2D graphics2d = img.createGraphics();
            graphics2d.setRenderingHints(renderingHints);
            graphics2d.drawImage(inputImage, 0, 0, newWidth, newHeight, 0, 0, width, height, null);
            graphics2d.dispose();
            return img;
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
