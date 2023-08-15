resource "aws_api_gateway_method" "vulnerable_method" {
  rest_api_id   = "dummy"
  resource_id   = "dummy"
  http_method   = "GET"
  authorization = "NONE"
}