resource "aws_api_gateway_method_settings" "vulnerable_example" {
  rest_api_id = "dummy"
  method_path = "dummy"
  stage_name = "dummy"
  settings {
    caching_enabled = true
  }
}