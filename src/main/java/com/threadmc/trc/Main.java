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

    @Option(names = {"--from"}, description = "Source namespace", required = true)
    String fromNamespace;

    @Option(names = {"--to"}, description = "Target namespace", required = true)
    String toNamespace;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        if (inputJar.toString().contains("/")) {
            System.err.println("Error: Input jar path must use '\\' instead of '/'.");
            return 1;
        }
        if (mappingsFile.toString().contains("/")) {
            System.err.println("Error: Mappings file path must use '\\' instead of '/'.");
            return 1;
        }
        if (outputJar.toString().contains("/")) {
            System.err.println("Error: Output jar path must use '\\' instead of '/'.");
            return 1;
        }
        RemapService remapper = new RemapService(inputJar, mappingsFile, outputJar, fromNamespace, toNamespace);
        remapper.run();
        return 0;
    }
}