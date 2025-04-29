package com.dsm.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class JsonPlaceholderReplacerUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(JsonPlaceholderReplacerUtil.class);

    /**
     * 替换 JSON 字符串中的占位符
     * @param jsonStr 原始 JSON 字符串
     * @param replacements 替换映射，key 为占位符名称，value 为替换值
     * @return 替换后的 JSON 字符串
     */
    public static String replacePlaceholders(String jsonStr, Map<String, String> replacements) {
        try {
            logger.info("开始替换占位符");
            logger.info("原始 JSON: {}", jsonStr);
            logger.info("替换映射: {}", replacements);
            
            JsonNode rootNode = objectMapper.readTree(jsonStr);
            replacePlaceholdersInNode(rootNode, replacements);
            
            String result = objectMapper.writeValueAsString(rootNode);
            logger.info("替换后的 JSON: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("替换占位符时发生错误", e);
            throw new RuntimeException("Failed to replace placeholders in JSON", e);
        }
    }

    private static void replacePlaceholdersInNode(JsonNode node, Map<String, String> replacements) {
        if (node.isObject()) {
            logger.debug("处理对象节点");
            ObjectNode objectNode = (ObjectNode) node;
            // 创建一个新的 ObjectNode 来存储替换后的键值对
            ObjectNode newNode = objectMapper.createObjectNode();
            
            objectNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                
                // 处理键中的占位符
                if (key.contains("{{")) {
                    key = replacePlaceholdersInText(key, replacements);
                }
                
                // 处理值中的占位符
                if (value.isTextual()) {
                    String text = value.asText();
                    logger.debug("处理文本值: key={}, value={}", key, text);
                    if (text.contains("{{")) {
                        String replacedText = replacePlaceholdersInText(text, replacements);
                        logger.debug("替换后的文本: {}", replacedText);
                        newNode.put(key, replacedText);
                    } else {
                        newNode.set(key, value);
                    }
                } else {
                    replacePlaceholdersInNode(value, replacements);
                    newNode.set(key, value);
                }
            });
            
            // 用新的节点替换原节点
            if (node instanceof ObjectNode) {
                ((ObjectNode) node).removeAll();
                ((ObjectNode) node).setAll(newNode);
            }
        } else if (node.isArray()) {
            logger.debug("处理数组节点");
            ArrayNode arrayNode = (ArrayNode) node;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode element = arrayNode.get(i);
                if (element.isTextual()) {
                    String text = element.asText();
                    logger.debug("处理数组元素: index={}, value={}", i, text);
                    if (text.contains("{{")) {
                        String replacedText = replacePlaceholdersInText(text, replacements);
                        logger.debug("替换后的数组元素: {}", replacedText);
                        arrayNode.set(i, objectMapper.getNodeFactory().textNode(replacedText));
                    }
                } else {
                    replacePlaceholdersInNode(element, replacements);
                }
            }
        }
    }

    private static String replacePlaceholdersInText(String text, Map<String, String> replacements) {
        logger.debug("开始处理文本: {}", text);
        StringBuilder result = new StringBuilder(text);
        int startIndex;
        while ((startIndex = result.indexOf("{{")) != -1) {
            int endIndex = result.indexOf("}}", startIndex);
            if (endIndex == -1) break;

            String placeholder = result.substring(startIndex + 2, endIndex).trim();
            logger.debug("找到占位符: {}", placeholder);
            String replacement = replacements.getOrDefault(placeholder, "");
            logger.debug("替换值: {}", replacement);
            result.replace(startIndex, endIndex + 2, replacement);
        }
        logger.debug("处理后的文本: {}", result.toString());
        return result.toString();
    }
}