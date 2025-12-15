package com.study.kgraph.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class EnvLoader {
  public EnvLoader() {
    load();
  }
  private void load() {
    try {
      File f = new File(".env");
      if (!f.exists()) return;
      try (BufferedReader br = new BufferedReader(new FileReader(f))) {
        String line;
        while ((line = br.readLine()) != null) {
          line = line.trim();
          if (line.isEmpty() || line.startsWith("#")) continue;
          int i = line.indexOf('=');
          if (i > 0) {
            String k = line.substring(0, i).trim();
            String v = line.substring(i + 1).trim();
            System.setProperty(k, v);
          }
        }
      }
    } catch (Exception ignored) {}
  }
}

