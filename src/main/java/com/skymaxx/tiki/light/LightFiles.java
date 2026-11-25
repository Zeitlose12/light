package com.skymaxx.tiki.light;

import com.skymaxx.tiki.TikiGambleClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;

/** Light: moves the settings from config/skymaxx to config/light on first start. */
public final class LightFiles {
    public static final String FOLDER = "light";

    private LightFiles() {
    }

    public static void migrate() {
        Path root = FabricLoader.getInstance().getConfigDir();
        Path oldDir = root.resolve("skymaxx");
        Path newDir = root.resolve(FOLDER);
        if (Files.exists(newDir) || !Files.isDirectory(oldDir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(oldDir)) {
            for (Path from : (Iterable<Path>)walk::iterator) {
                Path to = newDir.resolve(oldDir.relativize(from).toString());
                if (Files.isDirectory(from)) {
                    Files.createDirectories(to);
                } else {
                    Files.createDirectories(to.getParent());
                    Files.copy(from, to, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
            TikiGambleClient.LOGGER.info("[Light] copied settings from config/skymaxx to config/light");
        } catch (IOException e) {
            TikiGambleClient.LOGGER.warn("[Light] could not copy old settings", (Throwable)e);
        }
    }
}
