package com.haruhi.botServer.handlers.message;

import cn.hutool.core.img.gif.AnimatedGifEncoder;
import cn.hutool.core.img.gif.GifDecoder;
import com.haruhi.botServer.config.webResource.AbstractWebResourceConfig;
import com.haruhi.botServer.constant.CqCodeTypeEnum;
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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class HuaQHandler implements IGroupMessageEvent {


    @Override
    public int weight() {
        return 55;
    }

    @Override
    public String funName() {
        return "撅";
    }

    @Autowired
    private AbstractWebResourceConfig abstractPathConfig;


    @Override
    public boolean onGroup(WebSocketSession session, Message message, String command) {
        MatchResult<Pair<String, String>> pairMatchResult = matches(message);
        if (!pairMatchResult.isMatched()) {
            return false;
        }

        Pair<String, String> data = pairMatchResult.getData();

        ThreadPoolUtil.getHandleCommandPool().execute(()->{
            try {
                String fileName = "huaq_" + data.getKey() + "_" + data.getValue() + ".gif";
                String s = mackHuaQFace(Long.valueOf(data.getKey()), Long.valueOf(data.getValue()), fileName);

                KQCodeUtils instance = KQCodeUtils.getInstance();
                String imageUrl = abstractPathConfig.webFacePath() + "/" + fileName + "?t=" + System.currentTimeMillis();
                log.info("huaq表情图片地址：{}",s);
                String imageCq = instance.toCq(CqCodeTypeEnum.image.getType(), "file=" + imageUrl);
                Server.sendGroupMessage(session,message.getGroupId(),imageCq,false);

            }catch (Exception e){
                log.error("发送huaQ表情异常",e);
            }
        });
        return true;
    }


    public static void main(String[] args) throws IOException {
        BufferedImage read = ImageIO.read(new URL(CommonUtil.getAvatarUrl(1527188922L, true)));
        System.out.println();
    }
    private String mackHuaQFace(Long userId, Long atQQ, String fileName){
        try {


            BufferedImage bufferedImage = Thumbnails.of(ImageIO.read(new URL(CommonUtil.getAvatarUrl(userId,true))))
                    .size(120, 120) // 设置目标图片的宽度和高度为200x200像素
                    .asBufferedImage();


            BufferedImage bufferedImage1 = Thumbnails.of(ImageIO.read(new URL(CommonUtil.getAvatarUrl(atQQ,true))))
                    .size(112, 112) // 设置目标图片的宽度和高度为200x200像素
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

            String out = FileUtil.getFaceDir() + File.separator + fileName;
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
