package com.haruhi.botServer.ws;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.haruhi.botServer.constant.QqClientActionEnum;
import com.haruhi.botServer.constant.event.MessageTypeEnum;
import com.haruhi.botServer.dto.qqclient.*;
import com.haruhi.botServer.utils.CommonUtil;
import com.haruhi.botServer.utils.ThreadPoolUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;


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
     * 发送群聊合并消息
     * 自定义单条消息的uin和name
     * @param groupId
     * @param messages
     */
    public void sendGroupMessage(Long groupId, List<ForwardMsgItem> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            RequestBox<Params> requestBox = new RequestBox<>();
            requestBox.setAction(QqClientActionEnum.SEND_GROUP_FORWARD_MSG.getAction());
            Params params = new Params();
            params.setMessages(messages);
            params.setMessageType(MessageTypeEnum.group.getType());
            params.setGroupId(groupId);
            requestBox.setParams(params);
            sendMessage(JSONObject.toJSONString(requestBox));
        }
    }

    /**
     * 发送群合并消息
     * @param groupId 群号
     * @param uin 合并卡片内的消息发送人qq
     * @param name 合并卡片内的消息发送人名称
     * @param messages 消息集合
     */
    public void sendGroupMessage(Long groupId,Long uin,String name, List<String> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            RequestBox<Params> requestBox = createForwardMessageRequestBox(MessageTypeEnum.group,groupId,uin,name,messages);
            sendMessage(JSONObject.toJSONString(requestBox));
        }
    }

    /**
     * 发送群合并消息
     * 自动获取uin
     * @param groupId 群号
     * @param name 合并卡片内的消息发送人名称
     * @param messages 消息集合
     */
    public void sendGroupMessage(Long groupId,String name, List<String> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            RequestBox<Params> requestBox = createForwardMessageRequestBox(MessageTypeEnum.group,groupId, id,name,messages);
            sendMessage(JSONObject.toJSONString(requestBox));
        }
    }

    /**
     * 发送群同步合并消息
     * @param groupId
     * @param uin
     * @param name
     * @param messages
     * @param timeout
     * @return
     */
    public SyncResponse<String> sendSyncGroupMessage(Long groupId, Long uin, String name, List<String> messages, long timeout){
        Params params = createForwardMessageParams(MessageTypeEnum.group,groupId,uin,name,messages);
        return sendSyncRequest(QqClientActionEnum.SEND_GROUP_FORWARD_MSG, params, timeout, new TypeReference<SyncResponse<String>>(){});
    }


    /**
     * 发送私聊消息
     * @param userId 对方qq
     * @param message 消息
     * @param autoEscape 是否以纯文本发送 true:以纯文本发送，不解析cq码
     */
    public void sendPrivateMessage(Long userId, String message, boolean autoEscape){
        RequestBox<Params> paramsRequestBox = new RequestBox<>();
        paramsRequestBox.setAction(QqClientActionEnum.SEND_PRIVATE_MSG.getAction());

        Params params = new Params();
        params.setMessageType(MessageTypeEnum.privat.getType());
        params.setAutoEscape(autoEscape);
        params.setUserId(userId);
        params.setMessage(message);

        paramsRequestBox.setParams(params);

        sendMessage(JSONObject.toJSONString(paramsRequestBox));
    }

    /**
     * 发送私聊合并消息
     * 自定义单条消息的uin和name
     * @param userId
     * @param messages
     */
    public void sendPrivateMessage(Long userId,List<ForwardMsgItem> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            RequestBox<Params> paramsRequestBox = new RequestBox<>();
            paramsRequestBox.setAction(QqClientActionEnum.SEND_PRIVATE_FORWARD_MSG.getAction());
            Params params = new Params();
            params.setMessages(messages);
            params.setMessageType(MessageTypeEnum.privat.getType());
            params.setUserId(userId);
            paramsRequestBox.setParams(params);
            sendMessage(JSONObject.toJSONString(paramsRequestBox));
        }
    }

    /**
     * 发送私聊合并消息
     * @param userId 对方qq
     * @param uin
     * @param name
     * @param messages
     */
    public void sendPrivateMessage(Long userId,Long uin,String name, List<String> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            RequestBox<Params> paramsRequestBox = createForwardMessageRequestBox(MessageTypeEnum.privat,userId,uin,name,messages);
            sendMessage(JSONObject.toJSONString(paramsRequestBox));
        }
    }

    /**
     * 发送私聊合并消息
     * 自动获取uin
     * @param userId 对方qq
     * @param name
     * @param messages
     */
    public void sendPrivateMessage(Long userId,String name, List<String> messages){
        if (!CollectionUtils.isEmpty(messages)) {
            RequestBox<Params> paramsRequestBox = createForwardMessageRequestBox(MessageTypeEnum.privat,userId, id,name,messages);
            sendMessage(JSONObject.toJSONString(paramsRequestBox));
        }
    }

    /**
     * 发送私聊同步合并消息
     * @param userId
     * @param uin
     * @param name
     * @param messages
     * @param timeout
     * @return
     */
    public SyncResponse<String> sendSyncPrivateMessage(Long userId, Long uin, String name, List<String> messages, long timeout){
        Params params = createForwardMessageParams(MessageTypeEnum.privat,userId,uin,name,messages);
        return sendSyncRequest(QqClientActionEnum.SEND_PRIVATE_FORWARD_MSG, params, timeout,new TypeReference<SyncResponse<String>>() {});
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
     * 合并发送消息
     * 根据messageType发送群还是私聊
     * @param userId
     * @param groupId
     * @param messageType
     * @param uin
     * @param name
     * @param messages
     */
    public void sendMessage(Long userId,Long groupId,String messageType,Long uin,String name, List<String> messages){
        RequestBox<Params> paramsRequestBox = new RequestBox<>();
        Params params = new Params();
        if (MessageTypeEnum.privat.getType().equals(messageType)) {
            paramsRequestBox.setAction(QqClientActionEnum.SEND_PRIVATE_FORWARD_MSG.getAction());
            params.setUserId(userId);
        }else if (MessageTypeEnum.group.getType().equals(messageType)) {
            paramsRequestBox.setAction(QqClientActionEnum.SEND_GROUP_FORWARD_MSG.getAction());
            params.setGroupId(groupId);
        }
        params.setMessageType(messageType);
        List<ForwardMsgItem> forwardMsgs = new ArrayList<>(messages.size());
        for (String s : messages) {
            forwardMsgs.add(createForwardMsgItem(uin,name,s));
        }
        params.setMessages(forwardMsgs);
        paramsRequestBox.setParams(params);

        sendMessage(JSONObject.toJSONString(paramsRequestBox));
    }

    /**
     * 合并同步发送消息
     * 根据messageType发送群还是私聊
     * @param userId
     * @param groupId
     * @param messageType
     * @param uin
     * @param name
     * @param messages
     * @param timeout
     * @return
     */
    public SyncResponse<String> sendSyncMessage(Long userId,Long groupId,String messageType,Long uin,String name, List<String> messages,long timeout){
        Params params = new Params();
        QqClientActionEnum actionEnum = null;
        if (MessageTypeEnum.privat.getType().equals(messageType)) {
            actionEnum = QqClientActionEnum.SEND_PRIVATE_FORWARD_MSG;
            params.setUserId(userId);
        }else if (MessageTypeEnum.group.getType().equals(messageType)) {
            actionEnum = QqClientActionEnum.SEND_GROUP_FORWARD_MSG;
            params.setGroupId(groupId);
        }
        params.setMessageType(messageType);

        List<ForwardMsgItem> forwardMsgs = new ArrayList<>(messages.size());
        for (String s : messages) {
            forwardMsgs.add(createForwardMsgItem(uin,name,s));
        }
        params.setMessages(forwardMsgs);
        return sendSyncRequest(actionEnum, params, timeout,new TypeReference<SyncResponse<String>>() {});
    }

    public void sendMessage(String text){
        try {
            log.debug("发送消息给gocq {}",text);
            session.sendMessage(new TextMessage(text));
        } catch (Exception e) {
            log.error("发送消息发生异常,session:{},消息：{}",session,text,e);
        }
    }

    private RequestBox<Params> createForwardMessageRequestBox(MessageTypeEnum messageType, Long id, Long uin, String name, List<String> messages){
        RequestBox<Params> paramsRequestBox = new RequestBox<>();
        Params params = new Params();
        List<ForwardMsgItem> forwardMsgs = new ArrayList<>(messages.size());
        for (String message : messages) {
            forwardMsgs.add(createForwardMsgItem(uin,name,message));
        }
        params.setMessages(forwardMsgs);
        params.setMessageType(messageType.getType());
        if (MessageTypeEnum.group.getType().equals(messageType.getType())) {
            paramsRequestBox.setAction(QqClientActionEnum.SEND_GROUP_FORWARD_MSG.getAction());
            params.setGroupId(id);
        }else if(MessageTypeEnum.privat.getType().equals(messageType.getType())){
            paramsRequestBox.setAction(QqClientActionEnum.SEND_PRIVATE_FORWARD_MSG.getAction());
            params.setUserId(id);
        }
        paramsRequestBox.setParams(params);
        return paramsRequestBox;
    }

    private Params createForwardMessageParams(MessageTypeEnum messageType, Long id, Long uin, String name, List<String> messages){
        Params params = new Params();
        if (MessageTypeEnum.privat.getType().equals(messageType.getType())) {
            params.setUserId(id);
        }else if(MessageTypeEnum.group.getType().equals(messageType.getType())){
            params.setGroupId(id);
        }
        params.setMessageType(messageType.getType());
        List<ForwardMsgItem> forwardMsgs = new ArrayList<>(messages.size());
        for (String s : messages) {
            forwardMsgs.add(createForwardMsgItem(uin,name,s));
        }
        params.setMessages(forwardMsgs);
        return params;
    }

    private ForwardMsgItem createForwardMsgItem(Long uin, String name, String context){
        ForwardMsgItem item = new ForwardMsgItem();
        ForwardMsgItem.Data data = new ForwardMsgItem.Data();
        data.setUin(uin);
        data.setName(name);
        data.setContent(context);
        item.setData(data);
        return item;
    }




    private static long sleep = 1L;

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
        if(params != null){
            requestBox.setParams(params);
        }
        requestBox.setAction(action.getAction());
        String echo = CommonUtil.uuid();
        requestBox.setEcho(echo);
        sendMessage(JSONObject.toJSONString(requestBox));
        log.debug("echo: {}",echo);
        FutureTask<JSONObject> futureTask = new FutureTask<>(new Bot.Task(echo, resultMap));
        ThreadPoolUtil.getSharePool().submit(futureTask);
        try {
            JSONObject res;
            if(timeout <= sleep){
                res = futureTask.get();
            }else{
                res = futureTask.get(timeout, TimeUnit.MILLISECONDS);
            }
            log.debug("echo: {},result: {}",echo,res);
            SyncResponse<R> resJavaObject = res.toJavaObject(typeReference);
            resJavaObject.setRaw(res);
            return resJavaObject;
        }catch (InterruptedException e){
            log.error("发送同步消息线程中断异常,echo:{}",echo,e);
        } catch (ExecutionException e) {
            log.error("发送同步消息执行异常,echo:{}",echo,e);
        } catch (TimeoutException e) {
            log.error("发送同步消息超时,echo:{}",echo,e);
        } catch (Exception e) {
            log.error("发送同步消息异常,echo:{}",echo,e);
        }finally {
            futureTask.cancel(true);
            resultMap.remove(echo);
        }
        return SyncResponse.failed();
    }

    public <R> SyncResponse<R> sendSyncRequest(String text,String echo, long timeout, TypeReference<SyncResponse<R>> typeReference)
            throws ExecutionException, InterruptedException, TimeoutException {
        sendMessage(text);
        FutureTask<JSONObject> futureTask = new FutureTask<>(new Bot.Task(echo, resultMap));
        ThreadPoolUtil.getSharePool().submit(futureTask);
        JSONObject res;
        if(timeout <= sleep){
            res = futureTask.get();
        }else{
            res = futureTask.get(timeout, TimeUnit.MILLISECONDS);
        }
        SyncResponse<R> resJavaObject = res.toJavaObject(typeReference);
        resJavaObject.setRaw(res);
        return resJavaObject;
    }

    private static class Task implements Callable<JSONObject> {
        private final String echo;
        private final Map<String,JSONObject> resultMap;

        Task(String echo,Map<String,JSONObject> resultMap){
            if (Strings.isBlank(echo)) {
                throw new IllegalArgumentException("echo is blank");
            }
            this.resultMap = resultMap;
            this.echo = echo;
        }
        @Override
        public JSONObject call() {
            JSONObject res = null;
            while (!Thread.currentThread().isInterrupted()){
                res = resultMap.get(echo);
                if(res != null){
                    break;
                }else {
                    try {
                        Thread.sleep(Bot.sleep);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
            return res;
        }
    }
}
