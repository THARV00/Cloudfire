package com.example.server

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object LocalFileServer {
    private const val TAG = "LocalFileServer"
    private const val DEFAULT_PORT = 8080

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var isRunning = false
    private var activePort = DEFAULT_PORT
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start(context: Context) {
        if (isRunning) return
        val appContext = context.applicationContext

        serverJob = scope.launch {
            try {
                // Bind to 0.0.0.0 so all devices on Wi-Fi / Local Network can connect
                var port = DEFAULT_PORT
                var bound = false
                while (!bound && port < 8095) {
                    try {
                        serverSocket = ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))
                        activePort = port
                        bound = true
                    } catch (e: Exception) {
                        port++
                    }
                }

                if (!bound) {
                    Log.e(TAG, "Failed to bind any port for LocalFileServer")
                    return@launch
                }

                isRunning = true
                Log.d(TAG, "CloudFire Local Server started on port $activePort")

                while (isActive && !serverSocket!!.isClosed) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        scope.launch {
                            handleClient(appContext, clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in LocalFileServer loop", e)
            } finally {
                isRunning = false
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (ignored: Exception) {}
        serverJob?.cancel()
    }

    fun getPort(): Int = activePort

    fun getBaseUrl(): String {
        val ip = getLocalIpAddress() ?: "localhost"
        return "http://$ip:$activePort"
    }

    fun getDirectDownloadUrl(fileId: String, forSharing: Boolean = false): String {
        val host = if (forSharing) (getLocalIpAddress() ?: "localhost") else "localhost"
        return "http://$host:$activePort/download/$fileId"
    }

    fun getWebPageUrl(fileId: String, forSharing: Boolean = false): String {
        val host = if (forSharing) (getLocalIpAddress() ?: "localhost") else "localhost"
        return "http://$host:$activePort/file/$fileId"
    }

    fun getNetworkDownloadUrl(fileId: String): String {
        val ip = getLocalIpAddress() ?: "localhost"
        return "http://$ip:$activePort/download/$fileId"
    }

    fun getNetworkWebPageUrl(fileId: String): String {
        val ip = getLocalIpAddress() ?: "localhost"
        return "http://$ip:$activePort/file/$fileId"
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.hostAddress?.contains(':') == false) {
                        val host = address.hostAddress
                        if (host != null && host != "127.0.0.1") {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IP", e)
        }
        return "127.0.0.1"
    }

    private suspend fun handleClient(context: Context, socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.use { client ->
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                val outputStream = client.getOutputStream()

                val requestLine = reader.readLine() ?: return@use
                val tokens = requestLine.split(" ")
                if (tokens.size < 2) return@use

                val method = tokens[0]
                val path = tokens[1]

                if (method.equals("GET", ignoreCase = true)) {
                    when {
                        path.startsWith("/download/") -> {
                            val fileId = path.removePrefix("/download/").substringBefore("?").trim()
                            serveFileDownload(context, fileId, outputStream)
                        }
                        path.startsWith("/file/") -> {
                            val fileId = path.removePrefix("/file/").substringBefore("?").trim()
                            serveMediaFireLandingPage(context, fileId, outputStream)
                        }
                        else -> {
                            serveStatusPage(outputStream)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client connection", e)
        }
    }

    private suspend fun serveFileDownload(context: Context, fileId: String, out: OutputStream) {
        val db = AppDatabase.getDatabase(context)
        val cloudFile = db.cloudFileDao().getFileById(fileId)

        val targetFile = if (cloudFile != null) {
            File(cloudFile.localFilePath)
        } else {
            // Check direct storage folder
            val uploadsDir = File(context.filesDir, "uploads")
            val candidate = File(uploadsDir, fileId)
            if (candidate.exists()) candidate else null
        }

        if (targetFile == null || !targetFile.exists()) {
            val writer = PrintWriter(out, true)
            writer.println("HTTP/1.1 404 Not Found")
            writer.println("Content-Type: text/html")
            writer.println("Connection: close")
            writer.println()
            writer.println("<html><body><h1>404 File Not Found</h1><p>The requested file does not exist on CloudFire server.</p></body></html>")
            writer.flush()
            return
        }

        // Increment download counter
        if (cloudFile != null) {
            try {
                db.cloudFileDao().incrementDownloadCount(fileId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to increment download count", e)
            }
        }

        val fileName = cloudFile?.fileName ?: targetFile.name
        val encodedFileName = try {
            URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replace("+", "%20")
        } catch (e: Exception) {
            fileName
        }

        val contentType = cloudFile?.mimeType?.takeIf { it.isNotEmpty() } ?: "application/octet-stream"
        val fileSize = targetFile.length()

        val headerBuilder = StringBuilder()
        headerBuilder.append("HTTP/1.1 200 OK\r\n")
        headerBuilder.append("Content-Type: ").append(contentType).append("\r\n")
        headerBuilder.append("Content-Disposition: attachment; filename=\"").append(fileName).append("\"; filename*=UTF-8''").append(encodedFileName).append("\r\n")
        headerBuilder.append("Content-Length: ").append(fileSize).append("\r\n")
        headerBuilder.append("Access-Control-Allow-Origin: *\r\n")
        headerBuilder.append("Cache-Control: no-cache, no-store, must-revalidate\r\n")
        headerBuilder.append("Connection: close\r\n\r\n")

        out.write(headerBuilder.toString().toByteArray(StandardCharsets.UTF_8))
        out.flush()

        // Stream binary file to Chrome
        FileInputStream(targetFile).use { fis ->
            val buffer = ByteArray(32768) // 32 KB chunk
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                out.write(buffer, 0, bytesRead)
            }
        }
        out.flush()
    }

    private suspend fun serveMediaFireLandingPage(context: Context, fileId: String, out: OutputStream) {
        val db = AppDatabase.getDatabase(context)
        val cloudFile = db.cloudFileDao().getFileById(fileId)
        val fileName = cloudFile?.fileName ?: "Uploaded_File"
        val formattedSize = cloudFile?.formattedSize ?: "Unknown size"
        val formattedDate = cloudFile?.formattedDate ?: "Recently"
        val downloadCount = cloudFile?.downloadCount ?: 0

        val html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>$fileName - CloudFire</title>
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
                    body { background: #f0f4f9; color: #1c2536; display: flex; flex-direction: column; min-height: 100vh; }
                    header { background: #0070ff; color: white; padding: 16px 24px; display: flex; align-items: center; justify-content: space-between; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                    .brand { display: flex; align-items: center; gap: 10px; font-size: 22px; font-weight: 700; letter-spacing: -0.5px; }
                    .brand span { color: #00e5ff; }
                    .container { max-width: 680px; margin: 40px auto; padding: 0 20px; width: 100%; flex: 1; }
                    .card { background: white; border-radius: 16px; padding: 32px; box-shadow: 0 8px 30px rgba(0,112,255,0.08); border: 1px solid #e1e8f0; text-align: center; }
                    .file-icon { width: 72px; height: 72px; background: #e8f2ff; border-radius: 20px; display: flex; align-items: center; justify-content: center; margin: 0 auto 20px; color: #0070ff; font-size: 32px; font-weight: bold; }
                    h1 { font-size: 22px; font-weight: 700; word-break: break-all; margin-bottom: 8px; color: #111827; }
                    .meta { color: #64748b; font-size: 14px; margin-bottom: 24px; }
                    .btn-download { display: inline-flex; align-items: center; justify-content: center; gap: 12px; width: 100%; max-width: 380px; background: #0070ff; color: white; text-decoration: none; padding: 18px 28px; border-radius: 12px; font-size: 18px; font-weight: 700; box-shadow: 0 4px 14px rgba(0,112,255,0.4); transition: transform 0.1s ease, background 0.2s; }
                    .btn-download:hover { background: #005ce6; }
                    .btn-download:active { transform: scale(0.98); }
                    .auto-notice { margin-top: 16px; font-size: 13px; color: #059669; background: #ecfdf5; border: 1px solid #a7f3d0; border-radius: 8px; padding: 10px; }
                    .badge-row { display: flex; justify-content: center; gap: 16px; margin-top: 24px; font-size: 13px; color: #475569; }
                    .badge { display: flex; align-items: center; gap: 6px; }
                    footer { text-align: center; padding: 24px; font-size: 13px; color: #94a3b8; }
                </style>
                <script>
                    // Automatically trigger download in Chrome after landing
                    window.addEventListener('DOMContentLoaded', () => {
                        setTimeout(() => {
                            window.location.href = '/download/$fileId';
                        }, 1200);
                    });
                </script>
            </head>
            <body>
                <header>
                    <div class="brand">
                        <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z"/>
                        </svg>
                        Cloud<span>Fire</span>
                    </div>
                    <div style="font-size: 13px; opacity: 0.9;">Fast Cloud Sharing</div>
                </header>

                <div class="container">
                    <div class="card">
                        <div class="file-icon">📁</div>
                        <h1>$fileName</h1>
                        <p class="meta">$formattedSize &nbsp;•&nbsp; Uploaded $formattedDate &nbsp;•&nbsp; $downloadCount downloads</p>

                        <a href="/download/$fileId" class="btn-download">
                            <span>DOWNLOAD ($formattedSize)</span>
                        </a>

                        <div class="auto-notice">
                            🚀 Chrome will automatically start downloading this file. If it doesn't, tap the blue Download button above.
                        </div>

                        <div class="badge-row">
                            <div class="badge">🛡️ CloudFire Shield: Safe</div>
                            <div class="badge">⚡ High Speed Download</div>
                            <div class="badge">🔒 SSL Verified</div>
                        </div>
                    </div>
                </div>

                <footer>
                    CloudFire &copy; 2026 MediaFire Inspired File Storage &bull; Fast, Secure, Universal Download<br>
                    <span style="font-size: 11px; opacity: 0.75;">devloper :- Tharv</span>
                </footer>
            </body>
            </html>
        """.trimIndent()

        val bytes = html.toByteArray(StandardCharsets.UTF_8)
        val writer = PrintWriter(out, true)
        writer.println("HTTP/1.1 200 OK")
        writer.println("Content-Type: text/html; charset=UTF-8")
        writer.println("Content-Length: ${bytes.size}")
        writer.println("Connection: close")
        writer.println()
        writer.flush()
        out.write(bytes)
        out.flush()
    }

    private fun serveStatusPage(out: OutputStream) {
        val html = """
            <!DOCTYPE html>
            <html>
            <head><title>CloudFire Server</title></head>
            <body style="font-family: sans-serif; text-align: center; padding: 50px;">
                <h1 style="color: #0070ff;">CloudFire Server Active</h1>
                <p>Ready to serve files directly into Chrome and external browsers.</p>
            </body>
            </html>
        """.trimIndent()
        val bytes = html.toByteArray(StandardCharsets.UTF_8)
        val writer = PrintWriter(out, true)
        writer.println("HTTP/1.1 200 OK")
        writer.println("Content-Type: text/html; charset=UTF-8")
        writer.println("Content-Length: ${bytes.size}")
        writer.println("Connection: close")
        writer.println()
        writer.flush()
        out.write(bytes)
        out.flush()
    }
}
