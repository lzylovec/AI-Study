package com.study.kgraph.service;

import com.hankcs.hanlp.HanLP;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class NLPService {
  public List<String> keywords(String text, int n) {
    if (text == null || text.isEmpty())
      return Collections.emptyList();
    return HanLP.extractKeyword(text, n);
  }

  public Map<String, Integer> cooccurrence(String text, List<String> terms) {
    Map<String, Integer> map = new HashMap<>();
    if (text == null || terms.isEmpty())
      return map;
    String[] sentences = text.split("[。！？.!?]\\s*");
    for (String s : sentences) {
      List<String> present = new ArrayList<>();
      for (String t : terms)
        if (s.contains(t))
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
}
