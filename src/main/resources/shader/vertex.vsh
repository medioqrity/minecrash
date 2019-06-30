# version 330

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 color;
out vec3 bypassColor;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;

void main() {
    gl_Position = projectionMatrix * viewMatrix * vec4(position, 1.0);
    bypassColor = color;
}