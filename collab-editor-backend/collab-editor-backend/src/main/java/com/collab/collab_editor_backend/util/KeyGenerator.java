package com.collab.collab_editor_backend.util; // 包名和你的项目一致

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;

public class KeyGenerator {
    public static void main(String[] args) {
        // 生成HS256专用的256位密钥（自动符合安全规范）
        byte[] keyBytes = Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded();
        // 转换为Base64字符串（方便存储到配置文件）
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        System.out.println("===== 256位密钥（复制下面的字符串） =====");
        System.out.println(base64Key);
        System.out.println("======================================");
    }
}