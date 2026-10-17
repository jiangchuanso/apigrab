package com.gitee.apigrab.core.http;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.XmlUtil;
import lombok.extern.slf4j.Slf4j;
import com.jayway.jsonpath.JsonPath;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.RespType;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class HttpResponseParser {

    /**
     * 归一化标量数组元素时使用的固定键名：列表响应为 {@code [1,2,3]} / {@code ["a","b"]}
     * 这类标量/字符串数组时，每个元素被包装为 {@code {"_value": 元素}}，
     * 使流水线记录始终是键值映射（design C2）。详情补全据此以 {@code keyField=#this} 取键值。
     */
    public static final String SCALAR_VALUE_KEY = "_value";

    public List<Map<String, Object>> parse(String responseBody, ApiConfigEntity config) {
        if (config.getEnableMultilines() != null && config.getEnableMultilines() == 1) {
            return parseNdjson(responseBody, config);
        } else {
            if (config.getRespType() == RespType.XML) {
                return parseXml(responseBody, config);
            }
            return parseJson(responseBody, config);
        }
    }

    /**
     * Parse newline-delimited JSON/XML (NDJSON/NDXML): one record per non-empty line (design M6).
     */
    private List<Map<String, Object>> parseNdjson(String body, ApiConfigEntity config) {
        List<Map<String, Object>> records = new ArrayList<>();
        for (String line : body.split("\\r?\\n")) {
            if (line.trim().isEmpty()) {
                continue;
            }
            if (config.getRespType() == RespType.XML) {
                records.addAll(parseXml(line, config));
            } else {
                records.addAll(parseJson(line, config));
            }
        }
        return records;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJson(String responseBody, ApiConfigEntity config) {
        String dataPath = StrUtil.trimToNull(config.getDataPath());
        Object data;

        if (dataPath != null) {
            try {
                data = JsonPath.read(responseBody, dataPath);
            } catch (Exception e) {
                throw new RuntimeException(
                        "路径解析失败 (JSONPath=" + dataPath + ", 响应格式=JSON): " + e.getMessage(), e);
            }
        } else {
            data = JsonPath.read(responseBody, "$");
        }

        if (data instanceof List) {
            return normalizeList((List<?>) data);
        } else if (data instanceof Map) {
            List<Map<String, Object>> result = new ArrayList<>();
            result.add((Map<String, Object>) data);
            return result;
        } else {
            log.warn("Unexpected data type: {}", java.util.Optional.ofNullable(data).map(d -> d.getClass().getName()).orElse("null"));
            return new ArrayList<>();
        }
    }

    /**
     * 将定位到的集合归一化为记录列表：标量/字符串元素包装为单键记录，Map 元素原样保留。
     *
     * <p>为兼容既有行为（见 {@code testArrayExpansionWithoutTrailingWildcardYieldsNestedArrays}），
     * 非 Map 且非 List 的标量元素才被包装；嵌套数组元素仍原样返回。键名见 {@link #SCALAR_VALUE_KEY}。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Map<String, Object>> normalizeList(List<?> raw) {
        List records = new ArrayList(raw.size());
        boolean wrapped = false;
        for (Object element : raw) {
            if (element instanceof Map || element instanceof List) {
                records.add(element);
            } else {
                Map<String, Object> record = new LinkedHashMap<>();
                record.put(SCALAR_VALUE_KEY, element);
                records.add(record);
                wrapped = true;
            }
        }
        if (wrapped) {
            log.debug("Normalized {} scalar element(s) into '{}' records", raw.size(), SCALAR_VALUE_KEY);
        }
        return records;
    }

    /**
     * Parse an XML response: select record nodes via XPath ({@code dataPath}) and
     * convert each node to a Map. Convention: child element name -> value,
     * repeated child elements -> List, attribute -> {@code @name}, text -> {@code #text}.
     * Empty dataPath falls back to the document root as a single record.
     */
    private List<Map<String, Object>> parseXml(String xml, ApiConfigEntity config) {
        Document doc = XmlUtil.parseXml(xml);
        String xpath = StrUtil.trimToNull(config.getDataPath());
        List<Map<String, Object>> records = new ArrayList<>();

        if (xpath == null) {
            records.add(elementToMap(doc.getDocumentElement()));
            return records;
        }

        NodeList nodes;
        try {
            nodes = XmlUtil.getNodeListByXPath(xpath, doc);
        } catch (Exception e) {
            throw new RuntimeException(
                    "路径解析失败 (XPath=" + xpath + ", 响应格式=XML): " + e.getMessage(), e);
        }

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                records.add(elementToMap((Element) node));
            }
        }
        return records;
    }

    private Map<String, Object> elementToMap(Element element) {
        Map<String, Object> map = new LinkedHashMap<>();

        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            map.put("@" + attr.getNodeName(), attr.getNodeValue());
        }

        Map<String, List<Element>> childGroups = new LinkedHashMap<>();
        StringBuilder text = new StringBuilder();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                childGroups.computeIfAbsent(child.getNodeName(), k -> new ArrayList<>()).add((Element) child);
            } else if (isText(child)) {
                text.append(child.getNodeValue());
            }
        }

        for (Map.Entry<String, List<Element>> entry : childGroups.entrySet()) {
            List<Element> group = entry.getValue();
            if (group.size() == 1) {
                map.put(entry.getKey(), elementValue(group.get(0)));
            } else {
                List<Object> values = new ArrayList<>();
                for (Element child : group) {
                    values.add(elementValue(child));
                }
                map.put(entry.getKey(), values);
            }
        }

        String trimmed = text.toString().trim();
        if (!trimmed.isEmpty()) {
            map.put("#text", trimmed);
        }
        return map;
    }

    private Object elementValue(Element element) {
        boolean hasChildElements = false;
        StringBuilder text = new StringBuilder();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                hasChildElements = true;
            } else if (isText(child)) {
                text.append(child.getNodeValue());
            }
        }
        if (hasChildElements || element.hasAttributes()) {
            return elementToMap(element);
        }
        return text.toString().trim();
    }

    private boolean isText(Node node) {
        return node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE;
    }
}
