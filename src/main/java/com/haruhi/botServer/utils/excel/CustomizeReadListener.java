package com.haruhi.botServer.utils.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.metadata.data.ReadCellData;
import com.alibaba.excel.read.listener.ReadListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * 通用读取监听器
 * 可将多个表头映射到同一个实体字段 通过前缀匹配
 * 可自定义校验表头方法
 * 可自定义校验单行数据方法
 * @param <T>
 */
@Slf4j
public class CustomizeReadListener<T extends BaseImportBody> implements ReadListener<LinkedHashMap<Integer,String>> {

    // 是否要求表头名称完全一致
    // true 使用equals 
    // false 使用startsWith 默认
    private boolean headFullWordMatch = false;
    public void setHeadFullWordMatch(boolean headFullWordMatch){
        this.headFullWordMatch = headFullWordMatch;
    }


    private Map<String, Integer> fieldValue = new HashMap<>();
    public Class<T> classType;
    
    private List<T> excelData = new ArrayList<>();
    private Map<Integer, String> headMap = new LinkedHashMap<>();
    private Consumer<Map<Integer, String>> headValidator;
    private Consumer<T> bodyValidator;
    
    
    public List<T> getExcelData() {
        return excelData;
    }
    public Map<Integer, String> getHeadMap() {
        return headMap;
    }
    
    public CustomizeReadListener(Class<T> classType){
        this.classType = classType;
    }

    public CustomizeReadListener(Class<T> classType, Consumer<Map<Integer, String>> headValidator){
        this.classType = classType;
        this.headValidator = headValidator;
    }
    /**
     * 
     * @param classType
     * @param headValidator 自定义方法 检查表头
     * @param bodyValidator 自定义方法 检查行数据
     */
    public CustomizeReadListener(Class<T> classType, Consumer<Map<Integer, String>> headValidator, Consumer<T> bodyValidator){
        this.classType = classType;
        this.headValidator = headValidator;
        this.bodyValidator = bodyValidator;
    }
    
    @Override
    public void invokeHead(Map<Integer, ReadCellData<?>> cellHeadMap, AnalysisContext context) {
        
        for (Map.Entry<Integer, ReadCellData<?>> entry : cellHeadMap.entrySet()) {
            headMap.put(entry.getKey(),entry.getValue().getStringValue());
        }
        headMap = headMap.entrySet().stream().filter(e -> StringUtils.isNotBlank(e.getValue())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if(headValidator != null){
            headValidator.accept(headMap);
        }
        fieldValue.putAll(fieldValueSet(headMap, classType));
    }

    @Override
    public void invoke(LinkedHashMap<Integer,String> body, AnalysisContext analysisContext) {
        T t = null;
        try {
            t = classType.newInstance();
        } catch (Exception e) {
            log.error("实例化excel数据实体异常",e);
        }
        if(t == null){
            return;
        }
        setFieldValue(body, fieldValue, t);
        t.setRowIndex(analysisContext.readRowHolder().getRowIndex());

        if(bodyValidator != null){
            bodyValidator.accept(t);
        }
        excelData.add(t);
        
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

    }

    /**
     * 获取实体类字段 与 表头的对应关系
     *
     * @param headMap<表头列下标,表头名称> 表头map
     * @param cla     对应解析类
     * @return 实体字段与表头对应关系
     */
    public Map<String, Integer> fieldValueSet(Map<Integer, String> headMap, Class<T> cla) {
        // fieldValue<字段名称,列下标>
        Map<String, Integer> fieldValue = new HashMap<>();
        Field[] fields = cla.getDeclaredFields();
        for (Field field : fields) {
            //遍历每个属性
            if (field.isAnnotationPresent(ExcelProperty.class)) {
                ExcelProperty excelProperty = field.getAnnotation(ExcelProperty.class);
                for (Map.Entry<Integer, String> entry : headMap.entrySet()) {
                    for (String s : excelProperty.value()) {
                        if (StringUtils.isNotBlank(entry.getValue()) 
                                && (this.headFullWordMatch ? entry.getValue().equals(s) : entry.getValue().startsWith(s))) {
                            fieldValue.put(field.getName(), entry.getKey());
                            break;
                        }
                    }
                }
            }
        }
        return fieldValue;
    }

    /**
     * 给对象的字段填充值
     * valueMap(最原始的单行数据) --> obj(实体类 BaseImportBody)
     * 
     * @param valueMap<colIndex,value>       从excel解析出来的最原始的单行数据
     * @param fieldValue<fieldName,colIndex> 对象的字段与excel列的对应关系
     * @param obj 要填充的对象
     */
    private void setFieldValue(Map<Integer, String> valueMap, Map<String, Integer> fieldValue, T obj) {
        Field[] fields = obj.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(ExcelProperty.class) && fieldValue.containsKey(field.getName())) {
                field.setAccessible(true);
                try {
                    // 根据字段名称 获取字段对应的列所在下标
                    Integer colIndex = fieldValue.get(field.getName());
                    // 根据列下标 获取该单元格的内容(数据)
                    field.set(obj, valueMap.get(colIndex));
                } catch (IllegalAccessException e) {
                    log.error("设置属性异常 {}",field,e);
                }
            }
            
        }
    }
}
