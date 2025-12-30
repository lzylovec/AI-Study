package com.study.kgraph.service;

import com.study.kgraph.entity.Concept;
import com.study.kgraph.entity.Relation;
import com.study.kgraph.mapper.ConceptMapper;
import com.study.kgraph.mapper.RelationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class GraphService {
    @Autowired
    private AiSummaryService aiSummaryService;
    @Autowired
    private ConceptMapper conceptMapper;
    @Autowired
    private RelationMapper relationMapper;

    public Map<String, Object> extractGraph(Long userId, String text) {
        List<String> kws = keywordsByAi(text, 30);
        List<Concept> nodes = new ArrayList<>();
        for (String k : kws) {
            Concept c = new Concept();
            c.setUserId(userId);
            c.setName(k);
            // Simple frequency score
            int freq = countOccurrences(text, k);
            c.setScore((double) freq);
            nodes.add(c);
        }
        Map<String, Integer> co = cooccurrence(text, kws);
        Map<String, Long> nameToId = new HashMap<>();
        int idSeed = 1;
        for (Concept c : nodes)
            nameToId.put(c.getName(), (long) (idSeed++));

        String hub = pickHub(nodes);
        Map<String, Relation> edgesByPair = new HashMap<>();

        List<Map<String, Object>> llmRelations = aiSummaryService.extractRelationships(text, kws);
        mergeRelationsFromLlm(userId, nameToId, edgesByPair, llmRelations);

        List<String> pairs = topPairs(co, hub, 30);
        if (!pairs.isEmpty()) {
            List<Map<String, Object>> labeled = aiSummaryService.labelRelationshipsForPairs(text, pairs);
            mergeRelationsFromLlm(userId, nameToId, edgesByPair, labeled);
        }

        if (edgesByPair.isEmpty()) {
            for (Map.Entry<String, Integer> e : co.entrySet()) {
                String[] parts = e.getKey().split("\\|");
                if (parts.length < 2)
                    continue;
                String a = parts[0];
                String b = parts[1];
                addOrMergeEdge(userId, nameToId, edgesByPair, a, b, "cooccur", e.getValue());
            }
        }

        List<Relation> edges = new ArrayList<>(edgesByPair.values());

        Map<String, Object> json = new HashMap<>();
        List<Map<String, Object>> jsonNodes = new ArrayList<>();
        for (Concept c : nodes) {
            Map<String, Object> n = new HashMap<>();
            n.put("id", nameToId.get(c.getName()));
            n.put("name", c.getName());
            n.put("score", c.getScore());
            jsonNodes.add(n);
        }
        List<Map<String, Object>> jsonEdges = new ArrayList<>();
        for (Relation r : edges) {
            Map<String, Object> m = new HashMap<>();
            m.put("source", r.getSourceId());
            m.put("target", r.getTargetId());
            m.put("weight", r.getWeight());
            m.put("relation", r.getRelationType()); // Pass relation type to frontend
            jsonEdges.add(m);
        }
        json.put("nodes", jsonNodes);
        json.put("links", jsonEdges);
        return json;
    }

    private String pickHub(List<Concept> nodes) {
        if (nodes == null || nodes.isEmpty())
            return null;
        Concept best = null;
        for (Concept c : nodes) {
            if (c == null || c.getName() == null)
                continue;
            if (best == null)
                best = c;
            else if (c.getScore() != null && best.getScore() != null && c.getScore() > best.getScore())
                best = c;
        }
        return best == null ? null : best.getName();
    }

    private List<String> topPairs(Map<String, Integer> co, String hub, int limit) {
        if (co == null || co.isEmpty() || limit <= 0)
            return Collections.emptyList();
        List<Map.Entry<String, Integer>> list = new ArrayList<>(co.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : list) {
            if (e == null || e.getKey() == null)
                continue;
            String[] parts = e.getKey().split("\\|");
            if (parts.length < 2)
                continue;
            String a = parts[0];
            String b = parts[1];
            if (a == null || b == null)
                continue;
            a = a.trim();
            b = b.trim();
            if (a.isEmpty() || b.isEmpty() || a.equals(b))
                continue;
            if (hub != null && (hub.equals(a) || hub.equals(b)))
                continue;
            out.add(a + "|" + b);
            if (out.size() >= limit)
                return out;
        }
        for (Map.Entry<String, Integer> e : list) {
            if (out.size() >= limit)
                break;
            if (e == null || e.getKey() == null)
                continue;
            String[] parts = e.getKey().split("\\|");
            if (parts.length < 2)
                continue;
            String a = parts[0];
            String b = parts[1];
            if (a == null || b == null)
                continue;
            a = a.trim();
            b = b.trim();
            if (a.isEmpty() || b.isEmpty() || a.equals(b))
                continue;
            String key = a + "|" + b;
            if (!out.contains(key))
                out.add(key);
        }
        if (out.size() > limit)
            return out.subList(0, limit);
        return out;
    }

    private void mergeRelationsFromLlm(Long userId, Map<String, Long> nameToId, Map<String, Relation> edgesByPair,
            List<Map<String, Object>> llmRelations) {
        if (llmRelations == null || llmRelations.isEmpty())
            return;
        for (Map<String, Object> rel : llmRelations) {
            if (rel == null)
                continue;
            String s = rel.get("source") == null ? null : String.valueOf(rel.get("source"));
            String t = rel.get("target") == null ? null : String.valueOf(rel.get("target"));
            if (s == null || t == null)
                continue;
            s = s.trim();
            t = t.trim();
            if (s.isEmpty() || t.isEmpty())
                continue;
            String type = rel.get("relation") == null ? "related" : String.valueOf(rel.get("relation")).trim();
            if (type.isEmpty())
                type = "related";
            Integer weight = null;
            Object w = rel.get("weight");
            if (w instanceof Number) {
                weight = ((Number) w).intValue();
            } else if (w != null) {
                try {
                    weight = Integer.parseInt(String.valueOf(w));
                } catch (Exception ignored) {
                }
            }
            addOrMergeEdge(userId, nameToId, edgesByPair, s, t, type, weight == null ? 1 : weight);
        }
    }

    private void addOrMergeEdge(Long userId, Map<String, Long> nameToId, Map<String, Relation> edgesByPair, String a,
            String b, String relation, Integer weight) {
        if (a == null || b == null || relation == null || nameToId == null || edgesByPair == null)
            return;
        Long aId = nameToId.get(a);
        Long bId = nameToId.get(b);
        if (aId == null || bId == null || aId.equals(bId))
            return;
        long sId = Math.min(aId, bId);
        long tId = Math.max(aId, bId);
        String pairKey = sId + "|" + tId;
        Relation candidate = new Relation();
        candidate.setUserId(userId);
        candidate.setSourceId(sId);
        candidate.setTargetId(tId);
        candidate.setRelationType(relation);
        candidate.setWeight((double) (weight == null ? 1 : weight));

        Relation existing = edgesByPair.get(pairKey);
        if (existing == null) {
            edgesByPair.put(pairKey, candidate);
            return;
        }
        boolean newBetter = candidate.getWeight() != null && existing.getWeight() != null
                && candidate.getWeight() > existing.getWeight();
        boolean existingWeakType = existing.getRelationType() == null || existing.getRelationType().isEmpty()
                || "cooccur".equalsIgnoreCase(existing.getRelationType())
                || "related".equalsIgnoreCase(existing.getRelationType());
        boolean newStrongType = candidate.getRelationType() != null && !candidate.getRelationType().isEmpty()
                && !"cooccur".equalsIgnoreCase(candidate.getRelationType())
                && !"related".equalsIgnoreCase(candidate.getRelationType());
        if (newBetter || (existingWeakType && newStrongType)) {
            edgesByPair.put(pairKey, candidate);
        }
    }

    private List<String> keywordsByAi(String text, int n) {
        try {
            return aiSummaryService.extractKeywords(text, n);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Map<String, Integer> cooccurrence(String text, List<String> terms) {
        Map<String, Integer> map = new HashMap<>();
        if (text == null || terms == null || terms.isEmpty())
            return map;
        String[] sentences = text.split("[。！？.!?]\\s*");
        for (String s : sentences) {
            if (s == null || s.isEmpty())
                continue;
            List<String> present = new ArrayList<>();
            for (String t : terms)
                if (t != null && !t.isEmpty() && s.contains(t))
                    present.add(t);
            for (int i = 0; i < present.size(); i++) {
                for (int j = i + 1; j < present.size(); j++) {
                    String key = present.get(i) + "|" + present.get(j);
                    map.put(key, map.getOrDefault(key, 0) + 1);
                }
            }
        }
        return map;
    }

    private int countOccurrences(String text, String keyword) {
        if (text == null || keyword == null || keyword.isEmpty())
            return 0;
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(keyword, idx)) != -1) {
            count++;
            idx += keyword.length();
        }
        return count;
    }
}
