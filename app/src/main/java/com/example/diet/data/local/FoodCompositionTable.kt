package com.example.diet.data.local

import com.example.diet.domain.Per100g

/** 内置食物条目：按每 100g（饮品每 100ml）给出常见食物成分 */
data class FoodRef(
    val name: String,
    val keywords: List<String>,
    val per100: Per100g
)

/**
 * 内置精简食物成分表（常见中式主食/蛋白质/蔬果/零食/饮品）。
 * 作用是让「规则明确、可枚举」的常见食物走本地计算，不消耗大模型 token。
 * 覆盖率有限：未命中时不猜数值，交由模型补全或标记缺失。
 */
object FoodCompositionTable {

    val items: List<FoodRef> = listOf(
        // 主食
        FoodRef("米饭", listOf("米饭", "白饭"), Per100g(116f, 2.6f, 25.9f, 0.3f)),
        FoodRef("粥", listOf("白粥", "稀饭"), Per100g(46f, 1.1f, 9.9f, 0.3f)),
        FoodRef("面条", listOf("面条", "拉面", "汤面"), Per100g(110f, 4.5f, 17.0f, 2.5f)),
        FoodRef("馒头", listOf("馒头", "花卷"), Per100g(223f, 7.0f, 47.0f, 1.1f)),
        FoodRef("包子", listOf("包子"), Per100g(227f, 7.5f, 36.0f, 6.0f)),
        FoodRef("饺子", listOf("饺子", "水饺", "煎饺"), Per100g(220f, 8.0f, 25.0f, 9.0f)),
        FoodRef("炒饭", listOf("炒饭", "蛋炒饭"), Per100g(190f, 4.5f, 28.0f, 6.5f)),
        FoodRef("面包", listOf("面包", "吐司"), Per100g(246f, 8.5f, 45.0f, 3.3f)),
        FoodRef("油条", listOf("油条"), Per100g(388f, 6.9f, 51.0f, 17.6f)),
        FoodRef("土豆", listOf("土豆", "马铃薯"), Per100g(81f, 2.6f, 17.8f, 0.2f)),
        FoodRef("红薯", listOf("红薯", "地瓜"), Per100g(106f, 1.6f, 24.7f, 0.2f)),
        FoodRef("玉米", listOf("玉米"), Per100g(112f, 4.0f, 22.8f, 1.2f)),
        // 蛋白质
        FoodRef("鸡蛋", listOf("鸡蛋", "煎蛋", "水煮蛋", "荷包蛋"), Per100g(143f, 12.6f, 1.1f, 9.5f)),
        FoodRef("鸡胸肉", listOf("鸡胸", "鸡胸肉"), Per100g(133f, 24.6f, 0.6f, 3.6f)),
        FoodRef("鸡腿", listOf("鸡腿", "鸡翅"), Per100g(181f, 20.0f, 0.0f, 11.0f)),
        FoodRef("牛肉", listOf("牛肉", "牛排"), Per100g(190f, 20.2f, 0.0f, 12.0f)),
        FoodRef("猪肉", listOf("猪里脊", "瘦肉", "猪肉"), Per100g(155f, 20.2f, 0.7f, 7.9f)),
        FoodRef("五花肉", listOf("五花肉", "红烧肉"), Per100g(518f, 9.3f, 0.0f, 53.0f)),
        FoodRef("鱼", listOf("三文鱼", "鲈鱼", "鱼肉", "鱼"), Per100g(139f, 17.2f, 0.0f, 7.8f)),
        FoodRef("虾", listOf("虾仁", "虾"), Per100g(93f, 18.6f, 2.8f, 0.8f)),
        FoodRef("豆腐", listOf("豆腐"), Per100g(82f, 8.1f, 4.2f, 3.7f)),
        // 蔬菜/水果
        FoodRef("西兰花", listOf("西兰花", "broccoli"), Per100g(36f, 4.1f, 4.3f, 0.6f)),
        FoodRef("青菜", listOf("青菜", "小白菜", "生菜", "菠菜"), Per100g(15f, 1.5f, 2.7f, 0.3f)),
        FoodRef("番茄", listOf("番茄", "西红柿"), Per100g(20f, 0.9f, 4.0f, 0.2f)),
        FoodRef("黄瓜", listOf("黄瓜"), Per100g(16f, 0.8f, 2.9f, 0.2f)),
        FoodRef("苹果", listOf("苹果"), Per100g(53f, 0.2f, 13.5f, 0.2f)),
        FoodRef("香蕉", listOf("香蕉"), Per100g(93f, 1.4f, 22.0f, 0.2f)),
        FoodRef("橙子", listOf("橙子", "橘子"), Per100g(48f, 0.8f, 11.1f, 0.2f)),
        FoodRef("西瓜", listOf("西瓜"), Per100g(26f, 0.6f, 6.8f, 0.1f)),
        // 乳品/饮品（每 100ml）
        FoodRef("牛奶", listOf("牛奶"), Per100g(54f, 3.0f, 4.8f, 3.2f)),
        FoodRef("酸奶", listOf("酸奶"), Per100g(72f, 2.5f, 9.3f, 2.7f)),
        FoodRef("豆浆", listOf("豆浆"), Per100g(31f, 3.0f, 1.2f, 1.6f)),
        FoodRef("拿铁", listOf("拿铁", "latte"), Per100g(55f, 2.8f, 5.5f, 2.4f)),
        FoodRef("美式咖啡", listOf("美式", "黑咖啡"), Per100g(2f, 0.1f, 0.3f, 0.0f)),
        FoodRef("奶茶", listOf("奶茶"), Per100g(60f, 0.6f, 11.5f, 1.6f)),
        FoodRef("可乐", listOf("可乐"), Per100g(43f, 0.0f, 10.6f, 0.0f)),
        FoodRef("果汁", listOf("果汁", "橙汁"), Per100g(45f, 0.5f, 11.0f, 0.1f)),
        // 零食/快餐
        FoodRef("薯片", listOf("薯片"), Per100g(536f, 6.3f, 53.0f, 33.0f)),
        FoodRef("巧克力", listOf("巧克力"), Per100g(586f, 4.3f, 53.4f, 40.1f)),
        FoodRef("饼干", listOf("饼干"), Per100g(435f, 9.0f, 71.0f, 12.7f)),
        FoodRef("蛋糕", listOf("蛋糕"), Per100g(348f, 5.5f, 45.0f, 16.0f)),
        FoodRef("汉堡", listOf("汉堡", "堡"), Per100g(260f, 12.0f, 28.0f, 10.0f)),
        FoodRef("披萨", listOf("披萨", "pizza"), Per100g(266f, 11.0f, 33.0f, 10.0f)),
        FoodRef("炸鸡", listOf("炸鸡", "鸡块"), Per100g(260f, 20.0f, 8.0f, 16.0f)),
        FoodRef("薯条", listOf("薯条"), Per100g(312f, 3.4f, 41.0f, 15.0f)),
        FoodRef("蛋白粉", listOf("蛋白粉"), Per100g(400f, 80.0f, 8.0f, 5.0f))
    )

    const val SOURCE = "内置食物成分表"

    /**
     * 匹配规则（宁缺毋滥）：
     * - 归一化后完全相等，或名称只比关键词多 1 个字（如「白米饭」→ 米饭）；
     * - 取匹配到的最长关键词；
     * - 明显是复合菜名（如「番茄炒蛋」含「番茄」）不匹配，避免张冠李戴。
     */
    fun lookup(rawName: String): FoodRef? {
        val name = normalize(rawName)
        if (name.isBlank()) return null

        var best: FoodRef? = null
        var bestKeywordLength = 0
        items.forEach { ref ->
            ref.keywords.forEach { keyword ->
                val k = normalize(keyword)
                if (k.isNotBlank()) {
                    val exact = name == k
                    val nearMatch = name.contains(k) && name.length <= k.length + 1
                    if ((exact || nearMatch) && k.length > bestKeywordLength) {
                        best = ref
                        bestKeywordLength = k.length
                    }
                }
            }
        }
        return best
    }

    private fun normalize(value: String): String =
        value.replace(Regex("[\\s（）()【】\\[\\]·、,，.。/\\-]"), "").lowercase()
}
