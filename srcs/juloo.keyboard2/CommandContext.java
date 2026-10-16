package juloo.keyboard2;

import java.util.HashMap;
import java.util.Map;

public final class CommandContext {
    public static final String[] TOP_COMMANDS = {"git", "flutter", "dart", "ls", "cd", "echo", "adb", "grep", "find", "python", "node", "npm", "ssh", "tar", "cat", "mkdir", "rm", "chmod", "ps", "kill", "diff", "awk", "sed", "vim", "nano", "pwd", "ping", "gradle", "java", "javac", "su", "top"};
    static final Map<String, String[]> SUBCOMMANDS = new HashMap<>();

    static {
        SUBCOMMANDS.put("git", new String[]{"status", "add", "commit", "push", "pull", "checkout", "switch", "branch", "merge", "log", "diff", "clone", "fetch", "reset", "rebase", "stash", "tag", "show", "remote"});
        SUBCOMMANDS.put("flutter", new String[]{"run", "build", "test", "analyze", "pub", "doctor", "clean", "devices", "create", "upgrade"});
        SUBCOMMANDS.put("dart", new String[]{"run", "test", "analyze", "format", "fix", "pub", "compile", "create"});
        SUBCOMMANDS.put("adb", new String[]{"devices", "shell", "install", "uninstall", "push", "pull", "logcat", "reboot", "connect", "disconnect"});
        SUBCOMMANDS.put("gradle", new String[]{"build", "test", "assembleDebug", "assembleRelease", "clean", "installDebug", "connectedCheck", "tasks"});
    }

    private CommandContext() {}

    public static boolean isKnownCommand(String command) {
        return command != null && SUBCOMMANDS.containsKey(command);
    }

    public static String[] subcommands(String command) {
        return SUBCOMMANDS.get(command);
    }
}
