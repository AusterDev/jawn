package com.github.AusterDev.jawn;

import com.github.AusterDev.jawn.core.Config;
import com.github.AusterDev.jawn.core.JawnClient;
import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    static void main() throws Exception {
        String envrionment = System.getProperty("env", "dev");

        if ("dev".equalsIgnoreCase(envrionment)) {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

            dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
        }

        JawnClient client = new JawnClient(Config.load());
        client.start();
    }
}
