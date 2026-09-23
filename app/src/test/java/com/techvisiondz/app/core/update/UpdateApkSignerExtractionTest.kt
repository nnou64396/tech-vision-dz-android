package com.techvisiondz.app.core.update

import com.android.apksig.ApkSigner
import com.android.apksig.KeyConfig
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Random
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertArrayEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Hermetic JVM coverage for [apkSigningBlockCertificates]: builds a genuine
 * v2-only signed APK (the same scheme flag combination the production release
 * uses) with apksig itself, then exercises the extraction rules. No Android
 * APIs and no external fixtures are required.
 */
class UpdateApkSignerExtractionTest {

    @get:Rule
    val tmp = TemporaryFolder()

    // The binary AndroidManifest.xml is taken from the v1.1.6 production APK so
    // the fixture APKs apksig parses are structurally realistic.
    private fun manifestBytes(): ByteArray =
        javaClass.getResourceAsStream("/update/AndroidManifest.xml")?.use { it.readBytes() }
            ?: error("test fixture /update/AndroidManifest.xml is missing")

    private fun manifestOnlyApk(name: String): File {
        val file = File(tmp.root, name)
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zos.write(manifestBytes())
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("res/raw/note.txt"))
            zos.write("fixture content".encodeToByteArray())
            zos.closeEntry()
        }
        return file
    }

    private fun signV2Only(input: File, outputName: String, key: KeyPair, cert: X509Certificate): File {
        val output = File(tmp.root, outputName)
        val config = ApkSigner.SignerConfig.Builder(
            "fixture",
            KeyConfig.Jca(key.private),
            listOf(cert),
        ).build()
        ApkSigner.Builder(listOf(config))
            .setInputApk(input)
            .setOutputApk(output)
            .setV1SigningEnabled(false)
            .setV2SigningEnabled(true)
            .build()
            .sign()
        return output
    }

    private fun keyPair(): KeyPair =
        KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    private fun selfSignedCert(key: KeyPair, commonName: String): X509Certificate {
        val name = X500Name("CN=$commonName")
        val now = Instant.now()
        val builder = JcaX509v3CertificateBuilder(
            name,
            BigInteger.valueOf(Math.abs(Random().nextLong())),
            Date.from(now.minus(1, ChronoUnit.DAYS)),
            Date.from(now.plus(3650, ChronoUnit.DAYS)),
            name,
            key.public,
        )
        builder.addExtension(Extension.basicConstraints, true, BasicConstraints(true))
        builder.addExtension(
            Extension.keyUsage,
            true,
            KeyUsage(KeyUsage.keyCertSign or KeyUsage.digitalSignature),
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(key.private)
        return JcaX509CertificateConverter().getCertificate(builder.build(signer))
    }

    private fun flipAByte(file: File): File {
        val bytes = file.readBytes()
        bytes[bytes.size / 2] = (bytes[bytes.size / 2].toInt() xor 0xFF).toByte()
        val tampered = File(tmp.root, "${file.name}.tampered")
        tampered.writeBytes(bytes)
        return tampered
    }

    private fun assertVerified(extraction: ApkSigners): List<ByteArray> {
        assertTrue("expected Verified, got $extraction", extraction is ApkSigners.Verified)
        return (extraction as ApkSigners.Verified).certificates
    }

    @Test
    fun `verified apk exposes exactly its signing certificate`() {
        val key = keyPair()
        val cert = selfSignedCert(key, "fixture-a")
        val apk = signV2Only(manifestOnlyApk("unsigned-a.apk"), "signed-a.apk", key, cert)

        val certificates = assertVerified(apkSigningBlockCertificates(apk))

        assertEquals(1, certificates.size)
        assertArrayEquals("extraction must return the exact DER certificate bytes", cert.encoded, certificates[0])
        assertEquals(
            "the extracted certificate must match the installed one byte-for-byte",
            SignatureCompare.Match,
            compareSignatures(installed = listOf(cert.encoded), apk = certificates),
        )
    }

    @Test
    fun `a different signer is a definite mismatch, never unverifiable`() {
        val keyA = keyPair()
        val certA = selfSignedCert(keyA, "fixture-a")
        val apkA = signV2Only(manifestOnlyApk("unsigned-a.apk"), "signed-a.apk", keyA, certA)

        val keyB = keyPair()
        val certB = selfSignedCert(keyB, "fixture-b")
        val apkB = signV2Only(manifestOnlyApk("unsigned-b.apk"), "signed-b.apk", keyB, certB)

        val certsA = assertVerified(apkSigningBlockCertificates(apkA))
        val certsB = assertVerified(apkSigningBlockCertificates(apkB))

        assertArrayEquals("fixture-a extraction", certA.encoded, certsA[0])
        assertArrayEquals("fixture-b extraction", certB.encoded, certsB[0])

        assertEquals(
            "installed cert A vs a different APK cert B must be a mismatch",
            SignatureCompare.Mismatch,
            compareSignatures(installed = certsA, apk = certsB),
        )
    }

    @Test
    fun `tampered signed apk is unverifiable`() {
        val key = keyPair()
        val cert = selfSignedCert(key, "fixture-a")
        val apk = signV2Only(manifestOnlyApk("unsigned-a.apk"), "signed-a.apk", key, cert)

        assertEquals(ApkSigners.Verified::class, apkSigningBlockCertificates(apk)::class)
        assertEquals(
            "a single flipped byte must collapse to Unverifiable, not a match",
            ApkSigners.Unverifiable,
            apkSigningBlockCertificates(flipAByte(apk)),
        )
    }

    @Test
    fun `unsigned apk is unverifiable`() {
        assertEquals(
            ApkSigners.Unverifiable,
            apkSigningBlockCertificates(manifestOnlyApk("unsigned.apk")),
        )
    }

    @Test
    fun `a non apk file is unverifiable`() {
        val garbage = File(tmp.root, "garbage.bin")
        FileOutputStream(garbage).use { it.write("this is not a zip or apk".encodeToByteArray()) }

        assertEquals(ApkSigners.Unverifiable, apkSigningBlockCertificates(garbage))
    }

    @Test
    fun `missing file is unverifiable`() {
        assertEquals(
            ApkSigners.Unverifiable,
            apkSigningBlockCertificates(File(tmp.root, "does-not-exist.apk")),
        )
    }
}