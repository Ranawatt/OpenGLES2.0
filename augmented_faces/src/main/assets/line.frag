precision mediump float;
uniform vec4 vColor;

//varying vec4 v_position;
//
//varying vec3 startPos;
//varying vec3 vertPos;
////uniform vec2  u_resolution;
////uniform sampler2D u_Texture;
////varying vec2 v_TexCoord;
//
//varying vec3 v_ViewPosition;
//
//varying vec3 v_ScreenSpacePosition;
//
//uniform sampler2D u_Depth;
//uniform mat3 u_UvTransform;
//uniform float u_DepthTolerancePerMm;
//uniform float u_OcclusionAlpha;
//uniform float u_DepthAspectRatio;
//uniform float u_OcclusionBlurAmount;

//float GetDepthMillimeters(in vec2 depth_uv) {
//    // Depth is packed into the red and green components of its texture.
//    // The texture is a normalized format, storing millimeters.
//    vec3 packedDepthAndVisibility = texture2D(u_Depth, depth_uv).xyz;
//    return dot(packedDepthAndVisibility.xy, vec2(255.0, 256.0 * 255.0));
//}
//
//// Returns linear interpolation position of value between min and max bounds.
//// E.g., InverseLerp(1100, 1000, 2000) returns 0.1.
//float InverseLerp(in float value, in float min_bound, in float max_bound) {
//    return clamp((value - min_bound) / (max_bound - min_bound), 0.0, 1.0);
//}
//
//// Returns a value between 0.0 (not visible) and 1.0 (completely visible)
//// Which represents how visible or occluded is the pixel in relation to the
//// depth map.
//float GetVisibility(in vec2 depth_uv, in float asset_depth_mm) {
//    float depth_mm = GetDepthMillimeters(depth_uv);
//
//    // Instead of a hard z-buffer test, allow the asset to fade into the
//    // background along a 2 * u_DepthTolerancePerMm * asset_depth_mm
//    // range centered on the background depth.
//    float visibility_occlusion = clamp(0.5 * (depth_mm - asset_depth_mm) /
//    (u_DepthTolerancePerMm * asset_depth_mm) + 0.5, 0.0, 1.0);
//
//    // Depth close to zero is most likely invalid, do not use it for occlusions.
//    float visibility_depth_near = 1.0 - InverseLerp(
//    depth_mm, /*min_depth_mm=*/150.0, /*max_depth_mm=*/200.0);
//
//    // Same for very high depth values.
//    float visibility_depth_far = InverseLerp(
//    depth_mm, /*min_depth_mm=*/7500.0, /*max_depth_mm=*/8000.0);
//
//    float visibility =
//    max(max(visibility_occlusion, u_OcclusionAlpha),
//    max(visibility_depth_near, visibility_depth_far));
//
//    return visibility;
//}
//
//float GetBlurredVisibilityAroundUV(in vec2 uv, in float asset_depth_mm) {
//    // Kernel used:
//    // 0   4   7   4   0
//    // 4   16  26  16  4
//    // 7   26  41  26  7
//    // 4   16  26  16  4
//    // 0   4   7   4   0
//    const float kKernelTotalWeights = 269.0;
//    float sum = 0.0;
//
//    vec2 blurriness = vec2(u_OcclusionBlurAmount,
//    u_OcclusionBlurAmount * u_DepthAspectRatio);
//
//    float current = 0.0;
//
//    current += GetVisibility(uv + vec2(-1.0, -2.0) * blurriness, asset_depth_mm);
//    current += GetVisibility(uv + vec2(+1.0, -2.0) * blurriness, asset_depth_mm);
//    current += GetVisibility(uv + vec2(-1.0, +2.0) * blurriness, asset_depth_mm);
//    current += GetVisibility(uv + vec2(+1.0, +2.0) * blurriness, asset_depth_mm);
//    current += GetVisibility(uv + vec2(-2.0, +1.0) * blurriness, asset_depth_mm);
//    current += GetVisibility(uv + vec2(+2.0, +1.0) * blurriness, asset_depth_mm);
//    current += GetVisibility(uv + vec2(-2.0, -1.0) * blurriness, asset_depth_mm);
//    current += GetVisibility(uv + vec2(+2.0, -1.0) * blurriness, asset_depth_mm);
//    sum += current * 4.0;
//
//    current = 0.0;
//    current += GetVisibility(uv + vec2(-2.0, -0.0) * blurriness, asset_depth_mm);
//    current += GetVisibility(uv + vec2(+2.0, +0.0) * blurriness, asset_depth_mm);
//    current += GetVisibility(uv + vec2(+0.0, +2.0) * blurriness, asset_depth_mm);
//    current += GetVisibility(uv + vec2(-0.0, -2.0) * blurriness, asset_depth_mm);
//    sum += current * 7.0;
//
//    current = 0.0;
//    current += GetVisibility(uv + vec2(-1.0, -1.0) * blurriness, asset_depth_mm);
//    current += GetVisibility(uv + vec2(+1.0, -1.0) * blurriness, asset_depth_mm);
//    current += GetVisibility(uv + vec2(-1.0, +1.0) * blurriness, asset_depth_mm);
//    current += GetVisibility(uv + vec2(+1.0, +1.0) * blurriness, asset_depth_mm);
//    sum += current * 16.0;
//
//    current = 0.0;
//    current += GetVisibility(uv + vec2(+0.0, +1.0) * blurriness, asset_depth_mm);
//    current += GetVisibility(uv + vec2(-0.0, -1.0) * blurriness, asset_depth_mm);
//    current += GetVisibility(uv + vec2(-1.0, -0.0) * blurriness, asset_depth_mm);
//    current += GetVisibility(uv + vec2(+1.0, +0.0) * blurriness, asset_depth_mm);
//    sum += current * 26.0;
//
//    sum += GetVisibility(uv , asset_depth_mm) * 41.0;
//
//    return sum / kKernelTotalWeights;
//}

void main() {
//    const float kMToMm = 1000.0;
    vec4 colorLine = vec4 (1.0, 1.0, 1.0, 1.0);
    gl_FragColor = colorLine;

//    // Occlusions.
//    float asset_depth_mm = v_ViewPosition.z * kMToMm * -1.;
//    // Computes the texture coordinates to sample from the depth image.
//    vec2 depth_uvs = (u_UvTransform * vec3(v_ScreenSpacePosition.xy, 1)).xy;
//
//    // The following step is very costly. Replace the last line with the
//    // commented line if it's too expensive.
//    //gl_FragColor.a *= GetVisibility(depth_uvs, asset_depth_mm);
//    gl_FragColor.a *= GetBlurredVisibilityAroundUV(depth_uvs, asset_depth_mm);

}
