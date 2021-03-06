package ru.bozaro.gitlfs.client;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.yaml.snakeyaml.Yaml;
import ru.bozaro.gitlfs.client.auth.AuthProvider;
import ru.bozaro.gitlfs.client.internal.HttpClientExecutor;
import ru.bozaro.gitlfs.common.data.BatchReq;
import ru.bozaro.gitlfs.common.data.Meta;
import ru.bozaro.gitlfs.common.data.Operation;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Simple code for recording replay.
 *
 * @author Artem V. Navrotskiy
 */
public class Recorder {
  public static void main(@Nonnull String[] args) throws IOException {
    final AuthProvider auth = AuthHelper.create("git@github.com:bozaro/test.git");
    try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
      final HttpRecorder recorder = new HttpRecorder(new HttpClientExecutor(httpClient));

      doWork(new Client(auth, recorder));

      final Yaml yaml = YamlHelper.get();
      final File file = new File("build/replay.yml");
      //noinspection ResultOfMethodCallIgnored
      file.getParentFile().mkdirs();
      try (OutputStream replay = new FileOutputStream(file)) {
        yaml.dumpAll(recorder.getRecords().iterator(), new OutputStreamWriter(replay, StandardCharsets.UTF_8));
      }
    }
  }

  private static void doWork(@Nonnull Client client) throws IOException {
    client.postBatch(new BatchReq(
        Operation.Upload,
        Arrays.asList(
            new Meta("b810bbe954d51e380f395de0c301a0a42d16f115453f2feb4188ca9f7189074e", 28),
            new Meta("1cbec737f863e4922cee63cc2ebbfaafcd1cff8b790d8cfd2e6a5d550b648afa", 3)
        )
    ));
  }
}
