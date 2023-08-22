resource "aws_cloudtrail" "vulnerable_example" {
  s3_bucket_name = "dummy"
  name = "dummy"
  # kms_key_id is not set
}