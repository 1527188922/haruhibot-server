package com.haruhi.botServer.ws;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.constant.QqClientActionEnum;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.qqclient.*;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


/**
 * go-cqhttp文档：https://docs.go-cqhttp.org/api
 * napcat文档：https://napneko.github.io/develop/api
 *
 */
@Slf4j
public class Bot {

    @Setter
    @Getter
    private Long id;//机器人qq号
    @Setter
    private WebSocketSession session;
    @Getter
    private SelfInfo selfInfo;

    public Bot(Long id, WebSocketSession session) {
        this.id = id;
        this.session = session;
    }

    public void close() throws IOException {
        session.close();
    }

    public String getSessionId(){
        return session != null ? session.getId() : null;
    }

    public String getBotName(){
        if (selfInfo != null) {
            return StringUtils.isNotBlank(selfInfo.getNickname()) ? selfInfo.getNickname() : BotConfig.DEFAULT_NAME;
        }
        return BotConfig.DEFAULT_NAME;
    }

    public void refreshSelfInfo(){
        SyncResponse<SelfInfo> response = getLoginInfo(10 * 1000);
        if (response.isSuccess() && response.getData() != null) {
            selfInfo = response.getData();
        }
    }

    /**
     * 发送群消息
     * @param groupId 群号
     * @param message 消息
     * @param autoEscape 是否以纯文本发送 true:以纯文本发送，不解析cq码 false:解析cq码
     */
    public void sendGroupMessage(Long groupId, String message, boolean autoEscape){
        RequestBox<Params> requestBox = new RequestBox<>();
        requestBox.setAction(QqClientActionEnum.SEND_GROUP_MSG.getAction());

        Params params = new Params();
        params.setMessageType(MessageTypeEnum.group.getType());
        params.setAutoEscape(autoEscape);
        params.setGroupId(groupId);
        params.setMessage(message);

        requestBox.setParams(params);
        sendMessage(JSONObject.toJSONString(requestBox));
    }

    /**
     * 发送消息
     * 根据messageType来发送群还是私聊
     * @param userId 对方qq
     * @param groupId 群号
     * @param messageType private:发送私聊（userId生效）   group:发送群聊（groupId生效）
     * @param message 消息
     * @param autoEscape 是否以纯文本发送 true:以纯文本发送，不解析cq码
     */
    public void sendMessage(Long userId,Long groupId,String messageType, String message, boolean autoEscape){
        RequestBox<Params> paramsRequestBox = new RequestBox<>();
        paramsRequestBox.setAction(QqClientActionEnum.SEND_MSG.getAction());

        Params params = new Params();
        params.setMessageType(messageType);
        params.setAutoEscape(autoEscape);
        params.setUserId(userId);
        params.setGroupId(groupId);
        params.setMessage(message);

        paramsRequestBox.setParams(params);

        sendMessage(JSONObject.toJSONString(paramsRequestBox));
    }


    /**
     * 发送戳一戳
     * @param userId 必填
     * @param groupId 选填 不填时则：私发戳一戳
     */
    public void sendPoke(Long userId, Long groupId){
        RequestBox<PokeParams> pokeRequest = new RequestBox<>();
        pokeRequest.setAction(QqClientActionEnum.SEND_POKE.getAction());

        PokeParams params = new PokeParams();
        params.setUserId(userId);
        params.setTargetId(userId);
        params.setGroupId(groupId);
        pokeRequest.setParams(params);

        sendMessage(JSONObject.toJSONString(pokeRequest));
    }

    public void sendMessage(Long userId,Long groupId,String messageType, List<MessageHolder> message){
        RequestBox<Params<List<MessageHolder>>> paramsRequestBox = new RequestBox<>();
        paramsRequestBox.setAction(QqClientActionEnum.SEND_MSG.getAction());

        Params<List<MessageHolder>> params = new Params<>();
        params.setMessageType(messageType);
        params.setUserId(userId);
        params.setGroupId(groupId);
        params.setMessage(message);

        paramsRequestBox.setParams(params);

        sendMessage(JSONObject.toJSONString(paramsRequestBox));
    }

    public SyncResponse<SendMsgResp> sendSyncMessage(Long userId, Long groupId, String messageType, List<MessageHolder> message, long timeout){
        Params<List<MessageHolder>> params = new Params<>();
        params.setMessageType(messageType);
        params.setUserId(userId);
        params.setGroupId(groupId);
        params.setMessage(message);
        return sendSyncRequest(QqClientActionEnum.SEND_MSG, params, timeout, new TypeReference<SyncResponse<SendMsgResp>>(){});
    }

    /**
     * 发送合并转消息
     * 异步
     * @param userId
     * @param groupId
     * @param messageType
     * @param messages
     */
    public void sendForwardMessage(Long userId,Long groupId,String messageType, List<ForwardMsgItem> messages){
        RequestBox<ForwardMsgParams> forwardRequest = new RequestBox<>();
        forwardRequest.setAction(QqClientActionEnum.SEND_FORWARD_MSG.getAction());

        ForwardMsgParams params = new ForwardMsgParams();
        params.setUserId(userId);
        params.setGroupId(groupId);
        params.setMessageType(messageType);
        params.setMessages(messages);
        forwardRequest.setParams(params);

        sendMessage(JSONObject.toJSONString(forwardRequest));
    }

    /**
     * 发送合并转消息 增强
     * 异步
     * @param userId
     * @param groupId
     * @param messageType
     * @param messages
     * @param source 消息卡片顶部 和 打开消息框的标题 文案
     * @param news 消息卡片中间文案
     * @param summary 消息卡片底部文案
     * @param prompt 暂未发现用处 napcat文档解释：外显
     */
    public void sendForwardMessageEnhance(Long userId,Long groupId,String messageType, List<ForwardMsgItem> messages,
                                          String source,List<String> news,String summary,String prompt){
        RequestBox<ForwardMsgParams> forwardRequest = new RequestBox<>();
        forwardRequest.setAction(QqClientActionEnum.SEND_FORWARD_MSG.getAction());

        ForwardMsgParams params = new ForwardMsgParams();
        params.setUserId(userId);
        params.setGroupId(groupId);
        params.setMessageType(messageType);
        params.setMessages(messages);

        params.setSource(source);
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(news)) {
            params.setNews(
                    news.stream().map(ForwardMsgParams.News::new).collect(Collectors.toList())
            );
        }
        params.setSummary(summary);
        params.setPrompt(prompt);
        forwardRequest.setParams(params);
        sendMessage(JSONObject.toJSONString(forwardRequest));
    }


    public void sendMessage(String text){
        try {
            log.debug("发送消息给gocq {}",text);
            session.sendMessage(new TextMessage(text));
        } catch (Exception e) {
            log.error("发送消息发生异常,session:{},消息：{}",session,text,e);
        }
    }

    private final static long GET_SYNC_RESP_PERIOD = 1L;

    private final Map<String,JSONObject> resultMap = new ConcurrentHashMap<>();
    public void putEchoResult(String key, JSONObject val){
        log.debug("echo message {}\n{}",key,val);
        resultMap.put(key,val);
    }

    /**
     * 给好友点赞
     * @param userId
     * @param times
     * @param timeout
     * @return
     */
    public SyncResponse<String> sendLike(Long userId,int times,long timeout){
        Map<String, Object> map = new HashMap<>(2);
        map.put("user_id",userId);
        map.put("times",times);
        return sendSyncRequest(QqClientActionEnum.SEND_LIKE, map, timeout,new TypeReference<SyncResponse<String>>() {});
    }

    /**
     * 获取消息详情对象
     * @param messageId
     * @param timeout
     * @return
     */
    public SyncResponse<Message> getMsg(String messageId, long timeout){
        Map<String, Object> map = new HashMap<>(1);
        map.put("message_id",Long.parseLong(messageId));
        return sendSyncRequest(QqClientActionEnum.GET_MSG, map, timeout,new TypeReference<SyncResponse<Message>>() {});
    }


    public SyncResponse<SelfInfo> getLoginInfo(long timeout){
        return sendSyncRequest(QqClientActionEnum.GET_LOGIN_INGO, null, timeout, new TypeReference<SyncResponse<SelfInfo>>() {});
    }


    /**
     * 获取好友列表
     * @param noCache
     * @param timeout
     * @return
     */
    public SyncResponse<List<FriendInfo>> getFriendList(boolean noCache,long timeout){
        Map<String, Object> params = new HashMap<>(1);
        params.put("no_cache",noCache);
        return sendSyncRequest(QqClientActionEnum.GET_FRIEND_LIST, params, timeout, new TypeReference<SyncResponse<List<FriendInfo>>>() {
        });
    }

    /**
     * 获取群成员
     * @param groupId 群号
     * @return
     */
    public SyncResponse<List<GroupMember>> getGroupMemberList(Long groupId, long timeout){
        Map<String, Object> params = new HashMap<>(1);
        params.put("group_id",groupId);
        return sendSyncRequest(QqClientActionEnum.GET_GROUP_MEMBER_LIST, params, timeout, new TypeReference<SyncResponse<List<GroupMember>>>() {
        });
    }

    public SyncResponse<List<GroupInfo>> getGroupList(boolean noCache,long timeout){
        Map<String, Object> params = new HashMap<>(1);
        params.put("no_cache",noCache);
        return sendSyncRequest(QqClientActionEnum.GET_GROUP_LIST, params, timeout, new TypeReference<SyncResponse<List<GroupInfo>>>() {
        });
    }
    public SyncResponse<GroupInfo> getGroupInfo(Long groupId, boolean noCache,long timeout){
        Map<String, Object> params = new HashMap<>(2);
        params.put("group_id",groupId);
        params.put("no_cache",noCache);
        return sendSyncRequest(QqClientActionEnum.GET_GROUP_INFO, params, timeout, new TypeReference<SyncResponse<GroupInfo>>() {
        });
    }

    /**
     * 发送私聊文件
     * @param userId
     * @param filePath 该文件必须与gocqhttp在同一主机上
     * @param fileName
     * @param timeout
     * @return
     */
    public SyncResponse<String> uploadPrivateFile(Long userId, String filePath, String fileName, long timeout){
        Map<String, Object> param = new HashMap<>(3);
        param.put("user_id",userId);
        param.put("file",filePath);
        param.put("name",fileName);
        return sendSyncRequest(QqClientActionEnum.UPLOAD_PRIVATE_FILE, param, timeout, new TypeReference<SyncResponse<String>>() {});
    }

    /**
     * 上传群文件
     * @param groupId
     * @param filePath 文件绝对路径
     * @param fileName
     * @param folderId 父级目录id
     * @param timeout
     * @return
     */
    public SyncResponse<String> uploadGroupFile(Long groupId, String filePath, String fileName,String folderId, long timeout){
        Map<String, Object> param = new HashMap<>(3);
        param.put("group_id",groupId);
        param.put("file",filePath);
        param.put("name",fileName);
        param.put("folder",folderId);
        return sendSyncRequest(QqClientActionEnum.UPLOAD_GROUP_FILE, param, timeout, new TypeReference<SyncResponse<String>>() {});
    }

    /**
     * 用gocq去下载文件
     * @param url
     * @param threadCount
     * @param httpHeaders
     * @param timeout
     * @return 返回gocq下载到的文件绝对路径
     */
    public SyncResponse<DownloadFileResp> downloadFile(String url, int threadCount, HttpHeaders httpHeaders, long timeout){

        Map<String, Object> param = new HashMap<>(3);
        param.put("url",url);
        param.put("thread_count",threadCount);

        if(httpHeaders != null && !httpHeaders.isEmpty()){
            List<String> headStrs = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : httpHeaders.entrySet()) {
                StringBuilder item = new StringBuilder(entry.getKey() + "=");
                for (String s : entry.getValue()) {
                    item.append(s).append(";");
                }
                headStrs.add(item.toString());
            }
            param.put("headers",JSONObject.toJSONString(headStrs));

        }
        return sendSyncRequest(QqClientActionEnum.DOWNLOAD_FILE, param, timeout, new TypeReference<SyncResponse<DownloadFileResp>>() { });
    }


    /***
     * 发送同步消息
     * @param action 终结点
     * @param params 参数
     * @param timeout 超时 ms
     * @param <T>
     * @return
     */
    public <T,R> SyncResponse<R> sendSyncRequest(QqClientActionEnum action, T params, long timeout, TypeReference<SyncResponse<R>> typeReference){
        RequestBox<T> requestBox = new RequestBox<>();
        requestBox.setParams(params);
        requestBox.setAction(action.getAction());
        String echo = CommonUtil.uuid();
        requestBox.setEcho(echo);

        try {
            return sendSyncRequest(JSONObject.toJSONString(requestBox), echo, timeout, typeReference);
        }catch (InterruptedException e){
            log.error("发送同步消息线程中断异常,echo:{}",echo,e);
        } catch (ExecutionException e) {
            log.error("发送同步消息执行异常,echo:{}",echo,e);
        } catch (TimeoutException e) {
            log.error("发送同步消息超时,echo:{}",echo,e);
        } catch (Exception e) {
            log.error("发送同步消息异常,echo:{}",echo,e);
        }
        return SyncResponse.failed();
    }

    public <R> SyncResponse<R> sendSyncRequest(String text,String echo, long timeout, TypeReference<SyncResponse<R>> typeReference)
            throws ExecutionException, InterruptedException, TimeoutException {
        sendMessage(text);

        log.debug("echo: {}",echo);
        FutureTask<JSONObject> futureTask = new FutureTask<>(new GetSyncRespTask(echo, resultMap));
        ThreadPoolUtil.getSharePool().submit(futureTask);
        try {
            JSONObject res;
            if(timeout <= GET_SYNC_RESP_PERIOD){
                res = futureTask.get();
            }else{
                res = futureTask.get(timeout, TimeUnit.MILLISECONDS);
            }
            SyncResponse<R> resJavaObject = res.toJavaObject(typeReference);
            resJavaObject.setRaw(res);
            log.debug("echo: {},result: {}",echo,res);
            return resJavaObject;
        }finally {
            futureTask.cancel(true);
            resultMap.remove(echo);
        }
    }

    @AllArgsConstructor
    private static class GetSyncRespTask implements Callable<JSONObject> {
        private final String echo;
        private final Map<String,JSONObject> resultMap;

        @Override
        public JSONObject call() {
            JSONObject res = null;
            while (!Thread.currentThread().isInterrupted()){
                res = resultMap.get(echo);
                if(res != null){
                    break;
                }else {
                    try {
                        Thread.sleep(Bot.GET_SYNC_RESP_PERIOD);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
            return res;
        }
    }
}
