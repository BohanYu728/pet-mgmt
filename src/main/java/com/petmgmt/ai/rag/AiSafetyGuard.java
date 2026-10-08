package com.petmgmt.ai.rag;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Narrow, explainable screening rules; not a complete prompt-injection detector. */
public final class AiSafetyGuard {
    private static final Pattern INJECTION=Pattern.compile(
        "(忽略|无视|绕过).{0,12}(之前|前面|系统|安全|所有).{0,8}(指令|规则|限制|提示)|"+
        "ignore.{0,15}(previous|system|all).{0,15}(instructions|rules|prompts)|"+
        "(输出|泄露|显示|打印|告诉我).{0,10}(系统提示词|systemprompt|所有用户.{0,4}密码)|"+
        "(扮演|假装).{0,8}(管理员|系统开发者)|<\\|?(system|im_start)\\|?>");
    private static final Pattern SECRET=Pattern.compile(
        "sk-[a-zA-Z0-9_-]{16,}|(?i:bearer)\\s+[a-zA-Z0-9._-]{16,}|"+
        "(?i:api[_ -]?key|password|密码|密钥|token)\\s*[=:：]\\s*[\\\"']?[^\\s\\\"'，。;；]{4,}|"+
        "eyJ[a-zA-Z0-9_-]{8,}\\.[a-zA-Z0-9_-]{8,}\\.[a-zA-Z0-9_-]{8,}");
    private static final Pattern PERSONAL=Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)|(?<!\\d)\\d{17}[\\dXx](?!\\d)");
    public static String inspect(String text) {
        String normalized=Normalizer.normalize(text,Normalizer.Form.NFKC).replaceAll("[\\p{Cf}]", "");
        String compact=normalized.toLowerCase(Locale.ROOT).replaceAll("[\\p{P}\\p{Z}\\s]", "");
        if(INJECTION.matcher(compact).find())return "检测到要求绕过规则、变更权限或提取系统信息的指令，请改为普通养宠或项目功能问题。";
        if(SECRET.matcher(normalized).find())return "检测到疑似密钥、密码或令牌，请删除敏感内容后再提问。";
        if(PERSONAL.matcher(normalized).find())return "检测到疑似手机号或身份证号码，请删除个人信息后再提问。";
        return null;
    }
    private AiSafetyGuard() {}
}
