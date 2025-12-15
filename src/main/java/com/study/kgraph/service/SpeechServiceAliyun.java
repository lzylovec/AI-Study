package com.study.kgraph.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Service
public class SpeechServiceAliyun implements SpeechService {
  @Value("${aliyun.app-key:}")
  private String appKey;
  @Autowired
  private AliyunTokenService tokenService;
  @Value("${aliyun.region:cn-shanghai}")
  private String region;

  public String transcribe(File audioFile) throws Exception {
    if (appKey == null || appKey.isEmpty())
      return "";
    String token = tokenService.getToken();
    if (token == null || token.isEmpty())
      return "";
    String fmt = guessFormat(audioFile.getName());
    int sample = 16000;
    String url = "https://nls-gateway." + region + ".aliyuncs.com/stream/v1/asr" +
        "?appkey=" + appKey + "&format=" + fmt + "&sample_rate=" + sample +
        "&enable_punctuation_prediction=true&enable_inverse_text_normalization=true";
    byte[] data = readAll(audioFile);
    URL u = new URL(url);
    HttpURLConnection conn = (HttpURLConnection) u.openConnection();
    conn.setRequestMethod("POST");
    conn.setDoOutput(true);
    conn.setRequestProperty("X-NLS-Token", token);
    conn.setRequestProperty("Content-Type", "application/octet-stream");
    conn.getOutputStream().write(data);
    int code = conn.getResponseCode();
    InputStream is = code / 100 == 2 ? conn.getInputStream() : conn.getErrorStream();
    String body = readString(is);

    // 增加调试日志
    System.out.println("Aliyun ASR Response Code: " + code);
    System.out.println("Aliyun ASR Response Body: " + body);

    if (code != 200) {
      throw new RuntimeException("阿里云API错误 (Code " + code + "): " + body);
    }

    String result = extract(body, "\"result\":\"", "\"");
    if (result == null || result.isEmpty()) {
      // 尝试提取 message
      String msg = extract(body, "\"message\":\"", "\"");
      throw new RuntimeException("识别结果为空: " + (msg != null ? msg : body));
    }
    return result;
  }

  private String guessFormat(String name) {
    String n = name.toLowerCase();
    if (n.endsWith(".pcm"))
      return "pcm";
    if (n.endsWith(".opus"))
      return "opus";
    if (n.endsWith(".wav"))
      return "wav"; // 部分地域支持wav
    if (n.endsWith(".mp3"))
      return "mp3"; // 若不支持将由服务返回错误
    return "pcm";
  }

  private byte[] readAll(File f) throws Exception {
    FileInputStream in = new FileInputStream(f);
    try {
      byte[] buf = new byte[(int) f.length()];
      int off = 0;
      int n;
      while ((n = in.read(buf, off, buf.length - off)) > 0)
        off += n;
      return buf;
    } finally {
      in.close();
    }
  }

  private String readString(InputStream in) throws Exception {
    InputStreamReader r = new InputStreamReader(in, "UTF-8");
    BufferedReader br = new BufferedReader(r);
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      sb.append(line);
    }
    br.close();
    return sb.toString();
  }

  private String extract(String text, String start, String end) {
    int i = text.indexOf(start);
    if (i < 0)
      return null;
    int j = text.indexOf(end, i + start.length());
    if (j < 0)
      j = text.length();
    return text.substring(i + start.length(), j);
  }
}
