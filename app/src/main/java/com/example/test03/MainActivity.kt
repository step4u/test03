package com.example.test03

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipFile

class MainActivity : ComponentActivity() {
    private var resultText by mutableStateOf("")

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
                Button(
                    onClick = { runExifTool() },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text("ExifTool 실행")
                }

                TextField(
                    value = resultText,
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    label = { Text("ExifTool 결과") }
                )
            }
        }
    }

    private fun installRequiredFiles() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Perl 설치
                extractZipFromAssets("perl5-5.41.8_armv7a_precompiled.zip", "${filesDir}/perl")
                // ExifTool 설치
                extractZipFromAssets("exiftool_files.zip", "${filesDir}/exiftool")

                // Perl 바이너리에 실행 권한 부여
                setPerlExecutable()

                copyTestImage()

                withContext(Dispatchers.Main) {
                    resultText = "설치 완료"
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
                        "${filesDir}/exiftool/exiftool.pl",
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


}
