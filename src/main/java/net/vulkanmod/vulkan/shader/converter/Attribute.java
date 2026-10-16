package net.vulkanmod.vulkan.shader.converter;

public class Attribute implements GLSLParser.Node {

    String ioType;
    String type;
    String id;
    int location;

    public Attribute(String ioType, String type, String id) {
        switch (ioType) {
            case "in", "out" -> {}
            default -> throw new IllegalArgumentException();
        }

        this.ioType = ioType;
        this.type = type;
        this.id = id;
    }

    public void setLocation(int location) {
        this.location = location;
    }

    @Override
    public String getStringValue() {
        return "layout(location = %d) %s %s %s;\n".formatted(location, ioType, type, id);
    }
}
