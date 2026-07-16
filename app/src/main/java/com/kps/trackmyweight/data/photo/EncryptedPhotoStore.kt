package com.kps.trackmyweight.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stockage local chiffré (AES-256) des photos de progression.
 * - Photos originales : chiffrées via `EncryptedFile` (androidx.security)
 * - Miniatures : stockées en clair (leur exposition n'est pas critique et permet un affichage rapide)
 *
 * Clef maîtresse gérée par Android Keystore.
 */
@Singleton
class EncryptedPhotoStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val photosDir: File by lazy {
        File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }
    }

    private val thumbsDir: File by lazy {
        File(context.filesDir, "thumbs").apply { if (!exists()) mkdirs() }
    }

    /**
     * Écrit une photo chiffrée + génère et écrit sa miniature.
     * Retourne les chemins absolus (encryptedPath, thumbnailPath).
     */
    suspend fun save(bytes: ByteArray, baseName: String): Pair<String, String> {
        val encryptedFile = File(photosDir, "$baseName.enc")
        if (encryptedFile.exists()) encryptedFile.delete()
        EncryptedFile.Builder(
            context, encryptedFile, masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
        ).build().openFileOutput().use { it.write(bytes) }

        val thumb = generateThumbnail(bytes, maxDim = 480)
        val thumbFile = File(thumbsDir, "$baseName.jpg")
        FileOutputStream(thumbFile).use { thumb.compress(Bitmap.CompressFormat.JPEG, 80, it) }

        return encryptedFile.absolutePath to thumbFile.absolutePath
    }

    /**
     * Décrypte une photo. Utilise avec parcimonie (grosse taille).
     */
    fun readDecrypted(encryptedPath: String): ByteArray {
        val file = File(encryptedPath)
        val ef = EncryptedFile.Builder(
            context, file, masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
        ).build()
        return ef.openFileInput().use { it.readBytes() }
    }

    suspend fun delete(encryptedPath: String, thumbnailPath: String) {
        runCatching { File(encryptedPath).delete() }
        runCatching { File(thumbnailPath).delete() }
    }

    private fun generateThumbnail(bytes: ByteArray, maxDim: Int): Bitmap {
        val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val ratio = source.width.toFloat() / source.height.toFloat()
        val (w, h) = if (source.width >= source.height) {
            maxDim to (maxDim / ratio).toInt().coerceAtLeast(1)
        } else {
            (maxDim * ratio).toInt().coerceAtLeast(1) to maxDim
        }
        val scaled = Bitmap.createScaledBitmap(source, w, h, true)
        if (scaled != source) source.recycle()
        return scaled
    }
}
