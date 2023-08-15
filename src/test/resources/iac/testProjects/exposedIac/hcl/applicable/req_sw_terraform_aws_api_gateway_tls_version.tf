resource "aws_api_gateway_domain_name" "vulnerable_example" {
  domain_name = "dummy"
  # security_policy is not set
}