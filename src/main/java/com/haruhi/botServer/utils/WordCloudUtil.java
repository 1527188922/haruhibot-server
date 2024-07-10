package com.haruhi.botServer.utils;

import com.alibaba.fastjson.JSONObject;
import com.chenlb.mmseg4j.ComplexSeg;
import com.chenlb.mmseg4j.Dictionary;
import com.chenlb.mmseg4j.MMSeg;
import com.chenlb.mmseg4j.MaxWordSeg;
import com.chenlb.mmseg4j.Seg;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.constant.ThirdPartyURL;
import com.haruhi.botServer.dto.xml.bilibili.PlayerInfoResp;
import com.kennycason.kumo.CollisionMode;
import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.font.KumoFont;
import com.kennycason.kumo.font.scale.SqrtFontScalar;
import com.kennycason.kumo.image.AngleGenerator;
import com.kennycason.kumo.palette.ColorPalette;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.CollectionUtils;

import java.awt.Dimension;
import java.awt.Color;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class WordCloudUtil {
    private WordCloudUtil(){}
    private static Dictionary dic;
    private static Set<String> exclusionsList;
    private static Set<String> passList;
    static {
        initDictionary();
        initExclusionsWord();

    }
    private static void initExclusionsWord(){
        String[] exclusions = new String[]{"怎么","没有","都不","也要","了的","还能","又在","你又","我才","不到","我也","用来",
                "只能","我说","换个","能有","学人","不然","这一","搞了","放了","是的","想要","还要","不算","不会","有人","能看",
                "也是","还不","了吧","我不","之一","以看","会被","也得","接着","下去","更严","能放","不太","挂在","亦或","全是",
                "还用","说中","看样子","一拍","家里","看着","没啥","可是","都没","步子","又来","你们","应该","哪里","那个","过了",
                "我想","这种","就是","为了","一下","其他","只有","一点","下来","一样","真是","倒是","的话","至少","以为","时候",
                "反正","还好","个月","我的","只要","来着","居然","看过","之类","不起","要是","看到","那不","还行","看了","这么",
                "是个","罢了","就不","也没","是不是","不是","又不","甚至","有个","给我","可以","所以","他是","都是","不用","之前",
                "之后","三个","还是","让我","你这","或者","很多","很少","就会","那种","是一","多了","看不","用的","两个","一名",
                "一只","一些","是真","什么","就好","这是","这样","惨的","写的","不住","但是","也有","好像","这个","住了","但到",
                "跟我","一个","好多","好少","他之","你的","1k","不过","经常","一起","是谁","有些","他的","那就","来个","打不",
                "你是","我是","也不","不了","自己的","那么","不出","更有","也能","人的","14","31","几个","只是","除了","下一",
                "下了","之主","到了","来的","请使用","大笑","获取","一代","都有","有点","内容","那些","其实","一条","我都","主要",
                "这人","有所","大而","宁可","上看","别的","这才","出来","是说","放音","就算","能成","脸的","你有","这还","会有",
                "面的","边上","先试","又可"};
        exclusionsList = new HashSet<>(Arrays.asList(exclusions));
        String[] pass = new String[]{"杀"};
        passList = new HashSet<>(Arrays.asList(pass));
    }

    /**
     * 将 haruhi-bot/lib/mmseg4j-core-1.10.0.jar!/data 目录提前生成完成
     */
    private static void initDictionary(){
        log.info("开始初始化分词词库文件...");
        dic = Dictionary.getInstance();
        log.info("初始化分词词库完成");
    }
    private static String noSupport = "你的QQ暂不支持查看&#91;转发多条消息&#93;，请期待后续版本。";


    private static String replace(String s){
        return removeUrl(s.replace(noSupport,"").replaceAll(RegexEnum.CQ_CODE_REPLACR.getValue(), ""))
                .trim().replace(" ","")
                .replaceAll("&#93;|&#91;","")
                .replaceAll("[\\pP\\p{Punct}]","")
                .replaceAll("\\s*|\r|\n|\t","");
    }


    /**
     * 设置词的权重
     * @param corpus 已经分词的词料
     * @return key:词语 value:权重
     */
    public static Map<String,Integer> setFrequency(List<String> corpus){
        if (CollectionUtils.isEmpty(corpus)) {
            return null;
        }
        Map<String, Integer> map = new HashMap<>();
        for (String e : corpus) {
            if(map.containsKey(e)){
                Integer frequency = map.get(e) + 1;
                map.put(e,frequency);
            }else{
                map.put(e,1);
            }
        }
        return map;
    }
    public static Map<String,Integer> exclusionsWord(Map<String,Integer> corpus){
        if (CollectionUtils.isEmpty(corpus)) {
            return null;
        }
        corpus = corpus.entrySet().stream().filter(e -> exclusionsWord(e.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return corpus;
    }
    private static boolean exclusionsWord(String word){
        if(Strings.isBlank(word)){
            return false;
        }
        boolean pass = passList.contains(word);
        if (pass) {
            return true;
        }
        if(word.length() <= 1 || word.length() > 4){
            return false;
        }
        try {
            Integer.valueOf(word);
            return false;
        }catch (Exception e){}
        if(exclusionsList.contains(word)){
            return false;
        }
        return true;
    }
    /**
     * 生成词云图片
     * @param corpus
     * @param pngOutputPath 图片输出路径 png结尾
     */
    public static void generateWordCloudImage(Map<String,Integer> corpus, String pngOutputPath) {
        log.info("开始生成词云图,词料数量:{}",corpus.size());
        final List<WordFrequency> wordFrequencies = new ArrayList<>(corpus.size());
        // 加载词云有两种方式，一种是在txt文件中统计词出现的个数，另一种是直接给出每个词出现的次数，这里使用第二种
        // 文件格式如下
        for (Map.Entry<String, Integer> item : corpus.entrySet()) {
            wordFrequencies.add(new WordFrequency(item.getKey(),item.getValue()));
        }
        // 生成图片的像素大小  1 照片纵横比
        final Dimension dimension = new Dimension(1024, (int)(1024 * 1));
        final WordCloud wordCloud = new WordCloud(dimension, CollisionMode.PIXEL_PERFECT);
        // 调节词云的稀疏程度，越高越稀疏
        wordCloud.setPadding(10);

        //设置背景色
        wordCloud.setBackgroundColor(new Color(255,255,255));
        //设置背景图片
//        wordCloud.setBackground(new PixelBoundaryBackground(shapePicPath));

        // 颜色模板，不同频率的颜色会不同
        wordCloud.setColorPalette(new ColorPalette(new Color(255, 68, 51), new Color(208, 79, 8), new Color(225, 98, 50), new Color(231, 126, 88), new Color(175, 129, 3), new Color(243, 150, 9)));
        // 设置字体
        java.awt.Font font = new java.awt.Font("楷体", 0, 20);
        wordCloud.setKumoFont(new KumoFont(font));
        // 设置偏转角，角度为0时，字体都是水平的
        // wordCloud.setAngleGenerator(new AngleGenerator(0, 90, 9));
        wordCloud.setAngleGenerator(new AngleGenerator(0));
        // 字体的大小范围，最小是多少，最大是多少
        wordCloud.setFontScalar(new SqrtFontScalar(5, 80));
        wordCloud.build(wordFrequencies);
        wordCloud.writeToFile(pngOutputPath);
    }

    /**
     * 通过视频bv获取cid
     * @param bv
     * @return
     */
    public static PlayerInfoResp getPlayerInfo(String bv){
        Map<String, Object> param = new HashMap<>(2);
        param.put("bvid",bv);
        param.put("jsonp","jsonp");
        String responseStr = RestUtil.sendGetRequest(RestUtil.getRestTemplate(), ThirdPartyURL.PLAYER_CID, param, String.class);
        if(Strings.isNotBlank(responseStr)){
            JSONObject jsonObject = JSONObject.parseObject(responseStr);
            String data = jsonObject.getJSONArray("data").getString(0);
            return JSONObject.parseObject(data,PlayerInfoResp.class);
        }
        return null;
    }

    /**
     * 根据视频cid获取弹幕
     * @param cid
     * @return
     */
    public static List<String> getChatList(String cid){
        Map<String, Object> param = new HashMap<>();
        param.put("oid",cid);
        String responseSre = HttpClientUtil.doGet(HttpClientUtil.getHttpClient(10 * 1000),ThirdPartyURL.BULLET_CHAR, param);
        if(Strings.isNotBlank(responseSre)){
            Pattern compile = Pattern.compile("\">(.*?)</d>");
            Matcher matcher = compile.matcher(responseSre);
            List<String> res = new ArrayList<>();
            while (matcher.find()) {
                res.add(matcher.group(1));
            }
            return res;
        }
        return null;
    }

    /**
     * 根据av号获取bv号
     * @param av
     * @return
     */
    public static String getBvByAv(String av){

        String responseSre = HttpClientUtil.doGet(HttpClientUtil.getHttpClient(10 * 1000),ThirdPartyURL.BILIBILI_URL+"/" + av,null);
        if (Strings.isNotBlank(responseSre)) {
            Pattern compile = Pattern.compile("<meta data-vue-meta=\"true\" itemprop=\"url\" content=\"https://www.bilibili.com/video/(.*?)/\">");
            Matcher matcher = compile.matcher(responseSre);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }
    /**
     * mmseg4j 分词
     * https://www.jianshu.com/p/8ac06d2eef0d
     * https://blog.csdn.net/weixin_45248225/article/details/120847907
     * @param s
     * @return
     */
    public static List<String> mmsegWordSlices(String s){
        if(Strings.isBlank(s)){
            return null;
        }
        String replace = replace(s);
        if(Strings.isBlank(replace)){
            return null;
        }
        StringReader input = null;
        List<String> wordList;
        try {
            input = new StringReader(replace);
            Seg seg = new ComplexSeg(dic);//Complex分词
//            Seg seg = new MaxWordSeg(dic);//Complex分词
            // Seg seg = new SimpleSeg(dic);//Simple分词
            MMSeg mmSeg = new MMSeg(input, seg);
            com.chenlb.mmseg4j.Word word;
            wordList = new ArrayList<>();
            while ((word = mmSeg.next()) != null) {
                //word是单个分出的词
                wordList.add(word.getString());
            }
            return wordList;
        } catch (IOException e) {
            log.error("mmseg4j分词发生异常",e);
            return null;
        } finally {
            if(input != null){
                input.close();
            }
        }
    }

    private static String removeUrl(String commentStr) {
        if (Strings.isBlank(commentStr)) {
            return "";
        }
        String urlPattern = "((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
        Pattern p = Pattern.compile(urlPattern,Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(commentStr);
        int i = 0;
        while (m.find()) {
            String group = m.group(i);
            if(Strings.isBlank(group)){
                return "";
            }
            String s = commentStr.replaceAll(group, "");
            if (Strings.isBlank(s)) {
                return "";
            }
            commentStr = s.trim();
            i++;
        }
        return commentStr;
    }
}
