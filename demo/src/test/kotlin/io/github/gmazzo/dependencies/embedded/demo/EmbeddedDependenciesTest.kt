package io.github.gmazzo.dependencies.embedded.demo

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class EmbeddedDependenciesTest {

    @ParameterizedTest
    @CsvSource(
        "org.apache.commons.lang3.StringUtils",
        "org.apache.commons.collections4.ArrayUtils",
    )
    fun `classes should be reachable`(className: String) {
        val exists = runCatching { Class.forName(className) }.isSuccess

        assertTrue(exists)
    }

}
