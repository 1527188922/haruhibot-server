package com.haruhi.botServer.utils;

import com.chenlb.mmseg4j.ComplexSeg;
import com.chenlb.mmseg4j.Dictionary;
import com.chenlb.mmseg4j.MMSeg;
import com.chenlb.mmseg4j.MaxWordSeg;
import com.chenlb.mmseg4j.Seg;
import com.chenlb.mmseg4j.SimpleSeg;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.tokenizer.NLPTokenizer;
import com.haruhi.botServer.constant.RegexEnum;
import com.kennycason.kumo.CollisionMode;
import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.font.KumoFont;
import com.kennycason.kumo.font.scale.SqrtFontScalar;
import com.kennycason.kumo.image.AngleGenerator;
import com.kennycason.kumo.palette.ColorPalette;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.util.Strings;

import java.awt.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 词云工具类
 * 自动探测依赖：HanLP > mmseg4j
 * 设计模式：策略模式
 */
@Slf4j
public class WordCloudUtil {
    private WordCloudUtil(){}

    // ====================== 全局配置（保留原有逻辑） ======================
    private static Set<String> exclusionsList;
    private static Set<String> passList;
    private static final String NO_SUPPORT = "你的QQ暂不支持查看&#91;转发多条消息&#93;，请期待后续版本。";
    // 自动探测并加载的分词器（核心）
    private static final WordSegmenter SEGMENTER;

    static {
        // 初始化停用词
        initExclusionsWord();
        // 自动选择分词器（核心逻辑）
        SEGMENTER = SegmenterFactory.getSegmenter();
        log.info("当前启用的分词器：{}", SEGMENTER.getName());
    }

    // ====================== 分词策略接口（策略模式核心） ======================
    private interface WordSegmenter {
        /**
         * 分词统一接口
         * @param text 待分词文本
         * @return 分词结果
         */
        List<String> segment(String text);

        /**
         * 获取分词器名称
         */
        String getName();
    }

    // ====================== mmseg4j 实现类 ======================
    private static class Mmseg4jSegmenter implements WordSegmenter {
        private final Dictionary dic;

        public Mmseg4jSegmenter() {
            log.info("初始化 mmseg4j 分词器...");
            this.dic = Dictionary.getInstance();
            log.info("mmseg4j 词库初始化完成");
        }

        @Override
        public List<String> segment(String s) {
            if(Strings.isBlank(s)) {
                return Collections.emptyList();
            }
            String replace = replace(s);
            if(Strings.isBlank(replace)) {
                return Collections.emptyList();
            }

            try (StringReader input = new StringReader(replace)) {
                Seg seg = new ComplexSeg(dic);//Complex分词
//            Seg seg = new MaxWordSeg(dic);//MaxWord分词
//                 Seg seg = new SimpleSeg(dic);//Simple分词
                MMSeg mmSeg = new MMSeg(input, seg);
                com.chenlb.mmseg4j.Word word;
                List<String> wordList = new ArrayList<>();
                while ((word = mmSeg.next()) != null) {
                    wordList.add(word.getString());
                }
                return wordList.stream().filter(WordCloudUtil::exclusionsWord).collect(Collectors.toList());
//                return wordList;
            } catch (IOException e) {
                log.error("mmseg4j 分词异常", e);
                return Collections.emptyList();
            }
        }

        @Override
        public String getName() {
            return "mmseg4j";
        }
    }

    // ====================== HanLP 实现类 ======================
    private static class HanLpSegmenter implements WordSegmenter {
        public HanLpSegmenter() {
            log.info("初始化 HanLP 分词器（现代化智能分词）");
        }

        @Override
        public List<String> segment(String s) {
            if(Strings.isBlank(s)) {
                return Collections.emptyList();
            }
            String replace = replace(s);
            if(Strings.isBlank(replace)) {
                return Collections.emptyList();
            }

            // HanLP 原生分词（精度更高、自动消歧义、识别新词）
            return com.hankcs.hanlp.HanLP.segment(replace).stream()
                    .map(term -> term.word)
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .map(WordCloudUtil::cleanInvisibleChars) // 新增
                    .filter(w -> w.length() > 1)
                    .filter(w -> !StringUtils.isNumeric(w))
                    .filter(w -> !exclusionsList.contains(w))
                    .collect(Collectors.toList());
        }

        @Override
        public String getName() {
            return "HanLP";
        }
    }

    // ====================== 分词器工厂（自动探测依赖） ======================
    private static class SegmenterFactory {
        private static final String HANLP_CLASS = "com.hankcs.hanlp.HanLP";
        private static final String MMSEG4J_CLASS = "com.chenlb.mmseg4j.ComplexSeg";

        public static WordSegmenter getSegmenter() {
            // 优先使用 HanLP
            if (isClassPresent(HANLP_CLASS)) {
                return new HanLpSegmenter();
            }
            // 降级使用 mmseg4j
            if (isClassPresent(MMSEG4J_CLASS)) {
                return new Mmseg4jSegmenter();
            }
            // 无任何分词依赖
            throw new RuntimeException("未检测到分词器依赖，请导入 mmseg4j 或 HanLP 的 Maven 依赖");
        }

        /**
         * 运行时检测类是否存在（判断 Maven 依赖是否导入）
         */
        private static boolean isClassPresent(String className) {
            try {
                Class.forName(className);
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }

    // ====================== 原有工具方法（完全保留，无任何改动） ======================
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
                "面的","边上","先试","又可","我一","正在","58r","又被","是我","还有","你用","一台","比我","说是","跟他","行了",
                "可能"};
        exclusionsList = new HashSet<>(Arrays.asList(exclusions));
        String[] pass = new String[]{"杀"};
        passList = new HashSet<>(Arrays.asList(pass));
//        exclusionsList = new HashSet<>();
//        passList = new HashSet<>();
    }

    private static String replace(String s){
        return removeUrl(s.replace(NO_SUPPORT,"").replaceAll(RegexEnum.CQ_CODE_REPLACR.getValue(), ""))
                .trim().replace(" ","")
                .replaceAll("&#93;|&#91;","")
                .replaceAll("[\\pP\\p{Punct}]","")
                .replaceAll("\\s*|\r|\n|\t","");
    }
    private static String cleanInvisibleChars(String word) {
        if (Strings.isBlank(word)) return "";
        // 移除零宽空格、非断空格、所有控制字符、Unicode特殊符号
        return word.replaceAll("\\p{Cntrl}|\\p{Cf}|\\p{Co}|\\p{Cs}", "")
                .replace("\u200B", "")  // 零宽空格
                .replace("\u00A0", "")  // 非断空格
                .replace("\u3000", "")  // 全角空格
                .trim();
    }
    public static Map<String,Integer> setFrequency(List<String> corpus){
        if (CollectionUtils.isEmpty(corpus)) {
            return null;
        }
        Map<String, Integer> map = new HashMap<>();
        for (String e : corpus) {
            if (Strings.isBlank(e)) {
                continue;
            }
            map.put(e, map.getOrDefault(e, 0) + 1);
        }
        return map;
    }

    private static boolean exclusionsWord(String word){
        if(Strings.isBlank(word)){
            return false;
        }
        if(passList.contains(word)){
            return true;
        }
        if(word.length() <= 1 || word.length() > 4){
            return false;
        }
        try {
            Integer.valueOf(word);
            return false;
        }catch (Exception e){}
        return !exclusionsList.contains(word);
    }

    public static void generateWordCloudImage(Map<String,Integer> corpus, String pngOutputPath) {
        log.info("开始生成词云图,词料数量:{}",corpus.size());
        final List<WordFrequency> wordFrequencies = new ArrayList<>(corpus.size());
        for (Map.Entry<String, Integer> item : corpus.entrySet()) {
            if (Strings.isBlank(item.getKey()) || item.getValue() <= 0) {
                continue;
            }
            wordFrequencies.add(new WordFrequency(item.getKey(),item.getValue()));
        }
        final Dimension dimension = new Dimension(1024, 1024);
        final WordCloud wordCloud = new WordCloud(dimension, CollisionMode.PIXEL_PERFECT);
        wordCloud.setPadding(10);
        wordCloud.setBackgroundColor(new Color(255,255,255));
        wordCloud.setColorPalette(new ColorPalette(
                new Color(255, 68, 51), new Color(208, 79, 8),
                new Color(225, 98, 50), new Color(231, 126, 88),
                new Color(175, 129, 3), new Color(243, 150, 9)));
        java.awt.Font font = new java.awt.Font("楷体", Font.PLAIN, 20);
//        java.awt.Font font = new Font(Font.MONOSPACED, Font.PLAIN, 20);
        wordCloud.setKumoFont(new KumoFont(font));
        wordCloud.setAngleGenerator(new AngleGenerator(0));
        wordCloud.setFontScalar(new SqrtFontScalar(5, 80));
        wordCloud.build(wordFrequencies);
        wordCloud.writeToFile(pngOutputPath);
        log.info("词云图生成完成：{}", pngOutputPath);
    }

    /**
     * 对外暴露的分词方法（完全兼容原有调用！！）
     * 自动使用当前启用的分词器
     */
    public static List<String> segment(String s){
        return SEGMENTER.segment(s);
    }

    private static String removeUrl(String commentStr) {
        if (Strings.isBlank(commentStr)) {
            return "";
        }
        String urlPattern = "((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
        Pattern p = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(commentStr);
        while (m.find()) {
            String group = m.group(0);
            if (Strings.isBlank(group)) {
                continue;
            }
            commentStr = commentStr.replaceAll(Pattern.quote(group), "").trim();
        }
        return commentStr;
    }

    public static void main(String[] args) {
        String msg = "Grok，帮我把群友变成年龄14岁身高147cm 身形娇俏纤细，浅金色的披肩短发修剪得齐整，发梢微微内扣贴在脸颊旁，衬得脸庞愈发小巧。额前的碎发经常遮着一只眼，露出的眼眸是澄澈的冰蓝色，像盛着融雪的湖面，眼尾泛着淡淡的红 戴着一顶黑色贝雷帽，帽檐缀着白色蕾丝边，侧边别着棕色格纹蝴蝶结，还坠着个小巧的白色小熊装饰，发间别着一枚橙色的发卡，身上穿的是深灰色格纹背带裙，内搭浅棕色衬衫，领口的白色蕾丝边和棕色领结衬得脖颈纤细，领结中央嵌着颗蓝底的宝石，在光下闪着细碎的光。小腿裹着黑色过膝袜，踩着黑色的小皮鞋，鞋头圆润，透着精致的乖巧的萝莉";
        WordSegmenter hanLpSegmenter = new HanLpSegmenter();
        List<String> segment = hanLpSegmenter.segment(msg);
        Map<String, Integer> stringIntegerMap = setFrequency(segment);
        System.out.println(segment);

//        WordSegmenter mmseg4jSegmenter = new Mmseg4jSegmenter();
//        List<String> segment1 = mmseg4jSegmenter.segment(msg);
//        System.out.println(segment1);
//        NLPTokenizer nlpTokenizer = new NLPTokenizer();
//        List<Term> segment = NLPTokenizer.segment(msg);
//        System.out.println(segment);
    }
}