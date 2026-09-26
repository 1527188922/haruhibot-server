package com.haruhi.botserver.administration.service;

import com.haruhi.botserver.features.reply.service.CustomReplySqliteService;
import com.haruhi.botserver.features.reply.service.PokeReplySqliteService;
import com.haruhi.botserver.features.wordstrip.service.WordStripSqliteService;

import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.configuration.service.ConfigHub;

import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botserver.bootstrap.SysConstants;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import com.haruhi.botserver.shared.constant.RootTypeEnum;
import com.haruhi.botserver.infrastructure.persistence.persistence.entity.IndexInfoSqlite;
import com.haruhi.botserver.infrastructure.persistence.persistence.entity.SqliteSchema;
import com.haruhi.botserver.infrastructure.persistence.persistence.entity.TableInfoSqlite;
import com.haruhi.botserver.shared.error.BusinessException;
import com.haruhi.botserver.features.meme.handler.HuaQHandler;
import com.haruhi.botserver.features.reply.handler.ScoldMeHandler;
import com.haruhi.botserver.features.meme.handler.JumpHandler;
import com.haruhi.botserver.infrastructure.persistence.persistence.mapper.SqliteDatabaseInitMapper;
import com.haruhi.botserver.infrastructure.persistence.persistence.mapper.SqliteSchemaMapper;
import com.haruhi.botserver.shared.util.CMDUtil;
import com.haruhi.botserver.shared.util.FileUtil;
import com.haruhi.botserver.administration.model.BotWebSocketInfo;
import com.haruhi.botserver.administration.model.FileNode;
import com.haruhi.botserver.administration.model.DatabaseInfoNode;
import com.haruhi.botserver.bot.session.BotContainer;
import com.haruhi.botserver.integration.onebot.websocket.BotServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
public class SystemService {

    static {
        log.info("当前系统环境：{} / {}", System.getProperty("os.name"), System.getProperty("os.arch"));
    }

    @Autowired
    private PokeReplySqliteService pokeReplyService;
    @Autowired
    private CustomReplySqliteService customReplySqliteService;
    @Autowired
    private WordStripSqliteService wordStripService;
    @Autowired
    private SqliteDatabaseInitMapper sqliteDatabaseInitMapper;
    @Autowired
    private SqliteSchemaMapper sqliteSchemaMapper;
    @Autowired
    private ConfigHub configHub;

    private BotServer botServer;

    private BotServer getBotServer() {
        if (botServer == null) {
            botServer = SpringUtil.getBean(BotServer.class);
        }
        return botServer;
    }

    public void writeStopScript(){
        String s = null;
        String scriptName = null;
        String pidStr = String.valueOf(RuntimeUtil.getPid());
        if (SystemUtils.IS_OS_WINDOWS){
            s = MessageFormat.format("taskkill /pid {0} -t -f",pidStr);
            scriptName = FileUtil.FILE_NAME_KILL_SCRIPT_BAT;
        }else {
            s = MessageFormat.format("kill -9 {0}",pidStr);
            scriptName = FileUtil.FILE_NAME_KILL_SCRIPT_SH;
        }
        File file = new File(FileUtil.getAppDir() + File.separator + scriptName);
        try {
            FileUtil.writeText(file,s);
        } catch (IOException e) {
            log.error("生成停止脚本异常",e);
        }
        log.info("生成kill脚本完成:{}",file.getAbsolutePath());
    }

    /**
     * 加载缓存
     * <p>
     * 配置部分由 {@link ConfigHub} 负责，这里只处理需要"从数据库重建内存缓存"的数据。
     * <p>
     * 注意启动（mode=1）走的是<b>静默重载</b>：快照在 Spring 启动之前就已按文件加载好，各组件也是按最新配置初始化的，
     * 此时再强制通知一遍订阅者只会刷一堆 {@code 0 -> 0} 的变更日志（见 {@link ConfigHub#loadAll()}）。
     * 接口/命令（mode=2/3）是用户主动要求刷新，走 {@link ConfigHub#refreshAll()}：除了重读文件，
     * 还会强制通知所有订阅者，把运行期状态和文件对齐。
     *
     * @param mode 1系统启动刷新 2接口刷新 3bot命令刷新
     */
    public synchronized void loadCache(int mode){
       try {
           if (mode == 1) {
               configHub.loadAll();
           } else {
               configHub.refreshAll();
           }
           pokeReplyService.loadPokeReply();
           customReplySqliteService.loadToCache();
           wordStripService.loadWordStrip();
           ScoldMeHandler.refreshFile();
           log.info("加载缓存完成 mode:{}", mode);
       }catch (Exception e){
           log.error("加载缓存异常",e);
       }
    }
    
    public synchronized void clearCache(){
        pokeReplyService.clearCache();
        customReplySqliteService.clearCache();
        wordStripService.clearCache();
        HuaQHandler.clearHuaQFace();
        JumpHandler.clearJumpFace();
        log.info("清除缓存完成");
    }


    public String calcRootPath(String rootType){
        switch (rootType){
            case "1":
                File diskFile = FileUtil.getDisk();
                return diskFile.getAbsolutePath();
            case "2":
                return FileUtil.getAppDir();
            default:
                return FileUtil.getAppDir();
        }
    }

    public Long calcRootSize(String rootType){
        switch (rootType){
            case "1":
                File diskFile = FileUtil.getDisk();
                return calcUsedSpace(diskFile);
            case "2":
                return FileUtils.sizeOf(new File(FileUtil.getAppDir()));
            default:
                return FileUtils.sizeOf(new File(FileUtil.getAppDir()));
        }
    }

    private long calcUsedSpace(File file){
        long totalSpace = file.getTotalSpace();    // 总空间
        long freeSpace = file.getFreeSpace();      // 剩余空间
        return totalSpace - freeSpace;   // 已用空间
    }


    public List<FileNode> findNodesByParentPath(String parentPath, String rootType){
        File[] allFileList = FileUtil.getAllFileList(new File(parentPath));
        if(allFileList == null || allFileList.length == 0){
            return Collections.emptyList();
        }
        return Arrays.stream(allFileList).map(e -> {
            FileNode fileNode = new FileNode();

            fileNode.setFileName(e.getName());
            fileNode.setAbsolutePath(e.getAbsolutePath());
            fileNode.setIsDirectory(e.isDirectory());
            fileNode.setShowPreview(isShowPreview(e));
            fileNode.setShowDel(RootTypeEnum.BOT_TOOT.getType().equals(rootType));
            fixFieldSize(fileNode, e, rootType);
            fixFieldLeaf(fileNode, e);
            fixFieldTime(fileNode, e);
            return fileNode;
        }).sorted(Comparator.comparing(FileNode::getFileName)).collect(Collectors.toList());
    }

    private void fixFieldTime(FileNode fileNode, File e) {
        fileNode.setLastModified(e.lastModified());
        try {
            Path path = Paths.get(e.getAbsolutePath());
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            FileTime fileTime = attrs.creationTime();
            fileNode.setCreateTime(fileTime.toMillis());
        }catch (IOException ex){

        }
    }

    private void fixFieldLeaf(FileNode fileNode,File e){
        if(e.isFile()){
            fileNode.setLeaf(true);
            return;
        }
        File[] files = e.listFiles();
        boolean b = files == null || files.length == 0;
        fileNode.setLeaf(b);
        fileNode.setChildCount(b ? 0 : files.length);
    }

    private void fixFieldSize(FileNode fileNode,File e,String rootType){
        if("1".equals(rootType)){
            if(e.isFile()){
                fileNode.setSize(e.length());
            }
            return;
        }
        if ("2".equals(rootType)) {
            if (e.isDirectory()) {
                fileNode.setSize(FileUtils.sizeOf(e));
            }else{
                fileNode.setSize(e.length());
            }
        }
    }

    private boolean isShowPreview(File file){
        if(!file.exists() || file.isDirectory()){
            return false;
        }
        return file.length() <= 30 * 1024;
    }

    public String readFileContent(String filePath){
        File file = new File(filePath);
        if (!isShowPreview(file)) {
            return "";
        }
        String s = FileUtil.detectEncoding(file);
        return cn.hutool.core.io.FileUtil.readString(file, s);
    }

    public List<DatabaseInfoNode> databaseInfo(DatabaseInfoNode request) {
        if (request == null || StringUtils.isBlank(request.getType())) {
            List<SqliteSchema> tables = sqliteSchemaMapper.selectList(new LambdaQueryWrapper<SqliteSchema>()
                    .eq(SqliteSchema::getType, DatabaseInfoNode.TYPE_TABLE)
                    .ne(SqliteSchema::getTblName, DataBaseConst.SQLITE_SYS_T_SQLITE_SEQUENCE));
            return tables.stream().map(e -> {
                DatabaseInfoNode tableInfoNode = new DatabaseInfoNode();
                tableInfoNode.setType(DatabaseInfoNode.TYPE_TABLE);
                tableInfoNode.setName(e.getName());
                tableInfoNode.setKey(e.getName());
                tableInfoNode.setSql(e.getSql());
                tableInfoNode.setTableName(e.getTblName());
                tableInfoNode.setLeaf(false);
                return tableInfoNode;
            }).collect(Collectors.toList());
        }
        String tableName = request.getTableName();
        if(DatabaseInfoNode.TYPE_TABLE.equals(request.getType())){
            return getFixedTableInfoNodes(tableName);
        }

        String name = request.getName();
        if(DatabaseInfoNode.TYPE_FIXED.equals(request.getType()) && StringUtils.isNotBlank(name)
                && StringUtils.isNotBlank(tableName)){
            if(DatabaseInfoNode.TYPE_FIXED_COLUMN.equals(name)){
                // 获取表的列
                List<TableInfoSqlite> tableInfoSqlites = sqliteDatabaseInitMapper.pragmaTableInfo(tableName);
                return tableInfoSqlites.stream().map(e -> {
                    DatabaseInfoNode tableInfoNode = new DatabaseInfoNode();
                    tableInfoNode.setName(e.getName());
                    tableInfoNode.setKey(tableName+"_"+e.getName());
                    tableInfoNode.setColumnType(e.getType());
                    tableInfoNode.setNotnull(e.getNotnull());
                    tableInfoNode.setDefaultValue(e.getDfltValue());
                    tableInfoNode.setPk(e.getPk());
                    tableInfoNode.setTableName(tableName);
                    tableInfoNode.setType(DatabaseInfoNode.TYPE_COLUMN);
                    tableInfoNode.setLeaf(true);
                    return tableInfoNode;
                }).collect(Collectors.toList());
            }

            if(DatabaseInfoNode.TYPE_FIXED_INDEX.equals(name)){
                // 获取表的索引
                List<IndexInfoSqlite> indexInfoSqlites = sqliteDatabaseInitMapper.pragmaIndexList(tableName);
                return indexInfoSqlites.stream().map(e -> {
                    DatabaseInfoNode tableInfoNode = new DatabaseInfoNode();
                    tableInfoNode.setName(e.getName());
                    tableInfoNode.setKey(tableName+"_"+e.getName());
                    tableInfoNode.setUnique(e.getUnique());
                    tableInfoNode.setTableName(tableName);
                    tableInfoNode.setType(DatabaseInfoNode.TYPE_INDEX);
                    tableInfoNode.setLeaf(true);
                    return tableInfoNode;
                }).collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    private List<DatabaseInfoNode> getFixedTableInfoNodes(String tableName) {
        List<DatabaseInfoNode> tableInfoNodes = new ArrayList<>();
        DatabaseInfoNode tableInfoNode = new DatabaseInfoNode();
        tableInfoNode.setTableName(tableName);
        tableInfoNode.setName(DatabaseInfoNode.TYPE_FIXED_COLUMN);
        tableInfoNode.setKey(tableName+"_"+tableInfoNode.getName());
        tableInfoNode.setType(DatabaseInfoNode.TYPE_FIXED);
        tableInfoNode.setLeaf(false);
        tableInfoNodes.add(tableInfoNode);

        DatabaseInfoNode tableInfoNode2 = new DatabaseInfoNode();
        tableInfoNode2.setTableName(tableName);
        tableInfoNode2.setName(DatabaseInfoNode.TYPE_FIXED_INDEX);
        tableInfoNode2.setKey(tableName+"_"+tableInfoNode2.getName());
        tableInfoNode2.setType(DatabaseInfoNode.TYPE_FIXED);
        tableInfoNode2.setLeaf(false);
        tableInfoNodes.add(tableInfoNode2);
        return tableInfoNodes;
    }

    public String tableDDL(String tableName) {
        List<SqliteSchema> sqliteSchemas = sqliteSchemaMapper.selectList(new LambdaQueryWrapper<SqliteSchema>()
                .eq(SqliteSchema::getTblName, tableName)
                .isNotNull(SqliteSchema::getSql));
        if (CollectionUtils.isEmpty(sqliteSchemas)) {
            return "";
        }
        String createTableSql = sqliteSchemas.stream().filter(e -> DatabaseInfoNode.TYPE_TABLE.equals(e.getType())).map(SqliteSchema::getSql).findFirst().orElse("").trim();
        if (!createTableSql.endsWith(";")) {
            createTableSql+=";";
        }
        String createIndexSql = sqliteSchemas.stream()
                .filter(e -> DatabaseInfoNode.TYPE_INDEX.equals(e.getType()))
                .map(e->{
                    String s = e.getSql().trim();
                    if(!s.endsWith(";")){
                        s+=";";
                    }
                    return s;
                }).collect(Collectors.joining("\n"));
        return createTableSql+"\n"+createIndexSql;
    }


    public BotWebSocketInfo getBotWebSocketInfo(){
        BotWebSocketInfo botWebSocketInfo = new BotWebSocketInfo();
        botWebSocketInfo.setRunning(getBotServer().isRunning());
        botWebSocketInfo.setConnections(BotContainer.getConnections());
        botWebSocketInfo.setMaxConnections(Configs.getInt(ConfigKey.WS_MAX_CONNECTIONS, 0));
        botWebSocketInfo.setPath(SysConstants.WEB_SOCKET_PATH);
        botWebSocketInfo.setAccessToken(Configs.getStr(ConfigKey.WS_ACCESS_TOKEN, null));
        return botWebSocketInfo;
    }

    public void restartBot(){
        String restartScript = FileUtil.getRestartScript();
        if (StringUtils.isBlank(restartScript)) {
            throw new BusinessException("未获取到重启脚本 os："+ SystemUtils.OS_NAME);
        }
        File file = new File(restartScript);
        if (!file.exists()) {
            throw new BusinessException("重启脚本不存在："+ restartScript);
        }
        new Thread(()->{
            synchronized (SystemService.class) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }

                if (SystemUtils.IS_OS_WINDOWS) {
                    String s = CMDUtil.executeBatFile(restartScript, String.valueOf(Configs.getInt(ConfigKey.SERVER_PORT)), FileUtil.getAppDir() + File.separator);
                    log.info("执行bat结果：{}", s);
                } else {
                    String s = CMDUtil.executeShFile(restartScript, SystemUtils.getJavaHome().getAbsolutePath());
                    log.info("执行sh结果：{}", s);
                }
            }
        }).start();
    }
}
