package com.sunilsstartingseven.app.data.media

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.sunilsstartingseven.app.data.local.AppDatabase
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import kotlin.math.max

const val PLAYER_PHOTO_MAX_EDGE = 1024
const val GAME_PHOTO_MAX_EDGE = 1920
const val MEDIA_JPEG_QUALITY = 86
const val MAX_SOURCE_IMAGE_BYTES = 25 * 1024 * 1024
const val MAX_STORED_IMAGE_BYTES = 5 * 1024 * 1024

class MediaException(message: String) : Exception(message)

enum class MediaOwner(val dirName: String, val maxEdge: Int) { PLAYER("players", PLAYER_PHOTO_MAX_EDGE), GAME("games", GAME_PHOTO_MAX_EDGE) }

class MediaRepository(private val context: Context, private val db: AppDatabase) {
    private val root: File get() = File(context.filesDir, "media")
    private val tempRoot: File get() = File(context.cacheDir, "media-temp")

    fun fileFor(relativePath: String?): File? {
        if (relativePath.isNullOrBlank()) return null
        if (!isSafeMediaPath(relativePath)) return null
        val file = File(context.filesDir, relativePath)
        return if (file.exists()) file else null
    }

    fun prepareCameraFile(owner: MediaOwner): File {
        val dir = File(tempRoot, owner.dirName).apply { mkdirs() }
        return File(dir, "capture_${UUID.randomUUID()}.jpg")
    }

    suspend fun setPlayerPhoto(playerId: Long, resolver: ContentResolver, uri: Uri) {
        val next = importPhoto(resolver, uri, MediaOwner.PLAYER, "player_${playerId}.jpg")
        val previous = db.playerDao().get(playerId)?.photoPath
        if (db.playerDao().updatePhotoPath(playerId, next) != 1) {
            deleteQuietly(next)
            throw MediaException("Player no longer exists.")
        }
        cleanupPrevious(previous, next)
    }

    suspend fun removePlayerPhoto(playerId: Long) {
        val previous = db.playerDao().get(playerId)?.photoPath
        if (db.playerDao().updatePhotoPath(playerId, null) != 1) throw MediaException("Player no longer exists.")
        cleanupPrevious(previous, null)
    }

    suspend fun setGamePhoto(gameId: Long, resolver: ContentResolver, uri: Uri) {
        val next = importPhoto(resolver, uri, MediaOwner.GAME, "game_${gameId}.jpg")
        val previous = db.gameDao().get(gameId)?.photoPath
        if (db.gameDao().updatePhotoPath(gameId, next) != 1) {
            deleteQuietly(next)
            throw MediaException("Game no longer exists.")
        }
        cleanupPrevious(previous, next)
    }

    suspend fun removeGamePhoto(gameId: Long) {
        val previous = db.gameDao().get(gameId)?.photoPath
        if (db.gameDao().updatePhotoPath(gameId, null) != 1) throw MediaException("Game no longer exists.")
        cleanupPrevious(previous, null)
    }

    fun clearAllMedia() { deleteRecursively(root) }

    fun clearTemp() { deleteRecursively(tempRoot) }

    fun deleteOrphanedMedia(keep: Set<String>) {
        listMediaFiles().forEach { file ->
            val rel = relativeMediaPath(file)
            if (rel !in keep) file.delete()
        }
    }

    fun listMediaFiles(): List<File> = if (!root.exists()) emptyList() else root.walkTopDown().filter { it.isFile }.toList()

    fun relativeMediaPath(file: File): String = file.relativeTo(context.filesDir).invariantSeparatorsPath

    fun writeRestoredMedia(relativePath: String, bytes: ByteArray) {
        if (!isSafeMediaPath(relativePath)) throw MediaException("Backup contains an unsafe media path.")
        if (bytes.size > MAX_STORED_IMAGE_BYTES) throw MediaException("Backup contains an oversized image.")
        ensureJpeg(bytes)
        val out = File(context.filesDir, relativePath)
        out.parentFile?.mkdirs()
        FileOutputStream(out).use { it.write(bytes) }
    }

    fun readOwnedMedia(relativePath: String): ByteArray {
        val file = fileFor(relativePath) ?: throw MediaException("A referenced media file is missing.")
        val bytes = file.readBytes()
        if (bytes.size > MAX_STORED_IMAGE_BYTES) throw MediaException("A media file is too large for backup.")
        ensureJpeg(bytes)
        return bytes
    }

    fun isSafeMediaPath(path: String): Boolean {
        val normalized = path.replace('\\', '/')
        if (!normalized.startsWith("media/players/") && !normalized.startsWith("media/games/")) return false
        if (normalized.contains("..") || normalized.startsWith("/") || normalized.contains(':')) return false
        return normalized.endsWith(".jpg") && normalized == path
    }

    private fun importPhoto(resolver: ContentResolver, uri: Uri, owner: MediaOwner, fileName: String): String {
        val temp = File(tempRoot, "incoming_${UUID.randomUUID()}.jpg").apply { parentFile?.mkdirs() }
        try {
            resolver.openInputStream(uri)?.use { input -> normalize(input, temp, owner.maxEdge) } ?: throw MediaException("Couldn't use that photo.")
            if (temp.length() <= 0 || temp.length() > MAX_STORED_IMAGE_BYTES) throw MediaException("Couldn't use that photo.")
            val relative = "media/${owner.dirName}/$fileName"
            val final = File(context.filesDir, relative).apply { parentFile?.mkdirs() }
            temp.copyTo(final, overwrite = true)
            return relative
        } catch (error: MediaException) {
            throw error
        } catch (error: Exception) {
            Log.e("StartingSeven", "Unable to import photo", error)
            throw MediaException("Couldn't use that photo.")
        } finally {
            temp.delete()
        }
    }

    private fun normalize(input: InputStream, output: File, maxEdge: Int) {
        val source = input.readCapped(MAX_SOURCE_IMAGE_BYTES)
        if (source.size > MAX_SOURCE_IMAGE_BYTES) throw MediaException("Couldn't use that photo.")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw MediaException("Couldn't use that photo.")
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxEdge) }
        val decoded = BitmapFactory.decodeByteArray(source, 0, source.size, opts) ?: throw MediaException("Couldn't use that photo.")
        val oriented = orient(decoded, source)
        val scaled = scaleDown(oriented, maxEdge)
        FileOutputStream(output).use { out ->
            if (!scaled.compress(Bitmap.CompressFormat.JPEG, MEDIA_JPEG_QUALITY, out)) throw MediaException("Couldn't use that photo.")
        }
        if (scaled !== oriented) scaled.recycle()
        if (oriented !== decoded) oriented.recycle()
        decoded.recycle()
    }

    private fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while (max(width / sample, height / sample) > maxEdge * 2) sample *= 2
        return sample
    }

    private fun orient(bitmap: Bitmap, source: ByteArray): Bitmap {
        val orientation = runCatching { ExifInterface(source.inputStream()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val edge = max(bitmap.width, bitmap.height)
        if (edge <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / edge.toFloat()
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
    }

    private fun cleanupPrevious(previous: String?, next: String?) {
        if (previous != null && previous != next) deleteQuietly(previous)
    }

    private fun deleteQuietly(relativePath: String?) {
        val file = fileFor(relativePath) ?: return
        if (!file.delete()) Log.w("StartingSeven", "Could not delete media file $relativePath")
    }

    private fun deleteRecursively(file: File) {
        if (file.exists() && !file.deleteRecursively()) Log.w("StartingSeven", "Could not delete ${file.absolutePath}")
    }

    private fun InputStream.readCapped(maxBytes: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            output.write(buffer, 0, read)
            if (output.size() > maxBytes) break
        }
        return output.toByteArray()
    }

    private fun ensureJpeg(bytes: ByteArray) {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        if (opts.outWidth <= 0 || opts.outHeight <= 0) throw MediaException("Backup contains a media file that is not a valid image.")
    }
}

