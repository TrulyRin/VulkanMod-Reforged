package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class GlProgram {
    private static int nextShaderId = 1;
    private static int nextProgramId = 1;
    private static int nextUniformLocation = 1;

    private static final Int2ReferenceOpenHashMap<Shader> SHADERS = new Int2ReferenceOpenHashMap<>();
    private static final Int2ReferenceOpenHashMap<Program> PROGRAMS = new Int2ReferenceOpenHashMap<>();
    private static final Int2ObjectOpenHashMap<String> UNIFORM_NAMES_BY_LOCATION = new Int2ObjectOpenHashMap<>();

    private static int currentProgramId;

    private GlProgram() {
    }

    public static int createShader(int type) {
        int id = nextShaderId++;
        SHADERS.put(id, new Shader(type));
        return id;
    }

    public static void shaderSource(int shader, CharSequence source) {
        Shader shaderObj = SHADERS.get(shader);
        if (shaderObj != null) {
            shaderObj.source = source != null ? source.toString() : "";
        }
    }

    public static void compileShader(int shader) {
        Shader shaderObj = SHADERS.get(shader);
        if (shaderObj != null) {
            shaderObj.compiled = true;
        }
    }

    public static int getShaderi(int shader, int pname) {
        return GL20.GL_TRUE;
    }

    public static String getShaderInfoLog(int shader) {
        return "";
    }

    public static void deleteShader(int shader) {
        SHADERS.remove(shader);
    }

    public static int createProgram() {
        int id = nextProgramId++;
        PROGRAMS.put(id, new Program());
        return id;
    }

    public static void attachShader(int program, int shader) {
        Program programObj = PROGRAMS.get(program);
        if (programObj != null) {
            programObj.attachedShaders.put(shader, SHADERS.get(shader));
        }
    }

    public static void bindAttribLocation(int program, int index, CharSequence name) {
        Program programObj = PROGRAMS.get(program);
        if (programObj != null && name != null) {
            programObj.attributeLocations.put(name.toString(), index);
        }
    }

    public static void linkProgram(int program) {
        Program programObj = PROGRAMS.get(program);
        if (programObj != null) {
            programObj.linked = true;
        }
    }

    public static int getProgrami(int program, int pname) {
        return switch (pname) {

            case GL20.GL_ACTIVE_UNIFORMS, GL20.GL_ACTIVE_ATTRIBUTES,
                    GL20.GL_ACTIVE_UNIFORM_MAX_LENGTH, GL20.GL_ACTIVE_ATTRIBUTE_MAX_LENGTH,
                    GL20.GL_INFO_LOG_LENGTH, GL20.GL_ATTACHED_SHADERS,
                    GL_ACTIVE_UNIFORM_BLOCKS, GL_ACTIVE_UNIFORM_BLOCK_MAX_NAME_LENGTH,
                    GL_PROGRAM_BINARY_LENGTH -> 0;
            case GL20.GL_DELETE_STATUS -> GL20.GL_FALSE;

            default -> GL20.GL_TRUE;
        };
    }

    private static final int GL_ACTIVE_UNIFORM_BLOCKS = 0x8A36;
    private static final int GL_ACTIVE_UNIFORM_BLOCK_MAX_NAME_LENGTH = 0x8A35;
    private static final int GL_PROGRAM_BINARY_LENGTH = 0x8741;

    public static String getProgramInfoLog(int program) {
        return "";
    }

    public static void useProgram(int program) {
        currentProgramId = isProgram(program) ? program : 0;
    }

    public static boolean isProgram(int program) {
        return program == 0 || PROGRAMS.containsKey(program);
    }

    public static void deleteProgram(int program) {
        PROGRAMS.remove(program);
        if (currentProgramId == program) {
            currentProgramId = 0;
        }
    }

    public static int currentProgramId() {
        return currentProgramId;
    }

    public static int getAttribLocation(int program, CharSequence name) {
        Program programObj = PROGRAMS.get(program);
        if (programObj == null || name == null) {
            return -1;
        }

        return programObj.attributeLocations.computeIfAbsent(name.toString(), ignored -> {
            String lowerName = name.toString().toLowerCase(Locale.ROOT);
            if (lowerName.contains("position")) {
                return 0;
            }
            if (lowerName.contains("color")) {
                return 1;
            }
            return programObj.attributeLocations.size();
        });
    }

    public static int getUniformLocation(int program, CharSequence name) {
        Program programObj = PROGRAMS.get(program);
        if (programObj == null || name == null) {
            return -1;
        }

        return programObj.uniformLocations.computeIfAbsent(name.toString(), uniformName -> {
            int location = nextUniformLocation++;
            UNIFORM_NAMES_BY_LOCATION.put(location, uniformName);
            return location;
        });
    }

    public static void uniform1i(int location, int value) {
        UniformValue uniformValue = getOrCreateUniform(location, 1, 1);
        if (uniformValue != null) {
            uniformValue.floats[0] = value;
            uniformValue.ints[0] = value;
        }
    }

    public static void uniform1f(int location, float value) {
        UniformValue uniformValue = getOrCreateUniform(location, 1, 0);
        if (uniformValue != null) {
            uniformValue.floats[0] = value;
        }
    }

    public static void uniform2f(int location, float x, float y) {
        UniformValue uniformValue = getOrCreateUniform(location, 2, 0);
        if (uniformValue != null) {
            uniformValue.floats[0] = x;
            uniformValue.floats[1] = y;
        }
    }

    public static void uniform3f(int location, float x, float y, float z) {
        UniformValue uniformValue = getOrCreateUniform(location, 3, 0);
        if (uniformValue != null) {
            uniformValue.floats[0] = x;
            uniformValue.floats[1] = y;
            uniformValue.floats[2] = z;
        }
    }

    public static void uniform3i(int location, int x, int y, int z) {
        UniformValue uniformValue = getOrCreateUniform(location, 3, 3);
        if (uniformValue != null) {
            uniformValue.floats[0] = x;
            uniformValue.floats[1] = y;
            uniformValue.floats[2] = z;
            uniformValue.ints[0] = x;
            uniformValue.ints[1] = y;
            uniformValue.ints[2] = z;
        }
    }

    public static void uniform4f(int location, float x, float y, float z, float w) {
        UniformValue uniformValue = getOrCreateUniform(location, 4, 0);
        if (uniformValue != null) {
            uniformValue.floats[0] = x;
            uniformValue.floats[1] = y;
            uniformValue.floats[2] = z;
            uniformValue.floats[3] = w;
        }
    }

    public static void uniformMatrix4fv(int location, boolean transpose, FloatBuffer value) {
        if (value == null) {
            return;
        }

        UniformValue uniformValue = getOrCreateUniform(location, 16, 0);
        if (uniformValue == null) {
            return;
        }

        float[] matrix = uniformValue.floats;
        FloatBuffer src = value.duplicate();
        for (int i = 0; i < matrix.length; i++) {
            matrix[i] = src.hasRemaining() ? src.get() : 0.0f;
        }
    }

    public static float[] getFloatUniform(String name) {
        Program programObj = PROGRAMS.get(currentProgramId);
        if (programObj == null) {
            return null;
        }

        UniformValue value = programObj.uniformValues.get(name);
        return value != null ? value.floats : null;
    }

    public static int[] getIntUniform(String name) {
        Program programObj = PROGRAMS.get(currentProgramId);
        if (programObj == null) {
            return null;
        }

        UniformValue value = programObj.uniformValues.get(name);
        return value != null ? value.ints : null;
    }

    private static UniformValue getOrCreateUniform(int location, int floatCount, int intCount) {
        String name = UNIFORM_NAMES_BY_LOCATION.get(location);
        Program programObj = PROGRAMS.get(currentProgramId);
        if (name == null || programObj == null) {
            return null;
        }

        UniformValue value = programObj.uniformValues.get(name);
        if (value == null || !hasShape(value, floatCount, intCount)) {
            value = new UniformValue(new float[floatCount], intCount > 0 ? new int[intCount] : null);
            programObj.uniformValues.put(name, value);
        }

        return value;
    }

    private static boolean hasShape(UniformValue value, int floatCount, int intCount) {
        return value.floats.length == floatCount
                && (intCount == 0 ? value.ints == null : value.ints != null && value.ints.length == intCount);
    }

    private static final class Shader {
        final int type;
        String source = "";
        boolean compiled;

        Shader(int type) {
            this.type = type;
        }
    }

    private static final class Program {
        final Int2ReferenceOpenHashMap<Shader> attachedShaders = new Int2ReferenceOpenHashMap<>();
        final Map<String, Integer> attributeLocations = new HashMap<>();
        final Map<String, Integer> uniformLocations = new HashMap<>();
        final Map<String, UniformValue> uniformValues = new HashMap<>();
        boolean linked;
    }

    private record UniformValue(float[] floats, int[] ints) {
    }
}

