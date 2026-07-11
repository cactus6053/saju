package com.saju.domain.core

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ElementRelationTest {

    @Test
    fun `상생 순환 - 木火土金水木`() {
        assertEquals(Element.FIRE, Element.WOOD.generates)
        assertEquals(Element.EARTH, Element.FIRE.generates)
        assertEquals(Element.METAL, Element.EARTH.generates)
        assertEquals(Element.WATER, Element.METAL.generates)
        assertEquals(Element.WOOD, Element.WATER.generates)
    }

    @Test
    fun `상극 순환 - 木土水火金木`() {
        assertEquals(Element.EARTH, Element.WOOD.controls)
        assertEquals(Element.WATER, Element.EARTH.controls)
        assertEquals(Element.FIRE, Element.WATER.controls)
        assertEquals(Element.METAL, Element.FIRE.controls)
        assertEquals(Element.WOOD, Element.METAL.controls)
    }

    @Test
    fun `between - 같은 오행은 SAME`() {
        Element.entries.forEach {
            assertEquals(ElementRelation.SAME, ElementRelation.between(it, it))
        }
    }

    @Test
    fun `between - generates 프로퍼티와 일관성`() {
        Element.entries.forEach { e ->
            assertEquals(ElementRelation.GENERATES, ElementRelation.between(e, e.generates))
            assertEquals(ElementRelation.GENERATED, ElementRelation.between(e.generates, e))
        }
    }

    @Test
    fun `between - controls 프로퍼티와 일관성`() {
        Element.entries.forEach { e ->
            assertEquals(ElementRelation.CONTROLS, ElementRelation.between(e, e.controls))
            assertEquals(ElementRelation.CONTROLLED, ElementRelation.between(e.controls, e))
        }
    }

    @Test
    fun `모든 오행 쌍은 5가지 관계 중 하나로 분류됨`() {
        Element.entries.forEach { a ->
            Element.entries.forEach { b ->
                ElementRelation.between(a, b)  // 예외 없이 반환되어야 함
            }
        }
    }
}
