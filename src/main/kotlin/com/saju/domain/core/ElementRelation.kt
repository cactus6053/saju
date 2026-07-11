package com.saju.domain.core

enum class ElementRelation {
    SAME,       // 비화(比和): 같은 오행
    GENERATES,  // 상생(相生): 내가 생함
    GENERATED,  // 상생(相生): 나를 생함
    CONTROLS,   // 상극(相剋): 내가 극함
    CONTROLLED, // 상극(相剋): 나를 극함
    ;

    companion object {
        // 상생: 木→火→土→金→水→木
        private val GENERATION_ORDER = listOf(
            Element.WOOD, Element.FIRE, Element.EARTH, Element.METAL, Element.WATER,
        )

        fun between(from: Element, to: Element): ElementRelation {
            if (from == to) return SAME
            val diff = (GENERATION_ORDER.indexOf(to) - GENERATION_ORDER.indexOf(from) + 5) % 5
            return when (diff) {
                1 -> GENERATES   // 다음 오행 = 내가 생함
                4 -> GENERATED   // 이전 오행 = 나를 생함
                2 -> CONTROLS    // 두 칸 뒤 = 내가 극함 (木克土)
                3 -> CONTROLLED  // 나를 극함
                else -> error("unreachable")
            }
        }
    }
}

val Element.generates: Element
    get() = when (this) {
        Element.WOOD -> Element.FIRE
        Element.FIRE -> Element.EARTH
        Element.EARTH -> Element.METAL
        Element.METAL -> Element.WATER
        Element.WATER -> Element.WOOD
    }

val Element.controls: Element
    get() = when (this) {
        Element.WOOD -> Element.EARTH
        Element.EARTH -> Element.WATER
        Element.WATER -> Element.FIRE
        Element.FIRE -> Element.METAL
        Element.METAL -> Element.WOOD
    }
