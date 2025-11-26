<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>集合列表展示</title>
</head>
<body>

<!-- 1. 展示简单字符串集合（无序列表） -->
<h3>字符串列表：</h3>
<ul>
    <#list stringList as item> <!-- 遍历stringList，每个元素用item表示 -->
        <li>${item?xml}</li> <!-- ?xml转义特殊字符 -->
    </#list>
</ul>

<!-- 2. 展示自定义对象集合（有序列表） -->
<h3>用户列表：</h3>
<table>
    <thead>
    <tr>
        <th>id</th>
        <th>昵称</th>
        <th>内容</th>
    </tr>
    </thead>
    <tbody>
    <#list chatList as chat> <!-- 遍历userList，每个元素用user表示 -->
        <tr>
            <td>${chat.id?c}</td>
            <td>${chat.card?xml}</td>
            <td>${chat.content?xml}</td>
        </tr>
    </#list>
    </tbody>
</table>

<!-- 3. 处理集合为空的情况（可选） -->
<h3>空集合处理：</h3>
<ul>
    <#if emptyList??> <!-- 判断集合是否存在 -->
        <#list emptyList as item>
            <li>${item?xml}</li>
        <#else> <!-- 集合为空时显示 -->
            <li>暂无数据</li>
        </#list>
    <#else>
        <li>集合未定义</li>
    </#if>
</ul>

</body>
</html>