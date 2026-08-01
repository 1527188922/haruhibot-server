package com.haruhi.botServer.wordcloud;

import com.kennycason.kumo.CollisionMode;
import com.kennycason.kumo.Word;
import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.WordFrequency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentWordCloud extends WordCloud {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentWordCloud.class);
    private static final int DEFAULT_WORKERS = Runtime.getRuntime().availableProcessors();

    private final int workerCount;
    private final Object placementLock = new Object();

    public ConcurrentWordCloud(Dimension dimension, CollisionMode collisionMode) {
        this(dimension, collisionMode, DEFAULT_WORKERS);
    }

    public ConcurrentWordCloud(Dimension dimension, CollisionMode collisionMode, int workerCount) {
        super(dimension, collisionMode);
        this.workerCount = Math.max(1, workerCount);
    }

    @Override
    public void build(List<WordFrequency> wordFrequencies) {
        Collections.sort(wordFrequencies);
        this.wordPlacer.reset();
        this.skipped.clear();
        this.background.mask(this.backgroundCollidable);

        List<Word> words = this.buildWords(wordFrequencies, this.colorPalette);
        if (words.isEmpty()) {
            this.drawForegroundToBackground();
            return;
        }

        int poolSize = Math.min(this.workerCount, words.size());
        Set<Word> skippedWords = ConcurrentHashMap.newKeySet();
        AtomicInteger currentWord = new AtomicInteger(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(poolSize)) {
            List<Future<?>> futures = new ArrayList<>(words.size());
            for (Word word : words) {
                Point point = this.wordStartStrategy.getStartingPoint(this.dimension, word);
                futures.add(executor.submit(() -> placeAndLog(word, point, currentWord.getAndIncrement(), words.size(), skippedWords)));
            }

            for (Future<?> future : futures) {
                future.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Build concurrent word cloud failed", e);
        } catch (Exception e) {
            throw new IllegalStateException("Build concurrent word cloud failed", e);
        }

        this.skipped.addAll(skippedWords);
        this.drawForegroundToBackground();
    }

    private void placeAndLog(Word word, Point point, int currentWord, int totalWords, Set<Word> skippedWords) {
        boolean placed = this.placeConcurrent(word, point);
        if (placed) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("placed: {} ({}/{})", word.getWord(), currentWord, totalWords);
            }
            return;
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("skipped: {} ({}/{})", word.getWord(), currentWord, totalWords);
        }
        skippedWords.add(word);
    }

    private boolean placeConcurrent(Word word, Point start) {
        int maxRadius = computeRadius(this.dimension, start);
        Point position = word.getPosition();

        for (int r = 0; r < maxRadius; r += 2) {
            for (int x = Math.max(-start.x, -r); x <= Math.min(r, this.dimension.width - start.x - 1); ++x) {
                position.x = start.x + x;
                int offset = (int) Math.sqrt((double) (r * r - x * x));
                position.y = start.y + offset;
                if (position.y >= 0 && position.y < this.dimension.height && this.tryCommit(word, position)) {
                    return true;
                }

                position.y = start.y - offset;
                if (offset != 0 && position.y >= 0 && position.y < this.dimension.height && this.tryCommit(word, position)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean tryCommit(Word word, Point position) {
        synchronized (this.placementLock) {
            if (!this.canPlace(word)) {
                return false;
            }
            this.collisionRaster.mask(word.getCollisionRaster(), position);
            Graphics graphics = this.bufferedImage.getGraphics();
            graphics.drawImage(word.getBufferedImage(), position.x, position.y, (ImageObserver) null);
            graphics.dispose();
            return true;
        }
    }

    private boolean canPlace(Word word) {
        Point position = word.getPosition();
        Dimension dimensionOfWord = word.getDimension();
        if (position.y < 0 || position.y + dimensionOfWord.height > this.dimension.height) {
            return false;
        }
        if (position.x < 0 || position.x + dimensionOfWord.width > this.dimension.width) {
            return false;
        }

        return switch (this.collisionMode) {
            case RECTANGLE -> !this.backgroundCollidable.collide(word) && this.wordPlacer.place(word);
            case PIXEL_PERFECT -> !this.backgroundCollidable.collide(word);
        };
    }

    private static int computeRadius(Dimension dimension, Point start) {
        int maxDistanceX = Math.max(start.x, dimension.width - start.x) + 1;
        int maxDistanceY = Math.max(start.y, dimension.height - start.y) + 1;
        return (int) Math.ceil(Math.sqrt((double) (maxDistanceX * maxDistanceX + maxDistanceY * maxDistanceY)));
    }
}
