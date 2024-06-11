package com.haruhi.botServer.utils.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadFontStyle;
import com.alibaba.excel.annotation.write.style.HeadStyle;
import com.alibaba.excel.enums.poi.VerticalAlignmentEnum;
import lombok.Data;

@Data
public class ChatRecordExportBody {

    @ColumnWidth(17)
    @ExcelProperty(value = "群昵称",index = 0)
    @HeadStyle(verticalAlignment = VerticalAlignmentEnum.CENTER,fillForegroundColor = 44)
    @HeadFontStyle(fontHeightInPoints = 12)
    private String card;
    @ColumnWidth(17)
    @ExcelProperty(value = "QQ昵称",index = 1)
    @HeadStyle(verticalAlignment = VerticalAlignmentEnum.CENTER,fillForegroundColor = 44)
    @HeadFontStyle(fontHeightInPoints = 12)
    private String nickName;
    @ColumnWidth(17)
    @ExcelProperty(value = "QQ",index = 2)
    @HeadStyle(verticalAlignment = VerticalAlignmentEnum.CENTER,fillForegroundColor = 44)
    @HeadFontStyle(fontHeightInPoints = 12)
    private String userId;
    @ColumnWidth(45)
    @ExcelProperty(value = "消息",index = 3)
    @HeadStyle(verticalAlignment = VerticalAlignmentEnum.CENTER,fillForegroundColor = 44)
    @HeadFontStyle(fontHeightInPoints = 12)
    private String content;
    @ColumnWidth(17)
    @ExcelProperty(value = "发送时间",index = 4)
    @HeadStyle(verticalAlignment = VerticalAlignmentEnum.CENTER,fillForegroundColor = 44)
    @HeadFontStyle(fontHeightInPoints = 12)
    private String createTime;
}
