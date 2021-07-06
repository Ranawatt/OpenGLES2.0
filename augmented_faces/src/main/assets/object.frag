precision mediump float;

uniform sampler2D u_Texture;

uniform vec4 u_LightingParameters;
uniform vec4 u_MaterialParameters;
uniform vec4 u_ColorCorrectionParameters;

varying vec3 v_ViewPosition;
varying vec3 v_ViewNormal;
varying vec2 v_TexCoord;
uniform vec4 u_ObjColor;

void main() {
 // We support approximate sRGB gamma.
 const float kGamma = 0.4545454;
 const float kInverseGamma = 2.2;
 const float kMiddleGrayGamma = 0.466;

 // Unpack lighting and material parameters for better naming.
 vec3 viewLightDirection = u_LightingParameters.xyz;
 vec3 colorShift = u_ColorCorrectionParameters.rgb;
 float averagePixelIntensity = u_ColorCorrectionParameters.a;

 float materialAmbient = u_MaterialParameters.x;
 float materialDiffuse = u_MaterialParameters.y;
 float materialSpecular = u_MaterialParameters.z;
 float materialSpecularPower = u_MaterialParameters.w;

 // Normalize varying parameters, because they are linearly interpolated in the vertex shader.
 vec3 viewFragmentDirection = normalize(v_ViewPosition);
 vec3 viewNormal = normalize(v_ViewNormal);

 // Flip the y-texture coordinate to address the texture from top-left.
 vec4 objectColor = texture2D(u_Texture, vec2(v_TexCoord.x, 1.0 - v_TexCoord.y));

 // Apply color to grayscale image only if the alpha of u_ObjColor is
 // greater and equal to 255.0.
 objectColor.rgb *= mix(vec3(1.0), u_ObjColor.rgb / 255.0,
 step(255.0, u_ObjColor.a));

 // Apply inverse SRGB gamma to the texture before making lighting calculations.
 objectColor.rgb = pow(objectColor.rgb, vec3(kInverseGamma));

 // Ambient light is unaffected by the light intensity.
 float ambient = materialAmbient;

 // Approximate a hemisphere light (not a harsh directional light).
 float diffuse = materialDiffuse *
 1.0 * (dot(viewNormal, viewLightDirection) + 1.0);

 // Compute specular light.
 vec3 reflectedLightDirection = reflect(viewLightDirection, viewNormal);
 float specularStrength = max(0.0, dot(viewFragmentDirection, reflectedLightDirection));
 float specular = materialSpecular *
 pow(specularStrength, materialSpecularPower);

 // Compute final color, convert back to gamma color space, and apply ARCore
 // color correction.
 vec3 color = objectColor.rgb * (diffuse + ambient + specular) * averagePixelIntensity;
 // Apply SRGB gamma before writing the fragment color.
 color.rgb = pow(color, vec3(kGamma));
// Apply average pixel intensity and color shift
 color *= colorShift * (averagePixelIntensity / kMiddleGrayGamma);
 gl_FragColor.rgb = color ;//* 0.75;
 gl_FragColor.a = objectColor.a;


// float Brightness = 1.0f;
// float Contrast = Brightness;
// vec3 AverageLuminance = vec3(Brightness);
// gl_FragColor = vec4(mix(color.rgb * Brightness, mix(AverageLuminance, color.rgb, Contrast), 1.0), 1.0);
// gl_FragColor.a = objectColor.a;
}