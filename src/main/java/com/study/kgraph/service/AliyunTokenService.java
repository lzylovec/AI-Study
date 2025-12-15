package com.study.kgraph.service;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AliyunTokenService {
    @Value("${aliyun.access-key-id:}")
    private String akId;
    @Value("${aliyun.access-key-secret:}")
    private String akSecret;
    @Value("${aliyun.region:cn-shanghai}")
    private String region;

    private volatile String cachedToken = null;
    private volatile long expireEpochSec = 0L;

    public synchronized String getToken() {
        long now = System.currentTimeMillis() / 1000;
        if (cachedToken != null && now + 300 < expireEpochSec) return cachedToken;
        String id = envOr(akId, "ALIYUN_ACCESS_KEY_ID");
        String secret = envOr(akSecret, "ALIYUN_ACCESS_KEY_SECRET");
        if (isEmpty(id) || isEmpty(secret)) return "";
        try {
            DefaultProfile profile = DefaultProfile.getProfile(region, id, secret);
            IAcsClient client = new DefaultAcsClient(profile);
            CommonRequest request = new CommonRequest();
            request.setSysMethod(MethodType.POST);
            request.setSysDomain("nls-meta." + region + ".aliyuncs.com");
            request.setSysVersion("2019-02-28");
            request.setSysAction("CreateToken");
            CommonResponse response = client.getCommonResponse(request);
            String data = response.getData();
            // very small JSON parse without external libs
            String token = extract(data, "\"Id\":\"", "\"");
            String expire = extract(data, "\"ExpireTime\":", ",");
            if (token == null || token.isEmpty()) return "";
            cachedToken = token;
            try { expireEpochSec = Long.parseLong(expire.trim()); } catch (Exception ignore) {}
            return cachedToken;
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isEmpty(String s) { return s == null || s.isEmpty(); }
    private String envOr(String val, String env) {
        if (!isEmpty(val)) return val;
        String v = System.getenv(env);
        return v == null ? "" : v;
    }
    private String extract(String text, String start, String end) {
        int i = text.indexOf(start);
        if (i < 0) return null;
        int j = text.indexOf(end, i + start.length());
        if (j < 0) j = text.length();
        return text.substring(i + start.length(), j);
    }
}
