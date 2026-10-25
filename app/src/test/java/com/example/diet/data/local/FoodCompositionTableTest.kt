package com.example.diet.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 「宁缺毋滥」的匹配规则：宁可交给模型，也不要张冠李戴。
 */
class FoodCompositionTableTest {

    @Test
    fun lookupMatchesExactName() {
        assertEquals("米饭", FoodCompositionTable.lookup("米饭")?.name)
    }

    @Test
    fun lookupAllowsOneExtraCharacter() {
        assertEquals("米饭", FoodCompositionTable.lookup("白米饭")?.name)
        assertEquals("鸡胸肉", FoodCompositionTable.lookup("煎鸡胸肉")?.name)
    }

    @Test
    fun lookupRejectsCompositeDishes() {
        // 含「番茄」但明显是复合菜，不应套用番茄的数值
        assertNull(FoodCompositionTable.lookup("番茄炒蛋"))
        assertNull(FoodCompositionTable.lookup("青椒肉丝盖饭"))
    }

    @Test
    fun lookupRejectsBrandedOrLongNames() {
        // 包装食品应走商品营养标签或模型补数
        assertNull(FoodCompositionTable.lookup("乐事原味薯片"))
    }

    @Test
    fun lookupNormalizesSpacesAndCasing() {
        assertEquals("牛奶", FoodCompositionTable.lookup(" 牛 奶 ")?.name)
        assertEquals("西兰花", FoodCompositionTable.lookup("Broccoli")?.name)
    }

    @Test
    fun lookupPrefersLongestKeyword() {
        // 「水煮蛋」比「蛋」更具体
        assertEquals("鸡蛋", FoodCompositionTable.lookup("水煮蛋")?.name)
        assertEquals("五花肉", FoodCompositionTable.lookup("红烧肉")?.name)
    }

    @Test
    fun lookupReturnsNullForUnknownOrBlank() {
        assertNull(FoodCompositionTable.lookup(""))
        assertNull(FoodCompositionTable.lookup("   "))
        assertNull(FoodCompositionTable.lookup("燕麦拿铁特调"))
    }

    @Test
    fun tableEntriesAreWellFormed() {
        FoodCompositionTable.items.forEach { ref ->
            assertEquals("关键词不能为空：${ref.name}", false, ref.keywords.isEmpty())
            assertEquals("每 100g 热量不应为负：${ref.name}", true, ref.per100.kcal >= 0f)
        }
    }
}
