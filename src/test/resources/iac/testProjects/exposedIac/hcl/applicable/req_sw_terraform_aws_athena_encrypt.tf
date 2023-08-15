resource "aws_athena_database" "vulnerable_example" {
    name = "dummy"
    # encryption_configuration is not set
}