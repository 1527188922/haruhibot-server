package com.haruhi.botServer.handlers.message.face;

import cn.hutool.core.img.gif.AnimatedGifEncoder;
import cn.hutool.core.img.gif.GifDecoder;
import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.FileUtil;
import com.haruhi.botServer.utils.MatchResult;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import com.haruhi.botServer.ws.Server;
import com.simplerobot.modules.utils.KQCodeUtils;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class JumpHandler implements IGroupMessageEvent {

    @Override
    public int weight() {
        return HandlerWeightEnum.W_260.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_260.getName();
    }


    private static final String prefix = "jump_";


    @Autowired
    private AbstractWebResourceConfig abstractPathConfig;

    public static void clearJumpFace(){
        File file = new File(FileUtil.getFaceDir());
        if(file.isDirectory()){
            File[] files = file.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return !FileUtil.FILE_NAME_JUMP_TEMPLATE.equals(name)
                            && name.startsWith(prefix);
                }
            });

            if (files != null && files.length > 0) {
                for (File file1 : files) {
                    if (file1.delete()) {
                        log.info("删除jump表情：{}",file1.getAbsolutePath());
                    }else{
                        log.error("删除jump表情失败：{}",file1.getAbsolutePath());
                    }
                }
            }
        }
    }


    @Override
    public boolean onGroup(WebSocketSession session, Message message) {
        MatchResult<Pair<String, String>> pairMatchResult = matches(message);
        if (!pairMatchResult.isMatched()) {
            return false;
        }

        Pair<String, String> data = pairMatchResult.getData();

        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            try {
                String fileName = prefix + data.getRight() + ".gif";
                String out = FileUtil.getFaceDir() + File.separator + fileName;
                File file = new File(out);
                if(!file.exists()){
                    log.info("不存在该表情，开始生成: {}",fileName);
                    makeHuaQFace(Long.valueOf(data.getRight()), out);
                }
                log.info("jump表情图片地址：{}",out);
                sendFaceMsg(session,message.getGroupId(),fileName);
            }catch (Exception e){
                log.error("发送jump表情异常",e);
            }
        });
        return true;
    }

    private void sendFaceMsg(WebSocketSession session,Long groupId, String fileName){
        KQCodeUtils instance = KQCodeUtils.getInstance();
        String imageUrl = abstractPathConfig.webFacePath() + "/" + fileName + "?t=" + System.currentTimeMillis();
        log.info("jump图片url ：{}",imageUrl);
        String imageCq = instance.toCq(CqCodeTypeEnum.image.getType(), "file=" + imageUrl);
        Server.sendGroupMessage(session,groupId,imageCq,false);
    }

    private MatchResult<Pair<String,String>> matches(Message message){
        if(!message.isTextMsg() || !message.isAtMsg() || message.isAtSelf()){
            return MatchResult.unmatched();
        }

        if(message.getText(-1).trim().matches("跳")){
            return MatchResult.matched(Pair.of(String.valueOf(message.getUserId()), message.getAtQQs().get(0)));
        }

        return MatchResult.unmatched();
    }
    private String makeHuaQFace(Long atQQ, String out){
        try {
            BufferedImage bufferedImage = Thumbnails.of(ImageIO.read(new URL(CommonUtil.getAvatarUrl(atQQ,true))))
                    .size(40, 40)
                    .asBufferedImage();
            BufferedImage circularOverlay = CommonUtil.makeImageCircular(bufferedImage);

            GifDecoder gifDecoder = new GifDecoder();
            gifDecoder.read(new FileInputStream(FileUtil.getJumpFace()));
            int n = gifDecoder.getFrameCount();
            List<BufferedImage> frames = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                BufferedImage frame = gifDecoder.getFrame(i);  // 原gif的帧
                Graphics2D g2d = frame.createGraphics();
                g2d.drawImage(frame, 0, 0, null);

                if(i == 0){
                    g2d.drawImage(circularOverlay, 15, 49, null);
                }
                if(i == 1){
                    g2d.drawImage(circularOverlay, 13, 42, null);
                }
                if(i == 2){
                    g2d.drawImage(circularOverlay, 15, 23, null);
                }
                if(i == 3){
                    g2d.drawImage(circularOverlay, 14, 4, null);
                }
                if(i == 4){
                    g2d.drawImage(circularOverlay, 16, -4, null);
                }
                if(i == 5){
                    g2d.drawImage(circularOverlay, 16, -5, null);
                }
                if(i == 6){
                    g2d.drawImage(circularOverlay, 15, 3, null);
                }
                if(i == 7){
                    g2d.drawImage(circularOverlay, 15, 31, null);
                }
                g2d.dispose();
                frames.add(frame);
            }

            File output = new File(out);
            AnimatedGifEncoder animatedGifEncoder = new AnimatedGifEncoder();
            animatedGifEncoder.start(new FileOutputStream(output));
            animatedGifEncoder.setDelay(gifDecoder.getDelay(0));
            animatedGifEncoder.setRepeat(0);
            for (BufferedImage image : frames) {
                animatedGifEncoder.addFrame(image);
            }
            animatedGifEncoder.finish();
            return out;
        }catch (Exception e){
            log.error("生成jump图片异常",e);
        }
        return null;
    }
}
