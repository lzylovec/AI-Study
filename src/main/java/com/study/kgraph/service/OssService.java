package com.study.kgraph.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;
import java.net.URL;
import java.util.Date;

@Service
public class OssService {
    @Value("${aliyun.access-key-id:}")
    private String akId;
    @Value("${aliyun.access-key-secret:}")
    private String akSecret;
    @Value("${aliyun.oss.endpoint:}")
    private String endpoint;
    @Value("${aliyun.oss.bucket:}")
    private String bucket;

    public String uploadAndSign(File file, String keyPrefix) {
        if (isEmpty(endpoint) || isEmpty(bucket))
            return "";
        String key = (keyPrefix == null ? "audio/" : keyPrefix) + System.currentTimeMillis() + "_" + file.getName();
        OSS client = new OSSClientBuilder().build(endpoint, akId, akSecret);
        try {
            if (!client.doesBucketExist(bucket)) {
                try {
                    client.createBucket(bucket);
                } catch (Exception e) {
                    throw new RuntimeException("无法创建OSS Bucket [" + bucket + "]: " + e.getMessage());
                }
            }
            client.putObject(bucket, key, file);
            Date expire = new Date(System.currentTimeMillis() + 3600_000);
            URL url = client.generatePresignedUrl(bucket, key, expire);
            return url.toString();
        } finally {
            client.shutdown();
        }
    }

    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
