package fr.qgo.duckdbrestapi.service.defaultimpl.query;

import fr.qgo.duckdbrestapi.service.QueryBuilder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.stereotype.Service;
import org.sql2o.Connection;
import org.sql2o.Query;

import java.lang.reflect.Field;
import java.util.*;

import static org.apache.commons.lang3.ArrayUtils.contains;


@Slf4j
@Service
@AllArgsConstructor
public final class DefaultQueryBuilder implements QueryBuilder {
    public static final String QUERY_BUILDER_PRE_PARSE_CONFIG_KEY = "queryBuilder.preParse";
    private static final String[] PRE_PARSE_FUNC = {
            "read_parquet",
            "parquet_scan",
            "read_csv",
            "read_json"
    };

    @Override
    public Query prepareQuery(Connection connection, String queryStr, Object params, Map<String, Object> userQueryParams) {
        if (params != null && preParseOption(userQueryParams)) {
            List<PlaceholderPosition> placeholderPositions = preParse(queryStr);
            queryStr = doReplace(queryStr, placeholderPositions, params);
        }

        val query = connection.createQuery(queryStr, false);
        if (params == null) {
            return query;
        } else if (params instanceof Map<?, ?> payload) {
            Map<String, List<Integer>> paramNameToIdxMap = query.getParamNameToIdxMap();
            @SuppressWarnings("unchecked") val mPayload = ((Map<String, ?>) payload);
            mPayload.forEach((paramName, paramValue) -> {
                if (paramNameToIdxMap.containsKey(paramName)) {
                    query.addParameter(paramName, paramValue);
                } else {
                    log.debug("Ignore param {} with value {}", paramName, paramValue);
                }
            });
            return query;
        } else {
            return query.bind(params);
        }
    }

    @VisibleForTesting
    public String doReplace(String queryStr, @NotNull List<PlaceholderPosition> placeholderPositions, Object params) {
        if (placeholderPositions.isEmpty()) return queryStr;
        val sb = new StringBuilder(queryStr);
        Map<?, ?> mapParam = params instanceof Map<?, ?> ? (Map<?, ?>) params : null;
        for (PlaceholderPosition placeholderPosition : placeholderPositions) {
            val key = queryStr.substring(placeholderPosition.begin() + 1, placeholderPosition.end());
            Object value;
            if (mapParam != null) {
                value = mapParam.get(key);
            } else {
                value = tryGetObj(key, params);
            }
            if (value == null) {
                value = "__HIVE_DEFAULT_PARTITION__";
            } else if (value instanceof Collection<?> || value instanceof Map<?, ?> || value.getClass().isArray()) {
                throw new RuntimeException("Illegal type for " + key);
            }
            String strVal = Objects.toString(value);
            if (strVal.contains("/") || strVal.contains("\\") || strVal.contains("..")) {
                throw new RuntimeException("Illegal value for key " + key + " value: " + strVal);
            }

            sb.replace(placeholderPosition.begin(), placeholderPosition.end(), strVal);
        }

        return sb.toString();
    }

    private Object tryGetObj(String key, @NotNull Object params) {
        Class<?> clazz = params.getClass();
        try {
            Field declaredField = clazz.getDeclaredField(key);
            declaredField.setAccessible(true);
            return declaredField.get(params);
        } catch (Exception e) {
            throw new RuntimeException("Failed to find " + key + " from " + clazz);
        }
    }

    private static boolean preParseOption(Map<String, Object> userQueryParams) {
        if (userQueryParams == null) return false;

        Object preParseOptionObj = userQueryParams.getOrDefault(QUERY_BUILDER_PRE_PARSE_CONFIG_KEY, false);
        if (preParseOptionObj instanceof Boolean b) {
            return b;
        } else if (preParseOptionObj instanceof String s) {
            return "true".equalsIgnoreCase(s);
        }

        return false;
    }

    @VisibleForTesting
    public static @NotNull List<PlaceholderPosition> preParse(final String query) {
        val result = new ArrayList<PlaceholderPosition>();
        for (String func : PRE_PARSE_FUNC) {
            val r = parseQueryForFunc(query, func);
            result.addAll(r);
        }
        return result;
    }

    private static @NotNull List<PlaceholderPosition> parseQueryForFunc(String query, String func) {
        List<PlaceholderPosition> result = new ArrayList<>();
        int lookupIndex = 0;

        parse:
        while (true) {
            val funcIndex = query.indexOf(func, lookupIndex);
            int startParse = funcIndex + func.length();

            if (funcIndex >= 0) {
                lookupIndex = startParse;
                startParse = whileEmpty(query, startParse);
            } else {
                break;
            }

            if (startParse < query.length() && query.charAt(startParse) == '(') {
                startParse = whileEmpty(query, startParse + 1);
            } else {
                // Not what we expect, next
                continue;
            }

            boolean multiValue;
            if (startParse < query.length()) {
                char c = query.charAt(startParse);
                if (c == '\'') {
                    multiValue = false;
                } else if (c == '[') {
                    multiValue = true;
                } else {
                    break;
                }
            } else {
                break;
            }

            do {
                startParse += 1;
                if (multiValue) {
                    startParse = whileEmpty(query, startParse);
                    if (startParse >= query.length() || query.charAt(startParse) != '\'') {
                        break parse;
                    }
                    startParse += 1;
                }
                int beginPath = startParse;
                char c;
                while (startParse < query.length() && (c = query.charAt(startParse)) != '\'') {
                    if (c == '\\') { // Escape next char
                        startParse++;
                    }
                    startParse++;
                }
                if (startParse < query.length() && query.charAt(startParse) == '\'') {
                    val placeholderPositions = placeholderList(query, beginPath, startParse);
                    result.addAll(placeholderPositions);
                    startParse++;
                }
                startParse = whileEmpty(query, startParse);
            } while (startParse < query.length() && (multiValue && query.charAt(startParse) == ','));
        }

        result.sort(Comparator.comparing(PlaceholderPosition::begin).reversed());
        return result;
    }

    @VisibleForTesting
    public static List<PlaceholderPosition> placeholderList(final String query, final int beginPath, final int endPath) {
        val result = new ArrayList<PlaceholderPosition>();
        int beginSearch = beginPath;
        do {
            int placeholderStart = indexOf(query, new char[]{':'}, beginSearch, endPath);
            if (placeholderStart >= 0) {
                int placeholderEnd = indexOf(query, new char[]{':', '[', '/', ',', '{', '-', '\'', '*', '.', '\\'}, placeholderStart + 1, endPath);
                if (placeholderEnd >= 0) {
                    beginSearch = placeholderEnd;
                    if (placeholderEnd - placeholderStart == 1) {
                        // false positive the placeholder string can not be empty (':str' is valid but ':' is not) probably something like az:// => ':' or C:\\
                        continue;
                    }
                    result.add(new PlaceholderPosition(placeholderStart, placeholderEnd));
                    continue;
                }
            }
            return result;
        } while (true);
    }

    private static int indexOf(@NotNull String query, char[] c, int beginIndex, int endIndex) {
        endIndex = Integer.min(endIndex, query.length());
        while (beginIndex < endIndex) {
            if (contains(c, query.charAt(beginIndex))) {
                return beginIndex;
            }
            beginIndex++;
        }
        return -1;
    }

    private static int whileEmpty(@NotNull String query, int startParse) {
        while (startParse < query.length()) {
            char c = query.charAt(startParse);
            if (c != ' ' && c != '\n' && c != '\t') break;
            startParse++;
        }
        return startParse;
    }

    @VisibleForTesting
    public record PlaceholderPosition(int begin, int end) {
    }
}
