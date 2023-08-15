resource "aws_athena_workgroup" "vulnerable_example" {
    name = "dummy"
    # encryption_configuration is not set
}