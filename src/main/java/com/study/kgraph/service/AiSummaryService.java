package com.study.kgraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AiSummaryService {

    @Value("${modelscope.api.key}")
    private String apiKey;

    @Value("${modelscope.api.url}")
    private String apiUrl;

    @Value("${modelscope.model}")
    private String model;

    public String summarize(String content) throws Exception {
        if (content == null || content.trim().isEmpty()) {
            return "内容为空，无法总结";
        }
        return callModel("请对以下内容进行总结，提取关键信息：\n\n" + content);
    }

    public List<String> extractKeywords(String content, int n) throws Exception {
        if (content == null || content.trim().isEmpty() || n <= 0) {
            return Collections.emptyList();
        }
        String prompt = "你是一个关键词抽取器。请从下面文本中抽取最重要的" + n + "个关键词。\n"
                + "只输出严格的JSON数组，例如：[\"关键词1\",\"关键词2\"]，不要输出任何解释、前后缀、代码块。\n\n"
                + "文本：\n" + content;
        String raw = callModel(prompt);
        return parseKeywords(raw, n);
    }

    public List<java.util.Map<String, Object>> extractRelationships(String content, List<String> keywords) {
        if (content == null || content.trim().isEmpty() || keywords == null || keywords.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            String kwsStr = String.join(", ", keywords);
            String prompt = "你是一个关系抽取器。请分析下面的文本，找出给定的关键词之间存在的关系。\n"
                    + "给定关键词：" + kwsStr + "\n"
                    + "请输出一个JSON数组，每个元素包含：\n"
                    + "- source: 源实体（必须在给定关键词中）\n"
                    + "- target: 目标实体（必须在给定关键词中）\n"
                    + "- relation: 关系描述（简短动词短语，如“包含”、“属于”、“导致”等）\n"
                    + "- weight: 关系强度（1-10的整数）\n"
                    + "只输出严格的JSON数组，不要输出任何解释、前后缀、代码块。\n\n"
                    + "文本：\n" + content;

            String raw = callModel(prompt);
            return parseRelationships(raw);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<java.util.Map<String, Object>> labelRelationshipsForPairs(String content, List<String> pairs) {
        if (content == null || content.trim().isEmpty() || pairs == null || pairs.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            StringBuilder sb = new StringBuilder();
            for (String p : pairs) {
                if (p == null)
                    continue;
                String v = p.trim();
                if (!v.isEmpty())
                    sb.append("- ").append(v).append('\n');
            }
            if (sb.length() == 0)
                return Collections.emptyList();

            String prompt = "你是一个关系标注器。请仅针对下面给定的实体对，在文本中判断它们是否存在明确语义关系。\n"
                    + "如果能判断出关系，请输出JSON元素，字段包含：source, target, relation, weight(1-10整数)。\n"
                    + "如果无法判断或文本中没有体现关系，请不要输出该对。\n"
                    + "source/target 必须严格使用给定实体对中的名称。\n"
                    + "只输出严格的JSON数组，不要输出任何解释、前后缀、代码块。\n\n"
                    + "实体对列表（source|target）：\n" + sb
                    + "\n文本：\n" + content;

            String raw = callModel(prompt);
            return parseRelationships(raw);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private List<java.util.Map<String, Object>> parseRelationships(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Clean up potential markdown code blocks
            if (json.contains("```json")) {
                json = json.substring(json.indexOf("```json") + 7);
                if (json.contains("```")) {
                    json = json.substring(0, json.indexOf("```"));
                }
            } else if (json.contains("```")) {
                json = json.substring(json.indexOf("```") + 3);
                if (json.contains("```")) {
                    json = json.substring(0, json.indexOf("```"));
                }
            }
            json = json.trim();

            JsonNode root = mapper.readTree(json);
            List<java.util.Map<String, Object>> list = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode node : root) {
                    if (node.has("source") && node.has("target")) {
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        map.put("source", node.get("source").asText());
                        map.put("target", node.get("target").asText());
                        map.put("relation", node.has("relation") ? node.get("relation").asText() : "related");
                        map.put("weight", node.has("weight") ? node.get("weight").asInt() : 1);
                        list.add(map);
                    }
                }
            }
            return list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String callModel(String prompt) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        root.put("stream", false);

        ArrayNode messages = root.putArray("messages");
        ObjectNode msg = messages.addObject();
        msg.put("role", "user");
        msg.put("content", prompt);

        String jsonInputString = mapper.writeValueAsString(root);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());

                }
                throw new RuntimeException("API Error (" + code + "): " + response.toString());
            } catch (Exception e) {
                throw new RuntimeException("API Error (" + code + ")");
            }
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            JsonNode responseRoot = mapper.readTree(response.toString());
            if (responseRoot.has("choices") && responseRoot.get("choices").isArray()
                    && responseRoot.get("choices").size() > 0) {
                JsonNode choice = responseRoot.get("choices").get(0);
                if (choice.has("message") && choice.get("message").has("content")) {
                    return choice.get("message").get("content").asText();
                }
            }
            return response.toString();
        }
    }

    private List<String> parseKeywords(String raw, int n) {
        if (raw == null)
            return Collections.emptyList();
        String s = raw.trim();
        if (s.isEmpty())
            return Collections.emptyList();

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(s);
            if (node != null && node.isArray()) {
                Set<String> uniq = new LinkedHashSet<>();
                for (JsonNode item : node) {
                    if (item == null)
                        continue;
                    String v = item.asText();
                    if (v != null) {
                        v = v.trim();
                        if (!v.isEmpty())
                            uniq.add(v);
                    }
                    if (uniq.size() >= n)
                        break;
                }
                return new ArrayList<>(uniq);
            }
        } catch (Exception ignored) {
        }

        int lb = s.indexOf('[');
        int rb = s.lastIndexOf(']');
        if (lb >= 0 && rb > lb) {
            String sub = s.substring(lb, rb + 1);
            try {
                JsonNode node = mapper.readTree(sub);
                if (node != null && node.isArray()) {
                    Set<String> uniq = new LinkedHashSet<>();
                    for (JsonNode item : node) {
                        String v = item == null ? null : item.asText();
                        if (v != null) {
                            v = v.trim();
                            if (!v.isEmpty())
                                uniq.add(v);
                        }
                        if (uniq.size() >= n)
                            break;
                    }
                    return new ArrayList<>(uniq);
                }
            } catch (Exception ignored) {
            }
        }

        String normalized = s.replace("\n", " ").replace("\r", " ").trim();
        String[] parts = normalized.split("[,，、;；\\s]+");
        Set<String> uniq = new LinkedHashSet<>();
        for (String p : parts) {
            if (p == null)
                continue;
            String v = p.trim();
            v = v.replaceAll("^[-•]+", "").trim();
            if (v.isEmpty())
                continue;
            if (v.length() > 64)
                continue;
            uniq.add(v);
            if (uniq.size() >= n)
                break;
        }
        return new ArrayList<>(uniq);
    }
}
