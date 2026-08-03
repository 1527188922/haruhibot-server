package com.haruhi.botServer.picimagesearch;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PicImageSearchUtil {

    public static String firstHrefByText(Document document, String text, String prefix) {
        for (Element link : document.select("a[href]")) {
            if (text.equals(link.text())) {
                String href = link.attr("href");
                return href.startsWith("https:") || href.startsWith("http") ? href : prefix + href;
            }
        }
        return "";
    }

    public static String regex(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group(1) : "";
    }

    public static JSONObject parseJson(String text) {
        return StringUtils.isBlank(text) ? new JSONObject() : JSONObject.parseObject(text);
    }

    public static JSONArray jsonAtArray(JSONObject object, String path) {
        Object value = objectAt(object, path);
        return value instanceof JSONArray array ? array : null;
    }

    public static String stringAt(JSONObject object, String path) {
        Object value = objectAt(object, path);
        if (value instanceof JSONArray array && !array.isEmpty()) {
            return Objects.toString(array.get(0), "");
        }
        return Objects.toString(value, "");
    }

    public static Object objectAt(Object object, String path) {
        Object current = object;
        for (String rawPart : path.split("\\.")) {
            if (current == null) {
                return null;
            }
            Matcher matcher = Pattern.compile("([^\\[]+)(?:\\[(\\d+)])?").matcher(rawPart);
            if (!matcher.matches()) {
                return null;
            }
            String key = matcher.group(1);
            if (!(current instanceof JSONObject json)) {
                return null;
            }
            current = json.get(key);
            if (matcher.group(2) != null) {
                if (!(current instanceof JSONArray array)) {
                    return null;
                }
                int index = Integer.parseInt(matcher.group(2));
                current = index < array.size() ? array.get(index) : null;
            }
        }
        return current;
    }

    public static String firstString(JSONObject object, String... keys) {
        if (object == null) {
            return "";
        }
        for (String key : keys) {
            String value = object.getString(key);
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    public static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    public static double numberFromText(String text) {
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(text == null ? "" : text);
        return matcher.find() ? Double.parseDouble(matcher.group(1)) : 0D;
    }

    public static double round(double value, int scale) {
        StringBuilder pattern = new StringBuilder("0");
        if (scale > 0) {
            pattern.append(".").append("0".repeat(scale));
        }
        return Double.parseDouble(new DecimalFormat(pattern.toString()).format(value));
    }

    public static Map<String, Object> toObjectMap(Map<String, ?> params) {
        Map<String, Object> result = new LinkedHashMap<>();
        params.forEach((key, value) -> {
            if (value != null) {
                result.put(key, value);
            }
        });
        return result;
    }

    public static String ensureHttps(String url) {
        if (StringUtils.isBlank(url)) {
            return "";
        }
        return url.startsWith("http") ? url : "https:" + url;
    }

    public static String value(String value) {
        return value == null ? "" : value;
    }

    public static String trimSlash(String value) {
        String result = value;
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    public static String stripTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
