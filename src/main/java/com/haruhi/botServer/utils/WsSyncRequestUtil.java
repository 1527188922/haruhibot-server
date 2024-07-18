package com.haruhi.botServer.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.constant.GocqActionEnum;
import com.haruhi.botServer.dto.gocq.request.RequestBox;
import com.haruhi.botServer.dto.gocq.response.DownloadFileResp;
import com.haruhi.botServer.dto.gocq.response.GroupMember;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.dto.gocq.response.SelfInfo;
import com.haruhi.botServer.dto.gocq.response.SyncResponse;
import com.haruhi.botServer.utils.system.SystemInfo;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 向野生bot发送同步websocket消息工具类
 */
@Slf4j
public class WsSyncRequestUtil {
    private WsSyncRequestUtil(){}

    public static int poolSize = SystemInfo.AVAILABLE_PROCESSORS + 1;
    public static long sleep = 1L;
    public static final ExecutorService pool = new ThreadPoolExecutor(poolSize, Integer.MAX_VALUE, 24L, TimeUnit.HOURS, new SynchronousQueue<Runnable>(),new CustomizableThreadFactory("pool-sendSyncMessage-"));

    private static final Map<String,JSONObject> resultMap = new ConcurrentHashMap<>();
    public static void putEchoResult(String key, JSONObject val){
        log.debug("echo message {}\n{}",key,val);
        resultMap.put(key,val);
    }

    /**
     * 给好友点赞
     * @param session
     * @param userId
     * @param times
     * @param timeout
     * @return
     */
    public static SyncResponse sendLike(WebSocketSession session,Long userId,int times,long timeout){
        Map<String, Object> map = new HashMap<>(2);
        map.put("user_id",userId);
        map.put("times",times);
        JSONObject jsonObject = sendSyncRequest(session, GocqActionEnum.SEND_LIKE, map, timeout);
        if (jsonObject != null) {
            return JSONObject.parseObject(jsonObject.toJSONString(), SyncResponse.class);
        }
        return SyncResponse.failed();
    }

    /**
     * 获取消息详情对象
     * @param session
     * @param messageId
     * @param timeout
     * @return
     */
    public static Message getMsg(WebSocketSession session,String messageId,long timeout){
        Map<String, Object> map = new HashMap<>(1);
        map.put("message_id",Long.parseLong(messageId));
        JSONObject jsonObject = sendSyncRequest(session, GocqActionEnum.GET_MSG, map, timeout);
        if (jsonObject != null) {
            return JSONObject.parseObject(jsonObject.getString("data"), Message.class);
        }
        return null;
    }


    public static SelfInfo getLoginInfo(WebSocketSession session,long timeout){
        JSONObject responseStr = sendSyncRequest(session, GocqActionEnum.GET_LOGIN_INGO,null,timeout);
        if (responseStr != null) {
            SelfInfo data = JSONObject.parseObject(responseStr.getString("data"), SelfInfo.class);
            return data;
        }
        return null;
    }

    /**
     * 获取群成员
     * @param groupId 群号
     * @param exclude 需要排除的成员qq号
     * @return
     */
    public static List<GroupMember> getGroupMemberList(WebSocketSession session,Long groupId, List<Long> exclude,long timeout){
        Map<String, Object> params = new HashMap<>(1);
        params.put("group_id",groupId);
        JSONObject jsonObject = sendSyncRequest(session, GocqActionEnum.GET_GROUP_MEMBER_LIST, params, timeout);
        if (jsonObject == null) {
            return null;
        }
        String dataStr = jsonObject.getString("data");
        if(Strings.isBlank(dataStr)){
            return null;
        }
        List<GroupMember> data = JSONArray.parseArray(dataStr, GroupMember.class);
        if(!CollectionUtils.isEmpty(exclude) && !CollectionUtils.isEmpty(data)){
            data.removeIf(next -> exclude.contains(next.getUserId()));
        }
        return data;
    }

    /**
     * 发送私聊文件
     * @param session
     * @param userId
     * @param filePath 该文件必须与gocqhttp在同一主机上
     * @param fileName
     * @param timeout
     * @return
     */
    public static SyncResponse uploadPrivateFile(WebSocketSession session, Long userId, String filePath, String fileName, long timeout){
        Map<String, Object> param = new HashMap<>(3);
        param.put("user_id",userId);
        param.put("file",filePath);
        param.put("name",fileName);
        JSONObject responseStr = sendSyncRequest(session, GocqActionEnum.UPLOAD_PRIVATE_FILE,param,timeout);
        if (responseStr != null) {
            return JSONObject.parseObject(responseStr.toJSONString(), SyncResponse.class);
        }
        return SyncResponse.failed();
    }

    /**
     * 用gocq去下载文件
     * @param session
     * @param url
     * @param threadCount
     * @param httpHeaders
     * @param timeout
     * @return 返回gocq下载到的文件绝对路径
     */
    public static DownloadFileResp downloadFile(WebSocketSession session, String url, int threadCount, HttpHeaders httpHeaders, long timeout){

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
        JSONObject jsonObject = sendSyncRequest(session, GocqActionEnum.DOWNLOAD_FILE, param, timeout);
        if(jsonObject != null){
            return jsonObject.getObject("data",DownloadFileResp.class);
        }
        return null;
    }


    /***
     * 发送同步消息
     * @param session 客户端session
     * @param action 终结点
     * @param params 参数
     * @param timeout 超时 ms
     * @param <T>
     * @return
     */
    public static <T> JSONObject sendSyncRequest(WebSocketSession session, GocqActionEnum action, T params, long timeout){
        RequestBox<T> requestBox = new RequestBox<>();
        if(params != null){
            requestBox.setParams(params);
        }
        requestBox.setAction(action.getAction());
        String echo = CommonUtil.uuid();
        requestBox.setEcho(echo);
        String s = JSONObject.toJSONString(requestBox);
        Server.sendMessage(session,JSONObject.toJSONString(requestBox));
        log.debug("echo: {}",echo);
        FutureTask<JSONObject> futureTask = new FutureTask<>(new WsSyncRequestUtil.Task(echo));
        pool.submit(futureTask);
        try {
            JSONObject res;
            if(timeout <= sleep){
                res = futureTask.get();
            }else{
                res = futureTask.get(timeout, TimeUnit.MILLISECONDS);
            }
            log.debug("echo: {},result: {}",echo,res);
            return res;
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
        return null;
    }

    private static class Task implements Callable<JSONObject> {
        private final String echo;
        Task(String echo){
            if (Strings.isBlank(echo)) {
                throw new IllegalArgumentException("echo is blank");
            }
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
                        Thread.sleep(WsSyncRequestUtil.sleep);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
            return res;
        }
    }

    public static void main(String[] args) {
        FutureTask<JSONObject> futureTask = new FutureTask<>(new WsSyncRequestUtil.Task("echo"));
        pool.submit(futureTask);
//        new Thread(()->{
//            try {
//                Thread.sleep(600);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            final JSONObject jsonObject = new JSONObject();
//            jsonObject.put("ss","11");
//            resultMap.put("echo",jsonObject);
//        }).start();
        JSONObject res;
        try {

            if(1000 <= sleep){
                res = futureTask.get();
            }else{
                res = futureTask.get(1000, TimeUnit.MILLISECONDS);
            }
            System.out.println(res);
        }catch (Exception e){
            System.out.println("异常");
        }finally {
            futureTask.cancel(true);
        }
        

        System.out.println();
        
    }
}
