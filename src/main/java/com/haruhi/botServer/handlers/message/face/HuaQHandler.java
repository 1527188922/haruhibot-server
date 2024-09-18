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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class HuaQHandler implements IGroupMessageEvent {


    @Override
    public int weight() {
        return HandlerWeightEnum.W_270.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_270.getName();
    }
    
    private static final String prefix = "huaq_";

    @Autowired
    private AbstractWebResourceConfig abstractPathConfig;
    
    public static void clearHuaQFace(){
        File file = new File(FileUtil.getFaceDir());
        if(file.isDirectory()){
            File[] files = file.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return !FileUtil.FILE_NAME_HUAQ_TEMPLATE.equals(name)
                            && name.startsWith(prefix);
                }
            });

            if (files != null && files.length > 0) {
                for (File file1 : files) {
                    if (file1.delete()) {
                        log.info("删除huaq表情：{}",file1.getAbsolutePath());
                    }else{
                        log.error("删除huaq表情失败：{}",file1.getAbsolutePath());
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
                String fileName = prefix + data.getLeft() + "_" + data.getRight() + ".gif";
                String out = FileUtil.getFaceDir() + File.separator + fileName;
                File file = new File(out);
                if(!file.exists()){
                    log.info("不存在该表情，开始生成: {}",fileName);
                    makeHuaQFace(Long.valueOf(data.getLeft()), Long.valueOf(data.getRight()), out);
                }
                log.info("huaq表情图片地址：{}",out);
                sendFaceMsg(session,message.getGroupId(),fileName);
            }catch (Exception e){
                log.error("发送huaQ表情异常",e);
            }
        });
        return true;
    }
    
    private void sendFaceMsg(WebSocketSession session,Long groupId, String fileName){
        KQCodeUtils instance = KQCodeUtils.getInstance();
        String imageUrl = abstractPathConfig.webFacePath() + "/" + fileName + "?t=" + System.currentTimeMillis();
        log.info("huaq图片url ：{}",imageUrl);
        String imageCq = instance.toCq(CqCodeTypeEnum.image.getType(), "file=" + imageUrl);
        Server.sendGroupMessage(session,groupId,imageCq,false);
    }


    public static void main(String[] args) throws IOException {

        String imgPath = "D:\\temp\\pic\\e7657678d75003dd4c6162b44039b0b6.gif";
        GifDecoder gifDecoder = new GifDecoder();
        gifDecoder.read(new FileInputStream(imgPath));
        int n = gifDecoder.getFrameCount();
        BufferedImage bufferedImage = Thumbnails.of(ImageIO.read(new URL(CommonUtil.getAvatarUrl(1527188922L,true))))
                .size(74, 74)
                .asBufferedImage();
        BufferedImage circularOverlay = CommonUtil.makeImageCircular(bufferedImage);


        for (int i = 0; i < n; i++) {
            BufferedImage frame = gifDecoder.getFrame(i);  // 原gif的帧
            Graphics2D g2d = frame.createGraphics();

            g2d.drawImage(frame, 0, 0, null);
            if(i == 0){
                g2d.drawImage(circularOverlay, 82, 73, null);
//                ImageIO.write(frame,"png", new File("D:\\temp\\pic\\output\\output"+i+"-"+i+".png"));
            }

            if(i == 1 || i == 2){
                g2d.drawImage(circularOverlay, 84, 75, null);
//                ImageIO.write(frame,"png", new File("D:\\temp\\pic\\output\\output"+i+"-"+i+".png"));
            }

            if(i == 3){
                g2d.drawImage(circularOverlay, 79, 75, null);
//                ImageIO.write(frame,"png", new File("D:\\temp\\pic\\output\\output"+i+"-"+i+".png"));
            }
            if(i == 4){
                g2d.drawImage(circularOverlay, 74, 75, null);
//                ImageIO.write(frame,"png", new File("D:\\temp\\pic\\output\\output"+i+"-"+i+".png"));
            }
            if(i == 5){
                g2d.drawImage(circularOverlay, 74, 77, null);
//                ImageIO.write(frame,"png", new File("D:\\temp\\pic\\output\\output"+i+"-"+i+".png"));
            }
            if(i == 6 || i == 7 || i == 8){
                g2d.drawImage(circularOverlay, 75, 78, null);
//                ImageIO.write(frame,"png", new File("D:\\temp\\pic\\output\\output"+i+"-"+i+".png"));
            }
            if(i == 9 || i == 10){
                g2d.drawImage(circularOverlay, 77, 78, null);
                ImageIO.write(frame,"png", new File("D:\\temp\\pic\\output\\output"+i+"-"+i+".png"));
            }
            if(i == 11){
                circularOverlay = Thumbnails.of(circularOverlay)
                        .size(67, 67)
                        .asBufferedImage();
                circularOverlay = CommonUtil.rotateImage(circularOverlay,18,new Color(255, 255, 255));
                g2d.drawImage(circularOverlay, 75, 75, null);
//                ImageIO.write(frame,"png", new File("D:\\temp\\pic\\output\\output"+i+"-"+i+".png"));
            }

            if(i == 12){
                circularOverlay = CommonUtil.rotateImage(circularOverlay,25,new Color(255, 255, 255));
                g2d.drawImage(circularOverlay, 60, 55, null);
//                ImageIO.write(frame,"png", new File("D:\\temp\\pic\\output\\output"+i+"-"+i+".png"));
            }
            if(i == 13){
                circularOverlay = CommonUtil.rotateImage(circularOverlay,33,new Color(255, 255, 255));
                g2d.drawImage(circularOverlay, 60, 55, null);
                ImageIO.write(frame,"png", new File("D:\\temp\\pic\\output\\output"+i+"-"+i+".png"));
            }
        }



        System.out.println();
    }
    private String makeHuaQFace(Long userId, Long atQQ, String out){
        try {
            BufferedImage bufferedImage = Thumbnails.of(ImageIO.read(new URL(CommonUtil.getAvatarUrl(userId,true))))
                    .size(120, 120)
                    .asBufferedImage();


            BufferedImage bufferedImage1 = Thumbnails.of(ImageIO.read(new URL(CommonUtil.getAvatarUrl(atQQ,true))))
                    .size(112, 112) 
                    .asBufferedImage();


            bufferedImage1 = CommonUtil.rotateImage(bufferedImage1,90,new Color(255, 255, 255));

            GifDecoder gifDecoder = new GifDecoder();
            gifDecoder.read(new FileInputStream(FileUtil.getHuaQFace()));
            int n = gifDecoder.getFrameCount();
            List<BufferedImage> frames = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                BufferedImage frame = gifDecoder.getFrame(i);  // 原gif的帧
                Graphics2D g2d = frame.createGraphics();
                BufferedImage circularOverlay = CommonUtil.makeImageCircular(bufferedImage);
                BufferedImage circularOverlay1 = CommonUtil.makeImageCircular(bufferedImage1);

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
            log.error("生成huaq图片异常",e);
        }
        return null;
    }


    private MatchResult<Pair<String,String>> matches(Message message){
        if(!message.isTextMsg() || !message.isAtMsg() || message.isAtSelf()){
            return MatchResult.unmatched();
        }

        if(message.getText(-1).trim().matches("撅|撅你|撅撅你|超超你|超你|超市你|超死你|操你|操死你|草你|草饲你")){
            return MatchResult.matched(Pair.of(String.valueOf(message.getUserId()), message.getAtQQs().get(0)));
        }

        if(message.getText(-1).trim().matches("撅我|超我|撅撅我|超超我|超市我|超死我|操我|操死我|草我|草饲我")){
            return MatchResult.matched(Pair.of(message.getAtQQs().get(0), String.valueOf(message.getUserId())));
        }

        return MatchResult.unmatched();
    }



}
