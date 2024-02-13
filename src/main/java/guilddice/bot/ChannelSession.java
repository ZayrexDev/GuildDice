package guilddice.bot;

import com.alibaba.fastjson2.JSONObject;
import guilddice.Main;
import guilddice.bot.api.universal.Bot;
import guilddice.bot.api.universal.Message;
import guilddice.bot.logging.CheckLogEntry;
import guilddice.bot.logging.Log;
import guilddice.bot.logging.MsgLogEntry;
import guilddice.bot.logging.RollLogEntry;
import guilddice.util.dice.Dice;
import guilddice.util.dice.expr.DiceExpr;
import guilddice.util.dice.result.DiceResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static guilddice.util.dice.expr.DiceExpr.isDigit;

public class ChannelSession {
    private static final String[][] comments = {
            {"Σ(っ °Д °;)っ", "w(ﾟДﾟ)w", "(＃°Д°)", "还不快谢谢我( •̀ ω •́ )✧"}, //大成功
            {"∑( 口 ||)", "还不快大失败（"}, //极难
            {"还不快大失败（", "666（￣︶￣）↗　"}, //困难
            {"还不快大失败（", "(ง •_•)ง", "(。・ω・。)"}, //成功
            {"(✿◡‿◡)", ":P", "(っ °Д °;)っ"}, //失败
            {"(っ °Д °;)っ", "不是我的错哦~"} //大失败
    };
    private final Bot bot;
    private final String channelId;
    private final Log logEntries;
    private final LinkedHashMap<UUID, Player> players = new LinkedHashMap<>();
    private final Logger LOG = LogManager.getLogger(ChannelSession.class);
    private boolean on = false;

    public ChannelSession(Bot bot, String channelId) {
        this.bot = bot;
        this.channelId = channelId;
        logEntries = new Log(channelId);
    }

    public void handleMessage(Message message) {
        if (message.isBotMessage()) return;
        final String content = message.getContent();
        if (content == null || content.isBlank()) return;
        if (content.startsWith(".")) {
            handleCommand(message);
        } else {
            if (on) {
                final Player player = players.get(message.getAuthor().getID().asUuid());
                final String name;
                if (player != null && player.getCurrentCharacter() != null) {
                    name = player.getCurrentCharacter().getName();
                } else {
                    name = message.getAuthor().getUsername();
                }
                logEntries.attach(new MsgLogEntry(message.getContent(), message.getAuthor().getUsername(), name, message.getTimestamp()));
            }
        }
    }

    private void handleCommand(Message message) {
        final String[] args = message.getContent().split(" ");
        if (args.length == 0) return;

        final UUID universalID = message.getAuthor().getID().asUuid();
        try {
            final UUID uuid = UUID.fromString(Main.getConfig().getMasterId());
            if (args[0].equals(".inspect") && Objects.equals(universalID, uuid)) {
                bot.sendMessage(message, JSONObject.from(message).toString());
                return;
            }
            if (args[0].equals(".reload") && Objects.equals(universalID, uuid)) {
                Main.loadConfig();
                bot.sendMessage(message, "配置已重载。");
                return;
            }
        } catch (Exception e) {
            LOG.warn("骰主ID设置不正确。" + Main.getConfig().getMasterId());
        }

        if (!on && !args[0].equals(".switch")) {
            return;
        }

        switch (args[0]) {
            case ".switch" -> {
                if (args[1].equals("on")) {
                    if (on) {
                        bot.sendMessage(message, "Bot已经上线了呢~");
                    } else {
                        on = true;
                        bot.sendMessage(message, "Bot已上线~\n书写你们的冒险叭ヾ(≧▽≦*)o");
                    }
                } else {
                    if (on) {
                        on = false;
                        bot.sendMessage(message, "Bot已离线~\n冒险的篇章已然落下帷幕......");
                        try {
                            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd.HH_mm_ss");
                            final String format = simpleDateFormat.format(new Date());
                            final String path = "log-" + channelId + "-" + format + ".json";
                            logEntries.save(path);
                            bot.sendMessage(message, "日志文件成功保存至" + path);
                        } catch (IOException e) {
                            bot.sendMessage(message, "保存日志文件失败！");
                            LOG.error("保存日志文件失败！", e);
                        }
                    }
                }
            }
            case ".pc" -> {
                reloadPlayer(universalID);
                players.putIfAbsent(universalID, new Player(universalID, message.getAuthor().getUsername()));

                final Player player = players.get(universalID);

                switch (args[1]) {
                    case "list" -> {
                        final StringBuilder sb = new StringBuilder();
                        sb.append(player.getName()).append("的全部角色卡：\n");
                        for (PlayerCharacter character : player.getCharacters()) {
                            if (Objects.equals(player.getCurrentCharacter(), character)) {
                                sb.append("√ ");
                            } else {
                                sb.append("- ");
                            }

                            sb.append(character.getName()).append("\n");
                        }
                        sb.append("（共").append(player.getCharacters().size()).append("个）");
                        bot.sendMessage(message, sb.toString());
                    }
                    case "create" -> {
                        if (args.length < 3) {
                            bot.sendMessage(message, message.atAuthor() + "指令参数错误~");
                            return;
                        }

                        if (player.getCharacters().stream().anyMatch(pc -> Objects.equals(args[2], pc.getName()))) {
                            bot.sendMessage(message, message.atAuthor() + "命名重复！！");
                            return;
                        }

                        final PlayerCharacter pc = new PlayerCharacter(args[2]);
                        for (int i = 3; i < args.length; i++) {
                            final String[] attr = args[i].split(":");
                            final String attrName = PlayerCharacter.getStandardName(attr[0]);
                            final int attrVal = Integer.parseInt(attr[1]);

                            pc.setAttr(attrName, attrVal);
                        }

                        player.getCharacters().add(pc);
                        try {
                            player.save();
                        } catch (IOException e) {
                            LOG.error("无法保存PL信息", e);
                        }

                        bot.sendMessage(message, "角色 " + pc.getName() + " 创建成功！");
                    }
                    case "switch" -> {
                        if (args.length != 3) {
                            bot.sendMessage(message, "指令参数错误~");
                            return;
                        }

                        final String pcName = args[2];

                        final Optional<PlayerCharacter> any = player.getCharacters().stream().filter(pc -> Objects.equals(pc.getName(), pcName)).findAny();
                        any.ifPresentOrElse(pc -> {
                            player.setCurrentCharacter(pc);
                            bot.sendMessage(message, "切换角色" + pc.getName() + "成功~");
                        }, () -> bot.sendMessage(message, "无法找到角色 " + pcName + " ！"));
                        refreshPlayerNickname(message);

                        try {
                            player.save();
                        } catch (IOException e) {
                            LOG.error("无法保存PL信息", e);
                        }
                    }
                    case "attr" -> {
                        if (args.length < 3) {
                            bot.sendMessage(message, "指令参数错误~");
                            return;
                        }

                        switch (args[2]) {
                            case "get" -> {
                                if (args.length == 3) {
                                    final PlayerCharacter curPC = player.getCurrentCharacter();
                                    if (curPC != null) {
                                        bot.sendMessage(message, curPC.getName() +
                                                "的属性\n" + curPC.listAttr());
                                    } else {
                                        bot.sendMessage(message, message.atAuthor() + "未选择角色~");
                                    }
                                } else {
                                    final String pcName = args[3];
                                    final Optional<PlayerCharacter> op = player.getCharacters().stream().filter(pc -> Objects.equals(pc.getName(), pcName)).findAny();
                                    op.ifPresentOrElse(pc -> bot.sendMessage(message, message.atAuthor() + pc.getName() + "的属性：\n" + pc.listAttr())
                                            , () -> bot.sendMessage(message, message.atAuthor() + "无法找到角色 " + pcName + " !"));
                                }
                            }
                            case "set" -> {
                                if (args.length < 5 || args.length % 2 != 1) {
                                    bot.sendMessage(message, message.atAuthor() + "指令参数错误~");
                                    return;
                                }
                                final PlayerCharacter curPC = player.getCurrentCharacter();

                                if (curPC == null) {
                                    bot.sendMessage(message, message.atAuthor() + "未选择角色~");
                                    return;
                                }

                                final StringBuilder reply = new StringBuilder();
                                reply.append(message.atAuthor());
                                reply.append(curPC.getName()).append("属性更改：\n");
                                for (int i = 3; i < args.length; i += 2) {
                                    final String standardName = PlayerCharacter.getStandardName(args[i]);
                                    if (!isDigit(args[i + 1])) {
                                        reply.append(standardName).append("设置失败：非数字").append("\n");
                                    } else {
                                        final int orig = curPC.getAttr(standardName);
                                        curPC.setAttr(standardName, Integer.parseInt(args[i + 1]));
                                        reply.append(standardName).append("成功设置为：").append(orig).append("->").append(args[i + 1]).append("\n");
                                    }
                                }
                                refreshPlayerNickname(message);

                                try {
                                    player.save();
                                } catch (IOException e) {
                                    LOG.error("无法保存PL信息", e);
                                }

                                bot.sendMessage(message, reply.toString());
                            }
                            case "modify" -> {
                                if (args.length < 5 || args.length % 2 != 1) {
                                    bot.sendMessage(message, message.atAuthor() + "指令参数错误~");
                                    return;
                                }
                                final PlayerCharacter curPC = player.getCurrentCharacter();

                                if (curPC == null) {
                                    bot.sendMessage(message, message.atAuthor() + "未选择角色~");
                                    return;
                                }

                                final StringBuilder reply = new StringBuilder();
                                reply.append(message.atAuthor()).append(curPC.getName()).append("属性更改：\n");
                                for (int i = 3; i < args.length; i += 2) {
                                    final String standardName = PlayerCharacter.getStandardName(args[i]);

                                    if (!isDigit(args[i + 1])) {
                                        reply.append(standardName).append("更改失败：非数字").append("\n");
                                    } else {
                                        if (!curPC.hasAttr(standardName)) {
                                            reply.append(standardName).append("更改失败：找不到属性").append("\n");
                                        } else {
                                            final int orig = curPC.getAttr(standardName);
                                            curPC.modAttr(standardName, Integer.parseInt(args[i + 1]));
                                            reply.append(standardName).append("成功更改为：").append(orig).append("->").append(curPC.getAttr(standardName)).append("\n");
                                        }
                                    }
                                }
                                refreshPlayerNickname(message);
                                try {
                                    player.save();
                                } catch (IOException e) {
                                    LOG.error("无法保存PL信息", e);
                                }

                                bot.sendMessage(message, reply.toString());
                            }
                            default -> bot.sendMessage(message, message.atAuthor() + "找不到指令 " + args[2]);
                        }
                    }
                    case "rename" -> {
                        if (args.length != 4 && args.length != 3) {
                            bot.sendMessage(message, message.atAuthor() + "指令参数错误~");
                            return;
                        }

                        if (args.length == 3) {
                            if (player != null) {
                                final PlayerCharacter curPC = player.getCurrentCharacter();
                                if (curPC != null) {
                                    final String target = args[2];
                                    if (player.getCharacters().stream().anyMatch(pc -> Objects.equals(pc.getName(), target))) {
                                        bot.sendMessage(message, message.atAuthor() + "命名重复！！");
                                    } else {
                                        final String orig = curPC.getName();
                                        curPC.setName(target);
                                        try {
                                            player.save();
                                        } catch (IOException e) {
                                            LOG.error("无法保存玩家信息!", e);
                                        }
                                        bot.sendMessage(message, message.atAuthor() + "角色 " + orig + " 已更名为 " + target + " 。");
                                    }
                                }
                            }
                        } else {
                            if (player != null) {
                                final String orig = args[2];
                                final String target = args[3];

                                player.getCharacters().stream().filter(pc -> Objects.equals(pc.getName(), orig)).findFirst().ifPresentOrElse(pc -> {
                                    pc.setName(target);
                                    try {
                                        player.save();
                                    } catch (IOException e) {
                                        LOG.error("无法保存玩家信息!", e);
                                    }
                                    bot.sendMessage(message, message.atAuthor() + "角色 " + orig + " 已更名为 " + target + " 。");
                                }, () -> bot.sendMessage(message, message.atAuthor() + "角色 " + orig + " 未找到"));
                            }
                        }
                    }
                    case "delete" -> {
                        if (args.length != 3) {
                            bot.sendMessage(message, message.atAuthor() + "指令参数错误~");
                            return;
                        }

                        if (player.getCurrentCharacter() != null && player.getCurrentCharacter().getName().equals(args[2])) {
                            bot.sendMessage(message, message.atAuthor() + "不能删除已选择的角色 /_ \\");
                            return;
                        }

                        player.getCharacters().stream().filter(pc -> Objects.equals(pc.getName(), args[2])).findFirst().ifPresentOrElse(pc -> {
                            if (player.getCharacters().remove(pc)) {
                                bot.sendMessage(message, message.atAuthor() + "角色 " + args[2] + " 已删除。");
                            } else {
                                bot.sendMessage(message, message.atAuthor() + "角色 " + args[2] + " 删除失败!?");
                            }
                            try {
                                player.save();
                            } catch (IOException e) {
                                LOG.error("无法保存玩家信息!", e);
                            }
                        }, () -> bot.sendMessage(message, message.atAuthor() + "角色 " + args[2] + " 未找到"));
                    }
                    default -> bot.sendMessage(message, "未知的指令 " + args[1]);
                }
            }
            case ".attr" -> {
                if (args.length == 1 || (args.length != 2 && args.length % 2 == 0)) {
                    bot.sendMessage(message, message.atAuthor() + "指令参数错误~");
                    return;
                }
                reloadPlayer(universalID);
                players.putIfAbsent(universalID, new Player(universalID, message.getAuthor().getUsername()));
                final Player player = players.get(universalID);

                final PlayerCharacter curPC = player.getCurrentCharacter();
                if (curPC == null) {
                    bot.sendMessage(message, message.atAuthor() + "未选择角色😰");
                    return;
                }

                if (args.length == 2) {
                    final String attrStr = args[1];
                    if (!curPC.hasAttr(attrStr)) {
                        bot.sendMessage(message, message.atAuthor() + "找不到 " + curPC.getName() + " 的 " + attrStr + " 属性!");
                    } else {
                        bot.sendMessage(message, message.atAuthor() + "\n" + curPC.getName() + " 的 " + attrStr + " 为 " + curPC.getAttr(attrStr));
                    }
                    return;
                }

                final StringBuilder reply = new StringBuilder();
                reply.append(message.atAuthor()).append(curPC.getName()).append("属性更改：\n");

                for (int i = 1; i < args.length; i += 2) {
                    final String attrName = args[i];
                    final String op = args[i + 1];

                    reply.append("属性").append(attrName).append(" ");

                    if (op.startsWith("-") || op.startsWith("+")) {
                        if (!curPC.hasAttr(attrName)) {
                            reply.append("更改失败：找不到属性\n");
                            continue;
                        }

                        if (isDigit(op)) {
                            final int mod = Integer.parseInt(op);
                            final int orig = curPC.getAttr(attrName);

                            curPC.modAttr(attrName, mod);

                            reply.append("更改成功：").append(orig).append(op).append(" = ").append(curPC.getAttr(attrName)).append("\n");
                        } else {
                            reply.append("更改失败：不是有效操作\n");
                        }
                    } else if (op.startsWith("=")) {
                        if (isDigit(op.substring(1))) {
                            final int value = Integer.parseInt(op.substring(1));
                            String orig = "?";
                            if (curPC.hasAttr(attrName)) {
                                orig = String.valueOf(curPC.getAttr(attrName));
                            }
                            curPC.setAttr(attrName, value);
                            reply.append("设置成功：").append(orig).append(" -> ").append(curPC.getAttr(attrName)).append("\n");
                        } else {
                            reply.append("设置失败：不是有效操作\n");
                        }
                    }
                }

                try {
                    player.save();
                } catch (IOException e) {
                    LOG.error("无法保存玩家信息", e);
                }

                bot.sendMessage(message, reply.toString());
            }
            case ".r" -> {
                final StringBuilder sb = new StringBuilder();
                sb.append(message.atAuthor());
                if (args.length == 1) {
                    bot.sendMessage(message, sb.append("指令参数错误~").toString());
                    return;
                }

                if (!DiceExpr.isValid(args[1])) {
                    bot.sendMessage(message, sb.append("骰子表达式有误qwq").toString());
                    return;
                }

                final DiceExpr diceExpr = DiceExpr.parse(args[1]);
                final DiceResult result = new Dice().roll(diceExpr);

                reloadPlayer(universalID);
                final Player player = players.get(universalID);

                if (player != null && player.getCurrentCharacter() != null) {
                    sb.append(" ").append(player.getCurrentCharacter().getName()).append(" ");
                }

                if (args.length >= 3) {
                    final String reason = message.getContent().substring(message.getContent().indexOf(args[1]) + args[1].length());
                    bot.sendMessage(message, sb.append("投掷 ").append(reason).append(":").append(diceExpr).append("=").append(result.toString()).append("=").append(result.total()).toString());
                    if (player != null) {
                        logEntries.attach(new RollLogEntry(message.getContent(), message.getAuthor().getUsername(), player.getCurrentCharacter().getName(),
                                message.getTimestamp(), reason, args[1], String.valueOf(result.total()), null));
                    }
                } else {
                    bot.sendMessage(message, sb.append("投掷").append(":").append(diceExpr).append("=").append(result.toString()).append("=").append(result.total()).toString());
                    if (player != null) {
                        logEntries.attach(new RollLogEntry(message.getContent(), message.getAuthor().getUsername(), player.getCurrentCharacter().getName(),
                                message.getTimestamp(), "", args[1], String.valueOf(result.total()), null));
                    }
                }
            }
            case ".rc" -> {
                if (args.length != 2) {
                    bot.sendMessage(message, message.atAuthor() + "指令参数错误~");
                    return;
                }

                reloadPlayer(universalID);
                final Player player = players.get(universalID);

                if (player == null || player.getCurrentCharacter() == null) {
                    bot.sendMessage(message, message.atAuthor() + "未选择角色~");
                    return;
                }

                final PlayerCharacter pc = player.getCurrentCharacter();

                final String attrName = PlayerCharacter.getStandardName(args[1]);
                if (!pc.hasAttr(attrName)) {
                    bot.sendMessage(message, message.atAuthor() + "找不到属性 " + attrName + " ~");
                    return;
                }

                final StringBuilder reply = new StringBuilder();
                reply.append(pc.getName()).append(" 投掷 ").append(attrName).append(" 1d100=");

                final int result = new Dice().roll(DiceExpr.CHECK).total();
                final double value = pc.getAttr(attrName);

                reply.append(result).append("/").append((int) value).append(",");

                judgeCheckResult(message, reply, result, value);

                logEntries.attach(new CheckLogEntry(message.getContent(), message.getAuthor().getUsername(), player.getCurrentCharacter().getName(),
                        message.getTimestamp(), (int) value, result, attrName, null));
            }
            case ".rcpp" -> {
                if (args.length != 2) {
                    bot.sendMessage(message, message.atAuthor() + "指令参数错误~");
                    return;
                }

                reloadPlayer(universalID);
                final Player player = players.get(universalID);

                if (player == null || player.getCurrentCharacter() == null) {
                    bot.sendMessage(message, message.atAuthor() + "未选择角色~");
                    return;
                }

                final PlayerCharacter pc = player.getCurrentCharacter();

                final String attrName = PlayerCharacter.getStandardName(args[1]);
                if (!pc.hasAttr(attrName)) {
                    bot.sendMessage(message, message.atAuthor() + "找不到属性 " + attrName + " ~");
                    return;
                }
                final StringBuilder reply = new StringBuilder();
                reply.append(pc.getName()).append(" 投掷 ").append(attrName).append(" 1d100=");

                int xt = new Dice().roll(DiceExpr.TEN).total();
                int at = (new Dice().roll(DiceExpr.TEN).total() - 1) * 10;
                int bt = (new Dice().roll(DiceExpr.TEN).total() - 1) * 10;
                int ct = (new Dice().roll(DiceExpr.TEN).total() - 1) * 10;

                int a;
                int b;
                int c;

                if (at == 0 && xt == 0) {
                    a = 100;
                } else {
                    a = at + xt;
                }

                if (bt == 0 && xt == 0) {
                    b = 100;
                } else {
                    b = bt + xt;
                }

                if (ct == 0 && xt == 0) {
                    c = 100;
                } else {
                    c = ct + xt;
                }

                final int result = Math.max(a, Math.max(b, c));
                final double value = pc.getAttr(attrName);

                String mod = "[" + a +
                        "," + b + "," + c +
                        "]";
                reply.append(result).append("/").append((int) value).append(mod).append(",");

                judgeCheckResult(message, reply, result, value);

                logEntries.attach(new CheckLogEntry(message.getContent(), message.getAuthor().getUsername(), player.getCurrentCharacter().getName(),
                        message.getTimestamp(), (int) value, result, attrName, "惩罚骰" + mod));
            }
            case ".rcp" -> {
                if (args.length != 2) {
                    bot.sendMessage(message, message.atAuthor() + "指令参数错误~");
                    return;
                }

                reloadPlayer(universalID);
                final Player player = players.get(universalID);

                if (player == null || player.getCurrentCharacter() == null) {
                    bot.sendMessage(message, message.atAuthor() + "未选择角色~");
                    return;
                }

                final PlayerCharacter pc = player.getCurrentCharacter();

                final String attrName = PlayerCharacter.getStandardName(args[1]);
                if (!pc.hasAttr(attrName)) {
                    bot.sendMessage(message, message.atAuthor() + "找不到属性 " + attrName + " ~");
                    return;
                }

                final StringBuilder reply = new StringBuilder();
                reply.append(pc.getName()).append(" 投掷 ").append(attrName).append(" 1d100=");

                int xt = new Dice().roll(DiceExpr.TEN).total();
                int at = (new Dice().roll(DiceExpr.TEN).total() - 1) * 10;
                int bt = (new Dice().roll(DiceExpr.TEN).total() - 1) * 10;

                int a;
                int b;

                if (at == 0 && xt == 0) {
                    a = 100;
                } else {
                    a = at + xt;
                }

                if (bt == 0 && xt == 0) {
                    b = 100;
                } else {
                    b = bt + xt;
                }

                final int result = Math.max(a, b);
                final double value = pc.getAttr(attrName);

                final StringBuilder mod = new StringBuilder();
                mod.append("[").append(a).append(",").append(b).append("]");

                reply.append(result).append("/").append((int) value).append(mod).append(",");

                judgeCheckResult(message, reply, result, value);

                logEntries.attach(new CheckLogEntry(message.getContent(), message.getAuthor().getUsername(), player.getCurrentCharacter().getName(),
                        message.getTimestamp(), (int) value, result, attrName, "惩罚骰" + mod));
            }
            case ".rcb" -> {
                if (args.length != 2) {
                    bot.sendMessage(message, message.atAuthor() + "指令参数错误~");
                    return;
                }

                reloadPlayer(universalID);
                final Player player = players.get(universalID);

                if (player == null || player.getCurrentCharacter() == null) {
                    bot.sendMessage(message, message.atAuthor() + "未选择角色~");
                    return;
                }

                final PlayerCharacter pc = player.getCurrentCharacter();

                final String attrName = PlayerCharacter.getStandardName(args[1]);
                if (!pc.hasAttr(attrName)) {
                    bot.sendMessage(message, message.atAuthor() + "找不到属性 " + attrName + " ~");
                    return;
                }

                final StringBuilder reply = new StringBuilder();
                reply.append(pc.getName()).append(" 投掷 ").append(attrName).append(" 1d100=");

                int xt = new Dice().roll(DiceExpr.TEN).total();
                int at = (new Dice().roll(DiceExpr.TEN).total() - 1) * 10;
                int bt = (new Dice().roll(DiceExpr.TEN).total() - 1) * 10;

                int a;
                int b;

                if (at == 0 && xt == 0) {
                    a = 100;
                } else {
                    a = at + xt;
                }

                if (bt == 0 && xt == 0) {
                    b = 100;
                } else {
                    b = bt + xt;
                }

                final int result = Math.min(a, b);
                final double value = pc.getAttr(attrName);

                final StringBuilder mod = new StringBuilder();
                mod.append("[").append(a).append(",").append(b).append("]");

                reply.append(result).append("/").append((int) value).append(mod).append(",");

                judgeCheckResult(message, reply, result, value);
                logEntries.attach(new CheckLogEntry(message.getContent(), message.getAuthor().getUsername(), player.getCurrentCharacter().getName(),
                        message.getTimestamp(), (int) value, result, attrName, "奖励骰" + mod));

            }
            case ".rcbb" -> {
                if (args.length != 2) {
                    bot.sendMessage(message, message.atAuthor() + "指令参数错误~");
                    return;
                }

                reloadPlayer(universalID);
                final Player player = players.get(universalID);

                if (player == null || player.getCurrentCharacter() == null) {
                    bot.sendMessage(message, message.atAuthor() + "未选择角色~");
                    return;
                }

                final PlayerCharacter pc = player.getCurrentCharacter();

                final String attrName = PlayerCharacter.getStandardName(args[1]);
                if (!pc.hasAttr(attrName)) {
                    bot.sendMessage(message, message.atAuthor() + "找不到属性 " + attrName + " ~");
                    return;
                }

                final StringBuilder reply = new StringBuilder();
                reply.append(pc.getName()).append(" 投掷 ").append(attrName).append(" 1d100=");

                int xt = new Dice().roll(DiceExpr.TEN).total();
                int at = (new Dice().roll(DiceExpr.TEN).total() - 1) * 10;
                int bt = (new Dice().roll(DiceExpr.TEN).total() - 1) * 10;
                int ct = (new Dice().roll(DiceExpr.TEN).total() - 1) * 10;

                int a;
                int b;
                int c;

                if (at == 0 && xt == 0) {
                    a = 100;
                } else {
                    a = at + xt;
                }

                if (bt == 0 && xt == 0) {
                    b = 100;
                } else {
                    b = bt + xt;
                }

                if (ct == 0 && xt == 0) {
                    c = 100;
                } else {
                    c = ct + xt;
                }

                final int result = Math.min(a, Math.min(b, c));
                final double value = pc.getAttr(attrName);

                String mod = "[" + a + "," + b + "," + c + "]";

                reply.append(result).append("/").append((int) value).append(mod).append(",");

                judgeCheckResult(message, reply, result, value);
                logEntries.attach(new CheckLogEntry(message.getContent(), message.getAuthor().getUsername(), player.getCurrentCharacter().getName(),
                        message.getTimestamp(), (int) value, result, attrName, "奖励骰" + mod));
            }
            case ".sc" -> {
                if (args.length != 2 || args[1].split("/").length != 2) {
                    bot.sendMessage(message, message.atAuthor() + "指令参数错误~");
                    return;
                }

                final DiceExpr success = DiceExpr.parse(args[1].split("/")[0]);
                final DiceExpr fail = DiceExpr.parse(args[1].split("/")[1]);

                final Player player = players.get(universalID);

                if (player == null || player.getCurrentCharacter() == null) {
                    bot.sendMessage(message, message.atAuthor() + "未选择角色~");
                    return;
                }

                final StringBuilder reply = new StringBuilder();
                final PlayerCharacter pc = player.getCurrentCharacter();

                reply.append(pc.getName()).append(" 进行理智检定 1d100=");

                int san = pc.getAttr("理智");
                final int origSan = san;
                final int roll = new Random().nextInt(100) + 1;

                reply.append(roll).append("/").append(san).append(" ");

                final int total;
                final String str;
                if (roll <= san) {
                    reply.append("成功！");
                    total = new Dice().roll(success).total();
                    str = success.toString();
                } else {
                    reply.append("失败！");
                    total = new Dice().roll(fail).total();
                    str = fail.toString();
                }
                san -= total;
                reply.append("损失 ").append(str).append("=").append(total).append(" 点理智。当前理智为 ").append(san).append(" 。");

                if (pc.hasAttr("理智")) {
                    pc.setAttr("理智", san);
                }

                if (san <= 0) {
                    reply.append("陷入永久性疯狂！");
                } else if (total > 5) {
                    reply.append("陷入不定性疯狂！");
                }

                try {
                    player.save();
                } catch (IOException e) {
                    LOG.error("无法保存PL信息", e);
                }

                bot.sendMessage(message, reply.toString());
                refreshPlayerNickname(message);

                logEntries.attach(new CheckLogEntry(message.getContent(), message.getAuthor().getUsername(), player.getCurrentCharacter().getName(),
                        message.getTimestamp(), origSan, roll, "理智", null));

                logEntries.attach(new RollLogEntry(message.getContent(), message.getAuthor().getUsername(), player.getCurrentCharacter().getName(),
                        message.getTimestamp(), "损失理智", str, String.valueOf(total), null));
            }
        }
    }

    private void judgeCheckResult(Message message, StringBuilder reply, int result, double value) {
        if (result == 1) {
            reply.append("大成功！！！");
            reply.append(comments[0][new Random().nextInt(comments[0].length)]);
        } else if (result <= Math.floor(value / 5.0)) {
            reply.append("极难成功！！");
            reply.append(comments[1][new Random().nextInt(comments[1].length)]);
        } else if (result <= Math.floor(value / 2.0)) {
            reply.append("困难成功！");
            reply.append(comments[2][new Random().nextInt(comments[2].length)]);
        } else if (result <= Math.floor(value)) {
            reply.append("成功！");
            reply.append(comments[3][new Random().nextInt(comments[3].length)]);
        } else {
            if ((value <= 50 && result >= 96) || (value > 50 && result >= 100)) {
                reply.append("大失败！！！");
                reply.append(comments[5][new Random().nextInt(comments[5].length)]);
            } else {
                reply.append("失败！");
                reply.append(comments[4][new Random().nextInt(comments[4].length)]);
            }
        }

        bot.sendMessage(message, reply.toString());
    }

    private void reloadPlayer(UUID universalID) {
        try {
            final Player load = Player.load(universalID);
            if (load != null) {
                players.put(universalID, load);
            }
        } catch (IOException e) {
            LOG.warn("读取玩家信息失败", e);
        }
    }

    private void refreshPlayerNickname(Message msg) {
        final Player player = players.get(msg.getAuthor().getID().asUuid());
        if (player == null || player.getCurrentCharacter() == null) return;
        final PlayerCharacter pc = player.getCurrentCharacter();

        bot.changeNickname(msg.getAuthor().getID(), pc.buildName());
    }
}
