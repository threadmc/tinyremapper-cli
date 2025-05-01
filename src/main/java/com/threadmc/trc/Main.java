package com.threadmc.trc;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "tiny-remap-cli", mixinStandardHelpOptions = true, version = "1.0.0",
        description = "Remaps obfuscated Minecraft jars using TinyRemapper and Mojang mappings.")
public class Main implements Callable<Integer> {

    @Option(names = {"--input"}, description = "Input server jar file", required = true)
    Path inputJar;

    @Option(names = {"--mappings"}, description = "Tiny mappings file (mojang)", required = true)
    Path mappingsFile;

    @Option(names = {"--output"}, description = "Output jar file (remapped)", required = true)
    Path outputJar;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        RemapService remapper = new RemapService(inputJar, mappingsFile, outputJar);
        remapper.run();
        return 0;
    }
}