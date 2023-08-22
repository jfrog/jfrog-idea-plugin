resource "aws_cloudfront_distribution" "vulnerable_example" {
  default_cache_behavior {
    allowed_methods  = ["dummy"]
    cached_methods  = ["dummy"]
    target_origin_id = "dummy"
    viewer_protocol_policy = "https-only"
  }

  viewer_certificate {
    minimum_protocol_version  = "TLSv1"
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
}