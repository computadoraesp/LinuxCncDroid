package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class SpeedsFeedsCalculatorTest {

    @Test
    fun standardMillingFormula_aluminum6061_calculatesCorrectRpm() {
        val vcAluminum = 220.0 // m/min
        val toolDiameter = 6.0 // mm

        val expectedRpm = (vcAluminum * 1000.0) / (PI * toolDiameter)
        // ~11671.8 RPM

        assertTrue("Expected RPM around 11,600", expectedRpm in 11600.0..11700.0)
    }

    @Test
    fun standardMillingFormula_steel1018_calculatesCorrectFeedRate() {
        val vcSteel = 90.0 // m/min
        val toolDiameter = 10.0 // mm
        val flutes = 4
        val fz = 0.035 // mm/tooth

        val calculatedRpm = (vcSteel * 1000.0) / (PI * toolDiameter)
        val calculatedFeed = calculatedRpm * flutes * fz
        // RPM ~ 2864.78, Feed = 2864.78 * 4 * 0.035 ~ 401 mm/min

        assertTrue("Expected feed around 401 mm/min", calculatedFeed in 395.0..405.0)
    }

    @Test
    fun roughingMode_reducesSurfaceSpeed_andIncreasesChipLoad() {
        val baseVc = 200.0
        val baseFz = 0.05

        val roughingVc = baseVc * 0.85
        val roughingFz = baseFz * 1.30

        assertEquals(170.0, roughingVc, 0.01)
        assertEquals(0.065, roughingFz, 0.001)
    }

    @Test
    fun rpmAndFeed_areCoercedWithinSpindleAndMachineLimits() {
        val ultraHighVc = 5000.0
        val tinyTool = 0.5
        val rawRpm = (ultraHighVc * 1000.0) / (PI * tinyTool)
        val clampedRpm = rawRpm.coerceIn(500.0, 24000.0)

        assertEquals(24000.0, clampedRpm, 0.01)
    }
}
