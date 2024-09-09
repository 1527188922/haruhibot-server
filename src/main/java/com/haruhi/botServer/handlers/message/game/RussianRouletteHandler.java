package com.haruhi.botServer.handlers.message.game;

import com.haruhi.botServer.cache.CacheMap;
import com.haruhi.botServer.constant.CqCodeTypeEnum;
import com.haruhi.botServer.constant.HandlerWeightEnum;
import com.haruhi.botServer.constant.RegexEnum;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.event.message.IGroupMessageEvent;
import com.haruhi.botServer.utils.WsSyncRequestUtil;
import com.haruhi.botServer.ws.Server;
import com.simplerobot.modules.utils.KQCodeUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 俄罗斯轮盘
 * 过期时间一分钟 每个操作执行后刷新过期时间
 */
@Component
@Slf4j
public class RussianRouletteHandler implements IGroupMessageEvent {
    @Override
    public int weight() {
        return HandlerWeightEnum.W_370.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_370.getName();
    }

    private static final int maxPlayers = 2;
    private static final int maxBullets = 6; //子弹下标 0-5

    private final CacheMap<String,RussianRouletteGame> cache = new CacheMap<>(10, TimeUnit.MINUTES,999);
    private String cacheKey(Message message){
        return message.getGroupId() + "-" + message.getSelfId();
    }
    
    
    @Override
    public boolean onGroup(final WebSocketSession session,final Message message) {
        KQCodeUtils instance = KQCodeUtils.getInstance();
        // 对发起游戏的判断
        if(RegexEnum.GAME_RUSSIAN_ROULETTE.getValue().equals(message.getRawMessage())){
            final String cacheKey = cacheKey(message);
            RussianRouletteGame game = cache.get(cacheKey);
            if (game != null) {
                int size = game.getPlayers().size();
                if(size < maxPlayers){
                    Server.sendGroupMessage(session,message.getGroupId(),"一场游戏正在招募，你可以回复他的发起消息为:'参加' 即可参加",true);
                }else if(size == maxPlayers){
                    Server.sendGroupMessage(session,message.getGroupId(),"一场游戏正在进行，请稍后",true);
                }else{
                    Server.sendGroupMessage(session,message.getGroupId(),"当前群已发起一场游戏",true);
                }
                return true;
            }
            // 发起成功
            RussianRouletteGame russianRouletteGame = new RussianRouletteGame(message.getUserId());
            russianRouletteGame.addPlayer(message.getGroupId(),message.getUserId());
            cache.put(cacheKey,russianRouletteGame);
            String atSender = instance.toCq(CqCodeTypeEnum.at.getType(), "qq=" + message.getUserId());
            Server.sendGroupMessage(session,message.getGroupId(), 
                    MessageFormat.format("{0} 发起了俄罗斯轮盘\n发送消息为：参加，即可参加游戏\n玩家轮流发送：`扣动扳机`进行游戏\n弹夹最大容量"
                                    + maxBullets
                                    + "颗，随机填充1颗子弹\n最多{1}人，还缺人{2}人",
                            atSender,maxPlayers,(maxPlayers - russianRouletteGame.getPlayers().size())),
                    false);
            return true;
        }
        
        // 对参加游戏的判断
        String s = message.getText(-1);
        if(StringUtils.isNotBlank(s) && s.trim().matches("参加|参与|加入")){
//            if (!message.isReplyMsg()) {
//                return false;
//            }
            String cacheKey = cacheKey(message);
            RussianRouletteGame game = cache.get(cacheKey);
            if(game == null){
                return false;
            }

            // 判断回复的消息是否为发起消息
//            String messageId = message.getReplyMsgIds().get(0);
//            final String messageId1 = game.getMessageId();
//            if (Strings.isBlank(messageId) || !messageId.equals(messageId1)) {
//                return false;
//            }
            // 判断不是自己回复自己的发起消息
//            Message msg = WsSyncRequestUtil.getMsg(session,messageId,2L * 1000L);
//            if(msg == null || msg.getSender() == null || msg.getSender().getUserId() == null 
//                    || msg.getSender().getUserId().equals(message.getUserId())){
//                return false;
//            }
            if(game.getUserId().equals(message.getUserId())){
                return false;
            }
            
            
            if(game.getPlayers().size() == maxPlayers){
                Server.sendGroupMessage(session,message.getGroupId(),"当前群已开始游戏，无法再参加",true);
                return true;
            }
            // 参加成功
            game.addPlayer(message.getGroupId(),message.getUserId());
            RussianRouletteGame.Player currentPlayer = game.getCurrentPlayer();
            cache.refreshKey(cacheKey);
            String atSender = instance.toCq(CqCodeTypeEnum.at.getType(), "qq=" + message.getSender().getUserId());
            String s1 = instance.toCq(CqCodeTypeEnum.at.getType(), "qq=" + currentPlayer.getUserId());
            Server.sendGroupMessage(session,message.getGroupId(),MessageFormat.format("{0} 参加了游戏，对局开始\n请{1} 先手",atSender,
                    s1),false);
            return true;
        }
        
        if(message.isTextMsg() 
                && message.getText(0).trim()
                .matches("扣动扳机|开枪|开火|pull the trigger!|pull the trigger|PULL THE TRIGGER!|PULL THE TRIGGER")){
            String cacheKey = cacheKey(message);
            RussianRouletteGame game = cache.get(cacheKey);
            if(game == null){
                return false;
            }
            final List<RussianRouletteGame.Player> players = game.getPlayers();
            if(CollectionUtils.isEmpty(players) || players.size() < maxPlayers){
                return false;
            }
            boolean f = false;
            for (RussianRouletteGame.Player player : players) {
                if(player.getGroupId().equals(message.getGroupId()) && player.getUserId().equals(message.getUserId())){
                    f = true;
                    break;
                }
            }
            if(!f){
                return false;
            }
            final RussianRouletteGame.Player currentPlayer = game.getCurrentPlayer();
            if(!currentPlayer.getGroupId().equals(message.getGroupId())){
                return false;
            }
            if(!currentPlayer.getUserId().equals(message.getUserId())){
                Server.sendGroupMessage(session,message.getGroupId(),"当前不是你的回合",true);
                return true;
            }
            RussianRouletteGame.Player player = game.nextPlayer();
            String atSender = instance.toCq(CqCodeTypeEnum.at.getType(), "qq=" + message.getSender().getUserId());
            String s1 = instance.toCq(CqCodeTypeEnum.at.getType(), "qq=" + player.getUserId());
            
            if (game.isHit()) {
                cache.remove(cacheKey);
                Server.sendGroupMessage(session,message.getGroupId(),MessageFormat.format("当前第{0}次扣动扳机\n嘭！{1} 被击中了！\n{2} 获得胜利！\n本场游戏结束！",
                        game.getCurrentPosition() + 1,atSender,s1),false);
                
            }else{
                int i = game.nextPosition();
//                game.randomBulletPosition();// 每次扣完，重新摇一下
                cache.refreshKey(cacheKey);
                Server.sendGroupMessage(session,message.getGroupId(),MessageFormat.format("当前第{0}次扣动扳机\n{1} 未被击中\n接下来请{2} 扣动扳机！",
                        i,atSender,s1),false);
            }
            return true;
        }
        
        return false;
    }


    @Data
    public static class RussianRouletteGame{
        private static final Random RANDOM = new Random();


        // 发起消息的messageid
        private String messageId;
        // 发起者的userid
        private Long userId;
        // 玩家集合
        private List<Player> players;
        // 当前回合的玩家下标
        private int currentPlayerIndex;
        // 子弹位置下标
        private int bulletPosition;
        // 已扣动扳机次数 从0开始逐渐加1
        private int currentPosition;
        // 弹夹最大容量
        private int bullets;
        
        public RussianRouletteGame(String messageId,Long userId){
            this.messageId = messageId;
            this.userId = userId;
            currentPosition = 0;
            bullets = maxBullets;
            players = new ArrayList<>(maxPlayers);
            // 随机一位玩家先手
            currentPlayerIndex = RANDOM.nextInt(maxPlayers);
            // 子弹下标随机
            randomBulletPosition();
        }

        public RussianRouletteGame(Long userId){
            this(null,userId);
        }

        public int randomBulletPosition(){
            bulletPosition = RANDOM.nextInt(bullets);
            log.debug("子弹位置已更新 {}",bulletPosition);
            return bulletPosition;
        }

        public boolean isHit(){
//            return (currentPosition % bullets) == bulletPosition;
            return currentPosition == bulletPosition;
        }
        
        public Player nextPlayer(){
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            return getCurrentPlayer();
        }
        public int nextPosition(){
            if(currentPosition >= bullets){
                return currentPosition;
            }
            currentPosition += 1;
            return currentPosition;
        }

//        public int nextPosition(){
//            currentPosition += 1;
//            return currentPosition;
//        }
        
        public void addPlayer(Long groupId,Long userId){
            if (players.size() >= maxPlayers) {
                return;
            }
            players.add(new Player(groupId,userId));
        }
        
        public Player getCurrentPlayer(){
            return players.get(currentPlayerIndex);
        }
        
        @Data
        @AllArgsConstructor
        public static class Player{
            private Long groupId;
            private Long userId;
        }
    }
    
    
}
