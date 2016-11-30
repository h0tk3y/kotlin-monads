import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DoNotationTest {
    @Test fun testLinearDo() {
        val m = doWith(just(1)) { i ->
            val j = bind(returns(i * 2))
            val k = bind(returns(j * 3))
            then(returns(k + 1))
        }
        assertEquals(just(7), m)
    }

    @Test fun testControlFlow() {
        var called = false
        val m = doWith(just(1)) { i ->
            val j = bind(returns(i * 2))
            val k = bind(if (j % 2 == 0) none() else just(j))
            called = true
            then(returns(k))
        }
        assertEquals(Maybe.None, m)
        assertFalse(called)
    }

    @Test fun testMultipleBranches() {
        var iIterations = 0
        var jIterations = 0
        var kIterations = 0
        val m = doWith(monadListOf(0)) {
            val i = bind(monadListOf(1, 2, 3))
            ++iIterations
            val j = bind(monadListOf(i, i))
            ++jIterations
            then(monadListOf(j, j + 1))
            ++kIterations
        }
        assertEquals(monadListOf(1, 2, 1, 2, 2, 3, 2, 3, 3, 4, 3, 4), m)
        assertEquals(3, iIterations)
        assertEquals(6, jIterations)
        assertEquals(12, kIterations)
    }

    @Test fun testMultipleApplications() {
        val m = doWith(monadListOf(1)) {
            then(monadListOf(1, 1))
            then(monadListOf(1, 1))
            then(monadListOf(1, 1))
            then(monadListOf(1, 1))
        } as MonadList
        assertEquals(16, m.size)
    }

    @Test fun testBindLastStatement() {
        val results = mutableListOf<Int>()
        val m = doWith(monadListOf(2)) { i ->
            val x = bind(monadListOf(i + 1, i * i))
            val z = bind(returns(x))
            results.add(z)
        }
        assertEquals(listOf(3, 4), results)
        assertEquals(results, m)
    }
}