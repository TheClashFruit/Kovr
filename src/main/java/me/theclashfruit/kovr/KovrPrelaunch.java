package me.theclashfruit.kovr;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.io.*;

public class KovrPrelaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        Kovr.LOGGER.info("Kovr PreLaunch");

        // save the default config if it doesn't exist
        if (!new File("backup_rules.kovr").exists()) {
            try (InputStream in = getClass().getResourceAsStream("/default.kovr")) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter("backup_rules.kovr"))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            writer.write(line);
                            writer.newLine();
                        }
                    }
                }
            } catch (IOException e) {
                Kovr.LOGGER.error("Failed to copy default config", e);
            }
        }
    }
}
