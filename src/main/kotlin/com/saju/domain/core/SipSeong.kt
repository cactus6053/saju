package com.saju.domain.core

enum class SipSeongGroup(val hangul: String) {
    BIGEOP("비겁"),     // 비견·겁재
    SIKSANG("식상"),    // 식신·상관
    JAESEONG("재성"),   // 편재·정재
    GWANSEONG("관성"),  // 편관·정관
    INSEONG("인성"),    // 편인·정인
}

enum class SipSeong(
    val hanja: String,
    val hangul: String,
    val group: SipSeongGroup,
) {
    BIGYEON("比肩", "비견", SipSeongGroup.BIGEOP),
    GEOPJAE("劫財", "겁재", SipSeongGroup.BIGEOP),
    SIKSIN("食神", "식신", SipSeongGroup.SIKSANG),
    SANGGWAN("傷官", "상관", SipSeongGroup.SIKSANG),
    PYEONJAE("偏財", "편재", SipSeongGroup.JAESEONG),
    JEONGJAE("正財", "정재", SipSeongGroup.JAESEONG),
    PYEONGWAN("偏官", "편관", SipSeongGroup.GWANSEONG),
    JEONGGWAN("正官", "정관", SipSeongGroup.GWANSEONG),
    PYEONIN("偏印", "편인", SipSeongGroup.INSEONG),
    JEONGIN("正印", "정인", SipSeongGroup.INSEONG),
    ;

    companion object {
        // 일간 대비 대상 천간의 오행 관계 × 음양 동이(同異)로 십성 판정
        fun of(dayMaster: CheonGan, target: CheonGan): SipSeong {
            val samePolarity = dayMaster.yinYang == target.yinYang
            return when (ElementRelation.between(dayMaster.element, target.element)) {
                ElementRelation.SAME -> if (samePolarity) BIGYEON else GEOPJAE
                ElementRelation.GENERATES -> if (samePolarity) SIKSIN else SANGGWAN
                ElementRelation.CONTROLS -> if (samePolarity) PYEONJAE else JEONGJAE
                ElementRelation.CONTROLLED -> if (samePolarity) PYEONGWAN else JEONGGWAN
                ElementRelation.GENERATED -> if (samePolarity) PYEONIN else JEONGIN
            }
        }
    }
}
