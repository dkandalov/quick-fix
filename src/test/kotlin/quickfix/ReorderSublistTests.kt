package quickfix

import org.junit.Test
import kotlin.test.assertEquals

class ReorderSublistTests {
    @Test fun `empty list`() {
        emptyList<String>().reorderSublist(order = emptyList()) shouldEqual emptyList<String>()
        emptyList<String>().reorderSublist(order = listOf("a", "b")) shouldEqual emptyList<String>()
    }

    @Test fun `empty reorder`() {
        listOf("c", "b", "a").reorderSublist(order = emptyList()) shouldEqual listOf("c", "b", "a")
    }

    @Test fun `reorder two items`() {
        listOf("a", "b").reorderSublist(listOf("a", "b")) shouldEqual listOf("a", "b")
        listOf("b", "a").reorderSublist(listOf("a", "b")) shouldEqual listOf("a", "b")
        listOf("_", "b", "a").reorderSublist(listOf("a", "b")) shouldEqual listOf("_", "a", "b")
        listOf("b", "_", "a").reorderSublist(listOf("a", "b")) shouldEqual listOf("a", "_", "b")
        listOf("b", "a", "_").reorderSublist(listOf("a", "b")) shouldEqual listOf("a", "b", "_")
        listOf("_", "b", "a", ".").reorderSublist(listOf("a", "b")) shouldEqual listOf("_", "a", "b", ".")
        listOf("_", "b", "-", "a", ".").reorderSublist(listOf("a", "b")) shouldEqual listOf("_", "a", "-", "b", ".")
    }
}

private infix fun Any.shouldEqual(that: Any) {
    assertEquals(actual = this, expected = that)
}
