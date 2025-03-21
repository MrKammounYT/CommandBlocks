package com.geitenijs.commandblocks.commands;

import com.geitenijs.commandblocks.Strings;
import com.geitenijs.commandblocks.Utilities;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class Command_Remove implements CommandExecutor, TabCompleter {

    public boolean onCommand(final CommandSender s, final Command c, final String label, final String[] args) {
        if (args.length == 2) {
            final String name = args[1];
            if (Utilities.blocks.getConfigurationSection(name) != null) {
                Utilities.blocks.set(name, null);
                Utilities.saveBlocksFile();
                Utilities.reloadBlocksFile();
                Utilities.msg(s, Strings.IGPREFIX + "&fSuccessfully deleted CommandBlock &6'" + name + "'&f.");
            } else {
                Utilities.msg(s, Strings.IGPREFIX + "&cThere's no CommandBlock named &f'" + name + "'&c.");
            }
        } else {
            Utilities.msg(s, Strings.REMOVEUSAGE);
        }
        return true;
    }

    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] args) {
        ArrayList<String> tabs = new ArrayList<>();
        String[] newArgs = CommandWrapper.getArgs(args);
        if (newArgs.length == 1) {
            tabs.addAll(Utilities.blocks.getKeys(false));
        }
        return CommandWrapper.filterTabs(tabs, args);
    }
}