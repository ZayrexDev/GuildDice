package guilddice.bot;

import com.alibaba.fastjson2.JSONObject;
import guilddice.Main;
import guilddice.bot.api.msg.Message;
import guilddice.bot.logging.Log;
import guilddice.bot.logging.MsgLogEntry;
import guilddice.bot.logging.RollLogEntry;
import guilddice.util.MessageBuilder;
import guilddice.util.dice.Dice;
import guilddice.util.dice.expr.DiceExpr;
import guilddice.util.dice.result.DiceResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
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
    private final LinkedHashMap<String, Player> players = new LinkedHashMap<>();
    private final Logger LOG = LogManager.getLogger(ChannelSession.class);
    private boolean on = false;

    public ChannelSession(Bot bot, String channelId) {
        this.bot = bot;
        this.channelId = channelId;
        logEntries = new Log(channelId);
    }

    private static String attrToString(LinkedHashMap<String, Integer> attr) {
        final StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Map.Entry<String, Integer> en : attr.entrySet()) {
            if (i != 1 && i % 2 == 1) {
                sb.append("\n");
            }

            sb.append(en.getKey()).append(":").append(en.getValue());

            if (i % 2 != 0) {
                sb.append("\t\t");
            }

            i++;
        }

        return sb.toString();
    }

    public void handleMessage(Message message) {
        if (message.author().bot()) return;
        final String content = message.content();
        if (content.startsWith(".")) {
            handleCommand(message);
        } else {
            if (on) {
                final Player player = players.get(message.author().id());
                final String name;
                if (player != null && player.getCurrentCharacter() != null) {
                    name = player.getCurrentCharacter().getName();
                } else {
                    name = message.author().username();
                }
                logEntries.attach(new MsgLogEntry(message.content(), message.author().username(), name, message.timestamp()));
            }
        }
    }

    private void handleCommand(Message message) {
        final String[] args = message.content().split(" ");
        if (args.length == 0) return;

        if (args[0].equals(".inspect") && message.member().hasRole("4")) {
            bot.reply(message, JSONObject.from(message).toString());
            return;
        }

        if (!on && !args[0].equals(".switch")) {
            return;
        }

        switch (args[0]) {
            case ".switch" -> {
                if (!message.member().hasRole(Main.getConfig().getKpRoleId())) {
                    bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append("仅KP有权限使用此指令~").build());
                    return;
                }

                if (args.length != 2 || (!args[1].equals("on") && !args[1].equals("off"))) {
                    bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append("指令参数错误~").build());
                    return;
                }

                if (args[1].equals("on")) {
                    if (on) {
                        bot.reply(message, "Bot已经上线了呢~");
                    } else {
                        on = true;
                        bot.reply(message, "Bot已上线~\n书写你们的冒险叭ヾ(≧▽≦*)o");
                    }
                } else {
                    if (on) {
                        on = false;
                        bot.reply(message, "Bot已离线~\n冒险的篇章已然落下帷幕......");
                        try {
                            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd.HH_mm_ss");
                            final String format = simpleDateFormat.format(new Date());
                            final String path = "log-" + channelId + "-" + format + ".json";
                            logEntries.save(Path.of(path));
                            bot.reply(message, "日志文件成功保存至" + path);
                        } catch (IOException e) {
                            bot.reply(message, "保存日志文件失败！");
                            LOG.error("保存日志文件失败！", e);
                        }
                    }
                }
            }
            case ".pc" -> {
                players.putIfAbsent(message.author().id(), new Player(message.author().id(), message.author().username()));

                final Player player = players.get(message.author().id());

                if (args.length == 1 || (args.length == 2 && args[1].equals("list"))) {
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
                    bot.reply(message, sb.toString());
                } else if (args[1].equals("create")) {
                    if (args.length < 3) {
                        bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 指令参数错误~").build());
                        return;
                    }

                    final PlayerCharacter pc = new PlayerCharacter(args[2]);
                    for (int i = 3; i < args.length; i++) {
                        final String[] attr = args[i].split(":");
                        final String attrName = attr[0];
                        final Integer attrVal = Integer.valueOf(attr[1]);

                        pc.getAttr().put(attrName, attrVal);
                    }

                    player.getCharacters().add(pc);

                    bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append("角色").append(pc.getName()).append("创建成功！").build());
                } else if (args[1].equals("switch")) {
                    if (args.length != 3) {
                        bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 指令参数错误~").build());
                        return;
                    }

                    final String pcName = args[2];

                    final Optional<PlayerCharacter> any = player.getCharacters().stream().filter(pc -> Objects.equals(pc.getName(), pcName)).findAny();
                    any.ifPresentOrElse(pc -> {
                        player.setCurrentCharacter(pc);
                        bot.reply(message, MessageBuilder.newInstance().at(message.author().id())
                                .append(" 切换角色").append(pc.getName()).append("成功~").build());
                    }, () -> bot.reply(message, MessageBuilder.newInstance().at(message.author().id())
                            .append(" 无法找到角色 ").append(pcName).append(" ！").build()));
                } else if (args[1].equals("attr")) {
                    if (args.length < 3) {
                        bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 指令参数错误~").build());
                        return;
                    }

                    switch (args[2]) {
                        case "get" -> {
                            if (args.length == 3) {
                                if (player.getCurrentCharacter() != null) {
                                    bot.reply(message, player.getCurrentCharacter().getName() +
                                            "的属性\n" + attrToString(player.getCurrentCharacter().getAttr()));
                                } else {
                                    bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 未选择角色~").build());
                                }
                            } else {
                                final String pcName = args[3];
                                final Optional<PlayerCharacter> op = player.getCharacters().stream().filter(pc -> Objects.equals(pc.getName(), pcName)).findAny();
                                op.ifPresentOrElse(pc -> bot.reply(message, MessageBuilder.newInstance().at(message.author().id())
                                                .append(" ").append(pc.getName()).append("的属性：").append("\n").append(attrToString(pc.getAttr())).build())
                                        , () -> bot.reply(message, MessageBuilder.newInstance().at(message.author().id())
                                                .append(" 无法找到角色 ").append(pcName).append(" ！").build()));
                            }
                        }
                        case "set" -> {
                            if (args.length < 5 || args.length % 2 != 1) {
                                bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 指令参数错误~").build());
                                return;
                            }
                            final PlayerCharacter curPC = player.getCurrentCharacter();

                            if (curPC == null) {
                                bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 未选择角色~").build());
                                return;
                            }

                            final StringBuilder reply = new StringBuilder();
                            for (int i = 3; i < args.length; i += 2) {
                                if (!isDigit(args[i + 1])) {
                                    reply.append(args[i]).append("设置失败：非数字").append("\n");
                                } else {
                                    curPC.getAttr().put(args[i], Integer.valueOf(args[i + 1]));
                                    reply.append(args[i]).append("成功设置为：").append(args[i + 1]).append("\n");
                                }
                            }

                            bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append("\n").append(reply.toString()).build());
                        }
                        case "modify" -> {
                            if (args.length < 5 || args.length % 2 != 1) {
                                bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 指令参数错误~").build());
                                return;
                            }
                            final PlayerCharacter curPC = player.getCurrentCharacter();

                            if (curPC == null) {
                                bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 未选择角色~").build());
                                return;
                            }

                            final StringBuilder reply = new StringBuilder();
                            reply.append(curPC.getName()).append("属性更改：\n");
                            for (int i = 3; i < args.length; i += 2) {
                                if (!isDigit(args[i + 1])) {
                                    reply.append(args[i]).append("更改失败：非数字").append("\n");
                                } else if (!curPC.getAttr().containsKey(args[i])) {
                                    reply.append(args[i]).append("更改失败：找不到属性").append("\n");
                                } else {
                                    curPC.getAttr().put(args[i], curPC.getAttr().get(args[i]) + Integer.parseInt(args[i + 1]));
                                    reply.append(args[i]).append("成功更改为：").append(curPC.getAttr().get(args[i])).append("\n");
                                }
                            }

                            bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append("\n").append(reply.toString()).build());
                        }
                        default -> bot.reply(message, MessageBuilder.newInstance().at(message.author().id())
                                .append(" 找不到指令").append(args[2]).build());
                    }
                }
            }
            case ".r" -> {
                final MessageBuilder at = MessageBuilder.newInstance().at(message.author().id());
                if (args.length == 1) {
                    bot.reply(message, at.append("指令参数错误~").build());
                    return;
                }

                if (!DiceExpr.isValid(args[1])) {
                    bot.reply(message, at.append("骰子表达式有误qwq").build());
                    return;
                }

                final DiceExpr diceExpr = DiceExpr.parse(args[1]);
                final DiceResult result = new Dice().roll(diceExpr);

                final Player player = players.get(message.author().id());

                if (player != null && player.getCurrentCharacter() != null) {
                    at.append(" ").append(player.getCurrentCharacter().getName()).append(" ");
                }

                if (args.length >= 3) {
                    final String reason = message.content().substring(message.content().indexOf(args[1]) + args[1].length());
                    bot.reply(message, at.append("投掷 ").append(reason).append(":").append(diceExpr.toString()).append("=").append(result.toString()).append("=").append(String.valueOf(result.total())).build());
                    if (player != null) {
                        logEntries.attach(new RollLogEntry(message.content(), message.author().username(), player.getCurrentCharacter().getName(),
                                message.timestamp(), reason, args[1], String.valueOf(result.total()), null));
                    }
                } else {
                    bot.reply(message, at.append("投掷").append(":").append(diceExpr.toString()).append("=").append(result.toString()).append("=").append(String.valueOf(result.total())).build());
                    if (player != null) {
                        logEntries.attach(new RollLogEntry(message.content(), message.author().username(), player.getCurrentCharacter().getName(),
                                message.timestamp(), "", args[1], String.valueOf(result.total()), null));
                    }
                }
            }
            case ".rc" -> {
                if (args.length != 2) {
                    bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 指令参数错误~").build());
                    return;
                }

                final Player player = players.get(message.author().id());

                if (player == null || player.getCurrentCharacter() == null) {
                    bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 未选择角色~").build());
                    return;
                }

                final PlayerCharacter pc = player.getCurrentCharacter();

                if (!pc.getAttr().containsKey(args[1])) {
                    bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 找不到属性").append(args[1]).append("~").build());
                    return;
                }

                final StringBuilder reply = new StringBuilder();
                reply.append(pc.getName()).append("投掷").append(args[1]).append("1d100=");

                final int result = new Dice().roll(DiceExpr.CHECK).total();
                final double value = pc.getAttr().get(args[1]);

                reply.append(result).append("/").append((int) value).append(",");

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

                bot.reply(message, reply.toString());

                logEntries.attach(new RollLogEntry(message.content(), message.author().username(), player.getCurrentCharacter().getName(),
                        message.timestamp(), args[1], "1d100", String.valueOf(result), null));
            }
            case ".rcpp" -> {
                if (args.length != 2) {
                    bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 指令参数错误~").build());
                    return;
                }

                final Player player = players.get(message.author().id());

                if (player == null || player.getCurrentCharacter() == null) {
                    bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 未选择角色~").build());
                    return;
                }

                final PlayerCharacter pc = player.getCurrentCharacter();

                if (!pc.getAttr().containsKey(args[1])) {
                    bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 找不到属性").append(args[1]).append("~").build());
                    return;
                }

                final StringBuilder reply = new StringBuilder();
                reply.append(pc.getName()).append("投掷").append(args[1]).append("1d100=");

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
                final double value = pc.getAttr().get(args[1]);

                final StringBuilder mod = new StringBuilder();
                mod.append("[").append(a)
                        .append(",").append(b).append(",").append(c)
                        .append("]");
                reply.append(result).append("/").append((int) value).append(mod).append(",");

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

                bot.reply(message, reply.toString());

                logEntries.attach(new RollLogEntry(message.content(), message.author().username(), player.getCurrentCharacter().getName(),
                        message.timestamp(), args[1], "1d100", String.valueOf(result), "惩罚骰" + mod));
            }
            case ".rcp" -> {
                if (args.length != 2) {
                    bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 指令参数错误~").build());
                    return;
                }

                final Player player = players.get(message.author().id());

                if (player == null || player.getCurrentCharacter() == null) {
                    bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 未选择角色~").build());
                    return;
                }

                final PlayerCharacter pc = player.getCurrentCharacter();

                if (!pc.getAttr().containsKey(args[1])) {
                    bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 找不到属性").append(args[1]).append("~").build());
                    return;
                }

                final StringBuilder reply = new StringBuilder();
                reply.append(pc.getName()).append("投掷").append(args[1]).append("1d100=");

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
                final double value = pc.getAttr().get(args[1]);

                final StringBuilder mod = new StringBuilder();
                mod.append("[").append(a).append(",").append(b).append("]");

                reply.append(result).append("/").append((int) value).append(mod).append(",");

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

                bot.reply(message, reply.toString());

                logEntries.attach(new RollLogEntry(message.content(), message.author().username(), player.getCurrentCharacter().getName(),
                        message.timestamp(), args[1], "1d100", String.valueOf(result), "惩罚骰" + mod));
            }
            case ".rcb" -> {
                if (args.length != 2) {
                    bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 指令参数错误~").build());
                    return;
                }

                final Player player = players.get(message.author().id());

                if (player == null || player.getCurrentCharacter() == null) {
                    bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 未选择角色~").build());
                    return;
                }

                final PlayerCharacter pc = player.getCurrentCharacter();

                if (!pc.getAttr().containsKey(args[1])) {
                    bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 找不到属性").append(args[1]).append("~").build());
                    return;
                }

                final StringBuilder reply = new StringBuilder();
                reply.append(pc.getName()).append("投掷").append(args[1]).append("1d100=");

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
                final double value = pc.getAttr().get(args[1]);

                final StringBuilder mod = new StringBuilder();
                mod.append("[").append(a).append(",").append(b).append("]");

                reply.append(result).append("/").append((int) value).append(mod).append(",");

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

                bot.reply(message, reply.toString());
                logEntries.attach(new RollLogEntry(message.content(), message.author().username(), player.getCurrentCharacter().getName(),
                        message.timestamp(), args[1], "1d100", String.valueOf(result), "奖励骰" + mod));

            }
            case ".rcbb" -> {
                if (args.length != 2) {
                    bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 指令参数错误~").build());
                    return;
                }

                final Player player = players.get(message.author().id());

                if (player == null || player.getCurrentCharacter() == null) {
                    bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 未选择角色~").build());
                    return;
                }

                final PlayerCharacter pc = player.getCurrentCharacter();

                if (!pc.getAttr().containsKey(args[1])) {
                    bot.reply(message, MessageBuilder.newInstance().at(message.author().id()).append(" 找不到属性").append(args[1]).append("~").build());
                    return;
                }

                final StringBuilder reply = new StringBuilder();
                reply.append(pc.getName()).append("投掷").append(args[1]).append("1d100=");

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
                final double value = pc.getAttr().get(args[1]);

                String mod = "[" + a + "," + b + "," + c + "]";

                reply.append(result).append("/").append((int) value).append(mod).append(",");

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

                bot.reply(message, reply.toString());
                logEntries.attach(new RollLogEntry(message.content(), message.author().username(), player.getCurrentCharacter().getName(),
                        message.timestamp(), args[1], "1d100", String.valueOf(result), "奖励骰" + mod));

            }
        }
    }
}
