package tech.kekulta.phena

data class Anim(val name: String, val frames: List<Frame>, val aspectRatio: Float = 1f, val frameRate: Float = 4f)