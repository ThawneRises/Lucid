package com.lucid.gallery

import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.lucid.gallery", appContext.packageName)
    }
}