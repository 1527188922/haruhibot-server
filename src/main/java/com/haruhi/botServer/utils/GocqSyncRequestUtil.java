package com.haruhi.botServer.utils;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botServer.constant.GocqActionEnum;
import com.haruhi.botServer.dto.gocq.request.RequestBox;
import com.haruhi.botServer.dto.gocq.response.DownloadFileResp;
import com.haruhi.botServer.dto.gocq.response.Message;
import com.haruhi.botServer.dto.gocq.response.SelfInfo;
import com.haruhi.botServer.dto.gocq.response.SyncResponse;
import com.haruhi.botServer.utils.system.SystemInfo;
import com.haruhi.botServer.ws.Server;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
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
 * 用于请求gocq的数据
 * 并不用来做发送消息
 */
@Slf4j
public class GocqSyncRequestUtil {
    private GocqSyncRequestUtil(){}

    public static int poolSize = SystemInfo.AVAILABLE_PROCESSORS + 1;
    public static long sleep = 1L;
    public static final ExecutorService pool = new ThreadPoolExecutor(poolSize, Integer.MAX_VALUE, 24L, TimeUnit.HOURS, new SynchronousQueue<Runnable>(),new CustomizableThreadFactory("pool-sendSyncMessage-"));

    private static Map<String,JSONObject> resultMap = new ConcurrentHashMap<>();
    public static void putEchoResult(String key, JSONObject val){
        resultMap.put(key,val);
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
        map.put("message_id",messageId);
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
            SyncResponse response = JSONObject.parseObject(responseStr.toJSONString(), SyncResponse.class);
            return response;
        }
        return null;
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
     * @param timeout 超时
     * @param <T>
     * @return
     */
    public static <T> JSONObject sendSyncRequest(WebSocketSession session, GocqActionEnum action, T params, long timeout){
        RequestBox<T> requestBox = new RequestBox<>();
        if(params != null){
            requestBox.setParams(params);
        }
        requestBox.setAction(action.getAction());
        String echo = Thread.currentThread().getName() + "_" + session.getId() + "_" + action.getAction() + "_" + CommonUtil.uuid();
        requestBox.setEcho(echo);
        try {
            Server.sendMessage(session,JSONObject.toJSONString(requestBox));
            log.info("echo: {}",echo);
            FutureTask<JSONObject> futureTask = new FutureTask<>(new GocqSyncRequestUtil.Task(echo));
            pool.submit(futureTask);
            JSONObject res;
            if(timeout <= sleep){
                res = futureTask.get();
            }else{
                res = futureTask.get(timeout, TimeUnit.MILLISECONDS);
            }
            log.info("echo: {},result: {}",echo,res);
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
            resultMap.remove(echo);
        }
        return null;
    }

    private static class Task implements Callable<JSONObject> {
        private String echo;
        Task(String echo){
            if (Strings.isBlank(echo)) {
                throw new IllegalArgumentException("echo is blank");
            }
            this.echo = echo;
        }
        @Override
        public JSONObject call() throws Exception{
            JSONObject res = null;
            while (true){
                res = resultMap.get(echo);
                if(res == null){
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) { }
                }else {
                    break;
                }
            }
            return res;
        }
    }
}
