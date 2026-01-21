package com.movtery.zalithlauncher.game.version.installed.utils

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class VersionCompareTest {
    @Test
    fun testLegacySnapshotRecognition() {
        assertTrue("20w06a".isLegacySnapshotVer())
        assertTrue("23w18b".isLegacySnapshotVer())
        assertFalse("1.21.4".isLegacySnapshotVer())
    }

    @Test
    fun testLegacyReleaseRecognition() {
        assertTrue("1.7".isLegacyReleaseVer())
        assertTrue("1.8".isLegacyReleaseVer())
        assertFalse("25w41a".isLegacyReleaseVer())
    }

    @Test
    fun testNewVersionFormatRecognition() {
        assertTrue("25.4-snapshot-1".isNewVersionFormat())
        assertTrue("26.1".isNewVersionFormat())
        assertTrue("26.1-snapshot-2".isNewVersionFormat())
        assertTrue("26.3.5".isNewVersionFormat())
        assertFalse("random-string".isNewVersionFormat())
    }


    @Test
    fun testIsSnapshotVer() {
        assertTrue("20w14c".isSnapshotVer())
        assertTrue("25.4-snapshot-1".isSnapshotVer())
        assertFalse("1.21.4".isSnapshotVer())
    }

    @Test
    fun testIsReleaseVer() {
        assertTrue("1.21.5".isReleaseVer())
        assertTrue("26.2".isReleaseVer())
        assertFalse("23w51a".isReleaseVer())
    }


    @Test
    fun testCompareLegacyReleases() {
        assertTrue("1.8".isBiggerVer("1.7", "20w10a"))
        assertTrue("1.0".isBiggerOrEqualVer("1.0", "20w10a"))
        assertTrue("1.16".isLowerVer("1.17", "20w10a"))
    }

    @Test
    fun testCompareLegacySnapshots() {
        assertTrue("20w14b".isBiggerVer("someRelease", "20w14a"))
        assertTrue("20w14a".isLowerVer("someRelease", "20w14b"))
    }

    @Test
    fun testCompareNewReleases() {
        assertTrue("26.3".isBiggerOrEqualVer("26.2", "25.4-snapshot-1"))
        assertTrue("26.1".isLowerOrEqualVer("26.1", "someSnapshot"))
        assertFalse("26.1".isBiggerVer("26.2", "someSnapshot"))
    }

    @Test
    fun testCompareNewSnapshots() {
        assertTrue("25.4-snapshot-2".isBiggerVer("25.4", "25.4-snapshot-1"))
        assertTrue("25.4-snapshot-1".isLowerVer("25.4", "25.4-snapshot-2"))
    }


    @Test
    fun testNewVsLegacy() {
        assertTrue("26.1".isBiggerVer("1.21.11", "25.4-snapshot-1"))
        assertTrue("26.2-snapshot-1".isBiggerVer("1.21.11", "25w10a"))
        assertTrue("25.4-snapshot-1".isBiggerVer("23w40a", "1.21.11"))
        assertFalse("20w14a".isBiggerVer("1.21.11", "25w10a"))
    }



    @Test
    fun testUnknownFormats() {
        // 未知格式不被解析为 new/legacy 类型
        assertFalse("not-a-version".isNewVersionFormat())
        assertFalse("not-a-version".isSnapshotVer())
        assertFalse("not-a-version".isReleaseVer())
    }

    @Test
    fun testVersionEquality() {
        assertTrue("1.21.5".isBiggerOrEqualVer("1.21.5", "20w30a"))
        assertTrue("20w30a".isLowerOrEqualVer("1.21.5", "20w30a"))
    }

}