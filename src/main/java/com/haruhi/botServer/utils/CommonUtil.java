package com.haruhi.botServer.utils;

import cn.hutool.core.img.gif.AnimatedGifEncoder;
import cn.hutool.core.img.gif.GifDecoder;
import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.BaseResp;
import com.simplerobot.modules.utils.KQCodeUtils;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.CollectionUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

    public static BaseResp<String> commandStartsWith(final String command, RegexEnum regexEnum){
        String[] split = regexEnum.getValue().split("\\|");
        for (String s : split) {
            if (command.startsWith(s)) {
                return BaseResp.success(command.replaceFirst(s,""));
            }
        }
        return BaseResp.fail();
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

    /**
     * 将列表切分为指定数量的子列表
     */
    public static <T> List<List<T>> split(List<T> list, int splitCount) {
        List<List<T>> result = new ArrayList<>();

        // 处理空列表或无效参数
        if (list == null || list.isEmpty()) {
            return result;
        }

        int size = list.size();
        // 计算实际切分数量（确保不小于1）
        int actualSplitCount = Math.max(1, Math.min(splitCount, size));

        // 计算每个子列表的基础大小（向上取整）
        int chunkSize = (int) Math.ceil((double) size / actualSplitCount);

        // 执行切分
        for (int i = 0; i < size; i += chunkSize) {
            int end = Math.min(i + chunkSize, size);
            result.add(new ArrayList<>(list.subList(i, end)));
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
//        sss();
//        ss();
//        System.out.println(isValidMagnetLink("magnet:?xt=urn:btih:GHTGWJOAMHKQNBO5PA7HPAGWW3GINTVD"));

        String s = formatDuration(124124000L, TimeUnit.MILLISECONDS);
        System.out.println(s);

    }

    private static void sss(){
        // GIF图片文件路径
        String gifFilePath = "D:\\temp\\resources\\bot\\download.gif";
        // 叠加的图片文件路径
        String overlayImagePath = "D:\\temp\\resources\\bot\\g.jpg"; // 或 .png
        // 新的GIF图片输出文件路径
        String outputGifPath = "D:\\temp\\resources\\bot\\download1.gif";


        try {
            BufferedImage bufferedImage = Thumbnails.of(overlayImagePath)
                    .size(120, 120) // 设置目标图片的宽度和高度为200x200像素
                    .asBufferedImage();


            BufferedImage bufferedImage1 = Thumbnails.of(overlayImagePath)
                    .size(112, 112) // 设置目标图片的宽度和高度为200x200像素
                    .asBufferedImage();


            bufferedImage1 = rotateImage(bufferedImage1,90,new Color(255, 255, 255));

            GifDecoder gifDecoder = new GifDecoder();
            gifDecoder.read(new FileInputStream(gifFilePath));
            int n = gifDecoder.getFrameCount();
            List<BufferedImage> frames = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                BufferedImage frame = gifDecoder.getFrame(i);  // 原gif的帧
                Graphics2D g2d = frame.createGraphics();
                BufferedImage circularOverlay = makeImageCircular(bufferedImage);
                BufferedImage circularOverlay1 = makeImageCircular(bufferedImage1);

                g2d.drawImage(frame, 0, 0, null);
                if(i == 0){
                    g2d.drawImage(circularOverlay, 117, -7, null);
                    g2d.drawImage(circularOverlay1, 3, 176, null);
                }
                if(i == 1){
                    g2d.drawImage(circularOverlay, 110, 4, null);
                    g2d.drawImage(circularOverlay1, 13 , 173, null);
                }
                if(i == 2){
                    g2d.drawImage(circularOverlay, 132, -9, null);
                    g2d.drawImage(circularOverlay1, 7, 159, null);
                }
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
    private static void ss(){
        try {
            // 读取GIF图片
            File gifFile = new File("D:\\temp\\resources\\bot\\download1.gif");
            ImageInputStream imageInputStream = ImageIO.createImageInputStream(gifFile);
            Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageInputStream);
            ImageReader reader = imageReaders.next();
            reader.setInput(imageInputStream);

            // 获取GIF图片的帧数
            int numFrames = reader.getNumImages(true);
            // 0 x110 y-16
            // 解帧并保存
            for (int i = 0; i < numFrames; i++) {
                BufferedImage frame = reader.read(i);
                File outputFile = new File("D:\\temp\\resources\\bot\\frame_" + i + ".png");
                ImageIO.write(frame, "png", outputFile);
            }

            // 关闭流
            imageInputStream.close();
            reader.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static BufferedImage makeImageCircular(BufferedImage image) {
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


    /**
     * 创建任意角度的旋转图像
     * @param image
     * @param theta
     * @param backgroundColor
     * @return
     */
    public static BufferedImage rotateImage(BufferedImage image, double theta,Color backgroundColor) {
        int width = image.getWidth();
        int height = image.getHeight();
        double angle = theta * Math.PI / 180; // 度转弧度
        double[] xCoords = getX(width / 2, height / 2, angle);
        double[] yCoords = getY(width / 2, height / 2, angle);
        int WIDTH = (int) (xCoords[3] - xCoords[0]);
        int HEIGHT = (int) (yCoords[3] - yCoords[0]);
        BufferedImage resultImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                int x = i - WIDTH / 2;
                int y = HEIGHT / 2 - j;
                double radius = Math.sqrt(x * x + y * y);
                double angle1;
                if (y > 0) {
                    angle1 = Math.acos(x / radius);
                } else {
                    angle1 = 2 * Math.PI - Math.acos(x / radius);
                }
                x = (int) (radius * Math.cos(angle1 - angle));
                y = (int) (radius * Math.sin(angle1 - angle));
                if (x < (width / 2) & x > -(width / 2) & y < (height / 2) & y > -(height / 2)) {
                    int rgb = image.getRGB(x + width / 2, height / 2 - y);
                    resultImage.setRGB(i, j, rgb);
                }else {
                    int rgb = ((0 & 0xff) << 24) | ((backgroundColor.getRed() & 0xff) << 16) | ((backgroundColor.getGreen() & 0xff) << 8)
                            | ((backgroundColor.getBlue() & 0xff));
                    resultImage.setRGB(i, j, rgb);
                }
            }
        }
        return resultImage;
    }

    // 获取四个角点旋转后Y方向坐标
    private static double[] getY(int i, int j, double angle) {
        double results[] = new double[4];
        double radius = Math.sqrt(i * i + j * j);
        double angle1 = Math.asin(j / radius);
        results[0] = radius * Math.sin(angle1 + angle);
        results[1] = radius * Math.sin(Math.PI - angle1 + angle);
        results[2] = -results[0];
        results[3] = -results[1];
        Arrays.sort(results);
        return results;
    }

    // 获取四个角点旋转后X方向坐标
    private static double[] getX(int i, int j, double angle) {
        double results[] = new double[4];
        double radius = Math.sqrt(i * i + j * j);
        double angle1 = Math.acos(i / radius);
        results[0] = radius * Math.cos(angle1 + angle);
        results[1] = radius * Math.cos(Math.PI - angle1 + angle);
        results[2] = -results[0];
        results[3] = -results[1];
        Arrays.sort(results);
        return results;
    }


    /**
     * 判断字符串是否为磁力链接
     */
    public static boolean isValidMagnetLink(String input) {
        if (StringUtils.isBlank(input)) {
            return false;
        }

        return input.startsWith("magnet:?xt=urn:btih:") && !input.contains("\n") && !input.contains("\r");
    }

    /**
     * 替换字符串
     * 不区分大小写
     * @param str
     * @param oldStr
     * @param newStr
     * @return
     */
    public static String replaceIgnoreCase(String str, String oldStr, String newStr) {
        return str.replaceAll("(?i)" + Pattern.quote(oldStr), Matcher.quoteReplacement(newStr));
    }

    public static String formatDuration(long duration, TimeUnit unit) {
        long totalMillis = TimeUnit.MILLISECONDS.convert(duration, unit);

        long days = totalMillis / TimeUnit.DAYS.toMillis(1);
        long remaining = totalMillis % TimeUnit.DAYS.toMillis(1);

        long hours = remaining / TimeUnit.HOURS.toMillis(1);
        remaining %= TimeUnit.HOURS.toMillis(1);

        long minutes = remaining / TimeUnit.MINUTES.toMillis(1);
        remaining %= TimeUnit.MINUTES.toMillis(1);

        long seconds = remaining / TimeUnit.SECONDS.toMillis(1);
        long millis = remaining % TimeUnit.SECONDS.toMillis(1);

        List<String> parts = new ArrayList<>();
        addIfPositive(parts, days, "天");
        addIfPositive(parts, hours, "小时");
        addIfPositive(parts, minutes, "分钟");
        addIfPositive(parts, seconds, "秒");
        addIfPositive(parts, millis, "毫秒");

        if (parts.isEmpty()) {
            parts.add("0毫秒");
        }
        return String.join("", parts);
    }

    private static void addIfPositive(List<String> parts, long value, String unit) {
        if (value > 0) {
            parts.add(value + unit);
        }
    }

    public static String substring(String str, int endIndex){
        if(StringUtils.isEmpty(str) || endIndex <= 0){
            return str;
        }
        return str.length() > endIndex ? str.substring(0,endIndex) : str;
    }

    // 找出新增元素（newList有但oldList没有）
    public static <T, K> List<T> findAdded(List<T> newList, List<T> oldList, FieldExtractor<T, K> extractor) {
        Set<K> oldKeys = oldList.stream()
                .map(extractor::extract)
                .collect(Collectors.toSet());

        return newList.stream()
                .filter(item -> !oldKeys.contains(extractor.extract(item)))
                .collect(Collectors.toList());
    }

    // 找出减少元素（oldList有但newList没有）
    public static <T, K> List<T> findRemoved(List<T> newList, List<T> oldList, FieldExtractor<T, K> extractor) {
        Set<K> newKeys = newList.stream()
                .map(extractor::extract)
                .collect(Collectors.toSet());

        return oldList.stream()
                .filter(item -> !newKeys.contains(extractor.extract(item)))
                .collect(Collectors.toList());
    }

    // 字段提取接口
    @FunctionalInterface
    public interface FieldExtractor<T, K> {
        K extract(T item);
    }
}
