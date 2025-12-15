package com.study.kgraph.service;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FileTransServiceAliyun {
    @Value("${aliyun.access-key-id:}")
    private String akId;
    @Value("${aliyun.access-key-secret:}")
    private String akSecret;
    @Value("${aliyun.region:cn-shanghai}")
    private String region;
    @Value("${aliyun.app-key:}")
    private String appKey;

    private IAcsClient client() {
        DefaultProfile profile = DefaultProfile.getProfile(region, akId, akSecret);
        return new DefaultAcsClient(profile);
    }

    public String submit(String fileUrl) throws Exception {
        IAcsClient c = client();
        CommonRequest req = new CommonRequest();
        req.setSysDomain("filetrans." + region + ".aliyuncs.com");
        req.setSysVersion("2018-08-17");
        req.setSysAction("SubmitTask");
        req.setSysProduct("nls-filetrans");
        String task = "{\"appkey\":\"" + appKey + "\",\"file_link\":\"" + fileUrl
                + "\",\"version\":\"4.0\",\"enable_sample_rate_adaptive\":true}";
        req.putBodyParameter("Task", task);
        req.setSysMethod(MethodType.POST);
        CommonResponse resp = c.getCommonResponse(req);
        if (resp.getHttpStatus() != 200)
            return "";
        ObjectMapper om = new ObjectMapper();
        JsonNode root = om.readTree(resp.getData());
        String statusText = text(root, "StatusText");
        if (!"SUCCESS".equals(statusText))
            return "";
        return text(root, "TaskId");
    }

    public Result query(String taskId) throws Exception {
        IAcsClient c = client();
        CommonRequest req = new CommonRequest();
        req.setSysDomain("filetrans." + region + ".aliyuncs.com");
        req.setSysVersion("2018-08-17");
        req.setSysAction("GetTaskResult");
        req.setSysProduct("nls-filetrans");
        req.putQueryParameter("TaskId", taskId);
        CommonResponse resp = c.getCommonResponse(req);
        ObjectMapper om = new ObjectMapper();
        JsonNode root = om.readTree(resp.getData());
        Result r = new Result();
        r.status = text(root, "StatusText");
        JsonNode resultNode = root.get("Result");
        if (resultNode != null)
            r.resultText = resultNode.toString();
        return r;
    }

    public static class Result {
        public String status;
        public String resultText;
    }

    private String text(JsonNode n, String k) {
        JsonNode v = n.get(k);
        return v == null ? null : v.asText();
    }
}
