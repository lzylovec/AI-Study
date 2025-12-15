package com.study.kgraph.service;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileInputStream;

@Service
public class TikaService {
  public String extractText(File file) throws Exception {
    Tika tika = new Tika();
    FileInputStream input = new FileInputStream(file);
    try {
      return tika.parseToString(input);
    } finally {
      input.close();
    }
  }
}
