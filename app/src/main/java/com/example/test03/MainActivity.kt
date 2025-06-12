package com.example.test03

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.*
import java.util.zip.ZipFile

class MainActivity : ComponentActivity() {
    private val lifecycleScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var resultText by mutableStateOf("")
    private val dqtExtractor = JpegDQTExtractor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 앱 시작 시 필요한 파일들을 설치
        installRequiredFiles()

        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LazyRow(
                    modifier = Modifier.padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Button(onClick = { runExifTool() }) {
                            Text("ExifTool")
                        }
                    }
                    item {
                        Button(onClick = { extractDQTWithKotlin() }) {
                            Text("Kotlin DQT")
                        }
                    }
//                    item {
//                        Button(onClick = { saveDQTToFile() }) {
//                            Text("DQT 저장")
//                        }
//                    }
                }

                TextField(
                    value = resultText,
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                    label = { Text("DQT 추출 결과") }
                )
            }
        }

    }

    private fun installRequiredFiles() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 기존 ZIP 파일들 압축 해제
//                extractZipFromAssets("perl5-5.41.8_armv7a_precompiled.zip", "${filesDir}/perl")
//                extractZipFromAssets("exiftool_files.zip", "${filesDir}/exiftool")

                // ExifTool tar.gz 압축 해제 (새로 추가)
//                extractTarGzFromAssets("Image-ExifTool-13.30.tar.gz", "${filesDir}/exif")

                // 테스트 이미지 복사
                copyTestImage()

                // 실행 권한 설정
//                setPerlExecutable()
//                setExifToolExecutable()

                withContext(Dispatchers.Main) {
                    resultText = "모든 파일 설치 완료"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    resultText = "설치 실패: ${e.message}"
                }
            }
        }
    }




    private fun extractZipFromAssets(zipFileName: String, destDir: String) {
        // Assets에서 zip 파일을 임시로 복사
        val tempZipFile = File(filesDir, "temp_$zipFileName")
        assets.open(zipFileName).use { inputStream ->
            FileOutputStream(tempZipFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        // Zip 파일 압축 해제
        unzipFile(tempZipFile, destDir)

        // 임시 파일 삭제
        tempZipFile.delete()
    }

    private fun unzipFile(zipFile: File, destDir: String) {
        File(destDir).apply {
            if (!exists()) {
                mkdirs()
            }
        }

        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                zip.getInputStream(entry).use { input ->
                    val filePath = "$destDir${File.separator}${entry.name}"

                    if (!entry.isDirectory) {
                        // 파일 압축 해제
                        extractFile(input, filePath)
                    } else {
                        // 디렉토리 생성
                        File(filePath).mkdirs()
                    }
                }
            }
        }
    }

    private fun extractFile(inputStream: InputStream, destFilePath: String) {
        // 상위 디렉토리 생성
        File(destFilePath).parentFile?.mkdirs()

        BufferedOutputStream(FileOutputStream(destFilePath)).use { bos ->
            val bytesIn = ByteArray(4096)
            var read: Int
            while (inputStream.read(bytesIn).also { read = it } != -1) {
                bos.write(bytesIn, 0, read)
            }
        }
    }

    private fun setPerlExecutable() {
        val perlBinary = File("${filesDir}/perl/bin/perl")
        if (perlBinary.exists()) {
            perlBinary.setExecutable(true)
        }

        // 필요에 따라 다른 실행 파일들도 권한 설정
        val libFiles = File("${filesDir}/perl/lib").listFiles()
        libFiles?.forEach { file ->
            if (file.name.endsWith(".so")) {
                file.setExecutable(true)
            }
        }
    }

    private fun runExifTool() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = executeCommand(
                    listOf(
                        "${filesDir}/perl/bin/perl",
//                        "${filesDir}/exiftool/exiftool.pl",
                        "${filesDir}/exif/Image-ExifTool-13.30/exiftool",
                        "${filesDir}/test.jpg"
                    ),
                    filesDir
                )

                withContext(Dispatchers.Main) {
                    resultText = result
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    resultText = "실행 오류: ${e.message}"
                }
            }
        }
    }

    private fun executeCommand(command: List<String>, workingDir: File): String {
        val processBuilder = ProcessBuilder(command)
        processBuilder.directory(workingDir)
        processBuilder.redirectErrorStream(true)

        val process = processBuilder.start()

        val output = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
        }

        val exitCode = process.waitFor()

        return if (exitCode == 0) {
            output.toString()
        } else {
            "프로세스 실행 실패 (종료 코드: $exitCode)\n$output"
        }
    }

    private fun copyTestImage() {
        try {
            assets.open("test.jpg").use { inputStream ->
                FileOutputStream(File(filesDir, "test.jpg")).use { outputStream ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.flush()
                }
            }
        } catch (e: IOException) {
            throw IOException("이미지 복사 실패: ${e.message}")
        }
    }

    private fun extractTarGzFromAssets(tarGzFileName: String, destDir: String) {
        val tempTarGzFile = File(filesDir, "temp_$tarGzFileName")

        try {
            // Assets에서 임시 파일로 복사
            assets.open(tarGzFileName).use { inputStream ->
                FileOutputStream(tempTarGzFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }
            }

            // Tar.gz 압축 해제 수행
            uncompressTarGz(tempTarGzFile, destDir)

        } finally {
            // 임시 파일 정리
            if (tempTarGzFile.exists()) {
                tempTarGzFile.delete()
            }
        }
    }

    private fun uncompressTarGz(tarGzFile: File, destDir: String) {
        val destDirectory = File(destDir)
        if (!destDirectory.exists()) {
            destDirectory.mkdirs()
        }

        FileInputStream(tarGzFile).use { fis ->
            GzipCompressorInputStream(fis).use { gzIn ->
                TarArchiveInputStream(gzIn).use { tarIn ->
                    var entry: TarArchiveEntry? = tarIn.nextTarEntry

                    while (entry != null) {
                        val outputFile = File(destDirectory, entry.name)

                        if (entry.isDirectory) {
                            // 디렉토리 생성
                            outputFile.mkdirs()
                        } else {
                            // 상위 디렉토리 생성
                            outputFile.parentFile?.mkdirs()

                            // 파일 생성 및 내용 복사
                            FileOutputStream(outputFile).use { fos ->
                                BufferedOutputStream(fos).use { bos ->
                                    val buffer = ByteArray(4096)
                                    var len: Int
                                    while (tarIn.read(buffer).also { len = it } != -1) {
                                        bos.write(buffer, 0, len)
                                    }
                                }
                            }
                        }
                        entry = tarIn.nextTarEntry
                    }
                }
            }
        }
    }

    private fun setExifToolExecutable() {
        try {
            // ExifTool 메인 스크립트 권한 설정
            val exifToolScript = File("${filesDir}/exif/Image-ExifTool-13.30/exiftool")
            if (exifToolScript.exists()) {
                exifToolScript.setExecutable(true)
            }

            // Perl 스크립트 권한 설정
            val exifToolPl = File("${filesDir}/exif/Image-ExifTool-13.30/exiftool.pl")
            if (exifToolPl.exists()) {
                exifToolPl.setExecutable(true)
            }
        } catch (e: Exception) {
            throw IOException("ExifTool 실행 권한 설정 실패: ${e.message}")
        }
    }

    // DQT 코드
    private fun extractDQTWithKotlin() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val imageFile = File(filesDir, "test.jpg")
                if (!imageFile.exists()) {
                    withContext(Dispatchers.Main) {
                        resultText = "test.jpg 파일이 존재하지 않습니다"
                    }
                    return@launch
                }

                val dqtResult = dqtExtractor.extractDQT(imageFile.absolutePath)
                val formattedResult = formatDQTResult(dqtResult)

                withContext(Dispatchers.Main) {
                    resultText = formattedResult
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    resultText = "DQT 추출 오류: ${e.message}"
                }
            }
        }
    }

    private fun formatDQTResult(result: DQTExtractResult): String {
        val builder = StringBuilder()
        builder.append("=== JPEG DQT 추출 결과 ===\n\n")
        builder.append("총 ${result.tables.size}개의 양자화 테이블 발견\n\n")

        result.tables.forEachIndexed { index, table ->
            builder.append("테이블 ID: ${table.tableId}\n")
            builder.append("정밀도: ${if (table.precision == 0) "8-bit" else "16-bit"}\n")
            builder.append("양자화 값:\n")

            // 8x8 매트릭스 형태로 출력
            for (row in 0 until 8) {
                for (col in 0 until 8) {
                    val value = table.values[row * 8 + col]
                    builder.append("%4d".format(value))
                    if (col < 7) builder.append(" ")
                }
                builder.append("\n")
            }
            builder.append("\n")
        }

        return builder.toString()
    }


}
