package com.leilao.backend.shared.storage

import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.SetBucketPolicyArgs
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Primary
@Service
class MinIOStorageService(
    @Value("\${app.storage.minio.endpoint}") private val endpoint: String,
    @Value("\${app.storage.minio.access-key}") private val accessKey: String,
    @Value("\${app.storage.minio.secret-key}") private val secretKey: String,
    @Value("\${app.storage.minio.bucket}") private val bucket: String,
    @Value("\${app.storage.minio.public-url}") private val publicUrl: String,
) : StorageService {

    private val client: MinioClient by lazy {
        MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build()
    }

    @PostConstruct
    fun init() {
        val exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
            val policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [{
                    "Effect": "Allow",
                    "Principal": {"AWS": ["*"]},
                    "Action": ["s3:GetObject"],
                    "Resource": ["arn:aws:s3:::$bucket/*"]
                  }]
                }
            """.trimIndent()
            client.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build())
        }
    }

    override fun store(file: MultipartFile, fileKey: String) {
        client.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .`object`(fileKey)
                .stream(file.inputStream, file.size, -1)
                .contentType(file.contentType ?: "application/octet-stream")
                .build()
        )
    }

    override fun delete(fileKey: String) {
        client.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucket)
                .`object`(fileKey)
                .build()
        )
    }

    override fun toUrl(fileKey: String): String = "$publicUrl/$bucket/$fileKey"
}
