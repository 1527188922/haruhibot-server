
package com.kennycason.kumo;

import com.haruhi.botServer.utils.CommonUtil;
import com.kennycason.kumo.bg.Background;
import com.kennycason.kumo.bg.RectangleBackground;
import com.kennycason.kumo.collide.RectanglePixelCollidable;
import com.kennycason.kumo.collide.checkers.CollisionChecker;
import com.kennycason.kumo.collide.checkers.RectangleCollisionChecker;
import com.kennycason.kumo.collide.checkers.RectanglePixelCollisionChecker;
import com.kennycason.kumo.exception.KumoException;
import com.kennycason.kumo.font.FontWeight;
import com.kennycason.kumo.font.KumoFont;
import com.kennycason.kumo.font.scale.FontScalar;
import com.kennycason.kumo.font.scale.LinearFontScalar;
import com.kennycason.kumo.image.AngleGenerator;
import com.kennycason.kumo.image.CollisionRaster;
import com.kennycason.kumo.padding.Padder;
import com.kennycason.kumo.padding.RectanglePadder;
import com.kennycason.kumo.padding.WordPixelPadder;
import com.kennycason.kumo.palette.ColorPalette;
import com.kennycason.kumo.placement.RTreeWordPlacer;
import com.kennycason.kumo.placement.RectangleWordPlacer;
import com.kennycason.kumo.wordstart.RandomWordStart;
import com.kennycason.kumo.wordstart.WordStartStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.CollectionUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

public class WordCloud {
    private static final Logger LOGGER = LoggerFactory.getLogger(WordCloud.class);
    private static final int availableProcessors = Runtime.getRuntime().availableProcessors();
    public static final ExecutorService pool = new ThreadPoolExecutor(availableProcessors, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),new CustomizableThreadFactory("pool-buildWord-"));

    protected final Dimension dimension;
    protected final CollisionMode collisionMode;
    protected final CollisionChecker collisionChecker;
    protected final RectanglePixelCollidable backgroundCollidable;
    protected final CollisionRaster collisionRaster;
    protected final BufferedImage bufferedImage;
    protected final Padder padder;
    protected final Set<Word> skipped = new HashSet();
    protected int padding;
    protected Background background;
    protected Color backgroundColor;
    protected FontScalar fontScalar;
    protected KumoFont kumoFont;
    protected AngleGenerator angleGenerator;
    protected RectangleWordPlacer wordPlacer;
    protected ColorPalette colorPalette;
    protected WordStartStrategy wordStartStrategy;

    public WordCloud(Dimension dimension, CollisionMode collisionMode) {
        this.backgroundColor = Color.BLACK;
        this.fontScalar = new LinearFontScalar(10, 40);
        this.kumoFont = new KumoFont("Comic Sans MS", FontWeight.BOLD);
        this.angleGenerator = new AngleGenerator();
        this.wordPlacer = new RTreeWordPlacer();
        this.colorPalette = new ColorPalette(new int[]{177906, 3654384, 8178662, 12904434, 16777215});
        this.wordStartStrategy = new RandomWordStart();
        this.collisionMode = collisionMode;
        this.padder = derivePadder(collisionMode);
        this.collisionChecker = deriveCollisionChecker(collisionMode);
        this.collisionRaster = new CollisionRaster(dimension);
        this.bufferedImage = new BufferedImage(dimension.width, dimension.height, 2);
        this.backgroundCollidable = new RectanglePixelCollidable(this.collisionRaster, new Point(0, 0));
        this.dimension = dimension;
        this.background = new RectangleBackground(dimension);
    }

//    public void build(List<WordFrequency> wordFrequencies) {
//        Collections.sort(wordFrequencies);
//        this.wordPlacer.reset();
//        this.skipped.clear();
//        this.background.mask(this.backgroundCollidable);
//        int currentWord = 1;
//
//        for(Iterator var3 = this.buildWords(wordFrequencies, this.colorPalette).iterator(); var3.hasNext(); ++currentWord) {
//            Word word = (Word)var3.next();
//            Point point = this.wordStartStrategy.getStartingPoint(this.dimension, word);
//            boolean placed = this.place(word, point);
//            if (!placed) {
//                this.skipped.add(word);
//            }
//        }
//        this.drawForegroundToBackground();
//    }


    public void build(List<WordFrequency> wordFrequencies) {
        Collections.sort(wordFrequencies);
        this.wordPlacer.reset();
        this.skipped.clear();
        this.background.mask(this.backgroundCollidable);

        List<Word> words = this.buildWords(wordFrequencies, this.colorPalette);
        LOGGER.info("处理总数:{}",words.size());
        int i = CommonUtil.averageAssignNum(words.size(), availableProcessors);
        List<List<Word>> lists = CommonUtil.averageAssignList(words, i);
        CountDownLatch countDownLatch = new CountDownLatch(lists.size());
        LOGGER.info("分{}条线程处理",countDownLatch.getCount());
        for (List<Word> list : lists) {
            pool.execute(new BuildWordTask(list,countDownLatch));
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            LOGGER.info("countDownLatch.await() exception",e);
        }

        this.drawForegroundToBackground();
    }


    private class BuildWordTask implements Runnable {

        private List<Word> words;
        private CountDownLatch latch;
//        private Set<Word> skipped;
//        private Dimension dimension;
//        private WordStartStrategy wordStartStrategy;

//        ,Set<Word> skipped,Dimension dimension,WordStartStrategy wordStartStrategy
        public BuildWordTask(List<Word> words,CountDownLatch latch){
            this.words = words;
            this.latch = latch;
//            this.skipped = skipped;
//            this.dimension = dimension;
//            this.wordStartStrategy = wordStartStrategy;
        }

        @Override
        public void run() {
            try {
                if (!CollectionUtils.isEmpty(this.words)) {
                    LOGGER.info("当前线程处理数量:{}",words.size());
                    for (Word word : this.words) {
                        Point point = wordStartStrategy.getStartingPoint(dimension, word);
                        boolean placed = place(word, point);
                        if (!placed) {
                            skipped.add(word);
                        }
                    }
                }
            }catch (Exception e){
                LOGGER.info("Build word exception",e);
            }finally {
                latch.countDown();
            }
        }
    }

    public void writeToFile(String outputFileName) {
        String extension = "";
        int i = outputFileName.lastIndexOf(46);
        if (i > 0) {
            extension = outputFileName.substring(i + 1);
        }

        try {
            LOGGER.info("Saving WordCloud to: {}", outputFileName);
            ImageIO.write(this.bufferedImage, extension, new File(outputFileName));
        } catch (IOException var5) {
            LOGGER.error(var5.getMessage(), var5);
        }

    }

    public void writeToStreamAsPNG(OutputStream outputStream) {
        this.writeToStream("png", outputStream);
    }

    public void writeToStream(String format, OutputStream outputStream) {
        try {
            LOGGER.debug("Writing WordCloud image data to output stream");
            ImageIO.write(this.bufferedImage, format, outputStream);
            LOGGER.debug("Done writing WordCloud image data to output stream");
        } catch (IOException var4) {
            LOGGER.error(var4.getMessage(), var4);
            throw new KumoException("Could not write wordcloud to outputstream due to an IOException", var4);
        }
    }

    protected void drawForegroundToBackground() {
        if (this.backgroundColor != null) {
            BufferedImage backgroundBufferedImage = new BufferedImage(this.dimension.width, this.dimension.height, this.bufferedImage.getType());
            Graphics graphics = backgroundBufferedImage.getGraphics();
            graphics.setColor(this.backgroundColor);
            graphics.fillRect(0, 0, this.dimension.width, this.dimension.height);
            graphics.drawImage(this.bufferedImage, 0, 0, (ImageObserver)null);
            Graphics graphics2 = this.bufferedImage.getGraphics();
            graphics2.drawImage(backgroundBufferedImage, 0, 0, (ImageObserver)null);
        }
    }

    static int computeRadius(Dimension dimension, Point start) {
        int maxDistanceX = Math.max(start.x, dimension.width - start.x) + 1;
        int maxDistanceY = Math.max(start.y, dimension.height - start.y) + 1;
        return (int)Math.ceil(Math.sqrt((double)(maxDistanceX * maxDistanceX + maxDistanceY * maxDistanceY)));
    }

    protected boolean place(Word word, Point start) {
        Graphics graphics = this.bufferedImage.getGraphics();
        int maxRadius = computeRadius(this.dimension, start);
        Point position = word.getPosition();

        for(int r = 0; r < maxRadius; r += 2) {
            for(int x = Math.max(-start.x, -r); x <= Math.min(r, this.dimension.width - start.x - 1); ++x) {
                position.x = start.x + x;
                int offset = (int)Math.sqrt((double)(r * r - x * x));
                position.y = start.y + offset;
                if (position.y >= 0 && position.y < this.dimension.height && this.canPlace(word)) {
                    this.collisionRaster.mask(word.getCollisionRaster(), position);
                    graphics.drawImage(word.getBufferedImage(), position.x, position.y, (ImageObserver)null);
                    return true;
                }

                position.y = start.y - offset;
                if (offset != 0 && position.y >= 0 && position.y < this.dimension.height && this.canPlace(word)) {
                    this.collisionRaster.mask(word.getCollisionRaster(), position);
                    graphics.drawImage(word.getBufferedImage(), position.x, position.y, (ImageObserver)null);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean canPlace(Word word) {
        Point position = word.getPosition();
        Dimension dimensionOfWord = word.getDimension();
        if (position.y >= 0 && position.y + dimensionOfWord.height <= this.dimension.height) {
            if (position.x >= 0 && position.x + dimensionOfWord.width <= this.dimension.width) {
                switch(this.collisionMode) {
                    case RECTANGLE:
                        return !this.backgroundCollidable.collide(word) && this.wordPlacer.place(word);
                    case PIXEL_PERFECT:
                        return !this.backgroundCollidable.collide(word);
                    default:
                        return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    protected List<Word> buildWords(List<WordFrequency> wordFrequencies, ColorPalette colorPalette) {
        int maxFrequency = maxFrequency(wordFrequencies);
        List<Word> words = new ArrayList();
        Iterator var5 = wordFrequencies.iterator();

        while(var5.hasNext()) {
            WordFrequency wordFrequency = (WordFrequency)var5.next();
            if (!wordFrequency.getWord().isEmpty()) {
                words.add(this.buildWord(wordFrequency, maxFrequency, colorPalette));
            }
        }

        return words;
    }

    private Word buildWord(WordFrequency wordFrequency, int maxFrequency, ColorPalette colorPalette) {
        Graphics2D graphics = this.bufferedImage.createGraphics();
        graphics.setRenderingHints(Word.getRenderingHints());
        int frequency = wordFrequency.getFrequency();
        float fontHeight = this.fontScalar.scale(frequency, 0, maxFrequency);
        Font font = (wordFrequency.hasFont() ? wordFrequency.getFont() : this.kumoFont).getFont().deriveFont(fontHeight);
        FontMetrics fontMetrics = graphics.getFontMetrics(font);
        double theta = this.angleGenerator.randomNext();
        Word word = new Word(wordFrequency.getWord(), colorPalette.next(), fontMetrics, this.collisionChecker, theta);
        if (this.padding > 0) {
            this.padder.pad(word, this.padding);
        }

        return word;
    }

    private static int maxFrequency(List<WordFrequency> wordFrequencies) {
        return wordFrequencies.isEmpty() ? 1 : ((WordFrequency)wordFrequencies.get(0)).getFrequency();
    }

    private static Padder derivePadder(CollisionMode collisionMode) {
        switch(collisionMode) {
            case RECTANGLE:
                return new RectanglePadder();
            case PIXEL_PERFECT:
                return new WordPixelPadder();
            default:
                throw new IllegalArgumentException("CollisionMode can not be null");
        }
    }

    private static CollisionChecker deriveCollisionChecker(CollisionMode collisionMode) {
        switch(collisionMode) {
            case RECTANGLE:
                return new RectangleCollisionChecker();
            case PIXEL_PERFECT:
                return new RectanglePixelCollisionChecker();
            default:
                throw new IllegalArgumentException("CollisionMode can not be null");
        }
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }

    public void setColorPalette(ColorPalette colorPalette) {
        this.colorPalette = colorPalette;
    }

    public void setBackground(Background background) {
        this.background = background;
    }

    public void setFontScalar(FontScalar fontScalar) {
        this.fontScalar = fontScalar;
    }

    public void setKumoFont(KumoFont kumoFont) {
        this.kumoFont = kumoFont;
    }

    public void setAngleGenerator(AngleGenerator angleGenerator) {
        this.angleGenerator = angleGenerator;
    }

    public BufferedImage getBufferedImage() {
        return this.bufferedImage;
    }

    public Set<Word> getSkipped() {
        return this.skipped;
    }

    public void setWordStartStrategy(WordStartStrategy wordStartStrategy) {
        this.wordStartStrategy = wordStartStrategy;
    }

    public void setWordPlacer(RectangleWordPlacer wordPlacer) {
        this.wordPlacer = wordPlacer;
    }
}
