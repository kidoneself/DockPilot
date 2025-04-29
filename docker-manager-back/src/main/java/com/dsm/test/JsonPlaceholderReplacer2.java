package com.dsm.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Map;

public class JsonPlaceholderReplacer2 {
    private final ObjectMapper objectMapper;

    public JsonPlaceholderReplacer2() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 替换 JSON 字符串中的占位符
     * @param jsonStr 原始 JSON 字符串
     * @param replacements 替换映射，key 为占位符名称，value 为替换值
     * @return 替换后的 JSON 字符串
     */
    public String replacePlaceholders(String jsonStr, Map<String, String> replacements) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonStr);
            replacePlaceholdersInNode(rootNode, replacements);
            return objectMapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to replace placeholders in JSON", e);
        }
    }

    private void replacePlaceholdersInNode(JsonNode node, Map<String, String> replacements) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if (value.isTextual()) {
                    String text = value.asText();
                    if (text.contains("{{")) {
                        String replacedText = replacePlaceholdersInText(text, replacements);
                        objectNode.put(entry.getKey(), replacedText);
                    }
                } else {
                    replacePlaceholdersInNode(value, replacements);
                }
            });
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode element = arrayNode.get(i);
                if (element.isTextual()) {
                    String text = element.asText();
                    if (text.contains("{{")) {
                        String replacedText = replacePlaceholdersInText(text, replacements);
                        arrayNode.set(i, replacedText);
                    }
                } else {
                    replacePlaceholdersInNode(element, replacements);
                }
            }
        }
    }

    private String replacePlaceholdersInText(String text, Map<String, String> replacements) {
        StringBuilder result = new StringBuilder(text);
        int startIndex;
        while ((startIndex = result.indexOf("{{")) != -1) {
            int endIndex = result.indexOf("}}", startIndex);
            if (endIndex == -1) break;
            
            String placeholder = result.substring(startIndex + 2, endIndex);
            String replacement = replacements.get(placeholder);
            if (replacement != null) {
                result.replace(startIndex, endIndex + 2, replacement);
            }
        }
        return result.toString();
    }
} 