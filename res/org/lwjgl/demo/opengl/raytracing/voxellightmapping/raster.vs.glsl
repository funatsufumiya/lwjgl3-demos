/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
#version 330 core

uniform mat4 mvp;
uniform ivec2 lightmapSize;
uniform float lod;

layout(location=0) in vec4 position;
layout(location=2) in vec4 lightmapCoords;

centroid out vec2 lightmapCoords_varying;
flat out int matIndex;

vec3 offset() {
  int mxyz = int(position.w);
  return vec3(((mxyz>>8)&0x3)-1, ((mxyz>>10)&0x3)-1, ((mxyz>>12)&0x3)-1);
}

/**
 * Vertex morphing between two lod levels.
 * Reference: https://0fps.net/2018/03/03/a-level-of-detail-method-for-blocky-voxels/
 */
vec3 lodPosition() {
  uint lodI = uint(lod);
  uint scale0 = 1u<<lodI, scale1 = 1u<<(lodI+1u);
  float vblend = fract(lod);
  return floor(position.xyz / scale1) * scale1 * vblend
       + floor(position.xyz / scale0) * scale0 * (1.0 - vblend);
}

void main(void) {
  vec3 p = lodPosition();
  float w = dot(transpose(mvp)[3], vec4(p.xyz, 1.0));
  matIndex = int(position.w)&0xFF;
  lightmapCoords_varying = (lightmapCoords.xy + vec2(0.5)) / vec2(lightmapSize);
  gl_Position = mvp * vec4(p + offset() * 1E-4 * w, 1.0);
}
