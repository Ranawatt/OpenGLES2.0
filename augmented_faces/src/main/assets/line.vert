precision mediump float;
attribute float aData;
//attribute vec2 a_TexCoord;

uniform vec4 uEndData;
uniform vec4 uStartData;
uniform vec4 uControlData;

uniform mat4 u_MVPMatrix;
//uniform mat4 u_MVMatrix;
//varying vec4 v_position;

//varying vec3 startPos;
//varying vec2 v_TexCoord;

//attribute vec2 a_Normal;
//uniform float u_linewidth;

//varying vec3 v_ScreenSpacePosition;
//varying vec3 v_ViewPosition;

// Beizer Quadratic curve (1-t)^2P0 + 2(1-t)tP1 + t^2*P2
vec3 fun3(in vec3 p0, in vec3 p1, in vec3 p2, in float t)
{

 float tt = (1.0 - t) * (1.0 -t);

 return tt * p0 + 2.0 * t * (1.0 -t) * p1 + t * t * p2;

}

float toZValue(vec4 vertex)
{
 return (vertex.z/vertex.w);
}

void main() {

 vec4 pos;
 pos.w = 1.0;

 vec3 p0 = uStartData.xyz;
 vec3 p2 = uEndData.xyz;

 vec3 p1 = uControlData.xyz;


 float t = aData;

 vec3 point = fun3(p0, p1, p2, t);
// vec2 point = fun3(vec2(p0.x, p0.y), vec2(p1.x,p1.y), vec2(p2.x,p2.y), t);
 if (t < 0.0)
 {
 pos.xy = vec2(0.0, 0.0);
 }
 else
 {
 pos.xyz = point;
 }
 vec4 a_position = vec4(pos.xy, (toZValue(pos)), pos.w); //(toZValue(pos))


// vec4 delta = vec4(a_Normal * u_linewidth, 0, 0);
// v_ViewPosition = (u_MVMatrix * a_position).xyz;

 gl_Position = u_MVPMatrix * a_position;

// v_ScreenSpacePosition = gl_Position.xyz / gl_Position.w;
//
// v_position = a_position;
// gl_PointSize = 30.0f;
//
// startPos = uStartData.xyz;
// v_TexCoord = a_TexCoord;
}