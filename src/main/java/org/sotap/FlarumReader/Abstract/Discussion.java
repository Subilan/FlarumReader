package org.sotap.FlarumReader.Abstract;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sotap.FlarumReader.Utils.Calendars;
import org.sotap.FlarumReader.Utils.Files;

import net.md_5.bungee.api.chat.BaseComponent;
import xyz.upperlevel.spigot.book.BookUtil;
import xyz.upperlevel.spigot.book.BookUtil.PageBuilder;
import xyz.upperlevel.spigot.book.BookUtil.TextBuilder;

public final class Discussion {
    public String id;
    public String title;
    public String author;
    public Date createTime;
    public List<Reply> replyList;
    public String content;
    public String site;

    public Discussion(JSONObject object) {
        JSONObject base = object.getJSONObject("data");
        JSONObject attr = base.getJSONObject("attributes");
        JSONArray included = object.getJSONArray("included");
        replyList = new ArrayList<>();
        this.id = base.getString("id");
        this.title = attr.getString("title");
        this.createTime = Calendars.parse(attr.getString("createdAt"), Calendars.UTC_FORMAT_PATTERN);
        JSONObject current;
        for (int i = 0; i < included.length(); i++) {
            current = included.getJSONObject(i);
            JSONObject currentAttr;
            if (current.getString("type").equals("posts")) {
                currentAttr = current.getJSONObject("attributes");
                if (currentAttr.has("isHidden")) {
                    if (currentAttr.getBoolean("isHidden")) {
                        if (i == 0) {
                            current.put("attributes", new JSONObject());
                            current.getJSONObject("attributes").put("content", "*内容已删除*");
                            current.getJSONObject("attributes").put("createdAt", currentAttr.getString("createdAt"));
                            replyList.add(new Reply(current));
                        }
                        continue;
                    }
                }
                if (currentAttr.getString("contentType").equals("comment")) {
                    replyList.add(new Reply(current));
                }
            }
        }
        this.content = replyList.get(0).content;
        this.author = replyList.get(0).author;
        this.site = Files.config.getString("site");
    }

    public PageBuilder getFirstPageBuilder() {
        return new BookUtil.PageBuilder()
                .add(BookUtil.TextBuilder.of(this.title).style(ChatColor.BOLD)
                        .onHover(BookUtil.HoverAction.showText("在浏览器中打开"))
                        .onClick(BookUtil.ClickAction.openUrl(site + "/d/" + this.id)).build())
                .newLine().add(BookUtil.TextBuilder.of("by " + this.author).color(ChatColor.GRAY).build()).newLine()
                .add(BookUtil.TextBuilder.of(Calendars.toString(this.createTime)).color(ChatColor.GRAY).build())
                .newLine().newLine();
    }

    public ItemStack getBook() {
        String finalContent = this.content;
        if (this.replyList.size() > 1) {
            finalContent += "\n\n---\n**" + (this.replyList.size() - 1) + " 条回复**\n---\n";
            Reply current;
            for (int i = 1; i < this.replyList.size(); i++) {
                current = replyList.get(i);
                finalContent += "\n\n&l&a" + i + "&r&r **" + current.author + "**\n" + current.content;
            }

            if (this.replyList.size() > 10) {
                finalContent += "\n\n*由于 API 限制，回复可能显示不完整。[点击前往原帖查看](" + site + "/d/" + this.id + ")*";
            }
        }
        Markdown md = new Markdown(finalContent);
        String[] chars = md.parse().split("(?!^)");
        PageBuilder current = getFirstPageBuilder();
        // bugnote
        // 第一页由于标题+作者+日期会占用 3~4 行（标题 1~2 行，作者 1 行，日期 1 行）所以正文从（抽象）第 6 行开始。
        // 如果设置小于 6 会导致标题占 2 行的帖子第一页内容少一行。2021/06/07
        int line = 6;
        int k = 0;
        double lineLimit = 100.0;
        TextBuilder tb;
        boolean bold = false;
        boolean underline = false;
        boolean strikethrough = false;
        boolean italic = false;
        boolean linkContent = false;
        boolean linkHref = false;
        boolean link = false;
        boolean green = false;
        boolean aqua = false;
        boolean yellow = false;
        List<String> currentLinkContent = new ArrayList<>();
        List<String> currentLinkHref = new ArrayList<>();
        List<BaseComponent[]> components = new ArrayList<>();
        for (int i = 0; i < chars.length; i++) {
            if (k == chars.length && line < 15) {
                components.add(current.build());
                break;
            }
            String c = chars[k];
            if (c.equals("&")) {
                String t = chars[k + 1];
                if (t.equals("l"))
                    bold = true;
                if (t.equals("m"))
                    strikethrough = true;
                if (t.equals("n"))
                    underline = true;
                if (t.equals("o"))
                    italic = true;
                if (t.equals("a"))
                    green = true;
                if (t.equals("b"))
                    aqua = true;
                if (t.equals("e"))
                    yellow = true;
                if (t.equals("r")) {
                    bold = false;
                    strikethrough = false;
                    underline = false;
                    italic = false;
                    green = false;
                    aqua = false;
                    yellow = false;
                    if (link) {
                        String currentLinkContentString = String.join("", currentLinkContent);
                        String currentLinkHrefString = String.join("", currentLinkHref);
                        for (String c_ : currentLinkContent) {
                            try {
                                tb = BookUtil.TextBuilder.of(c_).style(ChatColor.UNDERLINE)
                                        .onHover(BookUtil.HoverAction.showText("点击打开链接"))
                                        .onClick(BookUtil.ClickAction
                                                .openUrl(currentLinkHrefString.equals("url") ? currentLinkContentString
                                                        : currentLinkHrefString));
                            } catch (IllegalArgumentException e) {
                                // invalid URL
                                tb = BookUtil.TextBuilder.of(c_);
                            }
                            current.add(tb.build());
                            lineLimit -= Markdown.getCharPercentage(c_, true);
                            if (k + 1 < chars.length) {
                                if (lineLimit < Markdown.getCharPercentage(chars[k + 1], true)) {
                                    lineLimit = 100.0;
                                    current = current.newLine();
                                    line++;
                                }
                            } else {
                                if (line < 15) {
                                    components.add(current.build());
                                    break;
                                }
                            }
                            if (line == 15) {
                                lineLimit = 100.0;
                                line = 1;
                                components.add(current.build());
                                current = new BookUtil.PageBuilder();
                            }
                        }
                        link = false;
                        currentLinkContent.clear();
                        currentLinkHref.clear();
                    }
                }
                if (t.equals("L")) {
                    link = true;
                    linkContent = true;
                }

                // skip next character, as is already used in this context.
                k += 2;
                continue;
            }
            if (link) {
                k++;
                if (c.equals("|")) {
                    linkHref = true;
                    linkContent = false;
                    continue;
                }
                if (linkContent) {
                    currentLinkContent.add(c);
                }
                if (linkHref) {
                    currentLinkHref.add(c);
                }
                continue;
            }
            if (c.equals("\n")) {
                lineLimit = 100.0;
                line++;
                if (line == 15) {
                    lineLimit = 100.0;
                    line = 1;
                    components.add(current.build());
                    current = new BookUtil.PageBuilder();
                } else {
                    current = current.newLine();
                }
                k++;
                continue;
            }
            tb = BookUtil.TextBuilder.of(c);
            if (bold)
                tb = tb.style(ChatColor.BOLD);
            if (strikethrough)
                tb = tb.style(ChatColor.STRIKETHROUGH);
            if (underline)
                tb = tb.style(ChatColor.UNDERLINE);
            if (italic)
                tb = tb.style(ChatColor.ITALIC);
            if (green)
                tb = tb.color(ChatColor.GREEN);
            if (aqua)
                tb = tb.color(ChatColor.AQUA);
            if (yellow)
                tb = tb.color(ChatColor.YELLOW);

            current.add(tb.build());

            if (k + 1 < chars.length) {
                lineLimit -= Markdown.getCharPercentage(c, bold);
                if (lineLimit < Markdown.getCharPercentage(chars[k + 1], true)) {
                    lineLimit = 100.0;
                    current = current.newLine();
                    line++;
                }
                if (line == 15) {
                    lineLimit = 100.0;
                    line = 1;
                    components.add(current.build());
                    current = new BookUtil.PageBuilder();
                }
            } else {
                if (line < 15) {
                    components.add(current.build());
                    break;
                }
            }

            k++;
        }
        return BookUtil.writtenBook().pages(components).build();
    }
}

final class Markdown {
    public String input;
    public String output;
    public List<String> normReplacements;
    public List<Pattern> patterns;

    public Markdown(String input) {
        this.input = input;
        this.patterns = new ArrayList<Pattern>();
        this.normReplacements = new ArrayList<>();
        this.patterns.add(Pattern.compile("(?m)^#{1,6}(?!#)(.*?)"));
        this.patterns.add(Pattern.compile("(\\*\\*|__)(.*?)(\\*\\*|__)"));
        this.patterns.add(Pattern.compile("(\\*|_)(.*?)(\\*|_)"));
        this.patterns.add(Pattern.compile("~~(.*?)~~"));
        this.patterns.add(Pattern.compile("<u>(.*?)</u>"));
        this.patterns.add(Pattern.compile("`(.*?)`"));
        this.patterns.add(Pattern.compile("\\!\\[.*?\\]\\(.*\\)"));
        this.patterns.add(Pattern.compile("\\[(.*?)\\]\\((.*)\\)"));
        this.patterns
                .add(Pattern.compile("<(\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])>"));
        this.patterns.add(Pattern.compile("&emsp;"));
        this.patterns.add(Pattern.compile("&nbsp;"));
        this.patterns.add(Pattern.compile("(@\\w+#\\d+)(\\s|\n)"));
        this.patterns.add(Pattern.compile("(@\\w+)(\\s|\n)"));
        // this.patterns.add(Pattern.compile("(\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])"));
        normReplacements.add("&l$1&r");
        normReplacements.add("&l$2&r");
        normReplacements.add("&o$2&r");
        normReplacements.add("&m$1&r");
        normReplacements.add("&n$1&r");
        normReplacements.add("$1");
        normReplacements.add("[图片]");
        normReplacements.add("&L$1|$2&r");
        normReplacements.add("&L$1|$1&r");
        normReplacements.add("  ");
        normReplacements.add(" ");
        normReplacements.add("&a$1&r");
        normReplacements.add("&a$1&r");
        // normReplacements.add("&L$1|$1&r");
    }

    public String parse() {
        this.output = this.input;
        int i = 0;
        for (Pattern v : patterns) {
            this.output = v.matcher(this.output).replaceAll(this.normReplacements.get(i));
            i++;
        }
        return this.output;
    }

    public static int getCharType(String ch) {
        if (Pattern.compile("[\u4E00-\u9FA5]").matcher(ch).find()
                || Pattern.compile("(、|。|-|\\+|\\*|_|@)").matcher(ch).find())
            return 1;
        if (Pattern.compile("[A-Z#]").matcher(ch).find())
            return 2;
        if (Pattern.compile("[a-z]").matcher(ch).find())
            return 3;
        if (Pattern.compile("\\d").matcher(ch).find())
            return 4;
        if (Pattern.compile("\\p{P}").matcher(ch).find())
            return 5;
        if (Pattern.compile("\\p{Z}").matcher(ch).find())
            return 6;
        return 0;
    }

    public static double getCharPercentage(String ch, boolean bold) {
        int charType = getCharType(ch);
        double val;
        switch (charType) {
            case 1:
                val = 8.33;
                if (bold) {
                    val = val * 1.1;
                }
                break;

            case 2:
                val = 5.55;
                if (bold) {
                    val = val * 1.3;
                }
                break;

            case 3:
                val = 5.0;
                if (bold) {
                    val = val * 1.3;
                }
                break;

            case 4:
                val = 5.5;
                break;

            case 5:
                val = 1.75;
                break;

            case 6:
                val = 3.45;
                break;

            default:
                val = 7.0;
        }
        return val;
    }
}

final class Reply {
    public String content;
    public String author;
    public String id;
    public Date createTime;

    public Reply(JSONObject object) {
        JSONObject attr = object.getJSONObject("attributes");
        JSONObject relations = object.getJSONObject("relationships");
        this.id = object.getString("id");
        this.createTime = Calendars.parse(attr.getString("createdAt"), Calendars.UTC_FORMAT_PATTERN);
        this.content = attr.getString("content");
        this.author = Files.getUsernameById(relations.getJSONObject("user").getJSONObject("data").getString("id"));
    }
}
