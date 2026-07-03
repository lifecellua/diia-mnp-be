package com.lifecell.diia.be.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lifecell.diia.be.exception.GenericException;
import com.lifecell.diia.be.util.ResourceUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TemplateService {
    private static final String PATH_SEPARATOR = "/";
    private static final String VAR_PREFIX = "%%";
    private static final String VAR_POSTFIX = "%%";

    public enum Operation {
        KEEP, REMOVE
    }

    @Getter
    @NoArgsConstructor
    public static class TrailChunk implements Comparable<TrailChunk> {
        private String name;
        private Integer index;

        @Builder
        private TrailChunk(String name) {
            this.name = name;
            var value = name;
            if (name.startsWith(VAR_PREFIX) && name.endsWith(VAR_POSTFIX)) {
                value = name.substring(VAR_PREFIX.length(), name.length() - VAR_POSTFIX.length());
            }
            try {
                this.index = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                this.index = null;
            }
        }

        @Override
        public int compareTo(TrailChunk o) {
            if (this.index != null) {
                if (o.index != null) {
                    return this.index.compareTo(o.index);
                } else {
                    return -1;
                }
            } else {
                if (o.index != null) {
                    return 1;
                } else {
                    return name.compareTo(o.name);
                }
            }
        }
    }

    @Getter
    public static class Trail implements Comparable<Trail> {
        private boolean local;
        private String trace;
        private List<TrailChunk> chunk;

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public Trail(String trace) {
            this.trace = trace;
            if (trace.startsWith(PATH_SEPARATOR)) {
                var track = trace.substring(PATH_SEPARATOR.length());
                this.local = false;
                this.trace = track;
                this.chunk = Arrays.stream(track.split(PATH_SEPARATOR))
                        .map(TrailChunk::new)
                        .toList();
            } else if (trace.startsWith(VAR_PREFIX) && trace.endsWith(VAR_POSTFIX)) {
                var track = trace;
                this.local = true;
                this.trace = track;
                this.chunk = Collections.singletonList(TrailChunk.builder().name(track).build());
            } else {
                throw new GenericException("Invalid trace: {}", trace);
            }
        }

        @Override
        public int compareTo(Trail o) {
            var sz = Math.min(this.chunk.size(), o.chunk.size());
            for (var i = 0; i < sz; i++) {
                var c = this.chunk.get(i).compareTo(o.chunk.get(i));
                if (c != 0) {
                    return c;
                }
            }
            return Integer.compare(this.chunk.size(), o.chunk.size());
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class VarDef {
        @JsonProperty
        private Trail target;
        @JsonProperty
        private Integer header;
        @JsonProperty
        private Integer footer;
        @JsonProperty
        private Integer span;
        @JsonProperty
        private Map<String, VarDef> variables;
    }

    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class NodeDef {
        private String trace;
        private JsonNode node;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Target {
        private NodeDef root;
        private TrailChunk chunk;
        private NodeDef node;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ParamDef {
        private String name;
        private VarDef def;
        private Object value;
    }

    public static Map<String, Object> mapOf(Object... values) {
        if (values.length % 2 != 0) {
            throw new GenericException("Invalid number of arguments");
        }
        var map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) {
            var key = values[i];
            var value = values[i + 1];
            if (key instanceof String keyStr) {
                map.put(keyStr, value);
            } else {
                throw new GenericException("Invalid key type at {}", i);
            }
        }
        return map;
    }

    public static List<Object> listOf(Object... values) {
        return Arrays.asList(values);
    }

    private final ObjectMapper mapper;

    public TemplateService() {
        this.mapper = ResourceUtils.getDefaultObjectMapper();
    }

    public String generate(String name, Map<String, Object> params) {
            var template = ResourceUtils.loadFormJson(name);
            var spec = ResourceUtils.loadFormJson(name + ".spc");
            try {
                return generateRaw(template, spec, params);
            } catch (IOException e) {
                throw new GenericException("Can't generate from template", e);
            }
    }

    public String generateRaw(String template, String spec, Map<String, Object> params) throws IOException {
        var tree = mapper.readTree(template);
        var def = mapper.readValue(spec, VarDef.class);
        var target = Target.builder()
                .node(NodeDef.builder()
                        .trace("")
                        .node(tree)
                        .build())
                .build();
        applyMap(target, def, params);
        return mapper.writeValueAsString(tree);
    }

    private void applyMap(Target target, VarDef def, Map<String, Object> params) {
        if (params != null) {
            var pis = params.entrySet()
                    .stream()
                    .map(e -> {
                        var name = e.getKey();
                        var info = def.getVariables().get(name);
                        if (info == null) {
                            throw new GenericException("Template ({}): variable {} not found", target.getNode().getTrace(), name);
                        }
                        var value = e.getValue();
                        return ParamDef.builder()
                                .name(name)
                                .def(info)
                                .value(value)
                                .build();
                    })
                    .sorted(Comparator.comparing(ParamDef::getDef, Comparator.comparing(VarDef::getTarget, Comparator.reverseOrder())))
                    .toList();
            for (var entry : pis) {
                var varDef = entry.getDef();
                var varValue = entry.getValue();
                if (varDef.getTarget().isLocal()) {
                    applyLocalVariable(target, varDef, varValue);
                } else {
                    var tt = varDef.getTarget();
                    var tn = target.getNode();
                    var walk = walkNodes(tn, tt);
                    applyVariable(walk, varDef, varValue);
                }
            }
        }
    }

    private void applyList(Target target, VarDef spec, List<Object> values) {
        var jn = target.getNode().getNode();
        var begin = spec.getHeader() == null ? 0 : spec.getHeader();
        var end = spec.getFooter() == null ? 0 : spec.getFooter();
        var span = spec.getSpan() == null ? 1 : spec.getSpan();
        if (jn.isArray() && (jn instanceof ArrayNode array)) {
            if (jn.size() < (begin + end + span)) {
                throw new GenericException("Template ({}): invalid array size", target.getNode().getTrace());
            }
            if ((jn.size() - begin - end) % span != 0) {
                throw new GenericException("Template ({}): invalid array size", target.getNode().getTrace());
            }
            if (values.size() == 0) {
                for (var i = array.size() - 1 - end; i >= begin; i--) {
                    array.remove(i);
                }
            } else {
                {
                    var from = array.size() - 1 - end;
                    var to = begin + span - 1;
                    for (var i = from; i > to; i--) {
                        array.remove(i);
                    }
                }
                {
                    for (var i = 1; i < values.size(); i++) {
                        for (var j = 0; j < span; j++) {
                            array.insert(begin + j, array.get(begin + (j * span)).deepCopy());
                        }
                    }
                }
                {
                    for (var i = 0; i < values.size(); i++) {
                        var idx = begin + (i * span);
                        var jnn = array.get(idx);
                        var value = values.get(i);
                        var tt = Target.builder()
                                .root(target.getNode())
                                .chunk(TrailChunk.builder()
                                        .name(Integer.toString(idx))
                                        .build())
                                .node(NodeDef.builder()
                                        .trace(target.getNode().getTrace() + PATH_SEPARATOR + Integer.toString(idx))
                                        .node(jnn)
                                        .build())
                                .build();
                        applyVariable(tt, spec, value);
                    }
                }
            }
        } else {
            throw new GenericException("Template ({}): node is not an array", target.getNode().getTrace());
        }
    }

    private void applySimple(Target target, Object value) {
        var jn = target.getRoot().getNode();
        if (jn.isObject() && (jn instanceof ObjectNode object)) {
            if (target.getChunk().getName() == null) {
                throw new GenericException("Template ({}): object property name is null", target.getRoot().getTrace());
            }
            var name = target.getChunk().getName();
            if (value == null) {
                object.putNull(name);
            } else if (value instanceof Boolean val) {
                object.put(name, val);
            } else if (value instanceof Integer val) {
                object.put(name, val);
            } else if (value instanceof Long val) {
                object.put(name, val);
            } else if (value instanceof Float val) {
                object.put(name, val);
            } else if (value instanceof Double val) {
                object.put(name, val);
            } else if (value instanceof Operation operation) {
                if (operation == Operation.REMOVE) {
                    object.remove(name);
                }
            } else {
                object.put(name, value.toString());
            }
        } else if (jn.isArray() && (jn instanceof ArrayNode array)) {
            if (target.getChunk().getIndex() == null) {
                throw new GenericException("Template ({}): array index is null", target.getRoot().getTrace());
            }
            var idx = target.getChunk().getIndex().intValue();
            if (value == null) {
                array.setNull(idx);
            } else if (value instanceof Boolean val) {
                array.set(idx, val);
            } else if (value instanceof Integer val) {
                array.set(idx, val);
            } else if (value instanceof Long val) {
                array.set(idx, val);
            } else if (value instanceof Float val) {
                array.set(idx, val);
            } else if (value instanceof Double val) {
                array.set(idx, val);
            } else if (value instanceof Operation operation) {
                if (operation == Operation.REMOVE) {
                    array.remove(idx);
                }
            } else {
                array.set(idx, value.toString());
            }
        }
    }

    private void applyVariable(Target target, VarDef def, Object value) {
        if (value instanceof List<?> listValue) {
            applyList(target, def, (List<Object>) listValue);
        } else if (value instanceof Map<?,?> mapValue) {
            applyMap(target, def, (Map<String, Object>) mapValue);
        } else {
            applySimple(target, value);
        }
    }

    private void applyLocalMap(Target target, VarDef def, Object value) {
        var jn = target.getRoot().getNode();
        if (jn.isObject() && (jn instanceof ObjectNode object)) {
            throw new GenericException("Template ({}): not implemented", target.getNode().getTrace());
        } else if (jn.isArray() && (jn instanceof ArrayNode array)) {
            var ox = target.getChunk().getIndex().intValue();
            var nd = def.getTarget().getChunk().get(0).getIndex().intValue();
            var nx = ox + nd;
            var jnn = jn.get(nx);
            var nt = Target.builder()
                    .root(target.getRoot())
                    .chunk(TrailChunk.builder().name(Integer.toString(nx)).build())
                    .node(NodeDef.builder()
                            .trace(target.getRoot().getTrace() + PATH_SEPARATOR + Integer.toString(nx))
                            .node(jnn)
                            .build())
                    .build();
            applyVariable(nt, def, value);
        } else {
            throw new GenericException("Template ({}): not implemented", target.getNode().getTrace());
        }
    }

    private void applyLocalSimple(Target target, VarDef def, Object value) {
        var jn = target.getRoot().getNode();
        if (jn instanceof ObjectNode object) {
            var name = target.getChunk().getName();
            var local = def.getTarget().getTrace();
            var ovn = object.get(name);
            if (!ovn.isTextual()) {
                throw new GenericException("Template ({}): value is not a string", target.getNode().getTrace());
            }
            var ov = ovn.asText();
            if (!ov.contains(local)) {
                throw new GenericException("Template ({}): value doesn't have variable {}", target.getNode().getTrace(), local);
            }
            var nv = ov.replace(local, value.toString());
            object.put(name, nv);
        } else {
            throw new GenericException("Template ({}): node is not an object", target.getRoot().getTrace());
        }
    }

    private void applyLocalVariable(Target target, VarDef def, Object value) {
        if (value instanceof List<?> listValue) {
            throw new GenericException("Template ({}): not implemented", target.getNode().getTrace());
        } else if (value instanceof Map<?,?> mapValue) {
            applyLocalMap(target, def, (Map<String, Object>) mapValue);
        } else {
            applyLocalSimple(target, def, value);
        }
    }

    private Target walkNodes(NodeDef node, Trail trail) {
        var rn1 = (NodeDef) null;
        var rn2 = node;
        var rnc = (TrailChunk) null;
        for (var chunk : trail.getChunk()) {
            var rn3t = rn2.getTrace() + PATH_SEPARATOR + chunk.getName();
            var rn3n = rn2.getNode();
            if (rn3n.isObject()) {
                if (rn3n.has(chunk.name)) {
                    rn3n = rn3n.get(chunk.name);
                } else {
                    rn3n = null;
                }
            } else if (rn3n.isArray()) {
                if ((chunk.index != null) && (rn3n.size() > chunk.index)) {
                    rn3n = rn3n.get(chunk.index);
                } else {
                    rn3n = null;
                }
            } else {
                rn3n = null;
            }
            if (rn3n == null) {
                throw new GenericException("Template ({}): can't find path", rn3t);
            }
            rn1 = rn2;
            rn2 = NodeDef.builder().trace(rn3t).node(rn3n).build();
            rnc = chunk;
        }
        return Target.builder().root(rn1).chunk(rnc).node(rn2).build();
    }
}
