resource "aws_cloudfront_distribution" "vulnerable_example" {
  default_cache_behavior {
    viewer_protocol_policy = "allow-all"

    target_origin_id = "dummy"
    allowed_methods  = ["dummy"]
    cached_methods  = ["dummy"]
  }

  enabled             = true
  origin {
    origin_id = "dummy"
    domain_name = "dummy"
  }
  restrictions {
    geo_restriction {
        restriction_type = "none"
    }
  }
  viewer_certificate {
  }
}