package com.petmgmt.ai.rag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class KnowledgeCatalog {
    private KnowledgeCatalog() {}

    public static List<KnowledgeChunk> load() {
        String path = "/knowledge/project-guide.md";
        try (var stream = KnowledgeCatalog.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("知识资料缺失: " + path);
            return parse("project-guide.md", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("无法读取知识资料", e);
        }
    }

    public static List<KnowledgeChunk> parse(String file, String markdown) {
        if (!file.matches("[a-zA-Z0-9._-]+\\.md")) throw new IllegalArgumentException("无效资料名");
        List<KnowledgeChunk> chunks = new ArrayList<>();
        String title = file;
        String section = null;
        StringBuilder body = new StringBuilder();
        for (String line : markdown.split("\\R")) {
            if (line.startsWith("# ")) {
                title = line.substring(2).trim();
            } else if (line.startsWith("## ")) {
                if (section != null) add(chunks, file, section, body.toString());
                section = line.substring(3).trim();
                body.setLength(0);
            } else if (section != null && !line.isBlank()) {
                body.append(line.trim()).append(' ');
            }
        }
        if (section != null) add(chunks, file, section, body.toString());
        if (chunks.isEmpty()) throw new IllegalArgumentException("知识资料无有效小节: " + title);
        return List.copyOf(chunks);
    }

    private static void add(List<KnowledgeChunk> chunks, String file, String section, String body) {
        String text = body.trim();
        if (section.isBlank() || text.isBlank()) return;
        int index = 0;
        while (index < text.length()) {
            int end = Math.min(index + 450, text.length());
            if (end < text.length()) {
                int space = text.lastIndexOf(' ', end);
                if (space > index + 220) end = space;
            }
            String suffix = index == 0 ? "" : "-" + (chunks.stream().filter(c -> c.id().startsWith(file + "#" + section)).count() + 1);
            chunks.add(new KnowledgeChunk(file + "#" + section + suffix, section, text.substring(index, end).trim()));
            index = end;
            while (index < text.length() && text.charAt(index) == ' ') index++;
        }
    }
}
