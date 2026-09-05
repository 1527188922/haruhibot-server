package com.haruhi.botServer.service;

import com.haruhi.botServer.constant.BusinessModuleEnum;
import com.haruhi.botServer.vo.CodeNameResp;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class EnumService {

    private static final Map<String, Class<? extends Enum>> ENUM_CLASS_MAP = Map.of(
            BusinessModuleEnum.class.getSimpleName(), BusinessModuleEnum.class
    );

    public List<CodeNameResp> list(String enumName) {
        Class<? extends Enum> enumClass = ENUM_CLASS_MAP.get(enumName);
        if (enumClass == null) {
            throw new IllegalArgumentException("不支持的枚举：" + enumName);
        }
        return Arrays.stream(enumClass.getEnumConstants())
                .map(value -> toCodeName((Enum<?>) value))
                .toList();
    }

    private CodeNameResp toCodeName(Enum<?> value) {
        return new CodeNameResp(value.name(), getDisplayName(value));
    }

    private String getDisplayName(Enum<?> value) {
        try {
            Method method = value.getClass().getMethod("getName");
            Object name = method.invoke(value);
            return name == null ? value.name() : name.toString();
        } catch (Exception e) {
            return value.name();
        }
    }
}
