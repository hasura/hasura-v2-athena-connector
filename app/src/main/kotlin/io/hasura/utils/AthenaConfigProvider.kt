package io.hasura.utils

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty

@ApplicationScoped
class AthenaConfigProvider {

    @ConfigProperty(name = "athena.aws.region", defaultValue = "us-east-1")
    lateinit var awsRegion: String

    @ConfigProperty(name = "athena.aws.accesskey")
    lateinit var awsAccessKey: String

    @ConfigProperty(name = "athena.aws.secretkey")
    lateinit var awsSecretKey: String

    @ConfigProperty(name = "athena.aws.s3.output.bucket")
    lateinit var awsS3OutputBucket: String

    fun getJdbcUrl(): String {
        return "jdbc:awsathena://AwsRegion=$awsRegion;User=$awsAccessKey;Password=$awsSecretKey;S3OutputLocation=$awsS3OutputBucket;"
    }
}
