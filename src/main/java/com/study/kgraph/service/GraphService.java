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
  private NLPService nlpService;
  @Autowired
  private ConceptMapper conceptMapper;
  @Autowired
  private RelationMapper relationMapper;

    public Map<String, Object> extractGraph(Long userId, String text) {
        List<String> kws = nlpService.keywords(text, 30);
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
        Map<String, Integer> co = nlpService.cooccurrence(text, kws);
        List<Relation> edges = new ArrayList<>();
        Map<String, Long> nameToId = new HashMap<>();
        int idSeed = 1;
        for (Concept c : nodes)
            nameToId.put(c.getName(), (long) (idSeed++));
        for (Map.Entry<String, Integer> e : co.entrySet()) {
            String[] parts = e.getKey().split("\\|");
            if (parts.length < 2) continue;
            String a = parts[0];
            String b = parts[1];
            Relation r = new Relation();
            r.setUserId(userId);
            r.setSourceId(nameToId.get(a));
            r.setTargetId(nameToId.get(b));
            r.setRelationType("cooccur");
            r.setWeight(e.getValue().doubleValue());
            edges.add(r);
        }
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
            jsonEdges.add(m);
        }
        json.put("nodes", jsonNodes);
        json.put("links", jsonEdges);
        return json;
    }

    private int countOccurrences(String text, String keyword) {
        if (text == null || keyword == null || keyword.isEmpty()) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(keyword, idx)) != -1) {
            count++;
            idx += keyword.length();
        }
        return count;
    }
}
