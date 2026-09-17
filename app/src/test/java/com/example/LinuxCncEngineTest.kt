package com.example

import com.example.model.HardwareArchitecture
import com.example.model.MachineStateEnum
import com.example.service.LinuxCncEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LinuxCncEngineTest {

    private lateinit var engine: LinuxCncEngine

    @Before
    fun setup() {
        engine = LinuxCncEngine()
    }

    @Test
    fun initialState_isIdle_andAxesExist() {
        assertEquals(MachineStateEnum.IDLE, engine.machineState.value)
        val axes = engine.axes.value
        assertTrue("Expected X axis", axes.containsKey("X"))
        assertTrue("Expected Y axis", axes.containsKey("Y"))
        assertTrue("Expected Z axis", axes.containsKey("Z"))
        assertTrue("Expected A axis", axes.containsKey("A"))
    }

    @Test
    fun toggleEstop_engagesEstop_abortsSpindle() {
        // Given spindle is running
        engine.powerOn()
        engine.setSpindleRpm(12000.0)
        engine.toggleSpindle()
        assertTrue(engine.spindle.value.isEnabled)

        // When E-Stop is hit
        engine.toggleEstop()

        // Then machine goes to ESTOP and spindle is immediately disabled
        assertEquals(MachineStateEnum.ESTOP, engine.machineState.value)
        assertFalse("Spindle must be disabled on ESTOP", engine.spindle.value.isEnabled)
    }

    @Test
    fun toggleEstop_fromEstop_recoversToOff() {
        // Engage E-Stop
        engine.toggleEstop()
        assertEquals(MachineStateEnum.ESTOP, engine.machineState.value)

        // Reset E-Stop
        engine.toggleEstop()
        assertEquals(MachineStateEnum.OFF, engine.machineState.value)
    }

    @Test
    fun powerOn_blockedWhenEstopEngaged() {
        engine.toggleEstop()
        assertEquals(MachineStateEnum.ESTOP, engine.machineState.value)

        engine.powerOn()
        assertEquals("Power ON must be rejected during ESTOP", MachineStateEnum.ESTOP, engine.machineState.value)
    }

    @Test
    fun powerOn_powersOnFromOff() {
        engine.powerOff()
        assertEquals(MachineStateEnum.OFF, engine.machineState.value)

        engine.powerOn()
        assertEquals(MachineStateEnum.ON, engine.machineState.value)
    }

    @Test
    fun spindleRpm_updatesCommandedRpm() {
        engine.setSpindleRpm(15000.0)
        assertEquals(15000.0, engine.spindle.value.commandedRpm, 0.01)

        engine.setSpindleRpm(8000.0)
        assertEquals(8000.0, engine.spindle.value.commandedRpm, 0.01)
    }

    @Test
    fun overrides_clampWithinPermittedRanges() {
        engine.setSpindleOverride(250)
        assertEquals(200, engine.spindle.value.overridePct)

        engine.setSpindleOverride(5)
        assertEquals(10, engine.spindle.value.overridePct)

        engine.setFeedOverride(250)
        assertEquals(200, engine.feed.value.feedOverridePct)

        engine.setFeedOverride(-5)
        assertEquals(0, engine.feed.value.feedOverridePct)
    }

    @Test
    fun stepJog_incrementsAxisPosition() {
        engine.powerOn()
        val initialX = engine.axes.value["X"]?.workPos ?: 0.0
        engine.stepJog("X", 1, 1.5) // +1.5 mm step

        val finalX = engine.axes.value["X"]?.workPos ?: 0.0
        assertEquals(initialX + 1.5, finalX, 0.001)
    }

    @Test
    fun zeroAxis_setsWorkCoordinateToZero() {
        engine.zeroAxis("Y")
        val yWork = engine.axes.value["Y"]?.workPos
        assertEquals(0.0, yWork ?: -1.0, 0.001)
    }

    @Test
    fun homeAxis_marksAxisAsHomed() {
        engine.homeAxis("Z")
        val zAxis = engine.axes.value["Z"]
        assertNotNull(zAxis)
        assertTrue("Axis Z should be marked as homed", zAxis!!.isHomed)
    }

    @Test
    fun loadGCodeContent_parsesBlocksAccurately() {
        val gcode = """
            G21 (Metric)
            G0 X10.5 Y20.0 Z5.0
            G1 Z-2.0 F300
            G1 X30.0 Y40.0 F1200
            M2
        """.trimIndent()

        engine.loadGCodeContent("test_part.nc", gcode)

        assertEquals("test_part.nc", engine.loadedFileName.value)
        val segments = engine.loadedGCode.value
        assertEquals(5, segments.size)

        val rapidSegment = segments[1]
        assertTrue("Expected rapid move for G0", rapidSegment.isRapid)
        assertEquals(10.5f, rapidSegment.endX, 0.01f)
        assertEquals(20.0f, rapidSegment.endY, 0.01f)

        val cutSegment = segments[2]
        assertTrue("Expected cutting move for G1", cutSegment.isCut)
        assertEquals(-2.0f, cutSegment.endZ, 0.01f)
    }

    @Test
    fun setHardwareArchitecture_updatesCapabilitiesAndSlaves() {
        // Switch to MESA FPGA
        engine.switchArchitecture(HardwareArchitecture.MESA_FPGA)
        assertEquals(HardwareArchitecture.MESA_FPGA, engine.capabilities.value.architecture)
        assertFalse("MESA architecture should not report EtherCAT", engine.capabilities.value.hasEtherCat)

        // Switch to Delta B3 EtherCAT
        engine.switchArchitecture(HardwareArchitecture.ETHERCAT_DELTA)
        assertEquals(HardwareArchitecture.ETHERCAT_DELTA, engine.capabilities.value.architecture)
        assertTrue("Delta B3 architecture should report EtherCAT", engine.capabilities.value.hasEtherCat)
    }
}
