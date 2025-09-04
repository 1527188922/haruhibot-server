package com.haruhi.botServer.utils.excel;

import cn.hutool.core.text.StrFormatter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor
public class BatchInsertListener extends AnalysisEventListener<Map<Integer, String>> {
    @Setter
    private int batchSize = 500;
    @Setter
    private String tableName;
    @Setter
    private JdbcTemplate jdbcTemplate;

    private List<String> headers;
    private List<List<Object>> batchCache = new ArrayList<>(batchSize);

    public BatchInsertListener(JdbcTemplate jdbcTemplate, String tableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
    }
 
    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        headers = headMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }
 
    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext context) {
        List<Object> row = new ArrayList<>(headers.size());
        for (int i = 0; i < headers.size(); i++) {
            row.add(data.get(i));
        }
        batchCache.add(row);
        if (batchCache.size() >= batchSize) {
            executeBatch();
        }
    }
 
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        if (!batchCache.isEmpty()) {
            executeBatch();
        }
    }
 
    private void executeBatch() {
        String sql = buildInsertSql();

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                List<Object> row = batchCache.get(i);
                for (int j = 0; j < row.size(); j++) {
                    ps.setObject(j + 1, row.get(j));
                }
            }

            @Override
            public int getBatchSize() {
                return batchCache.size();
            }
        });

        batchCache.clear();

    }
 
    private String buildInsertSql() {
        String columns = String.join(", ", headers);
        String placeholders = headers.stream()
                .map(h -> "?")
                .collect(Collectors.joining(", "));
        return StrFormatter.format("INSERT INTO {} ({}) VALUES ({})", tableName, columns, placeholders);
    }
}